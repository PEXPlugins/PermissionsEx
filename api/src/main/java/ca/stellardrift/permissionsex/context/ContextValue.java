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

package ca.stellardrift.permissionsex.context;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A (key, value) pair for one specific context entry.
 *
 * <p>This value holds both raw and parsed context values.</p>
 *
 * @param <V> value type
 */
public final class ContextValue<V> {
    private final String key;
    private final String rawValue;
    private @Nullable ContextDefinition<V> definition;
    private @Nullable V parsedValue;

    public ContextValue(final String key, final String rawValue) {
        this.key = key;
        this.rawValue = rawValue;
    }

    ContextValue(final ContextDefinition<V> def, final V value) {
        this(def.name(), def.serialize(value));
        this.definition = def;
        this.parsedValue = value;
    }

    /**
     * Get the key used to resolve a context value.
     *
     * @return value key
     */
    public String key() {
        return this.key;
    }

    /**
     * The raw value, before being deserialized by a context definition.
     *
     * @return raw value, as provided by the user
     */
    public String rawValue() {
        return this.rawValue;
    }

    public @Nullable ContextDefinition<V> definition() {
        return this.definition;
    }

    public @Nullable V parsedValue() {
        return this.parsedValue;
    }

    @SuppressWarnings("unchecked")
    public boolean tryResolve(final ContextDefinitionProvider provider) {
        if (this.definition != null && !(this.definition instanceof SimpleContextDefinition.Fallback)) {
            return this.parsedValue != null;
        }
        final @Nullable ContextDefinition<V> definition = (ContextDefinition<V>) provider.getContextDefinition(this.key);
        if (definition != null) {
            this.definition = definition;
            this.parsedValue = definition.deserialize(this.rawValue);
            return this.parsedValue != null;
        }
        return false;
    }

    public V getParsedValue(final ContextDefinition<V> definition) {
        if (this.definition != null && this.definition != definition) {
            throw new IllegalStateException("The provided context definition does not match the one this context object currently knows about");
        }

        this.definition = definition;
        @Nullable V parsedValue = this.parsedValue;
        if (parsedValue == null) {
            parsedValue = definition.deserialize(this.rawValue);
            this.parsedValue = parsedValue;
        }
        if (parsedValue == null) {
            throw new IllegalArgumentException("Invalid value provided for context " + definition.name());
        }
        return parsedValue;
    }

    public V getParsedValue(final ContextDefinitionProvider provider) {
        @Nullable V tempParsed = parsedValue;
        if (tempParsed != null) {
            return tempParsed;
        }

        final @Nullable ContextDefinition<?> def = provider.getContextDefinition(this.key);
        tempParsed = def == null ? null : (V) def.deserialize(this.rawValue);
        if (tempParsed == null) {
            throw new RuntimeException("No definition for context " + this.key);
        }

        parsedValue = tempParsed;
        return tempParsed;
    }

    public boolean equals(final @Nullable Object other) {
        if (this == other) return true;
        if (!(other instanceof ContextValue<?>)) return false;

        final ContextValue<?> that = (ContextValue<?>) other;
        if (!this.key.equals(that.key)) return false;
        if (!this.rawValue.equals(that.rawValue)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = this.key.hashCode();
        result = 31 * result + this.rawValue.hashCode();
        return result;
    }

    public String toString() {
        return this.key + ":" + this.parsedValue + " (raw: " + this.rawValue + ")";
    }
}
