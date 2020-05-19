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
import ca.stellardrift.permissionsex.commands.parse.CommandContext
import ca.stellardrift.permissionsex.commands.parse.CommandException
import ca.stellardrift.permissionsex.commands.parse.command
import ca.stellardrift.permissionsex.commands.parse.permission
import ca.stellardrift.permissionsex.commands.parse.permissionValue
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.util.styled
import ca.stellardrift.permissionsex.util.thenMessageSubject
import ca.stellardrift.permissionsex.util.toComponent

internal fun getPermissionCommand(pex: PermissionsEx<*>) =
    command(
        "permission",
        "permissions",
        "perm",
        "perms",
        "p"
    ) {
        val permission = permission(pex) key PERMISSION_ARG_KEY()
        val permissionValue = permissionValue() key PERMISSION_ARG_PERMISSION_VALUE()
        args(permission, permissionValue)
        executor(object : PermissionsExExecutor(pex) {
            @Throws(CommandException::class)
            override fun execute(src: Commander, args: CommandContext) {
                val ref = getDataRef(src, args, "permissionsex.permission.set")
                val contexts = args.getAll<ContextValue<*>>(COMMON_ARGS_CONTEXT).toSet()
                val key = args[permission]
                val value = args[permissionValue]
                ref.update { old -> old.setPermission(contexts, key, value) }
                    .thenMessageSubject(src) { send ->
                    send(
                        PERMISSION_SUCCESS(
                            permission(key, value),
                            subject(ref).styled { hl() },
                            contexts.toComponent()
                        )
                    )
                }
            }
        })

    }

internal fun getPermissionDefaultCommand(pex: PermissionsEx<*>) =
    command(
        "permission-default",
        "perms-def",
        "permsdef",
        "pdef",
        "pd",
        "default",
        "def"
    ) {
        val permissionVal = permissionValue() key PERMISSION_ARG_PERMISSION_VALUE()
        args = permissionVal
        executor(object : PermissionsExExecutor(pex) {
            @Throws(CommandException::class)
            override fun execute(src: Commander, args: CommandContext) {
                val ref = getDataRef(src, args, "permissionsex.permission.set-default")
                val contexts = args.getAll<ContextValue<*>>(COMMON_ARGS_CONTEXT).toSet()
                val value = args[permissionVal]
                ref.update { old -> old.setDefaultValue(contexts, value) }
                    .thenMessageSubject(src) { send ->
                        send(
                            PERMISSION_DEFAULT_SUCCESS(
                                value.toComponent(),
                                subject(ref).styled { hl() },
                                contexts.toComponent()
                            )
                        )
                    }
            }
        })
    }
