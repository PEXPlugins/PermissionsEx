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
package ca.stellardrift.permissionsex.impl.util;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 *  Tracks object listeners for a cache
 *
 * @param <Key> The cache key type
 * @param <CacheType> The cache value type
 */
public class CacheListenerHolder<Key, CacheType> {
    private final ConcurrentMap<Key, Set<Consumer<CacheType>>> listeners = new ConcurrentHashMap<>();

    private Set<Consumer<CacheType>> getListeners(Key key) {
        requireNonNull(key, "key");
        return this.listeners.computeIfAbsent(key, k -> Collections.newSetFromMap(Caffeine.newBuilder().weakKeys().<Consumer<CacheType>, Boolean>build().asMap()));
    }

    public void call(Key key, CacheType newData) {
        requireNonNull(key, "key");
        requireNonNull(newData, "newData");

        for (Consumer<CacheType> listener : getListeners(key)) {
            listener.accept(newData);
        }
    }

    public void addListener(Key key, Consumer<CacheType> listener) {
        requireNonNull(key, "key");
        requireNonNull(listener, "listener");

        getListeners(key).add(listener);
    }

    public void removeListener(Key key, Consumer<CacheType> listener) {
        requireNonNull(key, "key");
        requireNonNull(listener, "listener");

        getListeners(key).remove(listener);
    }

    public Iterable<Key> getAllKeys() {
        return Collections.unmodifiableSet(this.listeners.keySet());
    }

    public void removeAll(Key key) {
        this.listeners.remove(key);
    }
}
