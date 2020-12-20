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
import ca.stellardrift.permissionsex.subject.InvalidIdentifierException;
import ca.stellardrift.permissionsex.subject.SubjectDataCache;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectType;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.errorprone.annotations.concurrent.LazyInit;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Cache for subject data objects from a single data store.
 *
 */
public final class SubjectDataCacheImpl<I> implements SubjectDataCache<I> {
    private final SubjectType<I> type;

    @LazyInit
    private DataStore dataStore;
    private final AtomicReference<AsyncLoadingCache<I, ImmutableSubjectData>> cache = new AtomicReference<>();
    /**
     * Holds cache listeners to prevent them from being garbage-collected.
     */
    private final Map<I, Consumer<ImmutableSubjectData>> cacheHolders = new ConcurrentHashMap<>();
    private final CacheListenerHolder<I, ImmutableSubjectData> listeners;
    private final SubjectRef<String> defaultIdentifier;

    public SubjectDataCacheImpl(final SubjectType<I> type, final DataStore dataStore) {
        this.type = type;
        update(dataStore);
        this.defaultIdentifier = SubjectRef.subject(PermissionsEngine.SUBJECTS_DEFAULTS, type.name());
        this.listeners = new CacheListenerHolder<>();
    }

    /**
     * For internal use only. Replace the backing data store while maintaining cache entries, ex. when the engine is reloaded.
     *
     * @param newDataStore The new data store to use
     */
    @EnsuresNonNull("this.dataStore")
    public void update(final DataStore newDataStore) {
        this.dataStore = newDataStore;
        AsyncLoadingCache<I, ImmutableSubjectData> oldCache = this.cache.getAndSet(Caffeine.newBuilder()
                        .maximumSize(512)
                        .buildAsync((key, executor) -> dataStore.getData(this.type.name(), this.type.serializeIdentifier(key), clearListener(key))));
        if (oldCache != null) {
            oldCache.synchronous().asMap().forEach((k, v) -> {
                    data(k, null).thenAccept(data -> listeners.call(k, data));
                    // TODO: Not ignore this somehow? Add a listener in to the backend?
            });
        }
    }

    @Override
    public CompletableFuture<ImmutableSubjectData> data(final I identifier, final @Nullable Consumer<ImmutableSubjectData> listener) {
        requireNonNull(identifier, "identifier");

        CompletableFuture<ImmutableSubjectData> ret = cache.get().get(identifier);
        ret.thenRun(() -> {
            if (listener != null) {
                listeners.addListener(identifier, listener);
            }
        });
        return ret;
    }

    @Override
    public CompletableFuture<ToDataSubjectRefImpl<I>> referenceTo(final I identifier) {
        return referenceTo(identifier, true);
    }

    @Override
    public CompletableFuture<ToDataSubjectRefImpl<I>> referenceTo(final I identifier, final boolean strongListeners) {
        final ToDataSubjectRefImpl<I> ref = new ToDataSubjectRefImpl<>(identifier, this, strongListeners);
        return data(identifier, ref).thenApply(data -> {
            ref.data.set(data);
            return ref;
        });
    }

    @Override
    public CompletableFuture<ImmutableSubjectData> update(final I identifier, final UnaryOperator<ImmutableSubjectData> action) {
        return data(identifier, null)
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
    public void load(final I identifier) {
        requireNonNull(identifier, "identifier");

        cache.get().get(identifier);
    }

    @Override
    public void invalidate(final I identifier) {
        requireNonNull(identifier, "identifier");

        cache.get().synchronous().invalidate(identifier);
        cacheHolders.remove(identifier);
        listeners.removeAll(identifier);
    }

    @Override
    public void cacheAll() {
        for (String ident : dataStore.getAllIdentifiers(this.type.name())) {
            try {
                cache.get().synchronous().refresh(this.type.parseIdentifier(ident));
            } catch (final InvalidIdentifierException ex) {
                // TODO: log this
            }
        }
    }

    @Override
    public CompletableFuture<Boolean> isRegistered(final I identifier) {
        requireNonNull(identifier, "identifier");

        return dataStore.isRegistered(this.type.name(), this.type.serializeIdentifier(identifier));
    }

    @Override
    public CompletableFuture<ImmutableSubjectData> remove(final I identifier) {
        return set(identifier, null);
    }

    @Override
    public CompletableFuture<ImmutableSubjectData> set(final I identifier, final @Nullable ImmutableSubjectData newData) {
        requireNonNull(identifier, "identifier");

        return dataStore.setData(this.type.name(), this.type.serializeIdentifier(identifier), newData);
    }

    /**
     * Create a new listener to pass to the backing data store. This listener will update our cache and notify all
     * listeners to the cache that new data is available.
     *
     * @param name The subject identifier
     * @return A caching function
     */
    private Consumer<ImmutableSubjectData> clearListener(final I name) {
        Consumer<ImmutableSubjectData> ret = newData -> {
            cache.get().put(name, CompletableFuture.completedFuture(newData));
            listeners.call(name, newData);
        };
        cacheHolders.put(name, ret);
        return ret;
    }

    @Override
    public void addListener(final I identifier, final Consumer<ImmutableSubjectData> listener) {
        requireNonNull(identifier, "identifier");
        requireNonNull(listener, "listener");

        listeners.addListener(identifier, listener);
    }

    @Override
    public SubjectType<I> type() {
        return this.type;
    }

    @Override
    public Stream<I> getAllIdentifiers() {
        return this.dataStore.getAllIdentifiers(type.name()).stream()
                .map(this.type::parseIdentifier);
    }

    @Override
    public SubjectRef<String> getDefaultIdentifier() {
        return this.defaultIdentifier;
    }
}
