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
package ca.stellardrift.permissionsex.impl.backend;

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.datastore.DataStoreContext;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.impl.util.CacheListenerHolder;
import ca.stellardrift.permissionsex.exception.PermissionsException;
import ca.stellardrift.permissionsex.context.ContextInheritance;
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData;
import ca.stellardrift.permissionsex.datastore.DataStore;
import ca.stellardrift.permissionsex.datastore.DataStoreFactory;
import ca.stellardrift.permissionsex.datastore.ProtoDataStore;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.rank.RankLadder;
import ca.stellardrift.permissionsex.impl.util.Util;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.util.CheckedSupplier;
import org.spongepowered.configurate.util.UnmodifiableCollections;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Base implementation of a data store that provides common points for other data stores to hook into.
 *
 * @param <T> self type
 * @param <C> config type
 */
public abstract class AbstractDataStore<T extends AbstractDataStore<T, C>, C> implements DataStore {
    private final DataStoreContext context;
    private final ProtoDataStore<C> properties;
    private boolean firstRun;
    protected final CacheListenerHolder<Map.Entry<String, String>, ImmutableSubjectData> listeners = new CacheListenerHolder<>();
    protected final CacheListenerHolder<String, RankLadder> rankLadderListeners = new CacheListenerHolder<>();
    protected final CacheListenerHolder<Boolean, ContextInheritance> contextInheritanceListeners = new CacheListenerHolder<>();

    /**
     * Create the data store.
     *
     * <p>No actual loading or creation of threads should be performed here.</p>
     *
     * @param context the data store context
     * @param props properties defining this data store
     */
    protected AbstractDataStore(final DataStoreContext context, final ProtoDataStore<C> props) {
        this.context = context;
        this.properties = props;
    }

    @Override
    public String name() {
        return this.properties.identifier();
    }

    @Override
    public boolean firstRun() {
        return this.firstRun;
    }

    /**
     * Mark that this is the data store's first run.
     */
    protected void markFirstRun() {
        this.firstRun = true;
    }

    protected final PermissionsEngine engine() {
        return this.context.engine();
    }

    protected final DataStoreContext context() {
        return this.context;
    }

    protected C config() {
        return this.properties.config();
    }

    /**
     * Load any data necessary to initialize this data store.
     *
     * @throws PermissionsLoadingException if unable to load
     */
    protected abstract void load() throws PermissionsLoadingException;

    @Override
    public final CompletableFuture<ImmutableSubjectData> getData(final String type, final String identifier, final @Nullable Consumer<ImmutableSubjectData> listener) {
        requireNonNull(type, "type");
        requireNonNull(identifier, "identifier");

        final CompletableFuture<ImmutableSubjectData> ret = getDataInternal(type, identifier);
        ret.thenRun(() -> {
            if (listener != null) {
                listeners.addListener(UnmodifiableCollections.immutableMapEntry(type, identifier), listener);
            }
        });
        return ret;
    }

    @Override
    public final CompletableFuture<ImmutableSubjectData> setData(final String type, final String identifier, final @Nullable ImmutableSubjectData data) {
        requireNonNull(type, "type");
        requireNonNull(identifier, "identifier");

        final Map.Entry<String, String> lookupKey = UnmodifiableCollections.immutableMapEntry(type, identifier);
        return setDataInternal(type, identifier, data)
                .thenApply(newData -> {
                    if (newData != null) {
                        listeners.call(lookupKey, newData);
                    }
                    return newData;
                });
    }

    protected <V> CompletableFuture<V> runAsync(CheckedSupplier<V, ?> supplier) {
        return Util.asyncFailableFuture(supplier, this.context.asyncExecutor());
    }

    protected CompletableFuture<Void> runAsync(Runnable run) {
        return CompletableFuture.runAsync(run, this.context.asyncExecutor());
    }

    /**
     * Apply default data when creating a new file.
     *
     * This consists of
     * <ul>
     *     <li>Modifying default data to give all permissions to a user connecting locally</li>
     * </ul>
     */
    protected final void applyDefaultData() {
        getData(PermissionsEngine.SUBJECTS_DEFAULTS.name(), PermissionsEngine.SUBJECTS_DEFAULTS.name(), null)
                .thenApply(data -> data.withSegment(Collections.singleton(new ContextValue<>("localip", "127.0.0.1")), s -> s.withFallbackPermission(1)))
                .thenCompose(data -> setData(PermissionsEngine.SUBJECTS_DEFAULTS.name(), PermissionsEngine.SUBJECTS_DEFAULTS.name(), data));
    }

    protected abstract CompletableFuture<ImmutableSubjectData> getDataInternal(String type, String identifier);

    protected abstract CompletableFuture<ImmutableSubjectData> setDataInternal(String type, String identifier, @Nullable ImmutableSubjectData data);

    @Override
    public final Stream<Map.Entry<String, ImmutableSubjectData>> getAll(final String type) {
        requireNonNull(type, "type");
        return getAllIdentifiers(type)
                .map(id -> UnmodifiableCollections.immutableMapEntry(id, getData(type, id, null).join()));
    }

