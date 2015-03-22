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
package ninja.leaping.permissionsex;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import ninja.leaping.permissionsex.backends.DataStore;
import ninja.leaping.permissionsex.backends.memory.MemoryDataStore;
import ninja.leaping.permissionsex.config.PermissionsExConfiguration;
import ninja.leaping.permissionsex.data.CalculatedSubject;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

public class PermissionsEx implements ImplementationInterface {
    private static final Map.Entry<String, String> DEFAULT_IDENTIFIER = Maps.immutableEntry("default", "global");
    private final PermissionsExConfiguration config;
    private final ImplementationInterface impl;
    private DataStore activeDataStore;
    private final ConcurrentMap<String, SubjectCache> subjectCaches = new ConcurrentHashMap<>(), transientSubjectCaches = new ConcurrentHashMap<>();
    private final LoadingCache<Map.Entry<String, String>, CalculatedSubject> calculatedSubjects = CacheBuilder.newBuilder().maximumSize(512).build(new CacheLoader<Map.Entry<String, String>, CalculatedSubject>() {
        @Override
        public CalculatedSubject load(Map.Entry<String, String> key) throws Exception {
            return new CalculatedSubject(key, PermissionsEx.this);
        }
    });
    private final MemoryDataStore transientData;

    public PermissionsEx(PermissionsExConfiguration config, ImplementationInterface impl) throws PermissionsLoadingException {
        this.config = config;
        this.impl = impl;
        this.transientData = new MemoryDataStore();
        this.transientData.initialize(this);
        this.activeDataStore = config.getDefaultDataStore();
        this.activeDataStore.initialize(this);
        getSubjects("group").cacheAll();
    }

    public SubjectCache getSubjects(String type) {
        Preconditions.checkNotNull(type, "type");
        SubjectCache cache = subjectCaches.get(type);
        if (cache == null) {
            cache = new SubjectCache(type, activeDataStore);
            SubjectCache newCache = subjectCaches.putIfAbsent(type, cache);
            if (newCache != null) {
                cache = newCache;
            }
        }
        return cache;
    }

    public SubjectCache getTransientSubjects(String type) {
        Preconditions.checkNotNull(type, "type");
        SubjectCache cache = transientSubjectCaches.get(type);
        if (cache == null) {
            cache = new SubjectCache(type, transientData);
            SubjectCache newCache = transientSubjectCaches.putIfAbsent(type, cache);
            if (newCache != null) {
                cache = newCache;
            }
        }
        return cache;
    }

    public void uncache(String type, String identifier) {
        SubjectCache cache = subjectCaches.get(type);
        if (cache != null) {
            cache.invalidate(identifier);
        }
        cache = transientSubjectCaches.get(type);
        if (cache != null) {
            cache.invalidate(identifier);
        }
        calculatedSubjects.invalidate(Maps.immutableEntry(type, identifier));
    }

    /**
     * Imports data into the currently active backend from the backend identified by the provided identifier
     *
     * @param dataStoreIdentifier The identifier of the backend to import from
     * @return A future that completes once the import operation is complete
     */
    public ListenableFuture<Void> importDataFrom(String dataStoreIdentifier) {
        DataStore expected = config.getDataStore(dataStoreIdentifier);
        if (expected == null) {
            return Futures.immediateFailedFuture(new IllegalArgumentException("Data store " + dataStoreIdentifier + " is not present"));
        }

        // TODO: Actually suppress saves while running this -- maybe with a bulk setAll method?
        return Futures.transform(Futures.allAsList(Iterables.transform(expected.getAll(), new Function<Map.Entry<Map.Entry<String, String>, ImmutableOptionSubjectData>, ListenableFuture<ImmutableOptionSubjectData>>() {
            @Nullable
            @Override
            public ListenableFuture<ImmutableOptionSubjectData> apply(Map.Entry<Map.Entry<String, String>, ImmutableOptionSubjectData> input) {
                return activeDataStore.setData(input.getKey().getKey(), input.getKey().getValue(), input.getValue());
            }
        })), new Function<List<ImmutableOptionSubjectData>, Void>() {
            @Nullable
            @Override
            public Void apply(@Nullable List<ImmutableOptionSubjectData> input) {
                return null;
            }
        });
    }

    public boolean hasDebugMode() {
        return config.isDebugEnabled();
    }

    public void close() {
        this.activeDataStore.close();
    }

    @Override
    public File getBaseDirectory() {
        return impl.getBaseDirectory();
    }

    @Override
    public Logger getLogger() {
        return impl.getLogger();
    }

    @Override
    public DataSource getDataSourceForURL(String url) {
        return impl.getDataSourceForURL(url);
    }

    @Override
    public void executeAsyncronously(Runnable run) {
        impl.executeAsyncronously(run);
    }

    public PermissionsExConfiguration getConfig() {
        return this.config;
    }

    /**
     * Get the identifier to access default subject data
     *
     * @return The identifier referring to default subject data
     */
    public Map.Entry<String, String> getDefaultIdentifier() {
        return DEFAULT_IDENTIFIER;
    }

    public CalculatedSubject getCalculatedSubject(String type, String identifier) throws PermissionsLoadingException {
        try {
            return calculatedSubjects.get(Maps.immutableEntry(type, identifier));
        } catch (ExecutionException e) {
            throw new PermissionsLoadingException("While calculating subject data for " + type + ":" + identifier, e);
        }
    }

    public Iterable<? extends CalculatedSubject> getActiveCalculatedSubjects() {
        return Collections.unmodifiableCollection(calculatedSubjects.asMap().values());
    }
}
