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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.transformation.ConfigurationTransformation;
import ninja.leaping.configurate.util.MapFactories;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import ninja.leaping.permissionsex.backend.AbstractDataStore;
import ninja.leaping.permissionsex.backend.ConversionUtils;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.backend.memory.MemoryContextInheritance;
import ninja.leaping.permissionsex.data.ContextInheritance;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.rank.FixedRankLadder;
import ninja.leaping.permissionsex.rank.RankLadder;
import ninja.leaping.permissionsex.util.GuavaCollectors;
import ninja.leaping.permissionsex.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static ninja.leaping.permissionsex.util.Translations.t;

public final class FileDataStore extends AbstractDataStore {
    public static final String KEY_RANK_LADDERS = "rank-ladders";
    public static final Factory FACTORY = new Factory("file", FileDataStore.class);

    @Setting
    private String file;
    @Setting(value = "alphabetize-entries", comment = "Place file entries in alphabetical order")
    private boolean alphabetizeEntries = false;

    private ConfigurationLoader permissionsFileLoader;
    private ConfigurationNode permissionsConfig;
    private final AtomicInteger saveSuppressed = new AtomicInteger();
    private final AtomicBoolean dirty = new AtomicBoolean();

    public FileDataStore() {
        super(FACTORY);
    }


    private ConfigurationLoader<? extends ConfigurationNode> createLoader(Path file) {
        return GsonConfigurationLoader.builder()
                .setPath(file)
                .setIndent(4)
                .setLenient(true)
                .build();
    }

    private ConfigurationOptions getLoadOptions() {
        ConfigurationOptions ret = ConfigurationOptions.defaults();
        if (alphabetizeEntries) {
            ret = ret.setMapFactory(MapFactories.sortedNatural());
        }
        ret = ret.setSerializers(ret.getSerializers().newChild().registerType(TypeToken.of(SubjectRef.class), new SubjectRefTypeSerializer()));
        return ret;
    }

    private Path migrateLegacy(Path permissionsFile, String extension, ConfigurationLoader<?> loader, String formatName) throws PermissionsLoadingException {
        Path legacyPermissionsFile = permissionsFile;
        file = file.replace(extension, ".json");
        permissionsFile = getManager().getBaseDirectory().resolve(file);
        permissionsFileLoader = createLoader(permissionsFile);
        try {
            permissionsConfig = loader.load();
            permissionsFileLoader.save(permissionsConfig);
            Files.move(legacyPermissionsFile, legacyPermissionsFile.resolveSibling(legacyPermissionsFile.getFileName().toString() + ".legacy-backup"));
        } catch (IOException e) {
            throw new PermissionsLoadingException(t("While loading legacy %s permissions from %s", formatName, legacyPermissionsFile), e);
        }
        return permissionsFile;
    }

