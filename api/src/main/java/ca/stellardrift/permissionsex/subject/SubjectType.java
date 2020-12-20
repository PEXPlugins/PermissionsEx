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
import org.pcollections.PMap;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A definition for parameters controlling a {@link SubjectTypeCollection}'s handling.
 *
 * @param <I> identifier type
 * @since 2.0.0
 */
public final class SubjectType<I> {
    private final String name;
    private final Class<I> identifierType;
    private final boolean transientHasPriority;
    private final Function<I, Boolean> undefinedValueProvider;
    private final Function<String, I> identifierDeserializer;
    private final Function<I, String> identifierSerializer;
    private final Function<I, @Nullable ?> associatedObjectProvider;
    private final Function<String, I> friendlyDeserializer;

    /**
     * Create a new builder for subject types.
     *
     * @param name the subject type name
     * @param identifierType explicit type of identifiers
     * @param <V> the identifier type
     * @return a builder for a subject type
     * @since 2.0.0
     */
    public static <V> Builder<V> builder(final String name, final Class<V> identifierType) {
        return new Builder<>(name, identifierType);
    }

    /**
     * Create a new builder for subject types using a string identifier.
     *
     * @param name the subject type name
     * @return a builder for a subject type
     * @since 2.0.0
     */
    public static Builder<String> stringIdentBuilder(final String name) {
        return builder(name, String.class)
                .serializedBy(Function.identity())
                .deserializedBy(Function.identity());
    }

    /**
     * Create a subject type from a builder.
     *
     * @param builder the builder
     */
    SubjectType(final Builder<I> builder) {
        this.name = builder.name;
        this.identifierType = builder.identifierType;
        this.transientHasPriority = builder.transientHasPriority;
        this.undefinedValueProvider = builder.undefinedValueProvider;
        this.associatedObjectProvider = builder.associatedObjectProvider;
        this.friendlyDeserializer = builder.friendlyDeserializer;
        this.identifierDeserializer = requireNonNull(builder.identifierDeserializer, "Identifier deserializer has not been provided for subject type " + this.name);
        this.identifierSerializer = requireNonNull(builder.identifierSerializer, "Identifier serializer has not been set for subject type " + this.name);
    }

    /**
     * The name of the subject type this defines.
     *
     * @return the type name
     * @since 2.0.0
     */
    public final String name() {
        return this.name;
    }

    /**
     * Return whether or not transient data takes priority over persistent for this subject type.
     *
     * @return Whether or not transient data has priority.
     * @since 2.0.0
     */
    public final boolean transientHasPriority() {
        return this.transientHasPriority;
    }

    /**
     * Check if a name is a valid identifier for a given subject collection
     *
     * @param serialized The identifier to check
     * @return Whether or not the given name is a valid identifier
     */
    public boolean isIdentifierValid(final String serialized) {
        try {
            parseIdentifier(serialized);
            return true;
        } catch (final InvalidIdentifierException ex) {
            return false;
        }
    }

    /**
     * Parse an identifier given its serialized string representation.
     *
     * @param input the serialized form
     * @return a parsed identifier
     * @throws InvalidIdentifierException if an identifier is not of appropriate format for
     *         this subject type.
     */
    public I parseIdentifier(final String input) {
        return this.identifierDeserializer.apply(requireNonNull(input, "input"));
    }

    /**
     * Serialize an identifier to its canonical represenattion.
     *
     * @param input the identifier
     * @return the canonical representation of the identifier
     */
    public String serializeIdentifier(final I input)  {
        return this.identifierSerializer.apply(requireNonNull(input, "input"));
    }

    /**
     * Attempt to parse an identifier, while also attempting to resolve from any user-friendly
     * display name that may be available.
     *
     * <p>Unlike {@link #parseIdentifier(String)}, this will not throw a
     * {@link InvalidIdentifierException} when identifiers are of an invalid format. Instead it may
     * attempt to perform some sort of lookup to resolve an identifier from the
     * provided information.</p>
     *
     * @param name The friendly name that may be used
     * @return A standard representation of the subject identifier
     */
    public @Nullable I parseOrCoerceIdentifier(String name) {
        try {
            return this.parseIdentifier(name);
        } catch (final InvalidIdentifierException ex) {
            return this.friendlyDeserializer.apply(name);
        }
    }

    /**
     * The native object that may be held
     *
     * @param identifier type
     * @return A native object that has its permissions defined by this subject
     */
    public @Nullable Object getAssociatedObject(I identifier) {
        return this.associatedObjectProvider.apply(identifier);
    }

    /**
     * The boolean value an undefined permission should have for this subject type
     */
    public boolean undefinedPermissionValue(final I identifier) {
        return this.undefinedValueProvider.apply(requireNonNull(identifier));
    }

