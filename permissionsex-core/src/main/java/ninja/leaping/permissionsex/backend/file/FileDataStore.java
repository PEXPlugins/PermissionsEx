/**
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ninja.leaping.permissionsex.backend.file;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.json.FieldValueSeparatorStyle;
import ninja.leaping.configurate.json.JSONConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.transformation.ConfigurationTransformation;
import ninja.leaping.configurate.transformation.TransformAction;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import ninja.leaping.permissionsex.backend.AbstractDataStore;
import ninja.leaping.permissionsex.backend.ConversionUtils;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.rank.FixedRankLadder;
import ninja.leaping.permissionsex.rank.RankLadder;
import ninja.leaping.permissionsex.util.Util;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static ninja.leaping.permissionsex.util.Translations._;


public final class FileDataStore extends AbstractDataStore {
    public static final String KEY_RANK_LADDERS = "rank-ladders";
    public static final Factory FACTORY = new Factory("file", FileDataStore.class);

    @Setting
    private String file;
    @Setting
    private boolean compat = false;

    private ConfigurationLoader permissionsFileLoader;
    private ConfigurationNode permissionsConfig;
    private final AtomicInteger saveSuppressed = new AtomicInteger();

    public FileDataStore() {
        super(FACTORY);
    }


    private ConfigurationLoader<? extends ConfigurationNode> createLoader(File file) {
        JSONConfigurationLoader.Builder build = JSONConfigurationLoader.builder()
                .setFile(file)
                .setIndent(4)
                .setFieldValueSeparatorStyle(FieldValueSeparatorStyle.SPACE_AFTER);

        /*if (!compat) {
            build.getFactory().disable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);
            build.getFactory().enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        }*/
        return build.build();
    }

    private File migrateLegacy(File permissionsFile, String extension, ConfigurationLoader<?> loader, String formatName) throws PermissionsLoadingException {
        File legacyPermissionsFile = permissionsFile;
        file = file.replace(extension, ".json");
        permissionsFile = new File(getManager().getBaseDirectory(), file);
        permissionsFileLoader = createLoader(permissionsFile);
        try {
            permissionsConfig = loader.load();
            permissionsFileLoader.save(permissionsConfig);
            legacyPermissionsFile.renameTo(new File(legacyPermissionsFile.getCanonicalPath() + ".legacy-backup"));
        } catch (IOException e) {
            throw new PermissionsLoadingException(_("While loading legacy %s permissions from %s", formatName, permissionsFile), e);
        }
        return permissionsFile;
    }

    @Override
    protected void initializeInternal() throws PermissionsLoadingException {
        File permissionsFile = new File(getManager().getBaseDirectory(), file);
        if (file.endsWith(".yml")) {
            permissionsFile = migrateLegacy(permissionsFile, ".yml", YAMLConfigurationLoader.builder().setFile(permissionsFile).build(), "YML");
        } else if (file.endsWith(".conf")) {
            permissionsFile = migrateLegacy(permissionsFile, ".conf", HoconConfigurationLoader.builder().setFile(permissionsFile).build(), "HOCON");
        } else {
            permissionsFileLoader = createLoader(permissionsFile);
        }

        try {
            permissionsConfig = permissionsFileLoader.load(ConfigurationOptions.defaults());//.setMapFactory(MapFactories.unordered()));
        } catch (IOException e) {
            throw new PermissionsLoadingException(_("While loading permissions file from %s", permissionsFile), e);
        }

        if (permissionsConfig.getChildrenMap().isEmpty()) { // New configuration, populate with default data
            try {
                performBulkOperationSync(new Function<DataStore, Void>() {
                    @Nullable
                    @Override
                    public Void apply(@Nullable DataStore input) {
                        applyDefaultData();
                        permissionsConfig.getNode("schema-version").setValue(2);
                        return null;
                    }
                });
            } catch (PermissionsLoadingException e) {
                throw e;
            } catch (Exception e) {
                throw new PermissionsLoadingException(_("Error creating initial data for file backend"), e);
            }
        } else {

            ConfigurationTransformation versionUpdater = SchemaMigrations.versionedMigration(getManager().getLogger());
            int startVersion = permissionsConfig.getNode("schema-version").getInt(-1);
            versionUpdater.apply(permissionsConfig);
            int endVersion = permissionsConfig.getNode("schema-version").getInt();
            if (endVersion > startVersion) {
                getManager().getLogger().info(_("%s schema version updated from %s to %s", permissionsFile, startVersion, endVersion).translateFormatted(Locale.getDefault()));
                try {
                    save().get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new PermissionsLoadingException(_("While performing version upgrade"), e);
                }
            }
        }
    }

    @Override
    public void close() {

    }

    private ConfigurationNode getSubjectsNode() {
        return this.permissionsConfig.getNode("subjects");
    }

    private ListenableFuture<Void> save() {
        if (saveSuppressed.get() <= 0) {
            final ListenableFutureTask<Void> ret = ListenableFutureTask.create(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    saveSync();
                    return null;
                }
            });
            getManager().executeAsyncronously(ret);
            return ret;
        } else {
            return Futures.immediateFuture(null);
        }
    }

    private void saveSync() throws IOException {
        if (saveSuppressed.get() <= 0) {
            permissionsFileLoader.save(permissionsConfig);
        }
    }

    @Override
    public ImmutableOptionSubjectData getDataInternal(String type, String identifier) throws PermissionsLoadingException {
        try {
            return FileOptionSubjectData.fromNode(getSubjectsNode().getNode(type, identifier));
        } catch (ObjectMappingException e) {
            throw new PermissionsLoadingException(_("While deserializing subject data for %s:", identifier), e);
        }
    }

    @Override
    protected ListenableFuture<ImmutableOptionSubjectData> setDataInternal(String type, String identifier, final ImmutableOptionSubjectData data) {
        try {
            if (data == null) {
                getSubjectsNode().getNode(type, identifier).setValue(null);
                return Futures.transform(save(), Functions.<ImmutableOptionSubjectData>constant(null));
            }

            final FileOptionSubjectData fileData;

            if (data instanceof FileOptionSubjectData) {
                fileData = (FileOptionSubjectData) data;
            } else {
                fileData = new FileOptionSubjectData();
                ConversionUtils.transfer(data, fileData);
            }
            fileData.serialize(getSubjectsNode().getNode(type, identifier));
            return Futures.transform(save(), new Function<Void, ImmutableOptionSubjectData>() {
                @Nullable
                @Override
                public ImmutableOptionSubjectData apply(Void input) {
                    return fileData;
                }
            });
        } catch (ObjectMappingException e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public boolean isRegistered(String type, String identifier) {
        return !getSubjectsNode().getNode(type, identifier).isVirtual();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterable<String> getAllIdentifiers(String type) {
        return (Set) getSubjectsNode().getNode(type).getChildrenMap().keySet();
    }

    @Override
    public Set<String> getRegisteredTypes() {
        return ImmutableSet.copyOf(Iterables.transform(Maps.filterValues(getSubjectsNode().getChildrenMap(), new Predicate<ConfigurationNode>() {
            @Override
            public boolean apply(@Nullable ConfigurationNode input) {
                return input != null && input.hasMapChildren();
            }
        }).keySet(), Functions.toStringFunction()));
    }

    @Override
    public Iterable<Map.Entry<Map.Entry<String, String>, ImmutableOptionSubjectData>> getAll() {
        return Iterables.concat(Iterables.transform(getSubjectsNode().getChildrenMap().keySet(), new Function<Object, Iterable<Map.Entry<Map.Entry<String, String>, ImmutableOptionSubjectData>>>() {
            @Nullable
            @Override
            public Iterable<Map.Entry<Map.Entry<String, String>, ImmutableOptionSubjectData>> apply(@Nullable final Object type) {
                if (type == null) {
                    return null;
                }

                final String typeStr = type.toString();

                return Iterables.transform(getAll(typeStr), new Function<Map.Entry<String, ImmutableOptionSubjectData>, Map.Entry<Map.Entry<String, String>, ImmutableOptionSubjectData>>() {
                    @Nullable
                    @Override
                    public Map.Entry<Map.Entry<String, String>, ImmutableOptionSubjectData> apply(Map.Entry<String, ImmutableOptionSubjectData> input2) {
                        return Maps.immutableEntry(Maps.immutableEntry(type.toString(), input2.getKey()), input2.getValue());
                    }
                });
            }
        }));
    }

    private ConfigurationNode getRankLaddersNode() {
        return this.permissionsConfig.getNode(KEY_RANK_LADDERS);
    }

    @Override
    public Iterable<String> getAllRankLadders() {
        return Iterables.unmodifiableIterable(Iterables.transform(getRankLaddersNode().getChildrenMap().keySet(),Functions.toStringFunction()));
    }

    @Override
    public RankLadder getRankLadderInternal(String ladder) {
        return new FixedRankLadder(ladder, ImmutableList.copyOf(Lists.transform(getRankLaddersNode().getNode(ladder.toLowerCase()).getChildrenList(), new Function<ConfigurationNode, Map.Entry<String, String>>() {
            @Nullable
            @Override
            public Map.Entry<String, String> apply(ConfigurationNode input) {
                return Util.subjectFromString(input.getString());
            }
        })));
    }

    @Override
    public boolean hasRankLadder(String ladder) {
        return !getRankLaddersNode().getNode(ladder.toLowerCase()).isVirtual();
    }

    @Override
    public ListenableFuture<RankLadder> setRankLadderInternal(String identifier, RankLadder ladder) {
        ConfigurationNode childNode = getRankLaddersNode().getNode(identifier.toLowerCase());
        childNode.setValue(null);
        for (Map.Entry<String, String> rank : ladder.getRanks()) {
            childNode.getAppendedNode().setValue(Util.subjectToString(rank));

        }
        return Futures.transform(save(), Functions.constant(ladder));
    }

    @Override
    protected <T> T performBulkOperationSync(Function<DataStore, T> function) throws Exception {
        saveSuppressed.getAndIncrement();
        T ret;
        try {
            ret = function.apply(this);
        } finally {
            saveSuppressed.getAndDecrement();
        }
        saveSync();
        return ret;
    }
}
