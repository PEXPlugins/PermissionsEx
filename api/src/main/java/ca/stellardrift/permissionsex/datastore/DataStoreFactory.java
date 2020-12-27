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

import ca.stellardrift.permissionsex.PermissionsEngine;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A factory to create a data store from its identifier and configuration options.
 *
 * <p>Data store factory implementations are discovered using the
 * JDK {@link java.util.ServiceLoader} interface.</p>
 *
 * @since 2.0.0
 */
public interface DataStoreFactory {

    /**
     * Get a factory for the type identifier.
     *
     * <p>The type identifier is not case-sensitive.</p>
     *
     * @param type requested type
     * @return a factory, or null if none is available.
     */
    static @Nullable DataStoreFactory forType(final String type) {
        return DataStoreFactories.REGISTRY.get(requireNonNull(type, "type").toLowerCase(Locale.ROOT));
    }

    static Map<String, DataStoreFactory> all() {
        return DataStoreFactories.REGISTRY;
    }

    /**
     * A user-visible name for this data store type.
     *
     * @return the data store type name
     */
    Component friendlyName();

    /**
     * The name for the data store type, usable for lookup.
     *
     * @return data store type name
     * @since 2.0.0
     */
    String name();

    /**
     * Create a new data store.
     *
     * @param identifier the identifier for this specific data store instance.
     * @param config options used to configure the data store
     * @return a new data store instance, not yet loaded
     * @throws PermissionsLoadingException if any of the configuration options are invalid
     * @since 2.0.0
     */
    DataStore create(String identifier, ConfigurationNode config) throws PermissionsLoadingException;

    /**
     * A data store type that is used for migration from other permissions systems.
     *
     * <p>These data store types can discover existing data in the environment and present
     * conversion options to the user.</p>
     *
     * @since 2.0.0
     */
    interface Convertable extends DataStoreFactory {

        /**
         * Return a list of all possible conversion options for this data store.
         *
         * @param pex the permissions engine instance
         * @return an unmodifiable list of possible conversion results
         * @since 2.0.0
         */
        List<ConversionResult> listConversionOptions(final PermissionsEngine pex);
    }
}
