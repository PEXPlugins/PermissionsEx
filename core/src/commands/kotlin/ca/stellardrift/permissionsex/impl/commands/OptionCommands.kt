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
package ca.stellardrift.permissionsex.impl.commands

import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.impl.PermissionsEx
import ca.stellardrift.permissionsex.impl.commands.Messages.COMMON_ARGS_CONTEXT
import ca.stellardrift.permissionsex.impl.commands.Messages.OPTION_ARG_KEY
import ca.stellardrift.permissionsex.impl.commands.Messages.OPTION_ARG_VALUE
import ca.stellardrift.permissionsex.impl.commands.parse.CommandContext
import ca.stellardrift.permissionsex.impl.commands.parse.CommandException
import ca.stellardrift.permissionsex.impl.commands.parse.StructuralArguments.optional
import ca.stellardrift.permissionsex.impl.commands.parse.command
import ca.stellardrift.permissionsex.impl.commands.parse.option
import ca.stellardrift.permissionsex.impl.commands.parse.string
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData
import ca.stellardrift.permissionsex.util.thenMessageSubject
import ca.stellardrift.permissionsex.util.toComponent

internal fun getOptionCommand(pex: PermissionsEx<*>) =
    command("option", "options", "opt", "o", "meta") {
        val option = option(pex) key OPTION_ARG_KEY.tr()
        args(option, optional(string() key OPTION_ARG_VALUE.tr()))
        executor(object : PermissionsExExecutor(pex) {
            @Throws(CommandException::class)
            override fun execute(src: Commander, args: CommandContext) {
                val ref = getDataRef(src, args, "permissionsex.option.set")
                val contexts = args.getAll<ContextValue<*>>(COMMON_ARGS_CONTEXT).toSet()
                val key = args[option]
                val value = args.getOne<String>(OPTION_ARG_VALUE)
                if (value == null) {
                    ref.update { old ->
                        old.setOption(contexts, key, null)
                    }.thenMessageSubject(src) { send ->
                        send(
                            OPTION_SUCCESS_UNSET.tr(
                                key,
                                subject(ref).styled { hl() },
                                contexts.toComponent()
                            )
                        )
                    }
                } else {
                    ref.update { old: ImmutableSubjectData ->
                        old.setOption(contexts, key, value)
                    }.thenMessageSubject(src) { send ->
                        send(
                            OPTION_SUCCESS_SET.tr(
                                option(key, value),
                                subject(ref).styled { hl() },
                                contexts.toComponent()
                            )
                        )
                    }
                }
            }
        })
    }