    @Override
    protected void initializeInternal() throws PermissionsLoadingException {
        Path permissionsFile = getManager().getBaseDirectory().resolve(file);
        if (file.endsWith(".yml")) {
            permissionsFile = migrateLegacy(permissionsFile, ".yml", YAMLConfigurationLoader.builder().setPath(permissionsFile).build(), "YML");
        } else if (file.endsWith(".conf")) {
            permissionsFile = migrateLegacy(permissionsFile, ".conf", HoconConfigurationLoader.builder().setPath(permissionsFile).build(), "HOCON");
        } else {
            permissionsFileLoader = createLoader(permissionsFile);
        }

        try {
            permissionsConfig = permissionsFileLoader.load(getLoadOptions());
        } catch (IOException e) {
            throw new PermissionsLoadingException(t("While loading permissions file from %s", permissionsFile), e);
        }

        if (permissionsConfig.getChildrenMap().isEmpty()) { // New configuration, populate with default data
            try {
                performBulkOperationSync(input -> {
                        applyDefaultData();
                        permissionsConfig.getNode("schema-version").setValue(SchemaMigrations.LATEST_VERSION);
                        return null;
                });
            } catch (PermissionsLoadingException e) {
                throw e;
            } catch (Exception e) {
                throw new PermissionsLoadingException(t("Error creating initial data for file backend"), e);
            }
        } else {
            ConfigurationTransformation versionUpdater = SchemaMigrations.versionedMigration(getManager().getLogger());
            int startVersion = permissionsConfig.getNode("schema-version").getInt(-1);
            versionUpdater.apply(permissionsConfig);
            int endVersion = permissionsConfig.getNode("schema-version").getInt();
            if (endVersion > startVersion) {
                getManager().getLogger().info(t("%s schema version updated from %s to %s", permissionsFile, startVersion, endVersion));
                try {
                    save().get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new PermissionsLoadingException(t("While performing version upgrade"), e);
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

    private CompletableFuture<Void> save() {
        if (saveSuppressed.get() <= 0) {
            return Util.asyncFailableFuture(() -> {
                saveSync();
                return null;
            }, getManager().getAsyncExecutor());
        } else {
            return completedFuture(null);
        }
    }

    private void saveSync() throws IOException {
        if (saveSuppressed.get() <= 0) {
            if (dirty.compareAndSet(true, false)) {
                permissionsFileLoader.save(permissionsConfig);
            }
        }
    }

    @Override
    public CompletableFuture<ImmutableSubjectData> getDataInternal(String type, String identifier) {
        try {
            return completedFuture(FileSubjectData.fromNode(getSubjectsNode().getNode(type, identifier)));
        } catch (PermissionsLoadingException e) {
            return Util.failedFuture(e);
        } catch (ObjectMappingException e) {
            return Util.failedFuture(new PermissionsLoadingException(t("While deserializing subject data for %s:", identifier), e));
        }
    }

    @Override
    protected CompletableFuture<ImmutableSubjectData> setDataInternal(String type, String identifier, final ImmutableSubjectData data) {
        try {
            if (data == null) {
                getSubjectsNode().getNode(type, identifier).setValue(null);
                dirty.set(true);
                return save().thenApply(input -> null);
            }

            final FileSubjectData fileData;

            if (data instanceof FileSubjectData) {
                fileData = (FileSubjectData) data;
            } else {
                fileData = ConversionUtils.transfer(data, new FileSubjectData());
            }
            fileData.serialize(getSubjectsNode().getNode(type, identifier));
            dirty.set(true);
            return save().thenApply(none -> fileData);
        } catch (ObjectMappingException e) {
            return Util.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Boolean> isRegistered(String type, String identifier) {
        return completedFuture(!getSubjectsNode().getNode(type, identifier).isVirtual());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getAllIdentifiers(String type) {
        return (Set) getSubjectsNode().getNode(type).getChildrenMap().keySet();
    }

    @Override
    public Set<String> getRegisteredTypes() {
        return getSubjectsNode().getChildrenMap().entrySet().stream()
                .filter(ent -> ent.getValue().hasMapChildren())
                .map(Map.Entry::getKey)
                .map(Object::toString)
                .distinct()
                .collect(GuavaCollectors.toImmutableSet());
    }

    @Override
    public Iterable<Map.Entry<Map.Entry<String, String>, ImmutableSubjectData>> getAll() {
        /*return getSubjectsNode().getChildrenMap().keySet().stream()
                .filter(i -> i != null)
                .flatMap(type -> {
                    final String typeStr = type.toString();
                    return getAll(typeStr)
                            .stream()
                            .map(ent -> Maps.immutableEntry(Maps.immutableEntry(typeStr, ent.getKey()), ent.getValue()));
                })
                .collect(GuavaCollectors.toImmutableSet());*/
        return Iterables.concat(Iterables.transform(getSubjectsNode().getChildrenMap().keySet(), type -> {
                if (type == null) {
                    return null;
                }
                final String typeStr = type.toString();

                return Iterables.transform(getAll(typeStr), input2 -> Maps.immutableEntry(Maps.immutableEntry(type.toString(), input2.getKey()), input2.getValue()));
        }));
    }

    private ConfigurationNode getRankLaddersNode() {
        return this.permissionsConfig.getNode(KEY_RANK_LADDERS);
    }

    @Override
    public Iterable<String> getAllRankLadders() {
        return Iterables.unmodifiableIterable(Iterables.transform(getRankLaddersNode().getChildrenMap().keySet(), Object::toString));
    }

    @Override
    public CompletableFuture<RankLadder> getRankLadderInternal(String ladder) {
        try {
            return completedFuture(new FixedRankLadder(ladder, ImmutableList.copyOf(getRankLaddersNode().getNode(ladder.toLowerCase()).getList(SUBJECT_REF_TYPE))));
        } catch (ObjectMappingException e) {
            return Util.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Boolean> hasRankLadder(String ladder) {
        return completedFuture(!getRankLaddersNode().getNode(ladder.toLowerCase()).isVirtual());
    }

    @Override
    public CompletableFuture<ContextInheritance> getContextInheritanceInternal() {
        try {
            return completedFuture(this.permissionsConfig.getValue(TypeToken.of(MemoryContextInheritance.class)));
        } catch (ObjectMappingException e) {
            return Util.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<ContextInheritance> setContextInheritanceInternal(final ContextInheritance inheritance) {
        final MemoryContextInheritance realInheritance = MemoryContextInheritance.fromExistingContextInheritance(inheritance);
        try {
            this.permissionsConfig.setValue(TypeToken.of(MemoryContextInheritance.class), realInheritance);
        } catch (ObjectMappingException e) {
            throw new RuntimeException(e);
        }
        dirty.set(true);
        return save().thenApply(none -> realInheritance);
    }

    private static final TypeToken<SubjectRef> SUBJECT_REF_TYPE = TypeToken.of(SubjectRef.class);
    @Override
    public CompletableFuture<RankLadder> setRankLadderInternal(String identifier, RankLadder ladder) {
        ConfigurationNode childNode = getRankLaddersNode().getNode(identifier.toLowerCase());
        childNode.setValue(null);
        if (ladder != null) {
            for (SubjectRef rank : ladder.getRanks()) {
                try {
                    childNode.getAppendedNode().setValue(SUBJECT_REF_TYPE, rank);
                } catch (ObjectMappingException e) {
                    return Util.failedFuture(e);
                }

            }
        }
        dirty.set(true);
        return save().thenApply(none -> ladder);
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
