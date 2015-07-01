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
import com.google.common.collect.MapMaker;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public class CacheListenerHolder<Key, CacheType> {
    private final ConcurrentMap<Key, Set<Caching<CacheType>>> listeners = new MapMaker().concurrencyLevel(10).makeMap();

    private Set<Caching<CacheType>> getListeners(Key key) {
        Preconditions.checkNotNull(key, "key");

        Set<Caching<CacheType>> set = listeners.get(key);
        if (set == null) {
            set = Collections.newSetFromMap(new MapMaker().weakKeys().concurrencyLevel(10).<Caching<CacheType>, Boolean>makeMap());
            Set<Caching<CacheType>> potentialNewSet = listeners.putIfAbsent(key, set);
            if (potentialNewSet != null) {
                set = potentialNewSet;
            }
        }
        return set;
    }

    public void call(Key key, CacheType newData) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(newData, "newData");

        for (Caching<CacheType> listener : getListeners(key)) {
            listener.clearCache(newData);
        }

    }

    public void addListener(Key key, Caching<CacheType> listener) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(listener, "listener");

        getListeners(key).add(listener);
    }

    public void removeListener(Key key, Caching<CacheType> listener) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(listener, "listener");

        getListeners(key).remove(listener);
    }

    public void removeAll(Key key) {
        listeners.remove(key);
    }
}
