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
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.util.Util;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class SubjectCache {
    private final String type;
    private final DataStore dataStore;
    private final LoadingCache<String, ImmutableSubjectData> cache;
    private final Map<String, Caching<ImmutableSubjectData>> cacheHolders = new ConcurrentHashMap<>();
    private final CacheListenerHolder<String, ImmutableSubjectData> listeners;
    private final Map.Entry<String, String> defaultIdentifier;

    public SubjectCache(final String type, final DataStore dataStore) {
        this(type, null, dataStore);
    }

    public SubjectCache(final SubjectCache existing, final DataStore dataStore) {
        this(existing.getType(), existing, dataStore);
    }

    private SubjectCache(final String type, final SubjectCache existing, final DataStore dataStore) {
        this.type = type;
        this.dataStore = dataStore;
        this.defaultIdentifier = Maps.immutableEntry(PermissionsEx.SUBJECTS_DEFAULTS, type);
        cache = CacheBuilder.newBuilder()
                .maximumSize(512)
                .build(CacheLoader.from(identifier -> dataStore.getData(type, identifier, clearListener(identifier))));
        if (existing != null) {
            this.listeners = existing.listeners;
            existing.cache.asMap().forEach((k, v) -> {
                try {
                    listeners.call(k, getData(k, null));
                } catch (ExecutionException e) {
                    // TODO: Not ignore this somehow? Add a listener in to the backend?
                }
            });
        } else {
            this.listeners = new CacheListenerHolder<>();
        }
    }

    public ImmutableSubjectData getData(String identifier, Caching<ImmutableSubjectData> listener) throws ExecutionException {
        Objects.requireNonNull(identifier, "identifier");

        ImmutableSubjectData ret = cache.get(identifier);
        if (listener != null) {
            listeners.addListener(identifier, listener);
        }
        return ret;
    }

    public CompletableFuture<ImmutableSubjectData> update(String identifier, Function<ImmutableSubjectData, ImmutableSubjectData> action) {
        ImmutableSubjectData data;
        try {
            data = getData(identifier, null);
        } catch (ExecutionException e) {
            return Util.failedFuture(e);
        }

        ImmutableSubjectData newData = action.apply(data);
        if (newData != data) {
            return set(identifier, newData);
        } else {
            return CompletableFuture.completedFuture(data);
        }
    }

    public void load(String identifier) throws ExecutionException {
        Objects.requireNonNull(identifier, "identifier");

        cache.get(identifier);
    }

    public void invalidate(String identifier) {
        Objects.requireNonNull(identifier, "identifier");

        cache.invalidate(identifier);
        cacheHolders.remove(identifier);
        listeners.removeAll(identifier);
    }

    public void cacheAll() {
        for (String ident : dataStore.getAllIdentifiers(type)) {
            try {
                cache.get(ident);
            } catch (ExecutionException e) {
                // oh noes, but we'll still squash it
            }
        }
    }

    public boolean isRegistered(String identifier) {
        Objects.requireNonNull(identifier, "identifier");

        return dataStore.isRegistered(type, identifier);
    }

    public CompletableFuture<ImmutableSubjectData> set(String identifier, ImmutableSubjectData newData) {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(newData, "newData");

        return dataStore.setData(type, identifier, newData);
    }

    private Caching<ImmutableSubjectData> clearListener(final String name) {
        Caching<ImmutableSubjectData> ret = newData -> {
            cache.put(name, newData);
            listeners.call(name, newData);
        };
        cacheHolders.put(name, ret);
        return ret;
    }

    public void addListener(String identifier, Caching<ImmutableSubjectData> listener) {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(listener, "listener");

        listeners.addListener(identifier, listener);
    }

    public String getType() {
        return type;
    }

    public Iterable<String> getAllIdentifiers() {
        return dataStore.getAllIdentifiers(type);
    }

    /**
     * Get the identifier for the subject holding default data for subjects of this type
     * @return
     */
    public Map.Entry<String, String> getDefaultIdentifier() {
        return defaultIdentifier;
    }
}
