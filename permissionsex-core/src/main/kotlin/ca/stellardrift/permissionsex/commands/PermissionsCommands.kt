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

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.commands.Messages.COMMON_ARGS_CONTEXT
import ca.stellardrift.permissionsex.commands.Messages.PERMISSION_ARG_KEY
import ca.stellardrift.permissionsex.commands.Messages.PERMISSION_ARG_PERMISSION_VALUE
import ca.stellardrift.permissionsex.commands.Messages.PERMISSION_DEFAULT_SUCCESS
import ca.stellardrift.permissionsex.commands.Messages.PERMISSION_SUCCESS
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.util.command.CommandContext
import ca.stellardrift.permissionsex.util.command.CommandException
import ca.stellardrift.permissionsex.util.command.CommandSpec
import ca.stellardrift.permissionsex.util.command.args.CommandElement
import ca.stellardrift.permissionsex.util.command.args.GameArguments.permission
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.bool
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.choices
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.firstParsing
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.integer
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.seq
import ca.stellardrift.permissionsex.util.styled
import ca.stellardrift.permissionsex.util.thenMessageSubject
import ca.stellardrift.permissionsex.util.toComponent
import java.awt.Component

private fun permissionValue(key: Component): CommandElement {
    return firstParsing(
        integer(
            key
        ),
        bool(key),
        choices(
            key,
            mapOf("none" to 0, "null" to 0, "unset" to 0)
        )
    )
}

internal fun getPermissionCommand(pex: PermissionsEx<*>): CommandSpec {
    return CommandSpec.builder()
        .setAliases("permission", "permissions", "perm", "perms", "p")
        .setArguments(
            seq(
                permission(
                    PERMISSION_ARG_KEY(),
                    pex
                ), permissionValue(PERMISSION_ARG_PERMISSION_VALUE())
            )
        )
        .setExecutor(object : PermissionsExExecutor(pex) {
            @Throws(CommandException::class)
            override fun execute(
                src: Commander,
                args: CommandContext
            ) {
                val ref = getDataRef(src, args, "permissionsex.permission.set")
                val contexts = args.getAll<ContextValue<*>>(COMMON_ARGS_CONTEXT).toSet()
                val key = args.getOne<String>(PERMISSION_ARG_KEY)
                var value = args.getOne<Any>(PERMISSION_ARG_PERMISSION_VALUE)
                if (value is Boolean) {
                    value = if (value) 1 else -1
                }
                val intVal = value as Int
                ref.update { old ->
                    old.setPermission(contexts, key, intVal)
                }.thenMessageSubject(src) { send ->
                    send(PERMISSION_SUCCESS(
                        permission(key, intVal),
                        subject(ref).styled { hl() },
                        contexts.toComponent()
                    ))
                }
            }
        })
        .build()
}

internal fun getPermissionDefaultCommand(pex: PermissionsEx<*>): CommandSpec {
    return CommandSpec.builder()
        .setAliases("permission-default", "perms-def", "permsdef", "pdef", "pd", "default", "def")
        .setArguments(permissionValue(PERMISSION_ARG_PERMISSION_VALUE()))
        .setExecutor(object : PermissionsExExecutor(pex) {
            @Throws(CommandException::class)
            override fun execute(
                src: Commander,
                args: CommandContext
            ) {
                val ref = getDataRef(src, args, "permissionsex.permission.set-default")
                val contexts = args.getAll<ContextValue<*>>(COMMON_ARGS_CONTEXT).toSet()
                var value = args.getOne<Any>(PERMISSION_ARG_PERMISSION_VALUE)
                if (value is Boolean) {
                    value = if (value) 1 else -1
                }
                val intVal = value as Int
                ref.update { old ->
                    old.setDefaultValue(
                        contexts,
                        intVal
                    )
                }.thenMessageSubject(src) { send ->
                    send(
                        PERMISSION_DEFAULT_SUCCESS(
                        intVal.toComponent(),
                        subject(ref).styled {hl()},
                        contexts.toComponent()
                    ))
                }
            }
        })
        .build()
}
