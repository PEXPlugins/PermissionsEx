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
import ca.stellardrift.permissionsex.commands.CallbackController;
import ca.stellardrift.permissionsex.config.PermissionsExConfiguration;
import ca.stellardrift.permissionsex.context.*;
import ca.stellardrift.permissionsex.data.*;
import ca.stellardrift.permissionsex.exception.PEBKACException;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.logging.DebugPermissionCheckNotifier;
import ca.stellardrift.permissionsex.logging.PermissionCheckNotifier;
import ca.stellardrift.permissionsex.logging.RecordingPermissionCheckNotifier;
import ca.stellardrift.permissionsex.logging.FormattedLogger;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.subject.SubjectTypeDefinitionKt;
import ca.stellardrift.permissionsex.util.Util;
import ca.stellardrift.permissionsex.commands.parse.CommandSpec;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static ca.stellardrift.permissionsex.Messages.*;
import static ca.stellardrift.permissionsex.commands.PermissionsExCommandsKt.createRootCommand;
import static ca.stellardrift.permissionsex.commands.RankingCommandsKt.getDemoteCommand;
import static ca.stellardrift.permissionsex.commands.RankingCommandsKt.getPromoteCommand;


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
public class PermissionsEx<P> implements ImplementationInterface, Consumer<ContextInheritance> {
    public static final String SUBJECTS_USER = "user";
    public static final String SUBJECTS_GROUP = "group";
    public static final String SUBJECTS_DEFAULTS = "default";
    public static final String SUBJECTS_FALLBACK = "fallback";
    public static final Set<ContextValue<?>> GLOBAL_CONTEXT = Collections.emptySet();

    private final FormattedLogger logger;
    private final ImplementationInterface impl;
    private final MemoryDataStore transientData;
    private final RecordingPermissionCheckNotifier baseNotifier = new RecordingPermissionCheckNotifier();
    private volatile PermissionCheckNotifier notifier = baseNotifier;
    private final ConcurrentMap<String, ContextDefinition<?>> contextTypes = new ConcurrentHashMap<>();

    private final AtomicReference<State<P>> state = new AtomicReference<>();
    private final ConcurrentMap<String, SubjectType> subjectTypeCache = new ConcurrentHashMap<>();
    private RankLadderCache rankLadderCache;
    private volatile CompletableFuture<ContextInheritance> cachedInheritance;
    private final CacheListenerHolder<Boolean, ContextInheritance> cachedInheritanceListeners = new CacheListenerHolder<>();
    private final CallbackController callbackController;

    private static class State<P> {
        private final PermissionsExConfiguration<P> config;
        private final DataStore activeDataStore;
        private List<ConversionResult> availableConversions = Collections.emptyList();

        private State(PermissionsExConfiguration<P> config, DataStore activeDataStore) {
            this.config = config;
            this.activeDataStore = activeDataStore;
        }
    }

    public PermissionsEx(final PermissionsExConfiguration<P> config, ImplementationInterface impl) throws PermissionsLoadingException {
        this.impl = impl;
        this.logger = FormattedLogger.forLogger(impl.getLogger(), false);
        this.transientData = new MemoryDataStore("transient");
        this.transientData.initialize(this);
        this.callbackController = new CallbackController();
        setDebugMode(config.isDebugEnabled());
        registerContextDefinition(ServerTagContextDefinition.INSTANCE);
        registerContextDefinition(BeforeTimeContextDefinition.INSTANCE);
        registerContextDefinition(AfterTimeContextDefinition.INSTANCE);
        initialize(config);

        getSubjects(SUBJECTS_DEFAULTS).setTypeInfo(SubjectTypeDefinitionKt.subjectType(SUBJECTS_DEFAULTS, false));
    }

    /**
     * Pass all PermissionsEx commands to the provided consumer for registration.
     *
     * @param consumer Consumer to receive each command registration
     */
    public void registerCommandsTo(final Consumer<CommandSpec> consumer) {
        consumer.accept(createRootCommand(this));
        consumer.accept(getPromoteCommand(this));
        consumer.accept(getDemoteCommand(this));
    }

    private State<P> getState() throws IllegalStateException {
        State<P> ret = this.state.get();
        if (ret == null) {
            throw new IllegalStateException("Manager has already been closed!");
        }
        return ret;
    }


