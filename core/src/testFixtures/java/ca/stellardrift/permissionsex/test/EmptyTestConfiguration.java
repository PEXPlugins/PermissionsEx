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
package ca.stellardrift.permissionsex.test;

import ca.stellardrift.permissionsex.impl.backend.memory.MemoryDataStore;
import ca.stellardrift.permissionsex.impl.config.EmptyPlatformConfiguration;
import ca.stellardrift.permissionsex.impl.config.PermissionsExConfiguration;
import ca.stellardrift.permissionsex.datastore.DataStore;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class EmptyTestConfiguration implements PermissionsExConfiguration<EmptyPlatformConfiguration> {
    private final DataStore defaultDataStore = MemoryDataStore.create("test");
    private final EmptyPlatformConfiguration platformConfig = new EmptyPlatformConfiguration();

    @Override
     public DataStore getDataStore(final @Nullable String name) {
        if (Objects.equals(name, defaultDataStore.getName())) {
            return this.defaultDataStore;
        } else {
            throw new IllegalArgumentException("Unknown data store " + name);
        }
    }

    @Override
    public DataStore getDefaultDataStore() {
        return this.defaultDataStore;
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public List<String> getServerTags() {
        return Collections.emptyList();
    }

    @Override
    public void validate() {
    }

    @Override
    public EmptyPlatformConfiguration getPlatformConfig() {
        return this.platformConfig;
    }

    @Override
    public PermissionsExConfiguration<EmptyPlatformConfiguration> reload() {
        return this;
    }
}
