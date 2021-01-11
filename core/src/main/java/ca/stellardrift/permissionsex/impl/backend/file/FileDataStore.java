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
package ca.stellardrift.permissionsex.impl.backend.file;

import ca.stellardrift.permissionsex.datastore.DataStoreContext;
import ca.stellardrift.permissionsex.datastore.ProtoDataStore;
import ca.stellardrift.permissionsex.impl.backend.AbstractDataStore;
import ca.stellardrift.permissionsex.datastore.DataStore;
import ca.stellardrift.permissionsex.datastore.DataStoreFactory;
import ca.stellardrift.permissionsex.impl.backend.memory.MemoryContextInheritance;
import ca.stellardrift.permissionsex.context.ContextInheritance;
import ca.stellardrift.permissionsex.impl.config.FilePermissionsExConfiguration;
import ca.stellardrift.permissionsex.impl.config.SubjectRefSerializer;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.impl.rank.FixedRankLadder;
import ca.stellardrift.permissionsex.rank.RankLadder;
import ca.stellardrift.permissionsex.impl.util.Util;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import com.google.auto.service.AutoService;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.reference.ConfigurationReference;
import org.spongepowered.configurate.reference.WatchServiceListener;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.util.MapFactories;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.spongepowered.configurate.util.UnmodifiableCollections.immutableMapEntry;

public final class FileDataStore extends AbstractDataStore<FileDataStore, FileDataStore.Config> {
    static final String KEY_RANK_LADDERS = "rank-ladders";

    @ConfigSerializable
    static class Config {
        @Setting
        String file;
        @Setting
        @Comment("Place file entries in alphabetical order")
        boolean alphabetizeEntries = false;
        @Setting
        @Comment("Automatically reload the data file when changes have been made")
        boolean autoReload = true;
    }


    private @Nullable WatchServiceListener reloadService;
    private @MonotonicNonNull ConfigurationReference<BasicConfigurationNode> permissionsConfig;
    private final AtomicInteger saveSuppressed = new AtomicInteger();
    private final AtomicBoolean dirty = new AtomicBoolean();

    public FileDataStore(final DataStoreContext context, final ProtoDataStore<Config> properties) {
        super(context, properties);
    }

