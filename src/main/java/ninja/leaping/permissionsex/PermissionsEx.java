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

import com.google.common.base.Preconditions;
import ninja.leaping.permissionsex.backends.DataStore;
import ninja.leaping.permissionsex.backends.memory.MemoryDataStore;
import ninja.leaping.permissionsex.config.PermissionsExConfiguration;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PermissionsEx implements ImplementationInterface {
    private final PermissionsExConfiguration config;
    private final ImplementationInterface impl;
    private DataStore activeDataStore;
    private final ConcurrentMap<String, SubjectCache> subjectCaches = new ConcurrentHashMap<>(), transientSubjectCaches = new ConcurrentHashMap<>();
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

    public boolean hasDebugMode() {
        return config.isDebugEnabled();
    }

    public void close() {
        this.activeDataStore.close();
    }

    public File getBaseDirectory() {
        return impl.getBaseDirectory();
    }

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
}
