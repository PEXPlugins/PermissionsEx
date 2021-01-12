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
package ca.stellardrift.permissionsex.impl;

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.context.ContextDefinitionProvider;
import ca.stellardrift.permissionsex.context.ContextInheritance;
import ca.stellardrift.permissionsex.context.SimpleContextDefinition;
import ca.stellardrift.permissionsex.datastore.ConversionResult;
import ca.stellardrift.permissionsex.datastore.DataStore;
import ca.stellardrift.permissionsex.datastore.DataStoreContext;
import ca.stellardrift.permissionsex.datastore.DataStoreFactory;
import ca.stellardrift.permissionsex.datastore.ProtoDataStore;
import ca.stellardrift.permissionsex.impl.backend.memory.MemoryDataStore;
import ca.stellardrift.permissionsex.impl.config.PermissionsExConfiguration;
import ca.stellardrift.permissionsex.exception.PEBKACException;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.impl.context.PEXContextDefinition;
import ca.stellardrift.permissionsex.impl.context.ServerTagContextDefinition;
import ca.stellardrift.permissionsex.impl.context.TimeContextDefinition;
import ca.stellardrift.permissionsex.impl.util.CacheListenerHolder;
import ca.stellardrift.permissionsex.impl.rank.RankLadderCache;
import ca.stellardrift.permissionsex.impl.subject.SubjectDataCacheImpl;
import ca.stellardrift.permissionsex.impl.subject.ToDataSubjectRefImpl;
import ca.stellardrift.permissionsex.impl.logging.DebugPermissionCheckNotifier;
import ca.stellardrift.permissionsex.impl.subject.LazySubjectRef;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.logging.PermissionCheckNotifier;
import ca.stellardrift.permissionsex.impl.logging.RecordingPermissionCheckNotifier;
import ca.stellardrift.permissionsex.logging.FormattedLogger;
import ca.stellardrift.permissionsex.impl.logging.WrappingFormattedLogger;
import ca.stellardrift.permissionsex.impl.subject.CalculatedSubjectImpl;
import ca.stellardrift.permissionsex.rank.RankLadderCollection;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.impl.subject.SubjectTypeCollectionImpl;
import ca.stellardrift.permissionsex.impl.util.Util;
import ca.stellardrift.permissionsex.subject.SubjectTypeCollection;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ca.stellardrift.permissionsex.impl.Messages.*;
import static java.util.Objects.requireNonNull;


/**
 * The entry point to the PermissionsEx engine.
 *
 * <p>The fastest way to get going with working with subjects is to access a subject type collection
 * with {@link #subjects(SubjectType)} and request a {@link CalculatedSubjectImpl} to query data
 * from. Directly working with {@link ToDataSubjectRefImpl}s is another option, preferable if most
 * of the operations being performed are writes, or querying data directly defined on a subject.</p>
 *
 * <p>Keep in mind most of PEX's core data objects are immutable and must be resubmitted to their
 * holders to apply updates. Most write operations are done asynchronously, and futures are returned
 * that complete when the backend is finished writing out data. For larger operations, it can be
 * useful to perform changes within {@link #performBulkOperation(Supplier)}, which will reduce
 * unnecessary writes to the backing data store in some cases.</p>
 */
