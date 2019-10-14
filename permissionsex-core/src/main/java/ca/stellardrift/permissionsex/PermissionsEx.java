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

package ca.stellardrift.permissionsex;

import ca.stellardrift.permissionsex.backend.DataStore;
import ca.stellardrift.permissionsex.backend.conversion.ConversionProvider;
import ca.stellardrift.permissionsex.backend.conversion.ConversionProviderRegistry;
import ca.stellardrift.permissionsex.backend.conversion.ConversionResult;
import ca.stellardrift.permissionsex.backend.memory.MemoryDataStore;
import ca.stellardrift.permissionsex.command.PermissionsExCommands;
import ca.stellardrift.permissionsex.command.RankingCommands;
import ca.stellardrift.permissionsex.config.PermissionsExConfiguration;
import ca.stellardrift.permissionsex.context.*;
import ca.stellardrift.permissionsex.data.*;
import ca.stellardrift.permissionsex.exception.PEBKACException;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.logging.DebugPermissionCheckNotifier;
import ca.stellardrift.permissionsex.logging.PermissionCheckNotifier;
import ca.stellardrift.permissionsex.logging.RecordingPermissionCheckNotifier;
import ca.stellardrift.permissionsex.logging.TranslatableLogger;
import ca.stellardrift.permissionsex.rank.RankLadder;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.subject.SubjectTypeDefinition;
import ca.stellardrift.permissionsex.util.MinecraftProfile;
import ca.stellardrift.permissionsex.util.command.CommandSpec;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ca.stellardrift.permissionsex.util.Translations.t;


/**
 * The entry point to the PermissionsEx engine.
 *
 * The fastest way to get going with working with subjects is to access a subject type collection with {@link #getSubjects(String)}
 * and request a {@link CalculatedSubject} to query data from. Directly working with
 * {@link SubjectDataReference}s is another option, preferable if most of the operations
 * being performed are writes, or querying data directly defined on a subject.
 *
 * Keep in mind most of PEX's core data objects are immutable and must be resubmitted to their holders to apply updates.
 * Most write operations are done asynchronously, and futures are returned that complete when the backend is finished writing out data.
 * For larger operations, it can be useful to perform changes within {@link #performBulkOperation(Supplier)}, which will reduce unnecessary writes to the backing data store in some cases.
 */
public class PermissionsEx implements ImplementationInterface, Consumer<ContextInheritance> {
    public static final String SUBJECTS_USER = "user";
    public static final String SUBJECTS_GROUP = "group";
    public static final String SUBJECTS_DEFAULTS = "default";
    public static final ImmutableSet<ContextValue<?>> GLOBAL_CONTEXT = ImmutableSet.of();

    private final TranslatableLogger logger;
    private final ImplementationInterface impl;
    private final MemoryDataStore transientData;
    private final RecordingPermissionCheckNotifier baseNotifier = new RecordingPermissionCheckNotifier();
    private volatile PermissionCheckNotifier notifier = baseNotifier;
    private final ConcurrentMap<String, ContextDefinition<?>> contextTypes = new ConcurrentHashMap<>();

    private final AtomicReference<State> state = new AtomicReference<>();
    private final ConcurrentMap<String, SubjectType> subjectTypeCache = new ConcurrentHashMap<>();
    private RankLadderCache rankLadderCache;
    private volatile Mono<ContextInheritance> cachedInheritance;
    private final CacheListenerHolder<Boolean, ContextInheritance> cachedInheritanceListeners = new CacheListenerHolder<>();

    private static class State {
        private final PermissionsExConfiguration config;
        private final DataStore activeDataStore;
        private List<ConversionResult> availableConversions;

        private State(PermissionsExConfiguration config, DataStore activeDataStore) {
            this.config = config;
            this.activeDataStore = activeDataStore;
        }
    }

