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
package ca.stellardrift.permissionsex.config

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.function.Supplier
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer

/**
 * A supplier that makes values lazy-init
 */
object SupplierSerializer : TypeSerializer<Supplier<*>> {
    override fun deserialize(type: Type, value: ConfigurationNode): Supplier<*>? {
        if (type !is ParameterizedType) {
            throw SerializationException("Raw types are not supported for a supplier")
        }

        val wrappedType = type.actualTypeArguments[0]
        val wrappedSerializer = value.options().serializers()[wrappedType]
            ?: throw SerializationException("No type serializer available for type $wrappedType")

        val ret = lazy { wrappedSerializer.deserialize(wrappedType, value) }

        return Supplier { ret.value }
    }

    override fun serialize(type: Type, obj: Supplier<*>?, value: ConfigurationNode) {
        if (type !is ParameterizedType) {
            throw SerializationException("Raw types are not supported for a supplier")
        }

        val wrappedType = type.actualTypeArguments[0]
        @Suppress("UNCHECKED_CAST")
        val wrappedSerializer = (value.options().serializers()[wrappedType]
            ?: throw SerializationException("No type serializer available for type $wrappedType")) as TypeSerializer<Any>

        wrappedSerializer.serialize(wrappedType, obj?.get(), value)
    }
}
