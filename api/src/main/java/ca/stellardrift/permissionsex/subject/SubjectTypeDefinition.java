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
package ca.stellardrift.permissionsex.subject;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.pcollections.HashTreePMap;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A definition for parameters controlling a {@link SubjectType}'s handling.
 *
 * @param <A> associated object type
 */
public abstract class SubjectTypeDefinition<A> {
    private final String typeName;
    private final boolean transientHasPriority;

    protected SubjectTypeDefinition(final String typeName) {
        this(typeName, true);
    }

    protected SubjectTypeDefinition(final String typeName, final boolean transientHasPriority) {
        this.typeName = requireNonNull(typeName, "typeName");
        this.transientHasPriority = transientHasPriority;
    }

    /**
     * Create a type definition with default values for the provided type.
     *
     * <p>By default, transient data will take priority, so {@link #transientHasPriority()} will
     * return {@code true}.</p>
     *
     * @param typeName name of the subject type
     * @return a new subject type definition
     * @since 2.0.0
     */
    public static SubjectTypeDefinition<Void> of(final String typeName) {
        return of(typeName, true);
    }

    /**
     * Create a type definition with default values for the provided type.
     *
     * @param typeName name of the subject type
     * @param transientHasPriority Whether transient values take priority over persisted values for
     *                             this subject type.
     * @return a new subject type definition
     * @since 2.0.0
     */
    public static SubjectTypeDefinition<Void> of(final String typeName, final boolean transientHasPriority) {
        return new Default(typeName, transientHasPriority);
    }

    /**
     * Get a type definition that accepts a fixed set of subject identifiers.
     *
     * @param typeName name of the subject type
     * @param validEntries a set of valid values mapped to associated object suppliers.
     * @param <A> attached object type
     * @return a new subject type definition
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public static <A> SubjectTypeDefinition<A> of(final String typeName, final Map.Entry<String, Supplier<A>>... validEntries) {
        return of(typeName, null, validEntries);
    }

    /**
     * Get a type definition that accepts a fixed set of subject identifiers.
     *
     * @param typeName name of the subject type
     * @param undefinedValueProvider a callback to provide a default value when the unknown
     *                               permission is returned from a query.
     * @param validEntries a set of valid values mapped to associated object suppliers.
     * @param <A> attached object type
     * @return a new subject type definition
     */
    @SafeVarargs
    public static <A> SubjectTypeDefinition<A> of(final String typeName,
                                                  final @Nullable Function<String, Boolean> undefinedValueProvider,
                                                  final Map.Entry<String, Supplier<A>>... validEntries) {
        final Map<String, Supplier<A>> validEntryMap = new HashMap<>(validEntries.length);
        for (final Map.Entry<String, Supplier<A>> entry : validEntries) {
            validEntryMap.put(entry.getKey(), entry.getValue());
        }
        return new FixedEntries<>(requireNonNull(typeName, "typeName"), validEntryMap, undefinedValueProvider);
    }

    /**
     * Get a type definition that accepts a fixed set of subject identifiers.
     *
     * @param typeName name of the subject type
     * @param undefinedValueProvider a callback to provide a default value when the unknown
     *                               permission is returned from a query.
     * @param validEntries a map of valid values to associated object suppliers.
     * @param <A> attached object type
     * @return a new subject type definition
     */
    public static <A> SubjectTypeDefinition<A> of(final String typeName,
                                                  final @Nullable Function<String, Boolean> undefinedValueProvider,
                                                  final Map<String, Supplier<A>> validEntries) {
        return new FixedEntries<>(requireNonNull(typeName, "typeName"), HashTreePMap.from(validEntries), undefinedValueProvider);
    }

    /**
     * The name of the subject type this defines.
     *
     * @return the type name
     * @since 2.0.0
     */
    public final String typeName() {
        return this.typeName;
    }

    /**
     * Return whether or not transient data takes priority over persistent for this subject type.
     *
     * @return Whether or not transient data has priority.
     */
    public final boolean transientHasPriority() {
        return this.transientHasPriority;
    }

    /**
     * Check if a name is a valid identifier for a given subject collection
     *
     * @param name The identifier to check
     * @return Whether or not the given name is a valid identifier
     */
    public abstract boolean isNameValid(final String name);

    /**
     * Return the internal identifier to be used for a subject given its friendly name.
     * If the given name is already a valid identifier, this method may return an empty optional.
     *
     * @param name The friendly name that may be used
     * @return A standard representation of the subject identifier
     */
    public abstract @Nullable String getAliasForName(String name);

    /**
     * The native object that may be held
     *
     * @param identifier type
     * @return A native object that has its permissions defined by this subject
     */
    public abstract @Nullable A getAssociatedObject(String identifier);

    /**
     * The boolean value an undefined permission should have for this subject type
     */
    public boolean undefinedPermissionValue(final String identifier) {
        return false;
    }

    /**
     * A subject type definition with no validity restrictions or custom parameters.
     */
    static final class Default extends SubjectTypeDefinition<Void> {

        Default(final String typeName, final boolean transientHasPriority) {
            super(typeName, transientHasPriority);
        }

        @Override
        public boolean isNameValid(String name) {
            return true;
        }

        @Override
        public @Nullable String getAliasForName(String name) {
            return null;
        }

        @Override
        public @Nullable Void getAssociatedObject(String identifier) {
            return null;
        }
    }

    static final class FixedEntries<V> extends SubjectTypeDefinition<V> {
        private final Map<String, Supplier<V>> validEntries;
        private final @Nullable Function<String, Boolean> undefinedValueProvider;

        FixedEntries(final String typeName,
                     final Map<String, Supplier<V>> validEntries,
                     final @Nullable Function<String, Boolean> undefinedValueProvider) {
            super(typeName);
            this.validEntries = validEntries;
            this.undefinedValueProvider = undefinedValueProvider;
        }

        @Override
        public boolean isNameValid(final String name) {
            return this.validEntries.containsKey(name);
        }

        @Override
        public @Nullable String getAliasForName(final String name) {
            return null;
        }

        @Override
        public @Nullable V getAssociatedObject(final String identifier) {
            final @Nullable Supplier<V> provider = validEntries.get(requireNonNull(identifier, "identifier"));
            return provider == null ? null : provider.get();
        }

        @Override
        public boolean undefinedPermissionValue(final String identifier) {
            if (this.undefinedValueProvider != null) {
                return this.undefinedValueProvider.apply(identifier);
            } else {
                return super.undefinedPermissionValue(identifier);
            }
        }

    }
}
