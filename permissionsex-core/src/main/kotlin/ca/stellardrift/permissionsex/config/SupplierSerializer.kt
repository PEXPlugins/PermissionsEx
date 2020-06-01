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

import com.google.common.reflect.TypeToken
import java.lang.reflect.ParameterizedType
import java.util.function.Supplier
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer

/**
 * A supplier that makes values lazy-init
 */
object SupplierSerializer : TypeSerializer<Supplier<*>> {
    override fun deserialize(type: TypeToken<*>, value: ConfigurationNode): Supplier<*>? {
        if (type.type !is ParameterizedType) {
            throw ObjectMappingException("Raw types are not supported for a supplier")
        }

        val wrappedType = type.resolveType(Supplier::class.java.typeParameters[0])
        val wrappedSerializer = value.options.serializers[wrappedType]
            ?: throw ObjectMappingException("No type serializer available for type $wrappedType")

        val ret = lazy { wrappedSerializer.deserialize(wrappedType, value) }

        return Supplier { ret.value }
    }

    override fun serialize(type: TypeToken<*>, obj: Supplier<*>?, value: ConfigurationNode) {
        if (type.type !is ParameterizedType) {
            throw ObjectMappingException("Raw types are not supported for a supplier")
        }

        val wrappedType = type.resolveType(Supplier::class.java.typeParameters[0])
        @Suppress("UNCHECKED_CAST")
        val wrappedSerializer = (value.options.serializers[wrappedType]
            ?: throw ObjectMappingException("No type serializer available for type $wrappedType")) as TypeSerializer<Any>

        wrappedSerializer.serialize(wrappedType, obj?.get(), value)
    }
}
