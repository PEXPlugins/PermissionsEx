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

package ca.stellardrift.permissionsex.backend;

import ca.stellardrift.permissionsex.backend.conversion.OpsDataStore;
import ca.stellardrift.permissionsex.backend.conversion.groupmanager.GroupManagerDataStore;
import ca.stellardrift.permissionsex.backend.conversion.luckperms.LuckPermsFileDataStore;
import ca.stellardrift.permissionsex.backend.conversion.luckperms.LuckPermsSqlDataStore;
import ca.stellardrift.permissionsex.backend.file.FileDataStore;
import ca.stellardrift.permissionsex.backend.memory.MemoryDataStore;
import ca.stellardrift.permissionsex.backend.sql.SqlDataStore;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DataStoreFactories {
    private static final Map<String, DataStoreFactory> REGISTRY = new ConcurrentHashMap<>();

    static {
        register(FileDataStore.FACTORY);
        register(MemoryDataStore.FACTORY);
        register(SqlDataStore.FACTORY);
        register(GroupManagerDataStore.FACTORY);
        register(LuckPermsFileDataStore.FACTORY);
        register(LuckPermsSqlDataStore.FACTORY);
        register(OpsDataStore.FACTORY);
    }

    private DataStoreFactories() {
    }

    private static void register(AbstractDataStore.Factory<?> factory) {
        register(factory.getType(), factory);
    }

    public static void register(String type, DataStoreFactory factory) {
        REGISTRY.put(type, factory);
    }

    public static Optional<DataStoreFactory> get(String type) {
        return Optional.ofNullable(REGISTRY.get(type));
    }

    public static Set<String> getKnownTypes() {
        return ImmutableSet.copyOf(REGISTRY.keySet());
    }

}
