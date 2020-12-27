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
package ca.stellardrift.permissionsex.minecraft.command;

import ca.stellardrift.permissionsex.subject.SubjectRef;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.immutables.value.Value;

import static java.util.Objects.requireNonNull;

/**
 * A representation of a permission.
 *
 * [value] is a dotted string
 * [description] describes the permission's purpose, including ways it may be specialized in use
 * [default] is the value to resolve this permission to when unset
 */
@Value.Immutable(builder = false)
public interface Permission {

    static Permission of(final String permission) {
        return of(permission, Component.empty(), 0);
    }

    static Permission of(final String permission, final Component description) {
        return of(permission, description, 0);
    }

    static Permission of(final String permission, final Component component, final int defaultValue) {
        return new PermissionImpl(permission, component, defaultValue);
    }

    static Permission pex(final String value) {
        return new PermissionImpl("permissionsex." + value, Component.empty(), 0);
    }

    /**
     * A dot-separated string of permission elements.
     *
     * @return the permission value
     */
    @Value.Parameter
    String value();

    Permission value(String value);

    /**
     * A description of the purpose of this permission.
     *
     * @return the description, or empty
     */
    @Value.Parameter
    @Nullable Component description();

    /**
     * A default value for when this permission is unassigned.
     *
     * @return the default value
     */
    @Value.Parameter
    @Value.Default
    default int defaultValue() {
        return 0;
    }

    default Permission then(final SubjectRef<?> other) {
        requireNonNull(other, "other");
        return this.value(this.value() + '.' + other.type().name() + '.' + other.serializedIdentifier());
    }

    default Permission then(final String other) {
        requireNonNull(other, "other");
        return this.value(this.value() + '.' + other);
    }
}
