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

package ca.stellardrift.permissionsex.commands

import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.util.command.CommandException
import ca.stellardrift.permissionsex.util.command.CommandSpec
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.uuid
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

private class CachedCallback(
    val source: Commander,
    val func: (Commander) -> Unit,
    val oneUse: Boolean = false
) {
    operator fun invoke() {
        func(source)
    }
}

class CallbackController {
    private val knownCallbacks = ConcurrentHashMap<String, ConcurrentMap<UUID, CachedCallback>>()

    /**
     * Register a callback, returning the command string to send to execute the provided function.
     */
    fun registerCallback(source: Commander, func: (Commander) -> Unit): String {
        val id = UUID.randomUUID()
        knownCallbacks.computeIfAbsent(source.mapKey) { ConcurrentHashMap() }[id] =
            CachedCallback(source, func)
        return "/pex cb $id"
    }

    val Commander.mapKey: String get() = (this.subjectIdentifier?.value?: name ).toLowerCase(Locale.ROOT)

    fun clearOwnedBy(name: String) {
        knownCallbacks.remove(name)
    }

    fun clearOwnedBy(name: UUID) {
        knownCallbacks.remove(name.toString().toLowerCase(Locale.ROOT))
    }

    fun createCommand(): CommandSpec {
        return CommandSpec.builder().apply {
            setAliases("callback", "cb")
            setArguments(uuid(Messages.COMMAND_CALLBACK_ARG_CALLBACK_ID()))
            setDescription(Messages.COMMAND_CALLBACK_DESCRIPTION())
            setExecutor { src, args ->
                val callbackId = args.getOne<UUID>(Messages.COMMAND_CALLBACK_ARG_CALLBACK_ID)!!
                val userCallbacks = knownCallbacks[src.mapKey]
                val callback = userCallbacks?.get(callbackId)
                when {
                    callback == null -> throw CommandException(Messages.COMMAND_CALLBACK_ERROR_UNKNOWN_ID(callbackId))
                    callback.source.mapKey != src.mapKey -> throw CommandException(Messages.COMMAND_CALLBACK_ERROR_ONLY_OWN_ALLOWED())
                    else -> try {
                        callback()
                    } finally {
                        if (callback.oneUse) {
                            userCallbacks.remove(callbackId)
                        }
                    }
                }
            }
        }.build()

    }
}
