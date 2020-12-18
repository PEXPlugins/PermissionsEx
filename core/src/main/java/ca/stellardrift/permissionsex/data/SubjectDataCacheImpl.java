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
package ca.stellardrift.permissionsex.data;

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.datastore.DataStore;
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.util.UnmodifiableCollections;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Cache for subject data objects from a single data store.
 *
 */
public final class SubjectDataCacheImpl implements ca.stellardrift.permissionsex.subject.SubjectDataCache {
    private final String type;
    private DataStore dataStore;
    private final AtomicReference<AsyncLoadingCache<String, ImmutableSubjectData>> cache = new AtomicReference<>();
    /**
     * Holds cache listeners to prevent them from being garbage-collected
     */
    private final Map<String, Consumer<ImmutableSubjectData>> cacheHolders = new ConcurrentHashMap<>();
    private final CacheListenerHolder<String, ImmutableSubjectData> listeners;
    private final Map.Entry<String, String> defaultIdentifier;

    public SubjectDataCacheImpl(final String type, final DataStore dataStore) {
        this.type = type;
        update(dataStore);
        this.defaultIdentifier = UnmodifiableCollections.immutableMapEntry(PermissionsEngine.SUBJECTS_DEFAULTS, type);
        this.listeners = new CacheListenerHolder<>();
    }

    /**
     * For internal use only. Replace the backing data store while maintaining cache entries, ex. when the engine is reloaded.
     *
     * @param newDataStore The new data store to use
     */
    public void update(final DataStore newDataStore) {
        this.dataStore = newDataStore;
        AsyncLoadingCache<String, ImmutableSubjectData> oldCache = this.cache.getAndSet(Caffeine.newBuilder()
                        .maximumSize(512)
                        .buildAsync((key, executor) -> dataStore.getData(type, key, clearListener(key))));
        if (oldCache != null) {
            oldCache.synchronous().asMap().forEach((k, v) -> {
                    getData(k, null).thenAccept(data -> listeners.call(k, data));
                    // TODO: Not ignore this somehow? Add a listener in to the backend?
            });
        }
    }

    @Override
    public CompletableFuture<ImmutableSubjectData> getData(final String identifier, final @Nullable Consumer<ImmutableSubjectData> listener) {
        Objects.requireNonNull(identifier, "identifier");

        CompletableFuture<ImmutableSubjectData> ret = cache.get().get(identifier);
        ret.thenRun(() -> {
            if (listener != null) {
                listeners.addListener(identifier, listener);
            }
        });
        return ret;
    }

    @Override
    public CompletableFuture<SubjectDataReference> getReference(final String identifier) {
        return getReference(identifier, true);
    }

    @Override
    public CompletableFuture<SubjectDataReference> getReference(final String identifier, final boolean strongListeners) {
        final SubjectDataReference ref = new SubjectDataReference(identifier, this, strongListeners);
        return getData(identifier, ref).thenApply(data -> {
            ref.data.set(data);
            return ref;
        });
    }

    @Override
    public CompletableFuture<ImmutableSubjectData> update(final String identifier, final UnaryOperator<ImmutableSubjectData> action) {
        return getData(identifier, null)
                .thenCompose(data -> {
                    ImmutableSubjectData newData = action.apply(data);
                    if (data != newData) {
                        return set(identifier, newData);
                    } else {
                        return CompletableFuture.completedFuture(data);
                    }
                });
    }

    @Override
    public void load(final String identifier) {
        Objects.requireNonNull(identifier, "identifier");

        cache.get().get(identifier);
    }

    @Override
    public void invalidate(final String identifier) {
        Objects.requireNonNull(identifier, "identifier");

        cache.get().synchronous().invalidate(identifier);
        cacheHolders.remove(identifier);
        listeners.removeAll(identifier);
    }

    @Override
    public void cacheAll() {
        for (String ident : dataStore.getAllIdentifiers(type)) {
            cache.get().synchronous().refresh(ident);
        }
    }

    @Override
    public CompletableFuture<Boolean> isRegistered(final String identifier) {
        Objects.requireNonNull(identifier, "identifier");

        return dataStore.isRegistered(type, identifier);
    }

    @Override
    public CompletableFuture<ImmutableSubjectData> remove(final String identifier) {
        return set(identifier, null);
    }

    @Override
    public CompletableFuture<ImmutableSubjectData> set(final String identifier, final @Nullable ImmutableSubjectData newData) {
        Objects.requireNonNull(identifier, "identifier");

        return dataStore.setData(type, identifier, newData);
    }

    /**
     * Create a new listener to pass to the backing data store. This listener will update our cache and notify all
     * listeners to the cache that new data is available.
     *
     * @param name The subject identifier
     * @return A caching function
     */
    private Consumer<ImmutableSubjectData> clearListener(final String name) {
        Consumer<ImmutableSubjectData> ret = newData -> {
            cache.get().put(name, CompletableFuture.completedFuture(newData));
            listeners.call(name, newData);
        };
        cacheHolders.put(name, ret);
        return ret;
    }

    @Override
    public void addListener(final String identifier, final Consumer<ImmutableSubjectData> listener) {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(listener, "listener");

        listeners.addListener(identifier, listener);
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Set<String> getAllIdentifiers() {
        return dataStore.getAllIdentifiers(type);
    }

    @Override
    public Map.Entry<String, String> getDefaultIdentifier() {
        return defaultIdentifier;
    }
}
