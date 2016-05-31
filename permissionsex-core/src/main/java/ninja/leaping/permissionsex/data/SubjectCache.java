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

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.backend.DataStore;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

public class SubjectCache {
    private final String type;
    private DataStore dataStore;
    private final AtomicReference<AsyncLoadingCache<String, ImmutableSubjectData>> cache = new AtomicReference<>();
    private final Map<String, Caching<ImmutableSubjectData>> cacheHolders = new ConcurrentHashMap<>();
    private final CacheListenerHolder<String, ImmutableSubjectData> listeners;
    private final SubjectRef defaultIdentifier;

    public SubjectCache(final String type, final DataStore dataStore) {
        this.type = type;
        update(dataStore);
        this.defaultIdentifier = SubjectRef.of(PermissionsEx.SUBJECTS_DEFAULTS, type);
        this.listeners = new CacheListenerHolder<>();
    }

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

    public CompletableFuture<ImmutableSubjectData> getData(String identifier, Caching<ImmutableSubjectData> listener) {
        Objects.requireNonNull(identifier, "identifier");

        CompletableFuture<ImmutableSubjectData> ret = cache.get().get(identifier);
        ret.thenRun(() -> {
            if (listener != null) {
                listeners.addListener(identifier, listener);
            }
        });
        return ret;
    }

    public CompletableFuture<SubjectDataReference> getReference(String identifier) {
        return getReference(identifier, true);
    }

    public CompletableFuture<SubjectDataReference> getReference(String identifier, boolean strongListeners) {
        final SubjectDataReference ref = new SubjectDataReference(SubjectRef.of(this.type, checkNotNull(identifier, "identifier")), this, strongListeners);
        return getData(identifier, ref).thenApply(data -> {
            ref.data.set(data);
            return ref;
        });
    }

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

    public void load(String identifier) throws ExecutionException {
        Objects.requireNonNull(identifier, "identifier");

        cache.get().get(identifier);
    }

    public void invalidate(String identifier) {
        Objects.requireNonNull(identifier, "identifier");

        cache.get().synchronous().invalidate(identifier);
        cacheHolders.remove(identifier);
        listeners.removeAll(identifier);
    }

    public void cacheAll() {
        for (String ident : dataStore.getAllIdentifiers(type)) {
            cache.get().synchronous().refresh(ident);
        }
    }

    public CompletableFuture<Boolean> isRegistered(String identifier) {
        Objects.requireNonNull(identifier, "identifier");

        return dataStore.isRegistered(type, identifier);
    }

    public CompletableFuture<ImmutableSubjectData> remove(String identifier) {
        return set(identifier, null);
    }

    CompletableFuture<ImmutableSubjectData> set(String identifier, @Nullable ImmutableSubjectData newData) {
        Objects.requireNonNull(identifier, "identifier");

        return dataStore.setData(type, identifier, newData);
    }

    private Caching<ImmutableSubjectData> clearListener(final String name) {
        Caching<ImmutableSubjectData> ret = newData -> {
            cache.get().put(name, CompletableFuture.completedFuture(newData));
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

    public Set<String> getAllIdentifiers() {
        return dataStore.getAllIdentifiers(type);
    }

    /**
     * Get the identifier for the subject holding default data for subjects of this type
     * @return The id for the default subject of this type
     */
    public SubjectRef getDefaultIdentifier() {
        return defaultIdentifier;
    }
}
