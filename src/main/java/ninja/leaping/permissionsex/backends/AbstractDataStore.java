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
package ninja.leaping.permissionsex.backends;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.permissionsex.data.CacheListenerHolder;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Base implementation of a data store that provides common points for other data stores to hook into.
 */
public abstract class AbstractDataStore implements DataStore {
    private final Factory factory;
    private final CacheListenerHolder<Map.Entry<String, String>> listeners = new CacheListenerHolder<>();

    protected AbstractDataStore(Factory factory) {
        if (!factory.expectedClazz.equals(getClass())) {
            throw new ExceptionInInitializerError("Data store factory for wrong class " + factory.expectedClazz + " provided to a " + getClass());
        }
        this.factory = factory;
    }

    @Override
    public final ImmutableOptionSubjectData getData(String type, String identifier, Caching listener) {
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
                listeners.call(lookupKey, newData);
            }

            @Override
            public void onFailure(Throwable throwable) {

            }
        });
        return ret;
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
    @SuppressWarnings("unchecked") // Corect types are verified in the constructor
    public String serialize(ConfigurationNode node) throws PermissionsLoadingException {
        Preconditions.checkNotNull(node, "node");
        try {
            ((ObjectMapper) factory.mapper).bind(this).serialize(node);
        } catch (ObjectMappingException e) {
            throw new PermissionsLoadingException("Error while serializing backend " + node.getKey(), e);
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
                throw new PermissionsLoadingException("Error while deserializing backend " + identifier, e);
            }
        }
    }
}
