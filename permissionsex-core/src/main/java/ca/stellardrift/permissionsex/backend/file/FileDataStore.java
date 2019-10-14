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
import ca.stellardrift.permissionsex.util.Util;
import ca.stellardrift.permissionsex.util.configurate.ReloadableConfig;
import ca.stellardrift.permissionsex.util.configurate.WatchServiceListener;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ca.stellardrift.permissionsex.util.Translations.t;

public final class FileDataStore extends AbstractDataStore {
    static final String KEY_RANK_LADDERS = "rank-ladders";
    public static final Factory FACTORY = new Factory("file", FileDataStore.class);

    @Setting
    private String file;
    @Setting(value = "alphabetize-entries", comment = "Place file entries in alphabetical order")
    private boolean alphabetizeEntries = false;
    @Setting(value = "auto-reload", comment = "Automatically reload the data file when changes have been made")
    private boolean autoReload = true;

    private WatchServiceListener reloadService;
    private ReloadableConfig<ConfigurationNode> permissionsConfig;
    private final AtomicInteger saveSuppressed = new AtomicInteger();
    private final AtomicBoolean dirty = new AtomicBoolean();

    public FileDataStore() {
        super(FACTORY);
    }


    private ReloadableConfig<ConfigurationNode> createLoader(Path file) throws IOException {
        ConfigurationOptions configOptions;

        if (alphabetizeEntries) {
            configOptions = ConfigurationOptions.defaults().setMapFactory(MapFactories.sortedNatural());
        } else {
            configOptions = ConfigurationOptions.defaults();
        }

        Function1<Path, ConfigurationLoader<ConfigurationNode>> loaderFunc = path -> GsonConfigurationLoader.builder()
                .setDefaultOptions(configOptions)
                .setPath(path)
                .setIndent(4)
                .setLenient(true)
                .build();

        ReloadableConfig<ConfigurationNode> ret;
        if (reloadService != null) {
            ret = reloadService.createConfig(loaderFunc, file, this::refresh);
        } else {
            ret = new ReloadableConfig<>(loaderFunc.invoke(file));
        }

        ret.setErrorCallback((e, state) -> {
            getManager().getLogger().error(t("Error while %s permissions configuration: %s. Old configuration will continue to be used until the error is corrected.", state, e.getLocalizedMessage()), e);
            return Unit.INSTANCE;
        });

        return ret;
    }

