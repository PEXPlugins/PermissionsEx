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
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.rank.RankLadder;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class RankLadderCache {
    private final DataStore dataStore;
    private final LoadingCache<String, RankLadder> cache;
    private final Map<String, Caching<RankLadder>> cacheHolders = new ConcurrentHashMap<>();
    private final CacheListenerHolder<String, RankLadder> listeners;

    public RankLadderCache(final DataStore dataStore) {
        this(null, dataStore);
    }

    public RankLadderCache(final RankLadderCache existing, final DataStore dataStore) {
        this.dataStore = dataStore;
        cache = CacheBuilder.newBuilder()
                .maximumSize(512)
                .build(new CacheLoader<String, RankLadder>() {
                    @Override
                    public RankLadder load(String identifier) throws Exception {
                        return dataStore.getRankLadder(identifier, clearListener(identifier));
                    }
                });
        if (existing != null) {
            listeners = existing.listeners;
            existing.cache.asMap().forEach((k, old) -> listeners.call(k, get(k, null)));
        } else {
            listeners = new CacheListenerHolder<>();
        }
    }


    public RankLadder get(String identifier, Caching<RankLadder> listener) {
        Preconditions.checkNotNull(identifier, "identifier");

        RankLadder ret;
        try {
            ret = cache.get(identifier);
        } catch (ExecutionException e) {
            throw new RuntimeException(e); // This shouldn't happen? -- we throw now checked exceptions
        }
        if (listener != null) {
            listeners.addListener(identifier, listener);
        }
        return ret;
    }

    public void load(String identifier) {
        Preconditions.checkNotNull(identifier, "identifier");

        try {
            cache.get(identifier);
        } catch (ExecutionException e) {
        }
    }

    public void invalidate(String identifier) {
        Preconditions.checkNotNull(identifier, "identifier");

        cache.invalidate(identifier);
        cacheHolders.remove(identifier);
        listeners.removeAll(identifier);
    }

    public boolean has(String identifier) {
        Preconditions.checkNotNull(identifier, "identifier");

        return dataStore.hasRankLadder(identifier);
    }

    public CompletableFuture<RankLadder> set(String identifier, RankLadder newData) {
        Preconditions.checkNotNull(identifier, "identifier");
        Preconditions.checkNotNull(newData, "newData");

        return dataStore.setRankLadder(identifier, newData);
    }

    private Caching<RankLadder> clearListener(final String name) {
        Caching<RankLadder> ret = newData -> {
            cache.put(name, newData);
            listeners.call(name, newData);
        };
        cacheHolders.put(name, ret);
        return ret;
    }

    public void addListener(String identifier, Caching<RankLadder> listener) {
        Preconditions.checkNotNull(identifier, "identifier");
        Preconditions.checkNotNull(listener, "listener");

        try {
            cache.get(identifier);
        } catch (ExecutionException e) {
        }
        listeners.addListener(identifier, listener);
    }

    public Iterable<String> getAll() {
        return dataStore.getAllRankLadders();
    }
}
