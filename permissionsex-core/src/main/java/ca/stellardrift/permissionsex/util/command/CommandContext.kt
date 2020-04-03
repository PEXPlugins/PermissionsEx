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
package ca.stellardrift.permissionsex.util.command

import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.util.TranslatableProvider
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap

/**
 * Context that a command is executed in
 */
class CommandContext(val spec: CommandSpec, val rawInput: String) {
    private val parsedArgs: Multimap<String, Any> = ArrayListMultimap.create()

    fun <T> getAll(key: String?): Collection<T> {
        return parsedArgs[key] as Collection<T>
    }

    fun <T> getAll(key: TranslatableProvider): Collection<T> {
        return getAll(key.key)
    }

    fun <T> getOne(key: String): T? {
        val values = parsedArgs[key]
        return if (values.size != 1) {
            null
        } else {
            values.iterator().next() as T
        }
    }

    fun <T> getOne(key: TranslatableProvider): T? {
        return getOne(key.key)
    }

    fun putArg(key: String?, value: Any?) {
        if (value == null) {
            throw NullPointerException("value")
        }
        parsedArgs.put(key, value)
    }

    @Throws(CommandException::class)
    fun checkPermission(commander: Commander, permission: String) {
        if (!commander.hasPermission(permission)) {
            throw CommandException(CommonMessages.ERROR_PERMISSION())
        }
    }

    fun hasAny(key: String): Boolean {
        return parsedArgs.containsKey(key)
    }

    fun hasAny(key: TranslatableProvider): Boolean {
        return parsedArgs.containsKey(key.key)
    }
}
