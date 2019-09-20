/**
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
/**
 * This file contains information on how
 */
package ca.stellardrift.permissionsex.util.configurate

import ca.stellardrift.permissionsex.subject.CalculatedSubject
import com.google.common.reflect.TypeToken
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.SimpleConfigurationNode
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers

operator fun ConfigurationNode.get(vararg path: Any): ConfigurationNode {
    return getNode(*path)
}

operator fun ConfigurationNode.set(vararg path: Any, value: Any?) {
    getNode(*path).value = value
}

operator fun ConfigurationNode.contains(path: Array<Any>): Boolean {
    return !getNode(*path).isVirtual
}

/**
 * Contains for a single level
 *
 * @param path a single path element
 */
operator fun ConfigurationNode.contains(path: Any): Boolean {
    return !getNode(path).isVirtual
}

inline fun <reified T> CalculatedSubject.getOption(key: String): T? {
    val ret = getOption(key).orElse(null)
    val type = TypeToken.of(T::class.java)
    return TypeSerializers.getDefaultSerializers().get(TypeToken.of(T::class.java)).deserialize(type, SimpleConfigurationNode.root().setValue(ret))
}
