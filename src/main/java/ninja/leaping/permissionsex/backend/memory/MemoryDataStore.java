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
package ninja.leaping.permissionsex.backend.memory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.permissionsex.backend.AbstractDataStore;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A data store backed entirely in memory
 */
public class MemoryDataStore extends AbstractDataStore {
    public static final Factory FACTORY = new Factory("memory", MemoryDataStore.class);

    @Setting(comment = "Whether or not this data store will store subjects being set") private boolean track = true;

    private final ConcurrentMap<Map.Entry<String, String>, ImmutableOptionSubjectData> data = new ConcurrentHashMap<>();

    public MemoryDataStore() {
        super(FACTORY);
    }

    @Override
    protected void initializeInternal() {

    }

    @Override
    public void close() {

    }

    @Override
    public ImmutableOptionSubjectData getDataInternal(String type, String identifier) {
        final Map.Entry<String, String> key = Maps.immutableEntry(type, identifier);
        ImmutableOptionSubjectData ret = data.get(key);
        if (ret == null) {
            ret = new MemoryOptionSubjectData();
            if (track) {
                final ImmutableOptionSubjectData existingData = data.putIfAbsent(key, ret);
                if (existingData != null) {
                    ret = existingData;
                }
            }
        }
        return ret;
    }

    @Override
    public ListenableFuture<ImmutableOptionSubjectData> setDataInternal(String type, String identifier, ImmutableOptionSubjectData data) {
        if (track) {
            this.data.put(Maps.immutableEntry(type, identifier), data);
        }

        return Futures.immediateFuture(data);
    }

    @Override
    public boolean isRegistered(String type, String identifier) {
        return data.containsKey(Maps.immutableEntry(type, identifier));
    }

    @Override
    public Iterable<String> getAllIdentifiers(final String type) {
        return Iterables.transform(Maps.filterKeys(data, new Predicate<Map.Entry<String, String>>() {
            @Override
            public boolean apply(@Nullable Map.Entry<String, String> input) {
                return input.getKey().equals(type);
            }
        }).keySet(), new Function<Map.Entry<String, String>, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Map.Entry<String, String> input) {
                return input.getValue();
            }
        });
    }

    @Override
    public Iterable<String> getRegisteredTypes() {
        return ImmutableSet.copyOf(Iterables.transform(data.keySet(), new Function<Map.Entry<String, String>, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Map.Entry<String, String> input) {
                return input.getKey();
            }
        }));
    }

    @Override
    public Iterable<Map.Entry<Map.Entry<String, String>, ImmutableOptionSubjectData>> getAll() {
        return Iterables.unmodifiableIterable(data.entrySet());
    }

    @Override
    protected  <T> T performBulkOperationSync(Function<DataStore, T> function) {
        return function.apply(this);
    }
}
