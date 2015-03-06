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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ninja.leaping.permissionsex.backends.DataStore;

import java.util.Map;
import java.util.concurrent.ExecutionException;

public class SubjectCache {
    private final String type;
    private final DataStore dataStore;
    private final LoadingCache<String, ImmutableOptionSubjectData> cache;

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
        return cache.get(identifier);
    }

    public void load(String identifier) throws ExecutionException {
        cache.get(identifier);
    }

    public void invalidate(String identifier) {
        cache.invalidate(identifier);
    }

    public void cacheAll() {
        for (Map.Entry<String, ImmutableOptionSubjectData> ident : dataStore.getAll(type)) {
            cache.asMap().putIfAbsent(ident.getKey(), ident.getValue());
        }
    }

    public boolean isRegistered(String key) {
        return cache.getIfPresent(key) != null || dataStore.isRegistered(type, key);
    }

    public void update(String identifier, ImmutableOptionSubjectData newData) {
        dataStore.setData(type, identifier, newData);
    }

    private Caching clearListener(final String name) {
        return new Caching() {
            @Override
            public void clearCache(ImmutableOptionSubjectData newData) {
                cache.put(name, newData);
            }
        };
    }

    public String getType() {
        return type;
    }

    public Iterable<String> getAllIdentifiers() {
        return dataStore.getAllIdentifiers(type);
    }
}
