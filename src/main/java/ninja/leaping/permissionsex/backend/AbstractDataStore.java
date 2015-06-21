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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.CacheListenerHolder;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.rank.RankLadder;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.Callable;

import static ninja.leaping.permissionsex.util.Translations._;

/**
 * Base implementation of a data store that provides common points for other data stores to hook into.
 */
public abstract class AbstractDataStore implements DataStore {
    private PermissionsEx manager;
    private final Factory factory;
    private final CacheListenerHolder<Map.Entry<String, String>, ImmutableOptionSubjectData> listeners = new CacheListenerHolder<>();
    private final CacheListenerHolder<String, RankLadder> rankLadderListeners = new CacheListenerHolder<>();

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
    public final ImmutableOptionSubjectData getData(String type, String identifier, Caching<ImmutableOptionSubjectData> listener) {
        Preconditions.checkNotNull(type, "type");
        Preconditions.checkNotNull(identifier, "identifier");

        try {
            ImmutableOptionSubjectData ret = getDataInternal(type, identifier);
            if (listener != null) {
                listeners.addListener(Maps.immutableEntry(type, identifier), listener);
            }
            return ret;
        } catch (PermissionsLoadingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public final ListenableFuture<ImmutableOptionSubjectData> setData(String type, String identifier, ImmutableOptionSubjectData data) {
        Preconditions.checkNotNull(type, "type");
        Preconditions.checkNotNull(identifier, "identifier");

        final Map.Entry<String, String> lookupKey = Maps.immutableEntry(type, identifier);
        ListenableFuture<ImmutableOptionSubjectData> ret = setDataInternal(type, identifier, data);
        Futures.addCallback(ret, new FutureCallback<ImmutableOptionSubjectData>() {
            @Override
            public void onSuccess(@Nullable ImmutableOptionSubjectData newData) {
                if (newData != null) {
                    listeners.call(lookupKey, newData);
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
            }
        });
        return ret;
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
        final Map.Entry<String, String> defKey = this.manager.getDefaultIdentifier();
        setData(defKey.getKey(), defKey.getValue(), getData(defKey.getKey(), defKey.getValue(), null)
                .setDefaultValue(ImmutableSet.of(Maps.immutableEntry("srcip", "127.0.0.1")), 1));
    }

    protected abstract ImmutableOptionSubjectData getDataInternal(String type, String identifier) throws PermissionsLoadingException;

    protected abstract ListenableFuture<ImmutableOptionSubjectData> setDataInternal(String type, String identifier, ImmutableOptionSubjectData data);

    @Override
    public Iterable<Map.Entry<String, ImmutableOptionSubjectData>> getAll(final String type) {
        Preconditions.checkNotNull(type, "type");
        return Iterables.transform(getAllIdentifiers(type), new Function<String, Map.Entry<String, ImmutableOptionSubjectData>>() {
            @Nullable
            @Override
            public Map.Entry<String, ImmutableOptionSubjectData> apply(String input) {
                return Maps.immutableEntry(input, getData(type, input, null));
            }
        });
    }

    @Override
    public <T> ListenableFuture<T> performBulkOperation(final Function<DataStore, T> function) {
        final ListenableFutureTask<T> ret = ListenableFutureTask.create(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return performBulkOperationSync(function);
            }
        });
        getManager().executeAsyncronously(ret);
        return ret;
    }

    @Override
    public RankLadder getRankLadder(String ladderName, Caching<RankLadder> listener) {
        Preconditions.checkNotNull(ladderName, "ladderName");
        RankLadder ladder = getRankLadderInternal(ladderName);
        if (listener != null) {
            rankLadderListeners.addListener(ladderName.toLowerCase(), listener);
        }
        return ladder;
    }

    @Override
    public ListenableFuture<RankLadder> setRankLadder(final String identifier, RankLadder ladder) {
        ListenableFuture<RankLadder> ret = setRankLadderInternal(identifier, ladder);
        Futures.addCallback(ret, new FutureCallback<RankLadder>() {
            @Override
            public void onSuccess(@Nullable RankLadder newData) {
                if (newData != null) {
                    rankLadderListeners.call(identifier, newData);
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
            }
        });
        return ret;
    }


    protected abstract RankLadder getRankLadderInternal(String ladder);
    protected abstract ListenableFuture<RankLadder> setRankLadderInternal(String ladder, RankLadder newLadder);

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
        Preconditions.checkNotNull(node, "node");
        try {
            ((ObjectMapper) factory.mapper).bind(this).serialize(node);
        } catch (ObjectMappingException e) {
            throw new PermissionsLoadingException(_("Error while serializing backend %s", node.getKey()), e);
        }
        return factory.type;
    }

    protected static class Factory implements DataStoreFactory {
        private final String type;
        private final Class<? extends AbstractDataStore> expectedClazz;
        private final ObjectMapper<? extends AbstractDataStore> mapper;

        public Factory(final String type, Class<? extends AbstractDataStore> clazz) {
            Preconditions.checkNotNull(type, "type");
            Preconditions.checkNotNull(clazz, "clazz");
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
                throw new PermissionsLoadingException(_("Error while deserializing backend %s", identifier), e);
            }
        }
    }
}
