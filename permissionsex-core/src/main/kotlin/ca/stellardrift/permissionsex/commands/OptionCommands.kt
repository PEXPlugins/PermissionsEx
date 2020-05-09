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
import ca.stellardrift.permissionsex.commands.Messages.OPTION_ARG_KEY
import ca.stellardrift.permissionsex.commands.Messages.OPTION_ARG_VALUE
import ca.stellardrift.permissionsex.commands.Messages.OPTION_SUCCESS_SET
import ca.stellardrift.permissionsex.commands.Messages.OPTION_SUCCESS_UNSET
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.data.ImmutableSubjectData
import ca.stellardrift.permissionsex.util.command.CommandContext
import ca.stellardrift.permissionsex.util.command.CommandException
import ca.stellardrift.permissionsex.util.command.CommandSpec
import ca.stellardrift.permissionsex.util.command.args.GameArguments.option
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.optional
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.seq
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.string
import ca.stellardrift.permissionsex.util.styled
import ca.stellardrift.permissionsex.util.thenMessageSubject

internal fun getOptionCommand(pex: PermissionsEx<*>): CommandSpec {
    return CommandSpec.builder()
        .setAliases("options", "option", "opt", "o")
        .setArguments(seq(option(OPTION_ARG_KEY(), pex), optional(string(OPTION_ARG_VALUE()))))
        .setExecutor(object : PermissionsExExecutor(pex) {
            @Throws(CommandException::class)
            override fun execute(
                src: Commander,
                args: CommandContext
            ) {
                val ref = getDataRef(src, args, "permissionsex.option.set")
                val contexts = args.getAll<ContextValue<*>>(COMMON_ARGS_CONTEXT).toSet()
                val key = args.getOne<String>(OPTION_ARG_KEY)!!
                val value = args.getOne<String>(OPTION_ARG_VALUE)
                if (value == null) {
                    ref.update { old ->
                        old.setOption(contexts, key, null)
                    }.thenMessageSubject(src) { send ->
                            send(OPTION_SUCCESS_UNSET(
                            key,
                            subject(ref).styled { hl() },
                            contexts.toComponent()
                        ))
                    }
                } else {
                    ref.update { old: ImmutableSubjectData ->
                        old.setOption(
                            contexts,
                            key,
                            value
                        )
                    }.thenMessageSubject(src) { send ->
                        send(
                            OPTION_SUCCESS_SET(
                            option(key, value),
                            subject(ref).styled { hl()},
                            contexts.toComponent()
                        ))
                    }
                }
            }
        })
        .build()
}