    public PermissionsEx(final PermissionsExConfiguration config, ImplementationInterface impl) throws PermissionsLoadingException {
        Schedulers.setFactory(new Schedulers.Factory() {
            @Override
            public Scheduler newElastic(int ttlSeconds, ThreadFactory threadFactory) {
                return Schedulers.fromExecutor(getAsyncExecutor(), true);
            }

            @Override
            public Scheduler newBoundedElastic(int threadCap, int queuedTaskCap, ThreadFactory threadFactory, int ttlSeconds) {
                return Schedulers.fromExecutor(getAsyncExecutor(), true);
            }

            @Override
            public Scheduler newParallel(int parallelism, ThreadFactory threadFactory) {
                return Schedulers.fromExecutor(getAsyncExecutor(), true);
            }

            @Override
            public Scheduler newSingle(ThreadFactory threadFactory) {
                return Schedulers.fromExecutor(getAsyncExecutor(), true);
            }
        });

        this.impl = impl;
        this.logger = TranslatableLogger.forLogger(impl.getLogger());
        this.transientData = new MemoryDataStore();
        this.transientData.initialize(this);
        setDebugMode(config.isDebugEnabled());
        registerContextDefinition(ServerTagContextDefinition.INSTANCE);
        registerContextDefinition(BeforeTimeContextDefinition.INSTANCE);
        registerContextDefinition(AfterTimeContextDefinition.INSTANCE);
        initialize(config);

        getSubjects(SUBJECTS_DEFAULTS).setTypeInfo(SubjectTypeDefinition.defaultFor(SUBJECTS_DEFAULTS, false));
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
        } catch (UnknownHostException e) {
            getLogger().warn(t("Unable to resolve Mojang API for UUID conversion. Do you have an internet connection? UUID conversion will not proceed (but may not be necessary)."));
            return;
        }
        final DataStore dataStore = state.get().activeDataStore;
        performBulkOperation(() -> {
            Flux<String> toConvert = dataStore.getAllIdentifiers(SUBJECTS_USER)
                    .filter(ident -> {
                        if (ident.length() != 36) {
                            return true;
                        }
                        try {
                            UUID.fromString(ident);
                            return false;
                        } catch (IllegalArgumentException ex) {
                            return true;
                        }
                    });
            /**
             * The process:
             * - check if uuid is registered
             * - check if username is registered (both returned case and lower case)
             * - if uuid is already registered, warn of duplicate entry
             * - if username is not registered, do not convert
             *
             * then, the actual conversion:
             * - get the old subject data
             * - set the option `name` to the username in global context
             * - set the data at the uuid to the returned data
             * - delete the data at the old location
             *
             * and repeat for every user
             */
            return lookupMinecraftProfilesByName(toConvert)
                    .filterWhen(profile -> {
                        final String newIdentifier = profile.getUuid().toString();
                        String lookupName = profile.getName();
                        Mono<Boolean> uuidRegistered = dataStore.isRegistered(SUBJECTS_USER, newIdentifier);
                        Mono<Boolean> nameRegistered = dataStore.isRegistered(SUBJECTS_USER, lookupName);
                        Mono<Boolean> lowercaseNameRegistered = dataStore.isRegistered(SUBJECTS_USER, lookupName.toLowerCase());
                        uuidRegistered.doOnSuccess(x -> {
                            if (x) {
                                getLogger().warn(t("Duplicate entry for %s found while converting to UUID", newIdentifier + "/" + lookupName));
                            }
                        });
                        // !uuidRegistered && (lookupName || lowercaseLookupName)
                        return Flux.concat(uuidRegistered.map(x -> !x), nameRegistered.concatWith(lowercaseNameRegistered).any(x -> x)).all(x -> x);
                    }).flatMap(profile -> {
                        final String newIdent = profile.getUuid().toString();
                        return dataStore.getData(SUBJECTS_USER, profile.getName(), null)
                                .flatMap(oldData -> dataStore.setData(SUBJECTS_USER, newIdent, oldData.setOption(GLOBAL_CONTEXT, "name", profile.getName()))
                                        .flatMap(result -> dataStore.setData(SUBJECTS_USER, profile.getName(), null)));
                    }).doOnError(t -> getLogger().error(t("Error converting users to UUID"), t));
        }).count().subscribe(count -> {
            if (count > 0) {
                getLogger().info(t("Successfully converted %s users to UUID!", count));
            }
        }, error -> getLogger().error(t("Unable to convert users to UUID due to the following error!"), error));
    }

    /**
     * Get the collection of subjects of a given type. No data is loaded in this operation.
     * Any string is supported as a subject type, but some common types have been provided as constants
     * in this class for convenience.
     *
     * @see #SUBJECTS_DEFAULTS
     * @see #SUBJECTS_GROUP
     * @see #SUBJECTS_USER
     * @param type The type identifier requested. Can be any string
     * @return The subject type collection
     */
    public SubjectType getSubjects(String type) {
        return subjectTypeCache.computeIfAbsent(type,
                key -> new SubjectType(this, type, new SubjectCache(type, getState().activeDataStore), new SubjectCache(type, transientData)));
    }

    /**
     * Get a view of the currently cached subject types
     *
     * @return Unmodifiable view of the currently cached subject types
     */
    public Collection<SubjectType> getActiveSubjectTypes() {
        return Collections.unmodifiableCollection(subjectTypeCache.values());
    }

    /**
     * Get all registered subject types in the active data store.
     * The set is an immutable copy of the backend data.
     *
     * @return A set of registered subject types
     */
    public Flux<String> getRegisteredSubjectTypes() {
        return getState().activeDataStore.getRegisteredTypes();
    }

    /**
     * Suppress writes to the data store for the duration of a specific operation. Only really useful for extremely large operations
     *
     * @param func The operation to perform
     * @param <T> The type of data that will be returned
     * @return A future that completes once all data has been written to the store
     */
    public <T, P extends Publisher<T>> P performBulkOperation(Supplier<P> func) {
        return getState().activeDataStore.performBulkOperation(store -> func.get());
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
     * Imports data into the currently active backend from another configured backend.
     *
     * @param dataStoreIdentifier The identifier of the backend to import from
     * @return A future that completes once the import operation is complete
     */
    public Mono<?> importDataFrom(String dataStoreIdentifier) {
        final State state = getState();
        final DataStore source = state.config.getDataStore(dataStoreIdentifier);
        if (source == null) {
            return Mono.error(new IllegalArgumentException("Data store " + dataStoreIdentifier + " is not present"));
        }
        try {
            source.initialize(this);
        } catch (PermissionsLoadingException e) {
            return Mono.error(e);
        }


        return state.activeDataStore.performBulkOperation(destination -> {
            Flux<String> rankLadderNames = source.getAllRankLadders();
            Flux<ImmutableSubjectData> subjects = source.getAll()
                    .flatMap(ent -> destination.setData(ent.getFirst().getKey(), ent.getFirst().getValue(), ent.getSecond()));
            Mono<ContextInheritance> ctxInherit = source.getContextInheritance(null).flatMap(destination::setContextInheritance);
            Flux<RankLadder> rankLadders = rankLadderNames.zipWith(rankLadderNames.flatMap(ident -> source.getRankLadder(ident, null)), Pair::new)
                    .flatMap(ent -> destination.setRankLadder(ent.getFirst(), ent.getSecond()));
            return Flux.merge(subjects, ctxInherit, rankLadders).last();
        });
    }

    /**
     * Get the currently active notifier. This objet has callbacks triggered on every permission check
     *
     * @return The active notifier
     */
    public PermissionCheckNotifier getNotifier() {
        return this.notifier;
    }

    /**
     * Get the base notifier that logs any permission checks that gave taken place.
     * @return A log of all permission checks made
     */
    public RecordingPermissionCheckNotifier getRecordingNotifier() {
        return this.baseNotifier;
    }

    // TODO: Proper thread-safety

    /**
     * Know whether or not debug mode is enabled
     *
     * @return true if debug mode is enabled
     */
    public boolean hasDebugMode() {
        return this.getNotifier() instanceof DebugPermissionCheckNotifier;
    }

    /**
     * Set whether or not debug mode is enabled, with no filter pattern
     *
     * @see #setDebugMode(boolean, Pattern)
     * @param debug Whether or not debug mode is enabled
     */
    public void setDebugMode(boolean debug) {
        setDebugMode(debug, null);
    }

    /**
     * Set whether or not debug mode is enabled. Debug mode logs all permission, option, and inheritance
     * checks made to the console.
     *
     * @param debug Whether to enable debug mode
     * @param filterPattern A pattern to filter which permissions are logged. Null for no filter.
     */
    public synchronized void setDebugMode(boolean debug, Pattern filterPattern) {
        if (debug) {
            if (this.notifier instanceof DebugPermissionCheckNotifier) {
                this.notifier = new DebugPermissionCheckNotifier(getLogger(), ((DebugPermissionCheckNotifier) this.notifier).getDelegate(), filterPattern == null ? null : perm -> filterPattern.matcher(perm).find());
            } else {
                this.notifier = new DebugPermissionCheckNotifier(getLogger(), this.notifier, filterPattern == null ? null : perm -> filterPattern.matcher(perm).find());
            }
        } else {
            if (this.notifier instanceof DebugPermissionCheckNotifier) {
                this.notifier = ((DebugPermissionCheckNotifier) this.notifier).getDelegate();
            }
        }
    }

    /**
     * Synchronous helper to perform reloads
     *
     * @throws PEBKACException If the configuration couldn't be parsed
     * @throws PermissionsLoadingException When there's an error loading the data store
     */
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

    /**
     * Initialize the engine.
     *
     * May be called even if the engine has been initialized already, with results essentially equivalent to performing a reload
     *
     * @param config The configuration to use in this engine
     * @throws PermissionsLoadingException If an error occurs loading the backend
     */
    private void initialize(PermissionsExConfiguration config) throws PermissionsLoadingException {
        State newState = new State(config, config.getDefaultDataStore());
        boolean existingData = newState.activeDataStore.initialize(this);
        try {
            newState.config.save();
        } catch (IOException e) {
            throw new PermissionsLoadingException(t("Unable to write permissions configuration"), e);
        }

        if (!existingData) {
            getLogger().info(t("Welcome to PermissionsEx! We're creating some default data. If you have data from " +
                    "another plugin to import, it will be listed below now. Run the command /pex import [id] to import from one of those backends:"));

            List<ConversionResult> allResults = new LinkedList<>();
            for (ConversionProvider prov : ConversionProviderRegistry.INSTANCE.getAllProviders()) {
                List<ConversionResult> res = prov.listConversionOptions(this);
                if (!res.isEmpty()) {
                    getLogger().info(t("Plugin %s has data in stores: %s", prov.getName(), res.toString()));
                    allResults.addAll(res);
                }
            }
        }

        State oldState = this.state.getAndSet(newState);
        if (oldState != null) {
            try {
                oldState.activeDataStore.close();
            } catch (Exception e) {} // TODO maybe warn?
        }

        this.rankLadderCache = new RankLadderCache(this.rankLadderCache, newState.activeDataStore);
        this.subjectTypeCache.forEach((key, val) -> val.update(newState.activeDataStore));
        this.contextTypes.values().forEach(ctxDef -> {
            if (ctxDef instanceof PEXContextDefinition<?>) {
                ((PEXContextDefinition<?>) ctxDef).update(newState.config);
            }
        });
        getSubjects(SUBJECTS_GROUP).cacheAll();
        if (this.cachedInheritance != null) {
            this.cachedInheritance = null;
            getContextInheritance().subscribe(inheritance -> this.cachedInheritanceListeners.call(true, inheritance));
        }

        // Migrate over legacy subject data
        newState.activeDataStore.moveData("system", SUBJECTS_DEFAULTS, SUBJECTS_DEFAULTS, SUBJECTS_DEFAULTS).subscribe(x -> {
            getLogger().info(t("Successfully migrated old-style default data to new location"));
        }, err -> {});
    }

    /**
     * Reload the configuration file in use and refresh backend data
     *
     * @return A future that completes once a reload has finished
     */
    public Mono<Void> reload() {
        return Mono.fromCallable(() -> {
            reloadSync();
            return null;
        }).subscribeOn(getScheduler()).then();
    }

    /**
     * Shut down the PEX engine. Once this has been done, no further action can be taken
     * until the engine is reinitialized with a fresh configuration.
     */
    public void close() {
        State state = this.state.getAndSet(null);
        state.activeDataStore.close();
    }

    @Override
    public TranslatableLogger getLogger() {
        return this.logger;
    }

    // -- Implementation interface proxy methods --

    @Override
    public Path getBaseDirectory() {
        return impl.getBaseDirectory();
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

    @Override
    public Map.Entry<String, String> createSubjectIdentifier(String collection, String ident) {
        return impl.createSubjectIdentifier(collection, ident);
    }

    @Override
    public Flux<MinecraftProfile> lookupMinecraftProfilesByName(Flux<String> names) {
        return impl.lookupMinecraftProfilesByName(names);
    }

    /**
     * Get the current configuration PEX is operating with. This object is immutable.
     *
     * @return The current configuration object
     */
    public PermissionsExConfiguration getConfig() {
        return getState().config;
    }

    /**
     * Get context inheritance data. The result of the future is immutable -- to take effect, the object returned by any
     * update methods in {@link ContextInheritance} must be passed to {@link #setContextInheritance(ContextInheritance)}.
     *  It follows that anybody else's changes will not appear in the returned inheritance object -- so if updates are
     *  desired providing a callback function is important.
     *
     *  The returned stream will continue providing updated context inheritance until it is disposed.
     *
     * @return A future providing the current context inheritance data
     */
    public Mono<ContextInheritance> getContextInheritance() {
        if (this.cachedInheritance == null) {
            this.cachedInheritance = getState().activeDataStore.getContextInheritance(this);
        }
        return this.cachedInheritance;

    }

    /**
     * Update the context inheritance when values have been changed
     *
     * @param newInheritance The modified inheritance object
     * @return A future containing the latest context inheritance object
     */
    public Mono<ContextInheritance> setContextInheritance(ContextInheritance newInheritance) {
        return getState().activeDataStore.setContextInheritance(newInheritance);
    }

    /**
     * Listener method that handles changes to context inheritance. Should not be called by outside users
     *
     * @param newData The new data to replace cached information
     */
    @Override
    public void accept(ContextInheritance newData) {
        this.cachedInheritance = Mono.just(newData);
        this.cachedInheritanceListeners.call(true, newData);
    }

    public Flux<ContextDefinition<?>> getUsedContextTypes() {
        return Flux.merge(getState().activeDataStore.getDefinedContextKeys(), transientData.getDefinedContextKeys())
                .collect(Collectors.toSet())
                .flatMapMany(allKeys -> Flux.fromStream(this.contextTypes.values().stream()
                        .filter(it -> allKeys.contains(it.getName()))));
    }

    /**
     * Register a new context type that can be queried. If there is another context type registered with the same key
     * as the one trying to be registered, the registration will fail.
     *
     * @param contextDefinition The new context type
     * @param <T> The context value type
     * @return whether the context was successfully registered
     */
    public <T> boolean registerContextDefinition(ContextDefinition<T> contextDefinition) {
        if (contextDefinition instanceof PEXContextDefinition<?> && this.state.get() != null) {
            ((PEXContextDefinition<T>) contextDefinition).update(getConfig());
        }
       ContextDefinition<?> possibleOut =  this.contextTypes.putIfAbsent(contextDefinition.getName(), contextDefinition);
        if (possibleOut instanceof FallbackContextDefinition) {
            return this.contextTypes.replace(contextDefinition.getName(), possibleOut, contextDefinition);
        } else {
            return possibleOut == null;
        }
    }

    /**
     * Register multiple context definitions.
     *
     * @see #registerContextDefinition for details on how individual registrations occur
     * @param definitions The definitions to register
     * @return The number of definitions that were successfully registered
     */
    public int registerContextDefinitions(ContextDefinition<?>... definitions) {
        int numRegistered = 0;
        for (ContextDefinition<?> def : definitions) {
            if (registerContextDefinition(def)) {
                numRegistered++;
            }
        }
        return numRegistered;
    }

    /**
     * Get an immutable copy as a list of the registered context types
     *
     * @return The registered context types
     */
    public List<ContextDefinition<?>> getRegisteredContextTypes() {
        return ImmutableList.copyOf(this.contextTypes.values());
    }

    @Nullable
    public ContextDefinition<?> getContextDefinition(@NotNull String definitionKey) {
        return getContextDefinition(definitionKey, false);
    }

    @Nullable
    public ContextDefinition<?> getContextDefinition(@NotNull String definitionKey, boolean allowFallbacks) {
        ContextDefinition<?> ret = this.contextTypes.get(definitionKey);
        if (ret == null && allowFallbacks) {
            ContextDefinition<?> fallback = new FallbackContextDefinition(definitionKey);
            ret = this.contextTypes.putIfAbsent(definitionKey, fallback);
            if (ret == null) {
                ret = fallback;
            }
        }
        return ret;
    }
}
