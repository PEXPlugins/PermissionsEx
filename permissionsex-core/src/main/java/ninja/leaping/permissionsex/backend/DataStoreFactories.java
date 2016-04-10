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
package ninja.leaping.permissionsex.backend;

import com.google.common.base.Optional;
import ninja.leaping.permissionsex.backend.file.FileDataStore;
import ninja.leaping.permissionsex.backend.memory.MemoryDataStore;
import ninja.leaping.permissionsex.backend.sql.SqlDataStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStoreFactories {
    private static final Map<String, DataStoreFactory> REGISTRY = new ConcurrentHashMap<>();

    static {
        register("file", FileDataStore.FACTORY);
        register("memory", MemoryDataStore.FACTORY);
        register("sql", SqlDataStore.FACTORY);
    }

    private DataStoreFactories() {
    }

    public static void register(String type, DataStoreFactory factory) {
        REGISTRY.put(type, factory);
    }

    public static Optional<DataStoreFactory> get(String type) {
        return Optional.fromNullable(REGISTRY.get(type));
    }

}
