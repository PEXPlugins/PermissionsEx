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
import ca.stellardrift.permissionsex.commands.Messages.PARENT_ADD_SUCCESS
import ca.stellardrift.permissionsex.commands.Messages.PARENT_ARGS_PARENT
import ca.stellardrift.permissionsex.commands.Messages.PARENT_REMOVE_SUCCESS
import ca.stellardrift.permissionsex.commands.Messages.PARENT_SET_SUCCESS
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.data.ImmutableSubjectData
import ca.stellardrift.permissionsex.util.command.CommandContext
import ca.stellardrift.permissionsex.util.command.CommandException
import ca.stellardrift.permissionsex.util.command.CommandSpec
import ca.stellardrift.permissionsex.util.command.args.GameArguments.subject
import ca.stellardrift.permissionsex.util.styled
import ca.stellardrift.permissionsex.util.thenMessageSubject

internal fun getParentCommand(pex: PermissionsEx<*>): CommandSpec {
        return CommandSpec.builder()
            .setAliases("parents", "parent", "par", "p")
            .setChildren(
                getAddParentCommand(pex),
                getRemoveParentCommand(pex),
                getSetParentsCommand(pex)
            )
            .build()
    }

    private fun getAddParentCommand(pex: PermissionsEx<*>): CommandSpec {
        return CommandSpec.builder()
            .setAliases("add", "a", "+")
            .setArguments(subject(PARENT_ARGS_PARENT(), pex, PermissionsEx.SUBJECTS_GROUP))
            .setExecutor(object : PermissionsExExecutor(pex) {
                @Throws(CommandException::class)
                override fun execute(
                    src: Commander,
                    args: CommandContext
                ) {
                    val ref = getDataRef(src, args, "permissionsex.parent.add")
                    val contexts = args.getAll<ContextValue<*>>(COMMON_ARGS_CONTEXT).toSet()
                    val parent =
                        args.getOne<Map.Entry<String, String>>(PARENT_ARGS_PARENT)!!
                        ref.update { old: ImmutableSubjectData ->
                            old.addParent(
                                contexts,
                                parent.key,
                                parent.value
                            )
                        }.thenMessageSubject(src) { send ->
                                send(PARENT_ADD_SUCCESS(
                                subject(parent),
                                subject(ref).styled { hl() },
                                contexts.toComponent()
                            ))
                        }
                }
            })
            .build()
    }

    private fun getRemoveParentCommand(pex: PermissionsEx<*>): CommandSpec {
        return CommandSpec.builder()
            .setAliases("remove", "rem", "delete", "del", "-")
            .setArguments(subject(PARENT_ARGS_PARENT(), pex, PermissionsEx.SUBJECTS_GROUP))
            .setExecutor(object : PermissionsExExecutor(pex) {
                @Throws(CommandException::class)
                override fun execute(
                    src: Commander,
                    args: CommandContext
                ) {
                    val ref = getDataRef(src, args, "permissionsex.parent.remove")
                    val contexts = args.getAll<ContextValue<*>>(COMMON_ARGS_CONTEXT).toSet()
                    val parent =
                        args.getOne<Map.Entry<String, String>>(PARENT_ARGS_PARENT)!!
                        ref.update { old: ImmutableSubjectData ->
                            old.removeParent(
                                contexts,
                                parent.key,
                                parent.value
                            )
                        }.thenMessageSubject(src) { send ->
                            send(
                                PARENT_REMOVE_SUCCESS(
                                subject(parent),
                                subject(ref).styled { hl()},
                                contexts.toComponent()
                            ))
                        }
                }
            })
            .build()
    }

    private fun getSetParentsCommand(pex: PermissionsEx<*>): CommandSpec {
        return CommandSpec.builder()
            .setAliases("set", "replace", "=")
            .setArguments(subject(PARENT_ARGS_PARENT(), pex, PermissionsEx.SUBJECTS_GROUP))
            .setExecutor(object : PermissionsExExecutor(pex) {
                @Throws(CommandException::class)
                override fun execute(
                    src: Commander,
                    args: CommandContext
                ) {
                    val ref = getDataRef(src, args, "permissionsex.parent.set")
                    val contexts = args.getAll<ContextValue<*>>(COMMON_ARGS_CONTEXT).toSet()
                    val parent =
                        args.getOne<Map.Entry<String, String>>(PARENT_ARGS_PARENT)!!
                        ref.update { old: ImmutableSubjectData ->
                            old.clearParents(
                                contexts
                            ).addParent(contexts, parent.key, parent.value)
                        }.thenMessageSubject(src) { send ->
                            send(
                                PARENT_SET_SUCCESS(
                                subject(ref).styled { hl() },
                                subject(parent),
                                contexts.toComponent()
                            ))
                        }
                }
            })
            .build()
    }
