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

package ca.stellardrift.permissionsex.commands.parse

import ca.stellardrift.permissionsex.commands.CommonMessages
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.util.TranslatableProvider
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import net.kyori.text.Component
import net.kyori.text.TextComponent
import net.kyori.text.TranslatableComponent
import net.kyori.text.serializer.plain.PlainComponentSerializer

/**
 * Context that a command is executed in
 */
class CommandContext(val spec: CommandSpec, val rawInput: String) {
    private val parsedArgs: Multimap<String, Any> = ArrayListMultimap.create()

    fun <T> getAll(key: Component): Collection<T> {
        return getAll(key.toContextKey())
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getAll(key: String): Collection<T> {
        return parsedArgs[key] as Collection<T>
    }

    fun <T> getAll(key: TranslatableProvider): Collection<T> {
        return getAll(key.key)
    }

    fun <T> getOne(key: Component): T? {
        return getOne(key.toContextKey())
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getOne(key: String): T? {
        val values = parsedArgs[key]
        return if (values.size != 1) {
            null
        } else {
            values.iterator().next() as T
        }
    }

    operator fun <T> get(arg: ValueElement<T>): T {
        return parsedArgs[arg.key.toContextKey()].iterator().next() as T
    }

    fun <T> getOne(key: TranslatableProvider): T? {
        return getOne(key.key)
    }

    fun putArg(key: Component, value: Any) = putArg(key.toContextKey(), value)

    fun putArg(key: String, value: Any) {
        parsedArgs.put(key, value)
    }

    @Throws(CommandException::class)
    fun checkPermission(commander: Commander, permission: String) {
        if (!commander.hasPermission(permission)) {
            throw CommandException(CommonMessages.ERROR_PERMISSION.invoke())
        }
    }

    operator fun contains(key: Component): Boolean = parsedArgs.containsKey(key.toContextKey())
    operator fun contains(key: String): Boolean = parsedArgs.containsKey(key)
    operator fun contains(key: TranslatableProvider): Boolean = parsedArgs.containsKey(key.key)

    fun hasAny(key: String): Boolean {
        return parsedArgs.containsKey(key)
    }

    fun hasAny(key: TranslatableProvider): Boolean {
        return parsedArgs.containsKey(key.key)
    }
}

@JvmName("argKey")
fun Component.toContextKey(): String {
    return when (this) {
        is TextComponent -> content()
        is TranslatableComponent -> key()
        else -> PlainComponentSerializer.INSTANCE.serialize(this)
    }
}