    @Override
    public final <V> CompletableFuture<V> performBulkOperation(final Function<DataStore, V> function) {
        return Util.asyncFailableFuture(() -> performBulkOperationSync(function), this.context.asyncExecutor());
    }

    @Override
    public final CompletableFuture<RankLadder> getRankLadder(final String ladderName, final @Nullable Consumer<RankLadder> listener) {
        requireNonNull(ladderName, "ladderName");
        CompletableFuture<RankLadder> ladder = getRankLadderInternal(ladderName);
        if (listener != null) {
            rankLadderListeners.addListener(ladderName.toLowerCase(), listener);
        }
        return ladder;
    }

    @Override
    public final CompletableFuture<RankLadder> setRankLadder(final String identifier, final @Nullable RankLadder ladder) {
        return setRankLadderInternal(identifier, ladder)
                .thenApply(newData -> {
                    if (newData != null) {
                        rankLadderListeners.call(identifier, newData);
                    }
                    return newData;
                });
    }


    protected abstract CompletableFuture<RankLadder> getRankLadderInternal(String ladder);
    protected abstract CompletableFuture<RankLadder> setRankLadderInternal(String ladder, final @Nullable RankLadder newLadder);

    @Override
    public final CompletableFuture<ContextInheritance> getContextInheritance(final @Nullable Consumer<ContextInheritance> listener) {
        CompletableFuture<ContextInheritance> inheritance = getContextInheritanceInternal();
        if (listener != null) {
            contextInheritanceListeners.addListener(true, listener);
        }
        return inheritance;
    }


    @Override
    public final CompletableFuture<ContextInheritance> setContextInheritance(ContextInheritance contextInheritance) {
        return setContextInheritanceInternal(contextInheritance)
                .thenApply(newData -> {
                    if (newData != null) {
                        contextInheritanceListeners.call(true, newData);
                    }
                    return newData;
                });
    }



    protected abstract CompletableFuture<ContextInheritance> getContextInheritanceInternal();
    protected abstract CompletableFuture<ContextInheritance> setContextInheritanceInternal(ContextInheritance contextInheritance);

    /**
     * Internally perform a bulk operation.
     *
     * <p>Safe to call blocking operations from this method -- we're running it asyncly.</p>
     *
     * @param function The function to run
     * @param <V> The
     * @return result of operation, after a save
     * @throws Exception if thrown by operation
     */
    protected abstract <V> V performBulkOperationSync(Function<DataStore, V> function) throws Exception;

    @Override
    public CompletableFuture<Void> moveData(String oldType, String oldIdentifier, String newType, String newIdentifier) {
        return isRegistered(oldType, oldIdentifier).thenCombine(isRegistered(newType, newIdentifier), (oldRegistered, newRegistered) -> {
            if (oldRegistered && !newRegistered) {
                return getData(oldType, oldIdentifier, null)
                        .thenCompose(oldData -> setData(newType, newIdentifier, oldData))
                        .thenCompose(newData -> setData(oldType, oldIdentifier, null))
                        .thenApply(inp -> (Void) null);
            } else {
                return Util.<Void>failedFuture(new PermissionsException(Messages.DATASTORE_MOVE_ERROR.tr()));
            }

        }).thenCompose(future -> future);
    }

    public abstract static class Factory<T extends AbstractDataStore<T, C>, C> implements DataStoreFactory<C> {
        private final String name;
        private final Class<C> configType;
        /**
         * Function taking the name of a data store instance and its configuration, and creating the appropriate new object.
         */
        private final BiFunction<DataStoreContext, ProtoDataStore<C>, T> newInstanceSupplier;
        private final Component friendlyName;

        protected Factory(final String name, final Class<C> clazz, final BiFunction<DataStoreContext, ProtoDataStore<C>, T> newInstanceSupplier) {
            requireNonNull(name, "name");
            requireNonNull(clazz, "clazz");
            this.name = name;
            this.friendlyName = Component.text(name);
            this.configType = clazz;
            this.newInstanceSupplier = newInstanceSupplier;
        }

        @Override
        public Component friendlyName() {
            return this.friendlyName;
        }

        @Override
        public final String name() {
            return this.name;
        }

        @Override
        public final ProtoDataStore<C> create(String identifier, ConfigurationNode config) throws PermissionsLoadingException {
            try {
                final C dataStoreConfig = config.get(this.configType);
                return ProtoDataStore.of(identifier, dataStoreConfig, this);
            } catch (SerializationException e) {
                throw new PermissionsLoadingException(Messages.DATASTORE_ERROR_DESERIALIZE.tr(identifier), e);
            }
        }

        @Override
        public DataStore defrost(final DataStoreContext context, final ProtoDataStore<C> properties) throws PermissionsLoadingException {
            requireNonNull(context, "context");
            final T store = this.newInstanceSupplier.apply(context, properties);
            store.load();
            return store;
        }

        @Override
        public void serialize(final ConfigurationNode node, final ProtoDataStore<C> protoStore) throws SerializationException {
            node.set(protoStore.config());
        }

    }
}