    @Override
    public int hashCode() {
        return 7 * this.name.hashCode()
                + 31 * this.identifierType.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof SubjectType)) {
            return false;
        }

        final SubjectType<?> that = (SubjectType<?>) other;
        return this.name.equals(that.name)
                && this.identifierType.equals(that.identifierType);
    }

    @Override
    public String toString() {
        return "SubjectType<" + this.identifierType.getSimpleName() + ">(name=" + this.name + ")";
    }

    /**
     * A builder for a subject type
     * @param <I> identifier type
     */
    public static final class Builder<I> {
        private final String name;
        private final Class<I> identifierType;
        private boolean transientHasPriority = true;
        private Function<I, Boolean> undefinedValueProvider = $ -> false;
        private Function<I, @Nullable ?> associatedObjectProvider = $ -> null;
        private Function<String, @Nullable I> friendlyDeserializer = $ -> null;
        private @Nullable Function<String, I> identifierDeserializer; // required
        private @Nullable Function<I, String> identifierSerializer; // required

        Builder(final String name, final Class<I> identifierType) {
            requireNonNull(name, "name");
            requireNonNull(identifierType, "identifierType");
            this.name = name;
            this.identifierType = identifierType;
        }

        Builder(final SubjectType<I> existing) {
            this.name = existing.name;
            this.identifierType = existing.identifierType;
            this.transientHasPriority = existing.transientHasPriority;
            this.undefinedValueProvider = existing.undefinedValueProvider;
            this.associatedObjectProvider = existing.associatedObjectProvider;
            this.friendlyDeserializer = existing.friendlyDeserializer;
            this.identifierDeserializer = existing.identifierDeserializer;
            this.identifierSerializer = existing.identifierSerializer;
        }

        /**
         * Whether or not this subject resolves data transient-first or persistent first.
         *
         * <p>The default value for this property is {@code true}.</p>
         *
         * @param priority if transient data should take priority over persistent.
         * @return this builder
         * @since 2.0.0
         */
        public Builder<I> transientHasPriority(final boolean priority) {
            this.transientHasPriority = priority;
            return this;
        }

        /**
         * Set the provider for a fallback permissions value when an undefined
         * value ({@code 0}) is resolved.
         *
         * @param provider the value provider
         * @return this builder
         * @since 2.0.0
         */
        public Builder<I> undefinedValues(final Function<I, Boolean> provider) {
            requireNonNull(provider, "provider");
            this.undefinedValueProvider = provider;
            return this;
        }

        public Builder<I> serializedBy(final Function<I, String> serializer) {
            this.identifierSerializer = requireNonNull(serializer, "serializer");
            return this;
        }

        /**
         * Attempt to deserialize an identifier from the raw input
         *
         * <p>On failure, the function may throw a {@link InvalidIdentifierException}</p>
         *
         * @param deserializer the deserialization function
         * @return this builder
         */
        public Builder<I> deserializedBy(final Function<String, I> deserializer) {
            this.identifierDeserializer = requireNonNull(deserializer, "deserializer");
            return this;
        }

        /**
         * Provide a function that can resolve a deserialized identifier from a 'friendly' name.
         *
         * <p>This function should never throw an {@link InvalidIdentifierException}.</p>
         *
         * @param coercer the coercion function
         * @return this builder
         */
        public Builder<I> friendlyNameResolvedBy(final Function<String, @Nullable I> coercer) {
            this.friendlyDeserializer = requireNonNull(coercer, "coercer");
            return this;
        }

        /**
         * Set a provider for associated objects.
         *
         * @param provider the associated object provider. may return null if no associated object is available.
         * @return this builder
         * @since 2.0.0
         */
        public Builder<I> associatedObjects(final Function<I, @Nullable ?> provider) {
            this.associatedObjectProvider = provider;
            return this;
        }

        /**
         * Create a subject type with a fixed set of entries.
         *
         * @param entries a map of identifier to associated object provider
         * @return this builder
         * @since 2.0.0
         */
        public Builder<I> fixedEntries(final Map<I, ? extends Supplier<@Nullable ?>> entries) {
            requireNonNull(entries, "entries");

            // Use the map for discovering associated objects
            this.associatedObjectProvider = ident -> {
                final Supplier<?> value = entries.get(ident);
                return value == null ? null : value.get();
            };

            // And restrict the range of our identifier deserializer to available entries.
            final @Nullable Function<String, I> oldDeserializer = this.identifierDeserializer;
            if (oldDeserializer == null) {
                throw new IllegalStateException("An identifier deserializer must have already been set "
                        + "to be able to restrict the valid identifiers.");
            }
            this.identifierDeserializer = serialized -> {
                final I candidate = oldDeserializer.apply(serialized);
                if (!entries.containsKey(candidate)) {
                    throw new InvalidIdentifierException(serialized);
                }
                return candidate;
            };
            return this;
        }

        /**
         * Create a subject type with a fixed set of entries.
         *
         * @param entries a map of identifier to associated object provider
         * @return this builder
         * @since 2.0.0
         */
        @SafeVarargs
        public final Builder<I> fixedEntries(final Map.Entry<I, ? extends Supplier<@Nullable ?>>... entries) {
            PMap<I, Supplier<@Nullable ?>> result = HashTreePMap.empty();
            for (final Map.Entry<I, ? extends Supplier<@Nullable ?>> entry : entries) {
                result = result.plus(entry.getKey(), entry.getValue());
            }
            return this.fixedEntries(result);
        }

        /**
         * Create a subject type from the provided parameters
         *
         * @return the parameters
         */
        public SubjectType<I> build() {
            return new SubjectType<>(this);
        }

    }
}
