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
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.data.ImmutableSubjectData
import ca.stellardrift.permissionsex.util.Translations.t
import ca.stellardrift.permissionsex.util.command.CommandContext
import ca.stellardrift.permissionsex.util.command.CommandException
import ca.stellardrift.permissionsex.util.command.CommandSpec
import ca.stellardrift.permissionsex.util.command.args.GameArguments.option
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.optional
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.seq
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.string
import com.google.common.collect.ImmutableSet

internal fun getOptionCommand(pex: PermissionsEx<*>): CommandSpec {
    return CommandSpec.builder()
        .setAliases("options", "option", "opt", "o")
        .setArguments(seq(option(t("key"), pex), optional(string(t("value")))))
        .setExecutor(object : PermissionsExExecutor(pex) {
            @Throws(CommandException::class)
            override fun <TextType> execute(
                src: Commander<TextType>,
                args: CommandContext
            ) {
                val ref = getDataRef(src, args, "permissionsex.option.set")
                val contexts: Set<ContextValue<*>> = ImmutableSet.copyOf(args.getAll("context"))
                val key = args.getOne<String>("key")
                val value = args.getOne<String>("value")
                if (value == null) {
                    ref.update { old ->
                        old.setOption(contexts, key, null)
                    }.thenMessageSubject(src) { ->
                        t(
                            "Unset option '%s' for %s in %s context",
                            key,
                            subject(ref).hl(),
                            formatContexts(contexts)
                        )
                    }
                } else {
                    ref.update { old: ImmutableSubjectData ->
                        old.setOption(
                            contexts,
                            key,
                            value
                        )
                    }.thenMessageSubject(src) { ->
                        t(
                            "Set option %s for %s in %s context",
                            option(key, value),
                            subject(ref).hl(),
                            formatContexts(contexts)
                        )
                    }
                }
            }
        })
        .build()
}