    /**
     * Get the collection of subjects of a given type. No data is loaded in this operation.
     * Any string is supported as a subject type, but some common types have been provided as constants
     * in this class for convenience.
     *
     * @see #SUBJECTS_DEFAULTS
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
    public Set<String> getRegisteredSubjectTypes() {
        return getState().activeDataStore.getRegisteredTypes();
    }

    /**
     * Suppress writes to the data store for the duration of a specific operation. Only really useful for extremely large operations
     *
     * @param func The operation to perform
     * @param <T> The type of data that will be returned
     * @return A future that completes once all data has been written to the store
     */
    public <T> CompletableFuture<T> performBulkOperation(Supplier<CompletableFuture<T>> func) {
        return getState().activeDataStore.performBulkOperation(store -> func.get().join());
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
    public CompletableFuture<Void> importDataFrom(String dataStoreIdentifier) {
        final State<P> state = getState();
        final DataStore expected = state.config.getDataStore(dataStoreIdentifier);
        if (expected == null) {
            return Util.failedFuture(new IllegalArgumentException("Data store " + dataStoreIdentifier + " is not present"));
        }
        return importDataFrom(expected);
    }

    public CompletableFuture<Void> importDataFrom(ConversionResult conversion) {
        return importDataFrom(conversion.getStore());
    }

    private CompletableFuture<Void> importDataFrom(DataStore expected) {
        final State<P> state = getState();
        try {
            expected.initialize(this);
        } catch (PermissionsLoadingException e) {
            return Util.failedFuture(e);
        }

        return state.activeDataStore.performBulkOperation(store -> {
            CompletableFuture<Void> ret = CompletableFuture.allOf(Iterables.toArray(Iterables.transform(expected.getAll(),
                    input -> store.setData(input.getKey().getKey(), input.getKey().getValue(), input.getValue())), CompletableFuture.class))
                    .thenCombine(expected.getContextInheritance(null).thenCompose(store::setContextInheritance), (v, a) -> null);
            for (String ladder : expected.getAllRankLadders()) {
                ret = ret.thenCombine(expected.getRankLadder(ladder, null).thenCompose(ladderObj -> store.setRankLadder(ladder, ladderObj)), (v, a) -> null);
            }
            return ret;
        }).thenCompose(val -> Util.failableFuture(val::get));
    }

    /**
     * Get the currently active notifier. This object has callbacks triggered on every permission check
     *
     * @return The active notifier
     */
    public PermissionCheckNotifier getNotifier() {
        return this.notifier;
    }

    /**
     * Get the base notifier that logs any permission checks that gave taken place.
     * @return the notifier, even if not active
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
    public synchronized void setDebugMode(boolean debug, final @Nullable Pattern filterPattern) {
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
            PermissionsExConfiguration<P> config = getState().config.reload();
            config.validate();
            initialize(config);
            // TODO: Throw reload event to cache any relevant subject types
        } catch (IOException e) {
            throw new PEBKACException(CONFIG_ERROR_LOAD.toComponent(e.getLocalizedMessage()));
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
    private void initialize(PermissionsExConfiguration<P> config) throws PermissionsLoadingException {
        State<P> newState = new State<>(config, config.getDefaultDataStore());
        boolean shouldAnnounceImports = !newState.activeDataStore.initialize(this);
        try {
            newState.config.save();
        } catch (IOException e) {
            throw new PermissionsLoadingException(CONFIG_ERROR_SAVE.toComponent(), e);
        }

        if (shouldAnnounceImports) {
            getLogger().warn(CONVERSION_BANNER.toComponent());
        }

        List<ConversionResult> allResults = new LinkedList<>();
        for (ConversionProvider prov : ConversionProviderRegistry.INSTANCE.getAllProviders()) {
            List<ConversionResult> res = prov.listConversionOptions(this);
            if (!res.isEmpty()) {
                if (shouldAnnounceImports) {
                    getLogger().info(CONVERSION_PLUGINHEADER.toComponent(prov.getName()));
                    for (ConversionResult result : res) {
                        getLogger().info(CONVERSION_INSTANCE.toComponent(result.getTitle(), result.getStore().getName()));
                    }
                }
                allResults.addAll(res);
            }
        }
        newState.availableConversions = ImmutableList.copyOf(allResults);

        State<P> oldState = this.state.getAndSet(newState);
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
        getSubjects("group").cacheAll();
        if (this.cachedInheritance != null) {
            this.cachedInheritance = null;
            getContextInheritance(null).thenAccept(inheritance -> this.cachedInheritanceListeners.call(true, inheritance));
        }

        // Migrate over legacy subject data
        newState.activeDataStore.moveData("system", SUBJECTS_DEFAULTS, SUBJECTS_DEFAULTS, SUBJECTS_DEFAULTS).thenRun(() -> {
            getLogger().info(CONVERSION_RESULT_SUCCESS.toComponent());
        });
    }

    /**
     * Reload the configuration file in use and refresh backend data
     *
     * @return A future that completes once a reload has finished
     */
    public CompletableFuture<Void> reload() {
        return Util.asyncFailableFuture(() -> {
            reloadSync();
            return null;
        }, getAsyncExecutor());
    }

    /**
     * Shut down the PEX engine. Once this has been done, no further action can be taken
     * until the engine is reinitialized with a fresh configuration.
     */
    public void close() {
        State<P> state = this.state.getAndSet(null);
        state.activeDataStore.close();
    }

    public List<ConversionResult> getAvailableConversions() {
        return getState().availableConversions;
    }

    /**
     * Get the command callback controller for this permissions instance
     *
     * @return The callback controller
     */
    public CallbackController getCallbackController() {
        return this.callbackController;
    }

    @Override
    public FormattedLogger getLogger() {
        return this.logger;
    }

    // -- Implementation interface proxy methods --

    @Override
    public Path getBaseDirectory() {
        return impl.getBaseDirectory();
    }

    @Override
    public Path getBaseDirectory(BaseDirectoryScope scope) {
        return impl.getBaseDirectory(scope);
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
    public Set<CommandSpec> getImplementationSubcommands() {
        return impl.getImplementationSubcommands();
    }

    @Override
    public String getVersion() {
        return impl.getVersion();
    }

    @Override
    public Map.Entry<String, String> createSubjectIdentifier(String collection, String ident) {
        return impl.createSubjectIdentifier(collection, ident);
    }

    /**
     * Get the current configuration PEX is operating with. This object is immutable.
     *
     * @return The current configuration object
     */
    public PermissionsExConfiguration<P> getConfig() {
        return getState().config;
    }

    /**
     * Get context inheritance data. The result of the future is immutable -- to take effect, the object returned by any
     * update methods in {@link ContextInheritance} must be passed to {@link #setContextInheritance(ContextInheritance)}.
     *  It follows that anybody else's changes will not appear in the returned inheritance object -- so if updates are
     *  desired providing a callback function is important.
     *
     * @param listener A callback function that will be triggered whenever there is a change to the context inheritance
     * @return A future providing the current context inheritance data
     */
    public CompletableFuture<ContextInheritance> getContextInheritance(Consumer<ContextInheritance> listener) {
        if (this.cachedInheritance == null) {
            this.cachedInheritance = getState().activeDataStore.getContextInheritance(this);
        }
        if (listener != null) {
            this.cachedInheritanceListeners.addListener(true, listener);
        }
        return this.cachedInheritance;

    }

    /**
     * Update the context inheritance when values have been changed
     *
     * @param newInheritance The modified inheritance object
     * @return A future containing the latest context inheritance object
     */
    public CompletableFuture<ContextInheritance> setContextInheritance(ContextInheritance newInheritance) {
        return getState().activeDataStore.setContextInheritance(newInheritance);
    }

    /**
     * Listener method that handles changes to context inheritance. Should not be called by outside users
     *
     * @param newData The new data to replace cached information
     */
    @Override
    public void accept(ContextInheritance newData) {
        this.cachedInheritance = CompletableFuture.completedFuture(newData);
        this.cachedInheritanceListeners.call(true, newData);
    }

    public CompletableFuture<Set<ContextDefinition<?>>> getUsedContextTypes() {
        ImmutableSet.Builder<ContextDefinition<?>> build = ImmutableSet.builder();
        return getState().activeDataStore.getDefinedContextKeys().thenCombine(transientData.getDefinedContextKeys(), (persist, trans) -> {
            for (ContextDefinition<?> def : this.contextTypes.values()) {
                if (persist.contains(def.getName()) || trans.contains(def.getName())) {
                    build.add(def);
                }
            }
           return build.build();
        });
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
