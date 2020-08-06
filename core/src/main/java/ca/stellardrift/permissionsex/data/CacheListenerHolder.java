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

import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 *  Tracks object listeners for a cache
 *
 * @param <Key> The cache key type
 * @param <CacheType> The cache value type
 */
public class CacheListenerHolder<Key, CacheType> {
    private final ConcurrentMap<Key, Set<Consumer<CacheType>>> listeners = new MapMaker().concurrencyLevel(10).makeMap();

    private Set<Consumer<CacheType>> getListeners(Key key) {
        Preconditions.checkNotNull(key, "key");

        return listeners.computeIfAbsent(key, k -> Collections.newSetFromMap(new MapMaker().weakKeys().concurrencyLevel(10).makeMap()));
    }

    public void call(Key key, CacheType newData) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(newData, "newData");

        for (Consumer<CacheType> listener : getListeners(key)) {
            listener.accept(newData);
        }
    }

    public void addListener(Key key, Consumer<CacheType> listener) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(listener, "listener");

        getListeners(key).add(listener);
    }

    public void removeListener(Key key, Consumer<CacheType> listener) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(listener, "listener");

        getListeners(key).remove(listener);
    }

    public Iterable<Key> getAllKeys() {
        return Collections.unmodifiableSet(listeners.keySet());
    }

    public void removeAll(Key key) {
        listeners.remove(key);
    }
}
