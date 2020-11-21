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
package ca.stellardrift.permissionsex.util;

import org.immutables.value.Value;

/**
 * Represents a change in the value of some object.
 *
 * @param <T> the type of object being changed
 * @since 2.0.0
 */
@Value.Immutable(builder = false)
public interface Change<T> {

    static <T> Change<T> of(final T old, final T current) {
        return new ChangeImpl<>(old, current);
    }

    /**
     * The previous value before the operation.
     *
     * @return the previous value
     * @since 2.0.0
     */
    @Value.Parameter
    T old();

    /**
     * The current value.
     *
     * @return the current value
     * @since 2.0.0
     */
    @Value.Parameter
    T current();

    /**
     * Get whether any change actually occurred.
     *
     * @return if a change occurred
     * @since 2.0.0
     */
    default boolean changed() {
        return old() != current();
    }
}
