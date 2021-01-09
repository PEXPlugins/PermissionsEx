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
package ca.stellardrift.permissionsex.impl.rank;

import ca.stellardrift.permissionsex.datastore.DataStore;
import ca.stellardrift.permissionsex.impl.util.CacheListenerHolder;
import ca.stellardrift.permissionsex.rank.RankLadder;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Access information about rank ladders.
 */
public class RankLadderCache implements ca.stellardrift.permissionsex.rank.RankLadderCollection {
    private final DataStore dataStore;
    private final AsyncLoadingCache<String, RankLadder> cache;
    private final Map<String, Consumer<RankLadder>> cacheHolders = new ConcurrentHashMap<>();
    private final CacheListenerHolder<String, RankLadder> listeners;

    public RankLadderCache(final DataStore dataStore) {
        this(null, dataStore);
    }

    public RankLadderCache(final @Nullable RankLadderCache existing, final DataStore dataStore) {
        this.dataStore = dataStore;
        cache = Caffeine.newBuilder()
                .maximumSize(256)
                .buildAsync((key, executor) -> dataStore.getRankLadder(key, clearListener(key)));
        if (existing != null) {
            listeners = existing.listeners;
            existing.cache.synchronous().asMap().forEach((key, rankLadder) -> {
                get(key, null).thenAccept(data -> listeners.call(key, data));
            });
        } else {
            listeners = new CacheListenerHolder<>();
        }
    }

    @Override
    public CompletableFuture<RankLadder> get(final String identifier, final @Nullable Consumer<RankLadder> listener) {
        Objects.requireNonNull(identifier, "identifier");

        CompletableFuture<RankLadder> ret = cache.get(identifier);
        ret.thenRun(() -> {
            if (listener != null) {
                listeners.addListener(identifier, listener);
            }
        });
        return ret;
    }

    @Override
    public CompletableFuture<RankLadder> update(final String identifier, final UnaryOperator<RankLadder> updateFunc) {
        return cache.get(identifier)
                .thenCompose(oldLadder -> {
                    RankLadder newLadder = updateFunc.apply(oldLadder);
                    if (oldLadder == newLadder) {
                        return CompletableFuture.completedFuture(newLadder);
                    }
                    return set(identifier, newLadder);

                });
    }

    public void load(final String identifier) {
        Objects.requireNonNull(identifier, "identifier");
        cache.synchronous().refresh(identifier);
    }

    public void invalidate(final String identifier) {
        Objects.requireNonNull(identifier, "identifier");

        cache.synchronous().invalidate(identifier);
        cacheHolders.remove(identifier);
        listeners.removeAll(identifier);
    }

    @Override
    public CompletableFuture<Boolean> has(String identifier) {
        Objects.requireNonNull(identifier, "identifier");

        if (cache.synchronous().getIfPresent(identifier) != null) {
            return CompletableFuture.completedFuture(true);
        } else {
            return dataStore.hasRankLadder(identifier);
        }
    }

    @Override
    public CompletableFuture<RankLadder> set(String identifier, RankLadder newData) {
        Objects.requireNonNull(identifier, "identifier");

        return dataStore.setRankLadder(identifier, newData);
    }

    private Consumer<RankLadder> clearListener(final String name) {
        Consumer<RankLadder> ret = newData -> {
            cache.synchronous().put(name, newData);
            listeners.call(name, newData);
        };
        cacheHolders.put(name, ret);
        return ret;
    }

    @Override
    public void addListener(String identifier, Consumer<RankLadder> listener) {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(listener, "listener");

        listeners.addListener(identifier, listener);
    }

    @Override
    public Stream<String> names() {
        return dataStore.getAllRankLadders();
    }
}
