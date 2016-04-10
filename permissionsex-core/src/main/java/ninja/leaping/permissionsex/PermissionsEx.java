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
package ninja.leaping.permissionsex;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.sk89q.squirrelid.resolver.HttpRepositoryService;
import com.sk89q.squirrelid.resolver.ProfileService;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.backend.memory.MemoryDataStore;
import ninja.leaping.permissionsex.command.PermissionsExCommands;
import ninja.leaping.permissionsex.command.RankingCommands;
import ninja.leaping.permissionsex.config.PermissionsExConfiguration;
import ninja.leaping.permissionsex.data.CacheListenerHolder;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.exception.PEBKACException;
import ninja.leaping.permissionsex.logging.DebugPermissionCheckNotifier;
import ninja.leaping.permissionsex.logging.PermissionCheckNotifier;
import ninja.leaping.permissionsex.logging.RecordingPermissionCheckNotifier;
import ninja.leaping.permissionsex.logging.TranslatableLogger;
import ninja.leaping.permissionsex.data.ContextInheritance;
import ninja.leaping.permissionsex.data.RankLadderCache;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.subject.SubjectType;
import ninja.leaping.permissionsex.util.Util;
import ninja.leaping.permissionsex.util.command.CommandSpec;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static ninja.leaping.permissionsex.util.Translations.*;

public class PermissionsEx implements ImplementationInterface, Caching<ContextInheritance> {
    public static final String SUBJECTS_USER = "user";
    public static final String SUBJECTS_GROUP = "group";
    public static final String SUBJECTS_DEFAULTS = "default";
    public static final ImmutableSet<Map.Entry<String, String>> GLOBAL_CONTEXT = ImmutableSet.of();

    private final TranslatableLogger logger;
    private final ImplementationInterface impl;
    private final MemoryDataStore transientData;
    private final RecordingPermissionCheckNotifier baseNotifier = new RecordingPermissionCheckNotifier();
    private volatile PermissionCheckNotifier notifier = baseNotifier;

    private final AtomicReference<State> state = new AtomicReference<>();
    private final ConcurrentMap<String, SubjectType> subjectTypeCache = new ConcurrentHashMap<>();
    private RankLadderCache rankLadderCache;
    private volatile CompletableFuture<ContextInheritance> cachedInheritance;
    private final CacheListenerHolder<Boolean, ContextInheritance> cachedInheritanceListeners = new CacheListenerHolder<>();

    private static class State {
        private final PermissionsExConfiguration config;
        private final DataStore activeDataStore;

        private State(PermissionsExConfiguration config, DataStore activeDataStore) {
            this.config = config;
            this.activeDataStore = activeDataStore;
        }
    }

    public PermissionsEx(final PermissionsExConfiguration config, ImplementationInterface impl) throws PermissionsLoadingException {
        this.impl = impl;
        this.logger = TranslatableLogger.forLogger(impl.getLogger());
        this.transientData = new MemoryDataStore();
        this.transientData.initialize(this);
        setDebugMode(config.isDebugEnabled());
        initialize(config);
        convertUuids();

        registerCommand(PermissionsExCommands.createRootCommand(this));
        registerCommand(RankingCommands.getPromoteCommand(this));
        registerCommand(RankingCommands.getDemoteCommand(this));
    }

    private State getState() throws IllegalStateException {
        State ret = this.state.get();
        if (ret == null) {
            throw new IllegalStateException("Manager has already been closed!");
        }
        return ret;
    }

