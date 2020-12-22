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
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.util.CheckedSupplier;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

class SupplierSerializer implements TypeSerializer<CheckedSupplier<?, SerializationException>> {
    static final SupplierSerializer INSTANCE = new SupplierSerializer();

    private SupplierSerializer() {
    }

    @Override
    public CheckedSupplier<?, SerializationException> deserialize(final Type type, final ConfigurationNode value) throws SerializationException {
        if (!(type instanceof ParameterizedType)) {
            throw new SerializationException("Raw types are not supported for a supplier");
        }

        final Type wrappedType = ((ParameterizedType) type).getActualTypeArguments()[0];
        final @Nullable TypeSerializer<?> wrappedSerializer = value.options().serializers().get(wrappedType);
        if (wrappedSerializer == null) {
            throw new SerializationException("No type serializer available for type $wrappedType");
        }

        return () -> wrappedSerializer.deserialize(wrappedType, value);
    }

    @Override
    public void serialize(final Type type, final @Nullable CheckedSupplier<?, SerializationException> obj, final ConfigurationNode value) throws SerializationException {
        if (!(type instanceof ParameterizedType)) {
            throw new SerializationException("Raw types are not supported for a supplier");
        }

        final Type wrappedType = ((ParameterizedType) type).getActualTypeArguments()[0];
        @SuppressWarnings("unchecked")
        final @Nullable TypeSerializer<Object> wrappedSerializer = (TypeSerializer<Object>) value.options().serializers().get(wrappedType);
        if (wrappedSerializer == null) {
                throw new SerializationException("No type serializer available for wrapped type " + wrappedType);
        }

        wrappedSerializer.serialize(wrappedType, obj == null ? null : obj.get(), value);
    }
}
