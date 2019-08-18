package ca.stellardrift.permissionsex.config

import com.google.common.reflect.TypeToken
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMappingException
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer
import java.lang.reflect.ParameterizedType
import java.util.function.Supplier

/**
 * A supplier that makes values lazy-init
 */
object SupplierSerializer: TypeSerializer<Supplier<*>> {
    override fun deserialize(type: TypeToken<*>, value: ConfigurationNode): Supplier<*>? {
        if (!(type.type is ParameterizedType)) {
            throw ObjectMappingException("Raw types are not supported for a supplier")
        }

        val wrappedType = type.resolveType(Supplier::class.java.typeParameters[0])
        val wrappedSerializer = value.options.serializers[wrappedType] ?: throw ObjectMappingException("No type serializer available for type $wrappedType")

        val ret = lazy { wrappedSerializer.deserialize(wrappedType, value) }

        return Supplier { ret.value }
    }

    override fun serialize(type: TypeToken<*>, obj: Supplier<*>?, value: ConfigurationNode) {
        if (!(type.type is ParameterizedType)) {
            throw ObjectMappingException("Raw types are not supported for a supplier")
        }

        val wrappedType = type.resolveType(Supplier::class.java.typeParameters[0])
        @Suppress("UNCHECKED_CAST")
        val wrappedSerializer = (value.options.serializers[wrappedType] ?: throw ObjectMappingException("No type serializer available for type $wrappedType")) as TypeSerializer<Any>

        wrappedSerializer.serialize(wrappedType, obj?.get(), value)
    }
}