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
package ca.stellardrift.permissionsex.impl.config;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.pcollections.PCollection;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.util.Types;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

final class PCollectionSerializer<T extends PCollection<?>> implements TypeSerializer<T> {
    // originally a copy of AbstractListChildSerializer from Configurate, but heavily modified for just persistent collections
    private final Supplier<T> emptySupplier;

    PCollectionSerializer(final Supplier<T> emptySupplier) {
        this.emptySupplier = emptySupplier;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final T deserialize(final Type type, final ConfigurationNode node) throws SerializationException {
        final Type entryType = elementType(type);
        final @Nullable TypeSerializer<?> entrySerial = node.options().serializers().get(entryType);
        if (entrySerial == null) {
            throw new SerializationException(node, entryType, "No applicable type serializer for type");
        }

        if (node.isList()) {
            final List<? extends ConfigurationNode> values = node.childrenList();
            PCollection<Object> ret = (PCollection<Object>) this.emptySupplier.get();
            for (final ConfigurationNode value : values) {
                try {
                    ret = ret.plus(entrySerial.deserialize(entryType, value));
                } catch (final SerializationException ex) {
                    ex.initPath(value::path);
                    throw ex;
                }
            }
            return (T) ret;
        } else {
            final @Nullable Object unwrappedVal = node.raw();
            if (unwrappedVal != null) {
                return (T) ((PCollection<Object>) this.emptySupplier.get()).plus(entrySerial.deserialize(entryType, node));
            }
        }
        return this.emptySupplier.get();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public final void serialize(final Type type, final @Nullable T obj, final ConfigurationNode node) throws SerializationException {
        final Type entryType = elementType(type);
        final @Nullable TypeSerializer entrySerial = node.options().serializers().get(entryType);
        if (entrySerial == null) {
            throw new SerializationException(node, entryType, "No applicable type serializer for type");
        }

        node.raw(Collections.emptyList());
        if (obj != null) {
            for (final Object el : obj) {
                final ConfigurationNode child = node.appendListNode();
                try {
                    entrySerial.serialize(entryType, el, child);
                } catch (final SerializationException ex) {
                    ex.initPath(child::path);
                    throw ex;
                }
            }
        }
    }

    @Override
    public T emptyValue(final Type specificType, final ConfigurationOptions options) {
        return this.emptySupplier.get();
    }

    /**
     * Given the type of container, provide the expected type of an element. If
     * the element type is not available, an exception must be thrown.
     *
     * @param containerType the type of container with type parameters resolved
     *                      to the extent possible.
     * @return the element type
     */
    private Type elementType(Type containerType) {
        Types.requireCompleteParameters(containerType);
        return ((ParameterizedType) containerType).getActualTypeArguments()[0];
    }
}
