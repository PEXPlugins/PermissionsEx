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
package ninja.leaping.permissionsex.backend;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.CacheListenerHolder;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.data.ContextInheritance;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.rank.RankLadder;
import ninja.leaping.permissionsex.util.ThrowingSupplier;
import ninja.leaping.permissionsex.util.Util;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static ninja.leaping.permissionsex.util.Translations.t;

/**
 * Base implementation of a data store that provides common points for other data stores to hook into.
 */
public abstract class AbstractDataStore implements DataStore {
    private PermissionsEx manager;
    private final Factory factory;
    private final CacheListenerHolder<Map.Entry<String, String>, ImmutableSubjectData> listeners = new CacheListenerHolder<>();
    private final CacheListenerHolder<String, RankLadder> rankLadderListeners = new CacheListenerHolder<>();
    private final CacheListenerHolder<Boolean, ContextInheritance> contextInheritanceListeners = new CacheListenerHolder<>();

    protected AbstractDataStore(Factory factory) {
        if (!factory.expectedClazz.equals(getClass())) {
            throw new ExceptionInInitializerError("Data store factory for wrong class " + factory.expectedClazz + " provided to a " + getClass());
        }
        this.factory = factory;
    }

    protected PermissionsEx getManager() {
        return this.manager;
    }

    @Override
    public final void initialize(PermissionsEx core) throws PermissionsLoadingException {
        this.manager = core;
        initializeInternal();
    }

    protected abstract void initializeInternal() throws PermissionsLoadingException;

    @Override
    public final CompletableFuture<ImmutableSubjectData> getData(String type, String identifier, Caching<ImmutableSubjectData> listener) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(identifier, "identifier");

        CompletableFuture<ImmutableSubjectData> ret = getDataInternal(type, identifier);
        ret.thenRun(() -> {
            if (listener != null) {
                listeners.addListener(Maps.immutableEntry(type, identifier), listener);
            }
        });
        return ret;
    }

    @Override
    public final CompletableFuture<ImmutableSubjectData> setData(String type, String identifier, ImmutableSubjectData data) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(identifier, "identifier");

        final Map.Entry<String, String> lookupKey = Maps.immutableEntry(type, identifier);
        return setDataInternal(type, identifier, data)
                .thenApply(newData -> {
                    if (newData != null) {
                        listeners.call(lookupKey, newData);
                    }
                    return newData;
                });
    }

    protected <T> CompletableFuture<T> runAsync(ThrowingSupplier<T, ?> supplier) {
        return Util.asyncFailableFuture(supplier, getManager().getAsyncExecutor());
    }

    protected CompletableFuture<Void> runAsync(Runnable run) {
        return CompletableFuture.runAsync(run, getManager().getAsyncExecutor());
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
        getData(PermissionsEx.SUBJECTS_DEFAULTS, PermissionsEx.SUBJECTS_DEFAULTS, null)
                .thenApply(data -> data.setDefaultValue(ImmutableSet.of(Maps.immutableEntry("localip", "127.0.0.1")), 1))
                .thenCompose(data -> setData(PermissionsEx.SUBJECTS_DEFAULTS, PermissionsEx.SUBJECTS_DEFAULTS, data));
    }

    protected abstract CompletableFuture<ImmutableSubjectData> getDataInternal(String type, String identifier);

    protected abstract CompletableFuture<ImmutableSubjectData> setDataInternal(String type, String identifier, ImmutableSubjectData data);

    @Override
    public final Iterable<Map.Entry<String, ImmutableSubjectData>> getAll(final String type) {
        Objects.requireNonNull(type, "type");
        return Iterables.transform(getAllIdentifiers(type),
                input -> Maps.immutableEntry(input, Futures.getUnchecked(getData(type, input, null))));
    }

    @Override
    public final <T> CompletableFuture<T> performBulkOperation(final Function<DataStore, T> function) {
        return Util.asyncFailableFuture(() -> performBulkOperationSync(function), getManager().getAsyncExecutor());
    }

    @Override
    public final CompletableFuture<RankLadder> getRankLadder(String ladderName, Caching<RankLadder> listener) {
        Objects.requireNonNull(ladderName, "ladderName");
        CompletableFuture<RankLadder> ladder = getRankLadderInternal(ladderName);
        if (listener != null) {
            rankLadderListeners.addListener(ladderName.toLowerCase(), listener);
        }
        return ladder;
    }

    @Override
    public final CompletableFuture<RankLadder> setRankLadder(final String identifier, RankLadder ladder) {
        return setRankLadderInternal(identifier, ladder)
                .thenApply(newData -> {
                    if (newData != null) {
                        rankLadderListeners.call(identifier, newData);
                    }
                    return newData;
                });
    }


    protected abstract CompletableFuture<RankLadder> getRankLadderInternal(String ladder);
    protected abstract CompletableFuture<RankLadder> setRankLadderInternal(String ladder, RankLadder newLadder);

    @Override
    public final CompletableFuture<ContextInheritance> getContextInheritance(Caching<ContextInheritance> listener) {
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
     * Internally perform a bulk operation. Safe to call blocking operations from this method -- we're running it asyncly.
     *
     * @param function The function to run
     * @param <T> The
     * @return
     * @throws Exception
     */
    protected abstract <T> T performBulkOperationSync(Function<DataStore, T> function) throws Exception;

    @Override
    @SuppressWarnings("unchecked") // Correct types are verified in the constructor
    public String serialize(ConfigurationNode node) throws PermissionsLoadingException {
        Objects.requireNonNull(node, "node");
        try {
            ((ObjectMapper) factory.mapper).bind(this).serialize(node);
        } catch (ObjectMappingException e) {
            throw new PermissionsLoadingException(t("Error while serializing backend %s", node.getKey()), e);
        }
        return factory.type;
    }

    protected static class Factory implements DataStoreFactory {
        private final String type;
        private final Class<? extends AbstractDataStore> expectedClazz;
        private final ObjectMapper<? extends AbstractDataStore> mapper;

        public Factory(final String type, Class<? extends AbstractDataStore> clazz) {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(clazz, "clazz");
            this.type = type;
            this.expectedClazz = clazz;
            try {
                mapper = ObjectMapper.forClass(clazz);
            } catch (ObjectMappingException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        @Override
        public DataStore createDataStore(String identifier, ConfigurationNode config) throws PermissionsLoadingException {
            try {
                return mapper.bindToNew().populate(config);
            } catch (ObjectMappingException e) {
                throw new PermissionsLoadingException(t("Error while deserializing backend %s", identifier), e);
            }
        }
    }
}