    private void convertUuids() {
        try {
            InetAddress.getByName("api.mojang.com");
            final List<CompletableFuture<?>> actions = new ArrayList<>();
            getState().activeDataStore.performBulkOperation(dataStore -> {
                Iterable<String> toConvert = Iterables.filter(dataStore.getAllIdentifiers(SUBJECTS_USER), input1 -> {
                    if (input1 == null || input1.length() != 36) {
                        return true;
                    }
                    try {
                        UUID.fromString(input1);
                        return false;
                    } catch (IllegalArgumentException e) {
                        return true;
                    }
                });
                if (toConvert.iterator().hasNext()) {
                    getLogger().info(t("Trying to convert users stored by name to UUID"));
                } else {
                    return 0;
                }

                final ProfileService service = HttpRepositoryService.forMinecraft();
                try {
                    final int[] converted = {0};
                    service.findAllByName(toConvert, profile -> {
                        final String newIdentifier = profile.getUniqueId().toString();
                        String lookupName = profile.getName();
                        actions.add(dataStore.isRegistered(SUBJECTS_USER, newIdentifier).thenCombine(
                                dataStore.isRegistered(SUBJECTS_USER, lookupName)
                                        .thenCombine(dataStore.isRegistered(SUBJECTS_USER, lookupName.toLowerCase()), (a, b) -> (a || b)), (newRegistered, oldRegistered) -> {
                                    if (newRegistered) {
                                        getLogger().warn(t("Duplicate entry for %s found while converting to UUID", newIdentifier + "/" + profile.getName()));
                                        return false;
                                    } else if (!oldRegistered) {
                                        return false;
                                    }
                                    converted[0]++;
                                    return true;

                        }).thenCompose(doConvert -> {
                            if (!doConvert) {
                                return Util.emptyFuture();
                            }
                            return dataStore.getData(SUBJECTS_USER, profile.getName(), null)
                                    .thenCompose(oldData -> {
                                        return dataStore.setData(SUBJECTS_USER, newIdentifier, oldData.setOption(GLOBAL_CONTEXT, "name", profile.getName()))
                                                .thenAccept(result -> dataStore.setData(SUBJECTS_USER, profile.getName(), null)
                                                        .exceptionally(t -> {
                                                            t.printStackTrace();
                                                            return null;
                                                        }));
                                    });
                        }));
                        return true;
                    });
                    return converted[0];
                } catch (IOException | InterruptedException e) {
                    getLogger().error(t("Error while fetching UUIDs for users"), e);
                    return 0;
                }
            }).thenCombine(CompletableFuture.allOf(actions.toArray(new CompletableFuture[actions.size()])), (count, none) -> count).thenAccept(result -> {
                    if (result != null && result > 0) {
                        getLogger().info(tn("%s user successfully converted from name to UUID",
                                "%s users successfully converted from name to UUID!",
                                result, result));
                    }
                }).exceptionally(t -> {
                getLogger().error(t("Error converting users to UUID"), t);
                return null;
                });
        } catch (UnknownHostException e) {
            getLogger().warn(t("Unable to resolve Mojang API for UUID conversion. Do you have an internet connection? UUID conversion will not proceed (but may not be necessary)."));
        }

    }

    public SubjectType getSubjects(String type) {
        return subjectTypeCache.computeIfAbsent(type,
                key -> new SubjectType(this, type, new SubjectCache(type, getState().activeDataStore), new SubjectCache(type, transientData)));
    }

    public Collection<SubjectType> getActiveSubjectTypes() {
        return Collections.unmodifiableCollection(subjectTypeCache.values());
    }

    /**
     * Access rank ladders through a cached interface
     *
     * @return Access to rank ladders
     */
    public RankLadderCache getLadders() {
        return this.rankLadderCache;
    }

    /**
     * Imports data into the currently active backend from the backend identified by the provided identifier
     *
     * @param dataStoreIdentifier The identifier of the backend to import from
     * @return A future that completes once the import operation is complete
     */
    public CompletableFuture<Void> importDataFrom(String dataStoreIdentifier) {
        final State state = getState();
        final DataStore expected = state.config.getDataStore(dataStoreIdentifier);
        if (expected == null) {
            return Util.failedFuture(new IllegalArgumentException("Data store " + dataStoreIdentifier + " is not present"));
        }
        try {
            expected.initialize(this);
        } catch (PermissionsLoadingException e) {
            return Util.failedFuture(e);
        }

        return state.activeDataStore.performBulkOperation(store -> {
            CompletableFuture<Void> ret = CompletableFuture.allOf(Iterables.toArray(Iterables.transform(expected.getAll(),
                    input -> store.setData(input.getKey().getKey(), input.getKey().getValue(), input.getValue())), CompletableFuture.class))
                    .thenCombine(expected.getContextInheritance(null).thenCompose(store::setContextInheritance), (v, a) -> null);
            for (String ladder : store.getAllRankLadders()) {
                ret = ret.thenCombine(expected.getRankLadder(ladder, null).thenCompose(ladderObj -> store.setRankLadder(ladder, ladderObj)), (v, a) -> null);
            }
            return ret;
        }).thenCompose(val -> Util.failableFuture(val::get));
    }

    public Set<String> getRegisteredSubjectTypes() {
        return getState().activeDataStore.getRegisteredTypes();
    }

