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

import ca.stellardrift.permissionsex.backend.DataStore;
import ca.stellardrift.permissionsex.rank.RankLadder;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Access information about rank ladders
 */
public class RankLadderCache {
    private final DataStore dataStore;
    private final AsyncLoadingCache<String, RankLadder> cache;
    private final Map<String, Consumer<RankLadder>> cacheHolders = new ConcurrentHashMap<String, java.util.function.Consumer<RankLadder>>();
    private final CacheListenerHolder<String, RankLadder> listeners;

    public RankLadderCache(final DataStore dataStore) {
        this(null, dataStore);
    }

    public RankLadderCache(final RankLadderCache existing, final DataStore dataStore) {
        this.dataStore = dataStore;
        cache = Caffeine.newBuilder()
                .maximumSize(256)
                .buildAsync(((key, executor) -> dataStore.getRankLadder(key, clearListener(key))));
        if (existing != null) {
            listeners = existing.listeners;
            existing.cache.synchronous().asMap().forEach((key, rankLadder) -> {
                get(key, null).thenAccept(data -> listeners.call(key, data));
            });
        } else {
            listeners = new CacheListenerHolder<>();
        }
    }


    public CompletableFuture<RankLadder> get(String identifier, Consumer<RankLadder> listener) {
        Preconditions.checkNotNull(identifier, "identifier");

        CompletableFuture<RankLadder> ret = cache.get(identifier);
        ret.thenRun(() -> {
            if (listener != null) {
                listeners.addListener(identifier, listener);
            }
        });
        return ret;
    }

    public CompletableFuture<RankLadder> update(String identifier, Function<RankLadder, RankLadder> updateFunc) {
        return cache.get(identifier)
                .thenCompose(oldLadder -> {
                    RankLadder newLadder = updateFunc.apply(oldLadder);
                    if (oldLadder == newLadder) {
                        return CompletableFuture.completedFuture(newLadder);
                    }
                    return set(identifier, newLadder);

                });
    }

    public void load(String identifier) {
        Preconditions.checkNotNull(identifier, "identifier");
        cache.synchronous().refresh(identifier);
    }

    public void invalidate(String identifier) {
        Preconditions.checkNotNull(identifier, "identifier");

        cache.synchronous().invalidate(identifier);
        cacheHolders.remove(identifier);
        listeners.removeAll(identifier);
    }

    public CompletableFuture<Boolean> has(String identifier) {
        Preconditions.checkNotNull(identifier, "identifier");

        if (cache.synchronous().getIfPresent(identifier) != null) {
            return CompletableFuture.completedFuture(true);
        } else {
            return dataStore.hasRankLadder(identifier);
        }
    }

    public CompletableFuture<RankLadder> set(String identifier, RankLadder newData) {
        Preconditions.checkNotNull(identifier, "identifier");

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

    public void addListener(String identifier, Consumer<RankLadder> listener) {
        Preconditions.checkNotNull(identifier, "identifier");
        Preconditions.checkNotNull(listener, "listener");

        listeners.addListener(identifier, listener);
    }

    public Iterable<String> getAll() {
        return dataStore.getAllRankLadders();
    }
}
