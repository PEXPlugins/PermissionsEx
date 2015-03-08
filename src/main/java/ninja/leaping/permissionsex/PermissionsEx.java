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
import ninja.leaping.permissionsex.config.PermissionsExConfiguration;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import org.slf4j.Logger;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PermissionsEx {
    private final Logger logger;
    private final PermissionsExConfiguration config;
    private final File basedir;
    private DataStore activeDataStore;
    private final ConcurrentMap<String, SubjectCache> subjectCaches = new ConcurrentHashMap<>();
    private final SubjectCache userCache, groupCache;

    public PermissionsEx(PermissionsExConfiguration config, File basedir, Logger logger) throws PermissionsLoadingException {
        this.config = config;
        this.basedir = basedir;
        this.logger = logger;
        this.activeDataStore = config.getDefaultDataStore();
        this.activeDataStore.initialize(this);
        this.userCache = getSubjects("users");
        this.groupCache = getSubjects("groups");
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

    public void close() {
        this.activeDataStore.close();
    }

    public File getBaseDirectory() {
        return this.basedir;
    }

    public DataStore getActiveDataStore() {
        return this.activeDataStore;
    }

    public Logger getLogger() {
        return this.logger;
    }
}
