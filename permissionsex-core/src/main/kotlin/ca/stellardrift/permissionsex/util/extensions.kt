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

@file:JvmName("Utilities")
package ca.stellardrift.permissionsex.util
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import com.google.common.reflect.TypeToken
import ninja.leaping.configurate.SimpleConfigurationNode
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers
import java.util.Optional
import java.util.function.Predicate

typealias SubjectIdentifier = Map.Entry<String, String>

inline fun <reified T: Any> Optional<*>.castMap(operation: T.() -> Unit) {
    (this.orElse(null) as? T)?.apply(operation)
}

inline fun <reified T: Any, R> Optional<*>.castMap(operation: T.() -> R): R? {
    return (this.orElse(null) as? T)?.run(operation)
}

inline fun <reified T: Any> Optional<*>.cast(): Optional<T> {
    return this.map {
        if (it is T) {
            it
        } else {
            null
        }
    }
}

inline fun <reified T> CalculatedSubject.option(key: String): T? {
    val ret = getOption(key).orElse(null)
    val type = TypeToken.of(T::class.java)
    return TypeSerializers.getDefaultSerializers().get(TypeToken.of(T::class.java)).deserialize(type, SimpleConfigurationNode.root().setValue(ret))
}

fun <T: CharSequence> caseInsensitiveStartsWith(prefix: T): Predicate<T> {
    return Predicate { it.startsWith(prefix, ignoreCase = true) }
}
