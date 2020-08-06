/*
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

package ca.stellardrift.permissionsex.backend.file;

import ca.stellardrift.permissionsex.backend.AbstractDataStore;
import ca.stellardrift.permissionsex.backend.ConversionUtils;
import ca.stellardrift.permissionsex.backend.DataStore;
import ca.stellardrift.permissionsex.backend.memory.MemoryContextInheritance;
import ca.stellardrift.permissionsex.data.ContextInheritance;
import ca.stellardrift.permissionsex.data.ImmutableSubjectData;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.rank.FixedRankLadder;
import ca.stellardrift.permissionsex.rank.RankLadder;
import ca.stellardrift.permissionsex.util.GuavaCollectors;
import ca.stellardrift.permissionsex.util.Util;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.reference.ConfigurationReference;
import ninja.leaping.configurate.reference.WatchServiceListener;
import ninja.leaping.configurate.transformation.ConfigurationTransformation;
import ninja.leaping.configurate.util.MapFactories;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ca.stellardrift.permissionsex.backend.Messages.*;
import static java.util.concurrent.CompletableFuture.completedFuture;

public final class FileDataStore extends AbstractDataStore<FileDataStore> {
    static final String KEY_RANK_LADDERS = "rank-ladders";
    public static final Factory<FileDataStore> FACTORY = new Factory<>("file", FileDataStore.class, FileDataStore::new);

    @Setting
    private String file;
    @Setting(value = "alphabetize-entries", comment = "Place file entries in alphabetical order")
    private boolean alphabetizeEntries = false;
    @Setting(value = "auto-reload", comment = "Automatically reload the data file when changes have been made")
    private boolean autoReload = true;

    private WatchServiceListener reloadService;
    private ConfigurationReference<ConfigurationNode> permissionsConfig;
    private final AtomicInteger saveSuppressed = new AtomicInteger();
    private final AtomicBoolean dirty = new AtomicBoolean();

    public FileDataStore(String identifier) {
        super(identifier, FACTORY);
    }


    private ConfigurationReference<ConfigurationNode> createLoader(Path file) throws IOException {
        Function<Path, ConfigurationLoader<? extends ConfigurationNode>> loaderFunc = path -> GsonConfigurationLoader.builder()
                .setDefaultOptions(o -> {
                    if (alphabetizeEntries) {
                        return o.withMapFactory(MapFactories.sortedNatural());
                    } else {
                        return o;
                    }
                })
                .setPath(path)
                .setIndent(4)
                .setLenient(true)
                .build();

        ConfigurationReference<ConfigurationNode> ret;
        if (reloadService != null) {
            ret = reloadService.listenToConfiguration(loaderFunc, file);
        } else {
            ret = ConfigurationReference.createFixed(loaderFunc.apply(file));
        }

        ret.updates().subscribe(this::refresh);

        ret.errors().subscribe(e -> {
            getManager().getLogger().error(FILE_ERROR_AUTORELOAD.toComponent(e.getKey(), e.getValue().getLocalizedMessage()));
        });

        return ret;
    }

    /**
     * Handle automatic reloads of the permissions storage
     *
     * @param newNode The updated node
     */
    private void refresh(ConfigurationNode newNode) {
        this.listeners.getAllKeys().forEach(key -> {
            try {
                this.listeners.call(key, getDataSync(key.getKey(), key.getValue()));
            } catch (PermissionsLoadingException e) {
                getManager().getLogger().error(FILE_ERROR_SUBJECT_AUTORELOAD.toComponent(key.getKey(), key.getValue()));
            }
        });

        this.rankLadderListeners.getAllKeys().forEach(key ->
                this.rankLadderListeners.call(key, getRankLadderInternal(key).join()));

        this.contextInheritanceListeners.getAllKeys().forEach(key ->
                this.contextInheritanceListeners.call(key, getContextInheritanceInternal().join()));

        getManager().getLogger().info(FILE_RELOAD_AUTO.toComponent(this.file));
    }

    private Path migrateLegacy(Path permissionsFile, String extension, ConfigurationLoader<?> legacyLoader, String formatName) throws PermissionsLoadingException {
        Path legacyPermissionsFile = permissionsFile;
        file = file.replace(extension, ".json");
        permissionsFile = getManager().getBaseDirectory().resolve(file);
        try {
            permissionsConfig = createLoader(permissionsFile);
            permissionsConfig.save(legacyLoader.load());
            Files.move(legacyPermissionsFile, legacyPermissionsFile.resolveSibling(legacyPermissionsFile.getFileName().toString() + ".legacy-backup"));
        } catch (IOException e) {
            throw new PermissionsLoadingException(FILE_ERROR_LEGACY_MIGRATION.toComponent(formatName, legacyPermissionsFile), e);
        }
        return permissionsFile;
    }

    @Override
    protected boolean initializeInternal() throws PermissionsLoadingException {
        if (autoReload) {
            try {
                reloadService = WatchServiceListener.builder().setTaskExecutor(getManager().getAsyncExecutor()).build();
            } catch (IOException e) {
                throw new PermissionsLoadingException(e);
            }
        }

        Path permissionsFile = getManager().getBaseDirectory().resolve(file);
        if (file.endsWith(".yml")) {
            permissionsFile = migrateLegacy(permissionsFile, ".yml", YAMLConfigurationLoader.builder().setPath(permissionsFile).build(), "YML");
        } else if (file.endsWith(".conf")) {
            permissionsFile = migrateLegacy(permissionsFile, ".conf", HoconConfigurationLoader.builder().setPath(permissionsFile).build(), "HOCON");
        } else {
            try {
                permissionsConfig = createLoader(permissionsFile);
            } catch (IOException e) {
                throw new PermissionsLoadingException(FILE_ERROR_LOAD.toComponent(permissionsFile), e);
            }
        }

        if (permissionsConfig.getNode().getChildrenMap().isEmpty()) { // New configuration, populate with default data
            try {
                performBulkOperationSync(input -> {
                    applyDefaultData();
                    permissionsConfig.get("schema-version").setValue(SchemaMigrations.LATEST_VERSION);
                    return null;
                });
            } catch (PermissionsLoadingException e) {
                throw e;
            } catch (Exception e) {
                throw new PermissionsLoadingException(FILE_ERROR_INITIAL_DATA.toComponent(), e);
            }
            return false;
        } else {
            ConfigurationTransformation versionUpdater = SchemaMigrations.versionedMigration(getManager().getLogger());
            int startVersion = permissionsConfig.get("schema-version").getInt(-1);
            ConfigurationNode node = permissionsConfig.getNode();
            versionUpdater.apply(node);
            int endVersion = permissionsConfig.get("schema-version").getInt();
            if (endVersion > startVersion) {
                getManager().getLogger().info(FILE_SCHEMA_MIGRATION_SUCCESS.toComponent(permissionsFile, startVersion, endVersion));
                try {
                    permissionsConfig.save(node);
                } catch (IOException e) {
                    throw new PermissionsLoadingException(FILE_ERROR_SCHEMA_MIGRATION_SAVE.toComponent(), e);
                }
            }
            return true;
        }
    }

    @Override
    public void close() {
        if (this.reloadService != null) {
            try {
                this.reloadService.close();
            } catch (IOException e) {
                getManager().getLogger().error("Unable to shut down FileDataStore watch service", e);
            }
            this.reloadService = null;
        }
    }

    private ConfigurationNode getSubjectsNode() {
        return this.permissionsConfig.get("subjects");
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
                permissionsConfig.save();
            }
        }
    }

    @Override
    public CompletableFuture<ImmutableSubjectData> getDataInternal(String type, String identifier) {
        try {
            return completedFuture(getDataSync(type, identifier));
        } catch (PermissionsLoadingException e) {
            return Util.failedFuture(e);
        }
    }

    private ImmutableSubjectData getDataSync(String type, String identifier) throws PermissionsLoadingException {
        try {
            return FileSubjectData.fromNode(getSubjectsNode().getNode(type, identifier));
        } catch (ObjectMappingException e) {
            throw new PermissionsLoadingException(FILE_ERROR_DESERIALIZE_SUBJECT.toComponent(), e);
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
                .filter(ent -> ent.getValue().isMap())
                .map(Map.Entry::getKey)
                .map(Object::toString)
                .distinct()
                .collect(GuavaCollectors.toImmutableSet());
    }

    @Override
    public CompletableFuture<Set<String>> getDefinedContextKeys() {
        return CompletableFuture.completedFuture(getSubjectsNode().getChildrenMap().values().stream() // list of types
                .flatMap(typeNode -> typeNode.getChildrenMap().values().stream()) // list of subjects
                .flatMap(subjectNode -> subjectNode.getChildrenList().stream()) // list of segments
                .flatMap(segmentNode -> segmentNode.getNode(FileSubjectData.KEY_CONTEXTS).getChildrenMap().entrySet().stream()) // list of contexts
                .map(ctx -> ctx.getKey().toString()) // of context objets
                .collect(Collectors.toSet())); // to a list
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
        return this.permissionsConfig.get(KEY_RANK_LADDERS);
    }

    @Override
    public Iterable<String> getAllRankLadders() {
        return Iterables.unmodifiableIterable(Iterables.transform(getRankLaddersNode().getChildrenMap().keySet(), Object::toString));
    }

    @Override
    public CompletableFuture<RankLadder> getRankLadderInternal(String ladder) {
        return completedFuture(new FixedRankLadder(ladder, getRankLaddersNode().getNode(ladder.toLowerCase()).getChildrenList().stream()
                .map(node -> Util.subjectFromString(Objects.requireNonNull(node.getString())))
                .collect(Collectors.toList())));
    }

    @Override
    public CompletableFuture<Boolean> hasRankLadder(String ladder) {
        return completedFuture(!getRankLaddersNode().getNode(ladder.toLowerCase()).isVirtual());
    }

    @Override
    public CompletableFuture<ContextInheritance> getContextInheritanceInternal() {
        try {
            return completedFuture(this.permissionsConfig.getNode().getValue(TypeToken.of(MemoryContextInheritance.class)));
        } catch (ObjectMappingException e) {
            return Util.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<ContextInheritance> setContextInheritanceInternal(final ContextInheritance inheritance) {
        final MemoryContextInheritance realInheritance = MemoryContextInheritance.fromExistingContextInheritance(inheritance);
        try {
            this.permissionsConfig.getNode().setValue(TypeToken.of(MemoryContextInheritance.class), realInheritance);
        } catch (ObjectMappingException e) {
            throw new RuntimeException(e);
        }
        dirty.set(true);
        return save().thenApply(none -> realInheritance);
    }

    @Override
    public CompletableFuture<RankLadder> setRankLadderInternal(String identifier, RankLadder ladder) {
        ConfigurationNode childNode = getRankLaddersNode().getNode(identifier.toLowerCase());
        childNode.setValue(null);
        if (ladder != null) {
            for (Map.Entry<String, String> rank : ladder.getRanks()) {
                childNode.appendListNode().setValue(Util.subjectToString(rank));
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
