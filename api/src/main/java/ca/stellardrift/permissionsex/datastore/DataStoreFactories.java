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
package ca.stellardrift.permissionsex.datastore;

import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.util.Locale;
import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;

class DataStoreFactories {
    // service loader, needs explicitly specified ClassLoader as a workaround for Bukkit/Bungee brokenness
    private static final ServiceLoader<DataStoreFactory> LOADER = ServiceLoader.load(DataStoreFactory.class, DataStoreFactory.class.getClassLoader());
    static final PMap<String, DataStoreFactory> REGISTRY;

    static {
        PMap<String, DataStoreFactory> factories = HashTreePMap.empty();
        for (final DataStoreFactory factory : LOADER) {
            factories = factories.plus(requireNonNull(factory.name(),
                    () -> "Factory in class " + factory.getClass() + " had a null name()").toLowerCase(Locale.ROOT), factory);
        }

        REGISTRY = factories;
    }

    private DataStoreFactories() {
    }

}
