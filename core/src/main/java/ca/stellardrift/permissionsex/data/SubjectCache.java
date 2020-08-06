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

import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.backend.DataStore;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Cache for subject data objects from a single data store.
 *
 * Provides operations to manage querying, writing, and updating {@link ImmutableSubjectData} objects.
 */
public class SubjectCache {
    private final String type;
    private DataStore dataStore;
    private final AtomicReference<AsyncLoadingCache<String, ImmutableSubjectData>> cache = new AtomicReference<>();
    /**
     * Holds cache listeners to prevent them from being garbage-collected
     */
    private final Map<String, Consumer<ImmutableSubjectData>> cacheHolders = new ConcurrentHashMap<>();
    private final CacheListenerHolder<String, ImmutableSubjectData> listeners;
    private final Map.Entry<String, String> defaultIdentifier;

    public SubjectCache(final String type, final DataStore dataStore) {
        this.type = type;
        update(dataStore);
        this.defaultIdentifier = Maps.immutableEntry(PermissionsEx.SUBJECTS_DEFAULTS, type);
        this.listeners = new CacheListenerHolder<>();
    }

    /**
     * For internal use only. Replace the backing data store while maintaining cache entries, ex. when the engine is reloaded.
     *
     * @param newDataStore The new data store to use
     */
    public void update(DataStore newDataStore) {
        this.dataStore = newDataStore;
        AsyncLoadingCache<String, ImmutableSubjectData> oldCache = this.cache.getAndSet(Caffeine.newBuilder()
                        .maximumSize(512)
                        .buildAsync(((key, executor) -> dataStore.getData(type, key, clearListener(key)))));
        if (oldCache != null) {
            oldCache.synchronous().asMap().forEach((k, v) -> {
                    getData(k, null).thenAccept(data -> listeners.call(k, data));
                    // TODO: Not ignore this somehow? Add a listener in to the backend?
            });
        }
    }

    /**
     * Get data for a given subject.
     * This will return a data object even if the subject is not registered -- the data object will just be empty.
     *
     * For most longer-lifetime use cases, {@link #getReference(String)} will be the preferred method to get a reference
     * to the latest subject data.
     *
     * @param identifier The identifier of the subject to query
     * @param listener A callback that will be notified whenever a change is made to the data object
     * @return A future returning when the data is available
     */
    public CompletableFuture<ImmutableSubjectData> getData(String identifier, Consumer<ImmutableSubjectData> listener) {
        Objects.requireNonNull(identifier, "identifier");

        CompletableFuture<ImmutableSubjectData> ret = cache.get().get(identifier);
        ret.thenRun(() -> {
            if (listener != null) {
                listeners.addListener(identifier, listener);
            }
        });
        return ret;
    }

    /**
     * Get a reference to subject data for a given subject. The reference will update as changes are made to the backing
     * data store, and can always be used to query a specific subject's raw data.
     *
     * @param identifier The identifier of the subject to get data for
     * @return A future returning with a full reference to the given subject's data.
     */
    public CompletableFuture<SubjectDataReference> getReference(String identifier) {
        return getReference(identifier, true);
    }

    /**
     * Get a reference to subject data for a given subject
     *
     * @param identifier The identifier of the subject to get data for
     * @param strongListeners Whether to hold listeners to this subject data even after they would be otherwise GC'd
     * @return A future completing with the subject data reference
     */
    public CompletableFuture<SubjectDataReference> getReference(String identifier, boolean strongListeners) {
        final SubjectDataReference ref = new SubjectDataReference(identifier, this, strongListeners);
        return getData(identifier, ref).thenApply(data -> {
            ref.data.set(data);
            return ref;
        });
    }

    /**
     * Update data for a given subject, acting on the latest data available.
     * The {@code action} callback may be called within an asynchronous task.
     *
     * @param identifier The identifier of the subject to be updated
     * @param action A function taking an old subject data instance and returning an updated one
     * @return A future completing with the latest subject data after modifications are made
     */
    public CompletableFuture<ImmutableSubjectData> update(String identifier, Function<ImmutableSubjectData, ImmutableSubjectData> action) {
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

    /**
     * Load data (if any) known for the given identifier
     *
     * @param identifier The subject identifier
     */
    public void load(String identifier) {
        Objects.requireNonNull(identifier, "identifier");

        cache.get().get(identifier);
    }

    /**
     * Remove a given subject identifier from the cache
     *
     * @param identifier The identifier of the subject to be removed
     */
    public void invalidate(String identifier) {
        Objects.requireNonNull(identifier, "identifier");

        cache.get().synchronous().invalidate(identifier);
        cacheHolders.remove(identifier);
        listeners.removeAll(identifier);
    }

    /**
     * Enter all subjects of this type into cache
     */
    public void cacheAll() {
        for (String ident : dataStore.getAllIdentifiers(type)) {
            cache.get().synchronous().refresh(ident);
        }
    }

    /**
     * Check if a given subject is registered. This operation occurs asynchronously
     * Registered means that a subject has any sort of data stored.
     *
     * @param identifier The identifier of the subject to check
     * @return A future returning whether the subject has data stored
     */
    public CompletableFuture<Boolean> isRegistered(String identifier) {
        Objects.requireNonNull(identifier, "identifier");

        return dataStore.isRegistered(type, identifier);
    }

    /**
     * Remove a subject from the backing data store
     *
     * @param identifier The identifier of the subject to remove
     * @return A future returning the previous subject data.
     */
    public CompletableFuture<ImmutableSubjectData> remove(String identifier) {
        return set(identifier, null);
    }

    CompletableFuture<ImmutableSubjectData> set(String identifier, @Nullable ImmutableSubjectData newData) {
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

    /**
     * Add a listener to be notified on updates to the given subject
     *
     * @param identifier The identifier of the subject to receive notifications about
     * @param listener The callback function to notify
     */
    public void addListener(String identifier, Consumer<ImmutableSubjectData> listener) {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(listener, "listener");

        listeners.addListener(identifier, listener);
    }

    /**
     * Get the subject type identifier
     *
     * @return The identifier for this subject type
     */
    public String getType() {
        return type;
    }

    /**
     * Get a set of identifiers for all registered subjects of this type
     *
     * @return The set of identifiers
     */
    public Set<String> getAllIdentifiers() {
        return dataStore.getAllIdentifiers(type);
    }

    /**
     * Get the identifier for the subject holding default data for subjects of this type
     * @return The id for the default subject of this type
     */
    public Map.Entry<String, String> getDefaultIdentifier() {
        return defaultIdentifier;
    }
}
