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

import org.immutables.value.Value;

/**
 * Options to configure a DataStore
 *
 * @param <C> Type of the store configuration objcet
 * @since 2.0.0
 */
@Value.Immutable(builder = false)
public interface StoreProperties<C> {

    static <C> StoreProperties<C> of(final String identifier, final C config, final DataStoreFactory factory) {
        return new StorePropertiesImpl<>(identifier, config, factory);
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
    DataStoreFactory factory();

}
