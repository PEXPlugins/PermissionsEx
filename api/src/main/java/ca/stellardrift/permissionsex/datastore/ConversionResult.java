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

import net.kyori.adventure.text.Component;
import org.immutables.value.Value;

/**
 * A possible result of a conversion lookup.
 *
 * @since 2.0.0
 */
@Value.Immutable
public interface ConversionResult {

    /**
     * Create a new builder for a conversion result.
     *
     * @return new builder
     * @since 2.0.0
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * A short description of the data that will be converted.
     *
     * @return conversion description
     * @since 2.0.0
     */
    Component description();

    /**
     * The data store, configured based on the discovered environment but not yet initialized.
     *
     * @return convertible data store
     * @since 2.0.0
     */
    DataStore store();

    /**
     * Builder for a conversion result.
     *
     * @since 2.0.0
     */
    class Builder extends ConversionResultImpl.Builder {
        Builder() {}
    }

}