public class PermissionsEx<P> implements Consumer<ContextInheritance>,
        ContextDefinitionProvider,
        PermissionsEngine,
        DataStoreContext {

    // Mechanics
    private final FormattedLogger logger;
    private final ImplementationInterface impl;
    private final MemoryDataStore transientData;
    private final SubjectType<SubjectType<?>> defaultsType;
    private final SubjectType<SubjectType<?>> fallbacksType;

    // Caches
    private final ConcurrentMap<String, SubjectTypeCollectionImpl<?>> subjectTypeCache = new ConcurrentHashMap<>();
    private @MonotonicNonNull RankLadderCache rankLadderCache;
    private volatile @Nullable CompletableFuture<ContextInheritance> cachedInheritance;
    private final CacheListenerHolder<Boolean, ContextInheritance> cachedInheritanceListeners = new CacheListenerHolder<>();

    // Mutable state
    private final RecordingPermissionCheckNotifier baseNotifier = new RecordingPermissionCheckNotifier();
    private volatile PermissionCheckNotifier notifier = baseNotifier;
    private final ConcurrentMap<String, ContextDefinition<?>> contextTypes = new ConcurrentHashMap<>();
    private final AtomicReference<@Nullable State<P>> state = new AtomicReference<>();

    private static class State<P> {
        private final PermissionsExConfiguration<P> config;
        private final DataStore activeDataStore;
        private PVector<ConversionResult> availableConversions = TreePVector.empty();

        private State(PermissionsExConfiguration<P> config, DataStore activeDataStore) {
            this.config = config;
            this.activeDataStore = activeDataStore;
        }
    }

    public PermissionsEx(final PermissionsExConfiguration<P> config, ImplementationInterface impl) throws PermissionsLoadingException {
        this.impl = impl;
        this.logger = WrappingFormattedLogger.of(impl.logger(), false);
        this.registerContextDefinitions(
                ServerTagContextDefinition.INSTANCE,
                TimeContextDefinition.BEFORE_TIME,
                TimeContextDefinition.AFTER_TIME);
        this.defaultsType = this.subjectTypeBuilder("default")
            .transientHasPriority(false)
            .build();
        this.fallbacksType = this.subjectTypeBuilder("fallback").build();
        this.transientData = (MemoryDataStore) MemoryDataStore.create("transient").defrost(this);
        this.debugMode(config.isDebugEnabled());

        this.initialize(config);

        this.registerSubjectTypes(
            this.defaultsType,
            this.fallbacksType
        );
    }

    private SubjectType.Builder<SubjectType<?>> subjectTypeBuilder(final String id) {
        return  SubjectType.builder(id, new TypeToken<SubjectType<?>>() {})
            .serializedBy(SubjectType::name)
            .deserializedBy(name -> this.subjectTypeCache.get(name).type());
    }

    private State<P> state() throws IllegalStateException {
        final @Nullable State<P> ret = this.state.get();
        if (ret == null) {
            throw new IllegalStateException("Manager has already been closed!");
        }
        return ret;
    }

    @Override
    public SubjectTypeCollection<SubjectType<?>> defaults() {
        return this.subjects(this.defaultsType);
    }

    public SubjectType<SubjectType<?>> defaultsType() {
        return this.defaultsType;
    }

    @Override
    public SubjectTypeCollection<SubjectType<?>> fallbacks() {
        return this.subjects(this.fallbacksType);
    }

    public SubjectType<SubjectType<?>> fallbacksType() {
        return this.fallbacksType;
    }

    @Override
    public void registerSubjectTypes(final SubjectType<?>... types) {
        for (final SubjectType<?> type : types) {
            this.subjects(type);
        }
    }

    /**
     * Get the collection of subjects of a given type. No data is loaded in this operation.
     * Any string is supported as a subject type, but some common types have been provided as constants
     * in this class for convenience.
     *
     * @see PermissionsEngine#defaults()
     * @param type The type identifier requested. Can be any string
     * @return The subject type collection
     */
    @Override
    public <I> SubjectTypeCollectionImpl<I> subjects(final SubjectType<I> type) {
        @SuppressWarnings("unchecked")
        final SubjectTypeCollectionImpl<I> collection = (SubjectTypeCollectionImpl<I>) this.subjectTypeCache.computeIfAbsent(type.name(),
            key -> {
                final SubjectRef<SubjectType<?>> defaultIdentifier = SubjectRef.subject(this.defaultsType, type);
                return new SubjectTypeCollectionImpl<>(
                    this,
                    type,
                    new SubjectDataCacheImpl<>(type, defaultIdentifier, state().activeDataStore),
                    new SubjectDataCacheImpl<>(type, defaultIdentifier, transientData));
            });

        if (!type.equals(collection.type())) {
            throw new IllegalArgumentException("Provided subject type " + type + " is different from registered type " + collection.type());
        }
        return collection;
    }

    public SubjectType<?> subjectType(final String id) {
        return this.subjectTypeCache.get(id).type();
    }

    /**
     * Get a view of the currently cached subject types
     *
     * @return Unmodifiable view of the currently cached subject types
     */
    @Override
    public Collection<SubjectTypeCollectionImpl<?>> loadedSubjectTypes() {
        return Collections.unmodifiableCollection(this.subjectTypeCache.values());
    }

    /**
     * Get all registered subject types in the active data store.
     * The set is an immutable copy of the backend data.
     *
     * @return A set of registered subject types
     */
    @Override
    public Set<SubjectType<?>> knownSubjectTypes() {
        return this.subjectTypeCache.values().stream().map(SubjectTypeCollectionImpl::type).collect(Collectors.toSet());
    }

    // -- DataStoreContext -- //

    @Override
    public PermissionsEngine engine() {
        return this;
    }

    @Override
    public SubjectRef<?> deserializeSubjectRef(final String type, final String name) {
        final @Nullable SubjectTypeCollectionImpl<?> existingCollection = this.subjectTypeCache.get(type);
        if (existingCollection == null) {
            throw new IllegalArgumentException("Unknown subject type " + type);
        }
        return deserialize(existingCollection.type(), name);
    }

    @Override
    public SubjectRef<?> lazySubjectRef(String type, String identifier) {
        return new LazySubjectRef(
                this,
                requireNonNull(type, "type"),
                requireNonNull(identifier, "identifier")
        );
    }

    private <I> SubjectRef<I> deserialize(final SubjectType<I> type, final String serializedIdent) {
        return SubjectRef.subject(type, type.parseIdentifier(serializedIdent));
    }

    @Override
    public <V> CompletableFuture<V> doBulkOperation(Function<DataStore, CompletableFuture<V>> actor) {
        return this.state().activeDataStore.performBulkOperation(actor).thenCompose(it -> it);
    }

    /**
     * Suppress writes to the data store for the duration of a specific operation. Only really useful for extremely large operations
     *
     * @param func The operation to perform
     * @param <T> The type of data that will be returned
     * @return A future that completes once all data has been written to the store
     */
    public <T> CompletableFuture<T> performBulkOperation(Supplier<CompletableFuture<T>> func) {
        return state().activeDataStore.performBulkOperation(store -> func.get().join());
    }

    /**
     * Access rank ladders through a cached interface
     *
     * @return Access to rank ladders
     */
    @Override
    public RankLadderCollection ladders() {
        return this.rankLadderCache;
    }

    /**
     * Imports data into the currently active backend from another configured backend.
     *
     * @param dataStoreIdentifier The identifier of the backend to import from
     * @return A future that completes once the import operation is complete
     */
    public CompletableFuture<?> importDataFrom(String dataStoreIdentifier) {
        final State<P> state = state();
        final @Nullable ProtoDataStore<?> expected = state.config.getDataStore(dataStoreIdentifier);
        if (expected == null) {
            return Util.failedFuture(new IllegalArgumentException("Data store " + dataStoreIdentifier + " is not present"));
        }
        return importDataFrom(expected);
    }

    public CompletableFuture<?> importDataFrom(ConversionResult conversion) {
        return importDataFrom(conversion.store());
    }

    private CompletableFuture<?> importDataFrom(final ProtoDataStore<?> request) {
        final State<P> state = state();
        final DataStore expected;
        try {
            expected = request.defrost(this);
        } catch (PermissionsLoadingException e) {
            return Util.failedFuture(e);
        }

        return state.activeDataStore.performBulkOperation(store -> {
            CompletableFuture<?> result = CompletableFuture.allOf(expected.getAll().map(subject -> store.setData(subject.getKey(), subject.getValue())).toArray(CompletableFuture[]::new)); // subjects
            result = result.thenCombine(expected.getContextInheritance(null).thenCompose(store::setContextInheritance), (a, b) -> a); // context inheritance
            result = expected.getAllRankLadders()
                    .map(ladder -> expected.getRankLadder(ladder, null).thenCompose(ladderData -> store.setRankLadder(ladder, ladderData))) // combine all rank ladder futures
                    .reduce(result, (existing, next) -> existing.thenCombine(next, (v, a) -> null), (one, two) -> one.thenCombine(two, (v, a) -> null));
            return result;
        }).thenCompose(x -> x);
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
    @Override
    public boolean debugMode() {
        return this.getNotifier() instanceof DebugPermissionCheckNotifier;
    }

    /**
     * Set whether or not debug mode is enabled. Debug mode logs all permission, option, and inheritance
     * checks made to the console.
     *
     * @param debug Whether to enable debug mode
     * @param filterPattern A pattern to filter which permissions are logged. Null for no filter.
     */
    @Override
    public synchronized void debugMode(boolean debug, final @Nullable Pattern filterPattern) {
        if (debug) {
            if (this.notifier instanceof DebugPermissionCheckNotifier) {
                this.notifier = new DebugPermissionCheckNotifier(this.logger(), ((DebugPermissionCheckNotifier) this.notifier).getDelegate(), filterPattern == null ? null : perm -> filterPattern.matcher(perm).find());
            } else {
                this.notifier = new DebugPermissionCheckNotifier(this.logger(), this.notifier, filterPattern == null ? null : perm -> filterPattern.matcher(perm).find());
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
            PermissionsExConfiguration<P> config = state().config.reload();
            config.validate();
            initialize(config);
            // TODO: Throw reload event to cache any relevant subject types
        } catch (IOException e) {
            throw new PEBKACException(CONFIG_ERROR_LOAD.tr(e.getLocalizedMessage()));
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
    private void initialize(final PermissionsExConfiguration<P> config) throws PermissionsLoadingException {
        final DataStore newStore = config.getDefaultDataStore().defrost(this);
        State<P> newState = new State<>(config, newStore);
        boolean shouldAnnounceImports = newState.activeDataStore.firstRun();
        try {
            newState.config.save();
        } catch (IOException e) {
            throw new PermissionsLoadingException(CONFIG_ERROR_SAVE.tr(), e);
        }

        if (shouldAnnounceImports) {
            this.logger().warn(CONVERSION_BANNER.tr());
        }

        PVector<ConversionResult> allResults = TreePVector.empty();
        for (final DataStoreFactory<?> convertable : DataStoreFactory.all().values()) {
            if (!(convertable instanceof DataStoreFactory.Convertable))  {
                continue;
            }
            final DataStoreFactory.Convertable<?> prov = ((DataStoreFactory.Convertable<?>) convertable);

            List<ConversionResult> res = prov.listConversionOptions(this);
            if (!res.isEmpty()) {
                if (shouldAnnounceImports) {
                    this.logger().info(CONVERSION_PLUGINHEADER.tr(prov.friendlyName()));
                    for (ConversionResult result : res) {
                        this.logger().info(CONVERSION_INSTANCE.tr(result.description(), result.store().identifier()));
                    }
                }
                allResults = allResults.plusAll(res);
            }
        }
        newState.availableConversions = allResults;

        final @Nullable State<P> oldState = this.state.getAndSet(newState);
        if (oldState != null) {
            try {
                oldState.activeDataStore.close();
            } catch (final Exception ignore) {} // TODO maybe warn?
        }

        this.rankLadderCache = new RankLadderCache(this.rankLadderCache, newState.activeDataStore);
        this.subjectTypeCache.forEach((key, val) -> val.update(newState.activeDataStore));
        this.contextTypes.values().forEach(ctxDef -> {
            if (ctxDef instanceof PEXContextDefinition<?>) {
                ((PEXContextDefinition<?>) ctxDef).update(newState.config);
            }
        });
        if (this.cachedInheritance != null) {
            this.cachedInheritance = null;
            contextInheritance((Consumer<ContextInheritance>) null).thenAccept(inheritance -> this.cachedInheritanceListeners.call(true, inheritance));
        }

        // Migrate over legacy subject data
        newState.activeDataStore.moveData("system", this.defaultsType.name(), this.defaultsType.name(), this.defaultsType.name()).thenRun(() -> {
            this.logger().info(CONVERSION_RESULT_SUCCESS.tr());
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
        }, this.asyncExecutor());
    }

    /**
     * Shut down the PEX engine. Once this has been done, no further action can be taken
     * until the engine is reinitialized with a fresh configuration.
     */
    public void close() {
        final @Nullable State<P> state = this.state.getAndSet(null);
        if (state != null) {
            state.activeDataStore.close();
        }
    }

    public List<ConversionResult> getAvailableConversions() {
        return state().availableConversions;
    }

    @Override
    public FormattedLogger logger() {
        return this.logger;
    }

    // -- Implementation interface proxy methods --

    @Override
    public Path baseDirectory() {
        return impl.baseDirectory();
    }

    public Path baseDirectory(BaseDirectoryScope scope) {
        return impl.baseDirectory(scope);
    }

    @Override
    @Deprecated
    public @Nullable DataSource dataSourceForUrl(String url) throws SQLException {
        return impl.dataSourceForUrl(url);
    }

    /**
     * Get an executor to run tasks asynchronously on.
     *
     * @return The async executor
     */
    @Override
    public Executor asyncExecutor() {
        return this.impl.asyncExecutor();
    }

    public String version() {
        return this.impl.version();
    }

    /**
     * Get the current configuration PEX is operating with. This object is immutable.
     *
     * @return The current configuration object
     */
    public PermissionsExConfiguration<P> config() {
        return state().config;
    }

    public DataStore activeDataStore() {
        return state().activeDataStore;
    }

    /**
     * Get context inheritance data.
     *
     * <p>The result of the future is immutable -- to take effect, the object returned by any
     * update methods in {@link ContextInheritance} must be passed to {@link #contextInheritance(ContextInheritance)}.
     *  It follows that anybody else's changes will not appear in the returned inheritance object -- so if updates are
     *  desired providing a callback function is important.</p>
     *
     * @param listener A callback function that will be triggered whenever there is a change to the context inheritance
     * @return A future providing the current context inheritance data
     */
    @Override
    public CompletableFuture<ContextInheritance> contextInheritance(final @Nullable Consumer<ContextInheritance> listener) {
        if (this.cachedInheritance == null) {
            this.cachedInheritance = state().activeDataStore.getContextInheritance(this);
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
    @Override
    public CompletableFuture<ContextInheritance> contextInheritance(ContextInheritance newInheritance) {
        return state().activeDataStore.setContextInheritance(newInheritance);
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

    @Override
    public CompletableFuture<Set<ContextDefinition<?>>> usedContextTypes() {
        return state().activeDataStore.getDefinedContextKeys().thenCombine(transientData.getDefinedContextKeys(), (persist, trans) -> {
            final Set<ContextDefinition<?>> build = new HashSet<>();
            for (final ContextDefinition<?> def : this.contextTypes.values()) {
                if (persist.contains(def.name()) || trans.contains(def.name())) {
                    build.add(def);
                }
            }
           return Collections.unmodifiableSet(build);
        });
    }

    @Override
    public <T> boolean registerContextDefinition(ContextDefinition<T> contextDefinition) {
        if (contextDefinition instanceof PEXContextDefinition<?> && this.state.get() != null) {
            ((PEXContextDefinition<T>) contextDefinition).update(config());
        }
       final @Nullable ContextDefinition<?> possibleOut =  this.contextTypes.putIfAbsent(contextDefinition.name(), contextDefinition);
        if (possibleOut instanceof SimpleContextDefinition.Fallback) {
            return this.contextTypes.replace(contextDefinition.name(), possibleOut, contextDefinition);
        } else {
            return possibleOut == null;
        }
    }

    @Override
    public int registerContextDefinitions(ContextDefinition<?>... definitions) {
        int numRegistered = 0;
        for (ContextDefinition<?> def : definitions) {
            if (registerContextDefinition(def)) {
                numRegistered++;
            }
        }
        return numRegistered;
    }

    @Override
    public List<ContextDefinition<?>> registeredContextTypes() {
        return PCollections.asVector(this.contextTypes.values());
    }

    @Override
    public @Nullable ContextDefinition<?> contextDefinition(final String definitionKey, final boolean allowFallbacks) {
        @Nullable ContextDefinition<?> ret = this.contextTypes.get(definitionKey);
        if (ret == null && allowFallbacks) {
            ContextDefinition<?> fallback = new SimpleContextDefinition.Fallback(definitionKey);
            ret = this.contextTypes.putIfAbsent(definitionKey, fallback);
            if (ret == null) {
                ret = fallback;
            }
        }
        return ret;
    }
}