    private ConfigurationReference<BasicConfigurationNode> createLoader(Path file) throws ConfigurateException {
        Function<Path, ConfigurationLoader<? extends BasicConfigurationNode>> loaderFunc = path -> GsonConfigurationLoader.builder()
                .defaultOptions(o -> {
                    o = o.serializers(s -> FilePermissionsExConfiguration.populateSerializers(s)
                            .register(SubjectRefSerializer.TYPE, new SubjectRefSerializer(this.context(), null)));
                    if (config().alphabetizeEntries) {
                        return o.mapFactory(MapFactories.sortedNatural());
                    } else {
                        return o;
                    }
                })
                .path(path)
                .indent(4)
                .lenient(true)
                .build();

        ConfigurationReference<BasicConfigurationNode> ret;
        if (this.reloadService != null) {
            ret = this.reloadService.listenToConfiguration(loaderFunc, file);
        } else {
            ret = ConfigurationReference.fixed(loaderFunc.apply(file));
        }

        ret.updates().subscribe(this::refresh);

        ret.errors().subscribe(e ->
                engine().logger().error(Messages.FILE_ERROR_AUTORELOAD.tr(e.getKey(), e.getValue().getLocalizedMessage())));

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
                engine().logger().error(Messages.FILE_ERROR_SUBJECT_AUTORELOAD.tr(key.getKey(), key.getValue()));
            }
        });

        this.rankLadderListeners.getAllKeys().forEach(key ->
                this.rankLadderListeners.call(key, getRankLadderInternal(key).join()));

        this.contextInheritanceListeners.getAllKeys().forEach(key ->
                this.contextInheritanceListeners.call(key, getContextInheritanceInternal().join()));

        engine().logger().info(Messages.FILE_RELOAD_AUTO.tr(config().file));
    }

    private Path migrateLegacy(Path permissionsFile, String extension, ConfigurationLoader<?> legacyLoader, String formatName) throws PermissionsLoadingException {
        Path legacyPermissionsFile = permissionsFile;
        config().file = config().file.replace(extension, ".json");
        permissionsFile = engine().baseDirectory().resolve(config().file);
        try {
            permissionsConfig = createLoader(permissionsFile);
            permissionsConfig.save(legacyLoader.load());
            Files.move(legacyPermissionsFile, legacyPermissionsFile.resolveSibling(legacyPermissionsFile.getFileName().toString() + ".legacy-backup"));
        } catch (final IOException e) {
            throw new PermissionsLoadingException(Messages.FILE_ERROR_LEGACY_MIGRATION.tr(formatName, legacyPermissionsFile), e);
        }
        return permissionsFile;
    }

    @Override
    protected void load() throws PermissionsLoadingException {
        if (config().autoReload) {
            try {
                reloadService = WatchServiceListener.builder()
                        .taskExecutor(engine().asyncExecutor())
                        .build();
            } catch (IOException e) {
                throw new PermissionsLoadingException(e);
            }
        }

        final String rawFile = config().file;
        Path permissionsFile = engine().baseDirectory().resolve(rawFile);
        if (rawFile.endsWith(".yml")) {
            permissionsFile = migrateLegacy(permissionsFile, ".yml", YamlConfigurationLoader.builder().path(permissionsFile).build(), "YML");
        } else if (rawFile.endsWith(".conf")) {
            permissionsFile = migrateLegacy(permissionsFile, ".conf", HoconConfigurationLoader.builder().path(permissionsFile).build(), "HOCON");
        } else {
            try {
                permissionsConfig = createLoader(permissionsFile);
            } catch (final ConfigurateException e) {
                throw new PermissionsLoadingException(Messages.FILE_ERROR_LOAD.tr(permissionsFile), e);
            }
        }

        if (permissionsConfig.node().childrenMap().isEmpty()) { // New configuration, populate with default data
            try {
                performBulkOperationSync(input -> {
                    applyDefaultData();
                    permissionsConfig.get("schema-version").raw(SchemaMigrations.LATEST_VERSION);
                    return null;
                });
            } catch (PermissionsLoadingException e) {
                throw e;
            } catch (Exception e) {
                throw new PermissionsLoadingException(Messages.FILE_ERROR_INITIAL_DATA.tr(), e);
            }
            this.markFirstRun();
        } else {
            try {
                ConfigurationTransformation versionUpdater = SchemaMigrations.versionedMigration(engine().logger());
                int startVersion = permissionsConfig.get("schema-version").getInt(-1);
                ConfigurationNode node = permissionsConfig.node();
                versionUpdater.apply(node);
                int endVersion = permissionsConfig.get("schema-version").getInt();
                if (endVersion > startVersion) {
                    engine().logger().info(Messages.FILE_SCHEMA_MIGRATION_SUCCESS.tr(permissionsFile, startVersion, endVersion));
                    permissionsConfig.save(node);
                }
            } catch (final ConfigurateException ex) {
                throw new PermissionsLoadingException(Messages.FILE_ERROR_SCHEMA_MIGRATION_SAVE.tr(), ex);
            }
        }
    }

    @Override
    public void close() {
        if (this.reloadService != null) {
            try {
                this.reloadService.close();
            } catch (IOException e) {
                engine().logger().error("Unable to shut down FileDataStore watch service", e);
            }
            this.reloadService = null;
        }
    }

    private ConfigurationNode getSubjectsNode() {
        return this.permissionsConfig.get("subjects");
    }

    private CompletableFuture<Void> save() {
        if (this.saveSuppressed.get() <= 0) {
            return Util.asyncFailableFuture(() -> {
                saveSync();
                return null;
            }, engine().asyncExecutor());
        } else {
            return completedFuture(null);
        }
    }

    private void saveSync() throws ConfigurateException {
        if (this.saveSuppressed.get() <= 0) {
            if (this.dirty.compareAndSet(true, false)) {
                this.permissionsConfig.save();
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
            return FileSubjectData.fromNode(getSubjectsNode().node(type, identifier));
        } catch (SerializationException e) {
            throw new PermissionsLoadingException(Messages.FILE_ERROR_DESERIALIZE_SUBJECT.tr(), e);
        }
    }

    @Override
    protected CompletableFuture<ImmutableSubjectData> setDataInternal(String type, String identifier, final @Nullable ImmutableSubjectData data) {
        try {
            if (data == null) {
                getSubjectsNode().node(type, identifier).raw(null);
                dirty.set(true);
                return save().thenApply(input -> null);
            }

            final FileSubjectData fileData;

            if (data instanceof FileSubjectData) {
                fileData = (FileSubjectData) data;
            } else {
                fileData = (FileSubjectData) new FileSubjectData().mergeFrom(data);
            }
            fileData.serialize(getSubjectsNode().node(type, identifier));
            dirty.set(true);
            return save().thenApply(none -> fileData);
        } catch (SerializationException e) {
            return Util.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Boolean> isRegistered(String type, String identifier) {
        return completedFuture(!getSubjectsNode().node(type, identifier).virtual());
    }

    @Override
    public Stream<String> getAllIdentifiers(String type) {
        return getSubjectsNode().node(type)
                .childrenMap().keySet().stream()
                .map(Objects::toString);
    }

    @Override
    public Set<String> getRegisteredTypes() {
        return Collections.unmodifiableSet(getSubjectsNode().childrenMap().entrySet().stream()
                .filter(ent -> ent.getValue().isMap())
                .map(Map.Entry::getKey)
                .map(Object::toString)
                .collect(Collectors.toSet()));
    }

    @Override
    public CompletableFuture<Set<String>> getDefinedContextKeys() {
        return CompletableFuture.completedFuture(getSubjectsNode().childrenMap().values().stream() // list of types
                .flatMap(typeNode -> typeNode.childrenMap().values().stream()) // list of subjects
                .flatMap(subjectNode -> subjectNode.childrenList().stream()) // list of segments
                .flatMap(segmentNode -> segmentNode.node(FileSubjectData.KEY_CONTEXTS).childrenMap().entrySet().stream()) // list of contexts
                .map(ctx -> ctx.getKey().toString()) // of context objets
                .collect(PCollections.toPSet())); // to a set
    }

    @Override
    public Stream<Map.Entry<SubjectRef<?>, ImmutableSubjectData>> getAll() {
        return getSubjectsNode().childrenMap().keySet().stream() // all subject types
                .flatMap(type -> { // for each subject type
                    final String typeStr = type.toString();
                    return getAll(typeStr).map(pair -> immutableMapEntry(this.context().deserializeSubjectRef(typeStr, pair.getKey()), pair.getValue()));
                });
    }

    private ConfigurationNode getRankLaddersNode() {
        return this.permissionsConfig.get(KEY_RANK_LADDERS);
    }

    @Override
    public Stream<String> getAllRankLadders() {
        return getRankLaddersNode().childrenMap().keySet()
                .stream()
                .map(Object::toString);
    }

    @Override
    public CompletableFuture<RankLadder> getRankLadderInternal(String ladder) {
        return completedFuture(new FixedRankLadder(ladder, getRankLaddersNode().node(ladder.toLowerCase()).childrenList().stream()
                .map(node -> {
                    try {
                        return node.get(SubjectRef.TYPE);
                    } catch (final SerializationException ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .collect(PCollections.toPVector())));
    }

    @Override
    public CompletableFuture<Boolean> hasRankLadder(String ladder) {
        return completedFuture(!getRankLaddersNode().node(ladder.toLowerCase()).virtual());
    }

    @Override
    public CompletableFuture<ContextInheritance> getContextInheritanceInternal() {
        try {
            return completedFuture(this.permissionsConfig.node().get(MemoryContextInheritance.class));
        } catch (SerializationException e) {
            return Util.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<ContextInheritance> setContextInheritanceInternal(final ContextInheritance inheritance) {
        final MemoryContextInheritance realInheritance = MemoryContextInheritance.fromExistingContextInheritance(inheritance);
        try {
            this.permissionsConfig.node().set(MemoryContextInheritance.class, realInheritance);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
        dirty.set(true);
        return save().thenApply(none -> realInheritance);
    }

    @Override
    public CompletableFuture<RankLadder> setRankLadderInternal(final String identifier, final @Nullable RankLadder ladder) {
        ConfigurationNode childNode = getRankLaddersNode().node(identifier.toLowerCase());
        try {
            childNode.raw(null);
            if (ladder != null) {
                for (final SubjectRef<?> rank : ladder.ranks()) {
                    childNode.appendListNode().set(SubjectRef.TYPE, rank);
                }
            }
        } catch (final SerializationException ex) {
            return Util.failedFuture(ex);
        }
        this.dirty.set(true);
        return save().thenApply(none -> ladder);
    }

    @Override
    protected <T> T performBulkOperationSync(final Function<DataStore, T> function) throws Exception {
        this.saveSuppressed.getAndIncrement();
        T ret;
        try {
            ret = function.apply(this);
        } finally {
            this.saveSuppressed.getAndDecrement();
        }
        saveSync();
        return ret;
    }

    @AutoService(DataStoreFactory.class)
    public static class Factory extends AbstractDataStore.Factory<FileDataStore, Config> {
        public Factory() {
            super("file", Config.class, FileDataStore::new);
        }
    }
}
