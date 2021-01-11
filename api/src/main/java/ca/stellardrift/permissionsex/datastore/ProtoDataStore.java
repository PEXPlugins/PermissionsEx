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

import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import org.immutables.value.Value;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

/**
 * The deserialized configuration options from a data store.
 *
 * <p>This can be loaded to a full data store by querying the associated factory.</p>
 *
 * @param <C> Type of the store configuration object
 * @since 2.0.0
 */
@Value.Immutable(builder = false)
public interface ProtoDataStore<C> {

    static <C> ProtoDataStore<C> of(final String identifier, final C config, final DataStoreFactory<C> factory) {
        return new ProtoDataStoreImpl<>(identifier, config, factory);
    }

    /**
     * Identifier for a single data store instance.
     *
     * @return store identifier
     * @since 2.0.0
     */
    @Value.Parameter
    String identifier();

    /**
     * The object holding data store configuration.
     *
     * @return store configuration
     * @since 2.0.0
     */
    @Value.Parameter
    C config();

    /**
     * Factory for the type of data store used.
     *
     * @return factory instance
     * @since 2.0.0
     */
    @Value.Parameter
    DataStoreFactory<C> factory();

    /**
     * Given a data store's properties, resolve a full data store.
     *
     * @return the full data store
     */
    default DataStore defrost(final DataStoreContext context) throws PermissionsLoadingException {
        return this.factory().defrost(context, this);
    }

    default void serialize(final ConfigurationNode node) throws SerializationException {
        this.factory().serialize(node, this);
    }

}