    public PermissionCheckNotifier getNotifier() {
        return this.notifier;
    }

    public RecordingPermissionCheckNotifier getRecordingNotifier() {
        return this.baseNotifier;
    }

    // TODO: Proper thread-safety
    public boolean hasDebugMode() {
        return this.getNotifier() instanceof DebugPermissionCheckNotifier;
    }

    public void setDebugMode(boolean debug) {
        setDebugMode(debug, null);
    }

    public void setDebugMode(boolean debug, Pattern filterPattern) {
        if (debug) {
            if (!(this.notifier instanceof DebugPermissionCheckNotifier)) {
                this.notifier = new DebugPermissionCheckNotifier(getLogger(), this.notifier, filterPattern == null ? null : perm -> filterPattern.matcher(perm).find());
            }
        } else {
            if (this.notifier instanceof DebugPermissionCheckNotifier) {
                this.notifier = ((DebugPermissionCheckNotifier) this.notifier).getDelegate();
            }
        }
    }

    private void reloadSync() throws PEBKACException, PermissionsLoadingException {
        try {
            PermissionsExConfiguration config = getState().config.reload();
            config.validate();
            initialize(config);
            getSubjects(SUBJECTS_GROUP).cacheAll();
        } catch (IOException e) {
            throw new PEBKACException(t("Error while loading configuration: %s", e.getLocalizedMessage()));
        }
    }

    private void initialize(PermissionsExConfiguration config) throws PermissionsLoadingException {
        State newState = new State(config, config.getDefaultDataStore());
        newState.activeDataStore.initialize(this);
        State oldState = this.state.getAndSet(newState);
        if (oldState != null) {
            try {
                oldState.activeDataStore.close();
            } catch (Exception e) {} // TODO maybe warn?
        }

        this.rankLadderCache = new RankLadderCache(this.rankLadderCache, newState.activeDataStore);
        this.subjectTypeCache.forEach((key, val) -> val.update(newState.activeDataStore));
        getSubjects(SUBJECTS_GROUP).cacheAll();
        if (this.cachedInheritance != null) {
            this.cachedInheritance = null;
            getContextInheritance(null).thenAccept(inheritance -> this.cachedInheritanceListeners.call(true, inheritance));
        }

        // Migrate over legacy subject data
        newState.activeDataStore.moveData("system", SUBJECTS_DEFAULTS, SUBJECTS_DEFAULTS, SUBJECTS_DEFAULTS).thenRun(() -> {
            getLogger().info(t("Successfully migrated old-style default data to new location"));
        });
    }

    public CompletableFuture<Void> reload() {
        return Util.asyncFailableFuture(() -> {
            reloadSync();
            return null;
        }, getAsyncExecutor());
    }

    public void close() {
        State state = this.state.getAndSet(null);
        state.activeDataStore.close();
    }

    @Override
    public Path getBaseDirectory() {
        return impl.getBaseDirectory();
    }

    @Override
    public TranslatableLogger getLogger() {
        return this.logger;
    }

    @Override
    public DataSource getDataSourceForURL(String url) throws SQLException {
        return impl.getDataSourceForURL(url);
    }

    /**
     * Get an executor to run tasks asynchronously on.
     *
     * @return The async executor
     */
    @Override
    public Executor getAsyncExecutor() {
        return impl.getAsyncExecutor();
    }

    @Override
    public void registerCommand(CommandSpec command) {
        impl.registerCommand(command);
    }

    @Override
    public Set<CommandSpec> getImplementationCommands() {
        return impl.getImplementationCommands();
    }

    @Override
    public String getVersion() {
        return impl.getVersion();
    }

    public PermissionsExConfiguration getConfig() {
        return getState().config;
    }

    public CompletableFuture<ContextInheritance> getContextInheritance(Caching<ContextInheritance> listener) {
        if (this.cachedInheritance == null) {
            this.cachedInheritance = getState().activeDataStore.getContextInheritance(this);
        }
        if (listener != null) {
            this.cachedInheritanceListeners.addListener(true, listener);
        }
        return this.cachedInheritance;

    }

    public CompletableFuture<ContextInheritance> setContextInheritance(ContextInheritance newInheritance) {
        return getState().activeDataStore.setContextInheritance(newInheritance);
    }

    @Override
    public void clearCache(ContextInheritance newData) {
        this.cachedInheritance = CompletableFuture.completedFuture(newData);
        this.cachedInheritanceListeners.call(true, newData);
    }
}