    /**
     * Handle automatic reloads of the permissions storage
     *
     * @param newNode The updated node
     * @return void
     */
    private Unit refresh(ConfigurationNode newNode) {
        this.listeners.getAllKeys().forEach(key -> {
            try {
                this.listeners.call(key, getDataSync(key.getKey(), key.getValue()));
            } catch (PermissionsLoadingException e) {
                getManager().getLogger().error(t("During an automatic reload of the permissions storage, %s %s failed to load:", key.getKey(), key.getValue()), e);
            }
        });

        this.rankLadderListeners.getAllKeys().forEach(key ->
                this.rankLadderListeners.call(key, getRankLadderInternal(key).block()));

        this.contextInheritanceListeners.getAllKeys().forEach(key ->
                this.contextInheritanceListeners.call(key, getContextInheritanceInternal().block()));

        getManager().getLogger().info(t("Automatically reloaded %s based on a change made outside of PEX", this.file));

        return Unit.INSTANCE;
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
            throw new PermissionsLoadingException(t("While loading legacy %s permissions from %s", formatName, legacyPermissionsFile), e);
        }
        return permissionsFile;
    }

    @Override
    protected void initializeInternal() throws PermissionsLoadingException {
        if (autoReload) {
            reloadService = new WatchServiceListener(Executors.newCachedThreadPool(), getManager().getLogger());
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
                throw new PermissionsLoadingException(t("While loading permissions file from %s", permissionsFile), e);
            }
        }

        if (permissionsConfig.getNode().getChildrenMap().isEmpty()) { // New configuration, populate with default data
            try {
                performBulkOperationSync(input -> {
                    applyDefaultData();
                    permissionsConfig.set("schema-version", SchemaMigrations.LATEST_VERSION);
                    return null;
                });
            } catch (PermissionsLoadingException e) {
                throw e;
            } catch (Exception e) {
                throw new PermissionsLoadingException(t("Error creating initial data for file backend"), e);
            }
        } else {
            ConfigurationTransformation versionUpdater = SchemaMigrations.versionedMigration(getManager().getLogger());
            int startVersion = permissionsConfig.get("schema-version").getInt(-1);
            versionUpdater.apply(permissionsConfig.getNode());
            int endVersion = permissionsConfig.get("schema-version").getInt();
            if (endVersion > startVersion) {
                getManager().getLogger().info(t("%s schema version updated from %s to %s", permissionsFile, startVersion, endVersion));
                try {
                    save().block();
                } catch (RuntimeException e) {
                    throw new PermissionsLoadingException(t("While performing version upgrade"), e);
                }
            }
        }
    }

    @Override
    public void close() {
        if (this.reloadService != null) {
            this.reloadService.close();
            this.reloadService = null;
        }
    }

    private ConfigurationNode getSubjectsNode() {
        return this.permissionsConfig.get("subjects");
    }

    private Mono<Void> save() {
        if (saveSuppressed.get() <= 0) {
            return Mono.create(sink -> {
                try {
                    saveSync();
                    sink.success();
                } catch (Exception e) {
                    sink.error(e);
                }
            }).subscribeOn(getManager().getScheduler()).then();
        } else {
            return Mono.empty();
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
    public Mono<ImmutableSubjectData> getDataInternal(String type, String identifier) {
        try {
            return Mono.just(getDataSync(type, identifier));
        } catch (PermissionsLoadingException e) {
            return Mono.error(e);
        }
    }

    private ImmutableSubjectData getDataSync(String type, String identifier) throws PermissionsLoadingException {
        try {
            return FileSubjectData.fromNode(getSubjectsNode().getNode(type, identifier));
        } catch (ObjectMappingException e) {
            throw new PermissionsLoadingException(t("While deserializing subject data for %s:", identifier), e);
        }
    }

    @Override
    protected Mono<ImmutableSubjectData> setDataInternal(String type, String identifier, final ImmutableSubjectData data) {
        try {
            if (data == null) {
                getSubjectsNode().getNode(type, identifier).setValue(null);
                dirty.set(true);
                return save().map(it -> null);
            }

            final FileSubjectData fileData;

            if (data instanceof FileSubjectData) {
                fileData = (FileSubjectData) data;
            } else {
                fileData = ConversionUtils.transfer(data, new FileSubjectData());
            }
            fileData.serialize(getSubjectsNode().getNode(type, identifier));
            dirty.set(true);
            return save().map(none -> fileData);
        } catch (ObjectMappingException e) {
            return Mono.error(e);
        }
    }

    @Override
    public Mono<Boolean> isRegistered(String type, String identifier) {
        return Mono.just(!getSubjectsNode().getNode(type, identifier).isVirtual());
    }

    @Override
    public Flux<String> getAllIdentifiers(String type) {
        return Flux.fromIterable(getSubjectsNode().getNode(type).getChildrenMap().keySet())
                .map(Object::toString);
    }

    @Override
    public Flux<String> getRegisteredTypes() {
        return Flux.fromStream(getSubjectsNode().getChildrenMap().entrySet().stream()
                .filter(ent -> ent.getValue().hasMapChildren())
                .map(Map.Entry::getKey)
                .map(Object::toString)
                .distinct());
    }

    @Override
    public Flux<String> getDefinedContextKeys() {
        return Flux.fromStream(getSubjectsNode().getChildrenMap().values().stream() // list of types
                .flatMap(typeNode -> typeNode.getChildrenMap().values().stream()) // list of subjects
                .flatMap(subjectNode -> subjectNode.getChildrenList().stream()) // list of segments
                .flatMap(segmentNode -> segmentNode.getNode(FileSubjectData.KEY_CONTEXTS).getChildrenMap().entrySet().stream()) // list of contexts
                .map(ctx -> ctx.getKey().toString())); // of context objets
    }

    @Override
    public Flux<Pair<Map.Entry<String, String>, ImmutableSubjectData>> getAll() {
        return getRegisteredTypes().flatMap(type -> getAll(type).map(it -> new Pair<>(Maps.immutableEntry(type, it.getFirst()), it.getSecond())));
    }

    private ConfigurationNode getRankLaddersNode() {
        return this.permissionsConfig.get(KEY_RANK_LADDERS);
    }

    @Override
    public Flux<String> getAllRankLadders() {
        return Flux.fromIterable(getRankLaddersNode().getChildrenMap().keySet())
                .map(Object::toString);
    }

    @Override
    public Mono<RankLadder> getRankLadderInternal(String ladder) {
        return Mono.just(new FixedRankLadder(ladder, getRankLaddersNode().getNode(ladder.toLowerCase()).getChildrenList().stream()
                .map(node -> Util.subjectFromString(Objects.requireNonNull(node.getString())))
                .collect(Collectors.toList())));
    }

    @Override
    public Mono<Boolean> hasRankLadder(String ladder) {
        return Mono.just(!getRankLaddersNode().getNode(ladder.toLowerCase()).isVirtual());
    }

    @Override
    public Mono<ContextInheritance> getContextInheritanceInternal() {
        try {
            return Mono.just(this.permissionsConfig.getNode().getValue(TypeToken.of(MemoryContextInheritance.class)));
        } catch (ObjectMappingException e) {
            return Mono.error(e);
        }
    }

    @Override
    public Mono<ContextInheritance> setContextInheritanceInternal(final ContextInheritance inheritance) {
        final MemoryContextInheritance realInheritance = MemoryContextInheritance.fromExistingContextInheritance(inheritance);
        try {
            this.permissionsConfig.getNode().setValue(TypeToken.of(MemoryContextInheritance.class), realInheritance);
        } catch (ObjectMappingException e) {
            throw new RuntimeException(e);
        }
        dirty.set(true);
        return save().map(none -> realInheritance);
    }

    @Override
    public Mono<RankLadder> setRankLadderInternal(String identifier, RankLadder ladder) {
        ConfigurationNode childNode = getRankLaddersNode().getNode(identifier.toLowerCase());
        childNode.setValue(null);
        if (ladder != null) {
            for (Map.Entry<String, String> rank : ladder.getRanks()) {
                childNode.getAppendedNode().setValue(Util.subjectToString(rank));
            }
        }
        dirty.set(true);
        return save().map(none -> ladder);
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
