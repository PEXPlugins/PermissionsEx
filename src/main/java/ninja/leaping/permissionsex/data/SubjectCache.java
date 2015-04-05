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
package ninja.leaping.permissionsex.data;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import ninja.leaping.permissionsex.backend.DataStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class SubjectCache {
    private final String type;
    private final DataStore dataStore;
    private final LoadingCache<String, ImmutableOptionSubjectData> cache;
    private final Map<String, Caching> cacheHolders = new ConcurrentHashMap<>();
    private final CacheListenerHolder<String> listeners = new CacheListenerHolder<>();

    public SubjectCache(final String type, final DataStore dataStore) {
        this.type = type;
        this.dataStore = dataStore;
        cache = CacheBuilder.newBuilder()
                .maximumSize(512)
                .build(new CacheLoader<String, ImmutableOptionSubjectData>() {
                    @Override
                    public ImmutableOptionSubjectData load(String identifier) throws Exception {
                        return dataStore.getData(type, identifier, clearListener(identifier));
                    }
                });
    }

    public ImmutableOptionSubjectData getData(String identifier, Caching listener) throws ExecutionException {
        Preconditions.checkNotNull(identifier, "identifier");

        ImmutableOptionSubjectData ret = cache.get(identifier);
        if (listener != null) {
            listeners.addListener(identifier, listener);
        }
        return ret;
    }

    public void load(String identifier) throws ExecutionException {
        Preconditions.checkNotNull(identifier, "identifier");

        cache.get(identifier);
    }

    public void invalidate(String identifier) {
        Preconditions.checkNotNull(identifier, "identifier");

        cache.invalidate(identifier);
        cacheHolders.remove(identifier);
        listeners.removeAll(identifier);
    }

    public void cacheAll() {
        for (Map.Entry<String, ImmutableOptionSubjectData> ident : dataStore.getAll(type)) {
            cache.asMap().putIfAbsent(ident.getKey(), ident.getValue());
        }
    }

    public boolean isRegistered(String identifier) {
        Preconditions.checkNotNull(identifier, "identifier");

        return cache.getIfPresent(identifier) != null || dataStore.isRegistered(type, identifier);
    }

    public ListenableFuture<ImmutableOptionSubjectData> update(String identifier, ImmutableOptionSubjectData newData) {
        Preconditions.checkNotNull(identifier, "identifier");
        Preconditions.checkNotNull(newData, "newData");

        return dataStore.setData(type, identifier, newData);
    }

    private Caching clearListener(final String name) {
        Caching ret = new Caching() {
            @Override
            public void clearCache(ImmutableOptionSubjectData newData) {
                cache.put(name, newData);
                listeners.call(name, newData);
            }
        };
        cacheHolders.put(name, ret);
        return ret;
    }

    public void addListener(String identifier, Caching listener) {
        Preconditions.checkNotNull(identifier, "identifier");
        Preconditions.checkNotNull(listener, "listener");

        listeners.addListener(identifier, listener);

    }

    public String getType() {
        return type;
    }

    public Iterable<String> getAllIdentifiers() {
        return dataStore.getAllIdentifiers(type);
    }
}
