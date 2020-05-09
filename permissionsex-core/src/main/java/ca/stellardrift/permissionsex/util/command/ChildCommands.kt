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
import ca.stellardrift.permissionsex.util.command.args.ArgumentParseException
import ca.stellardrift.permissionsex.util.command.args.CommandArgs
import ca.stellardrift.permissionsex.util.command.args.CommandElement
import ca.stellardrift.permissionsex.util.unaryPlus
import com.google.common.collect.ImmutableMap
import net.kyori.text.Component
import net.kyori.text.TextComponent
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/**
 * Utility methods for handling child commands
 */
object ChildCommands {
    @JvmStatic
    fun args(vararg children: CommandSpec): CommandElement {
        val mapping: MutableMap<String, CommandSpec> =
            HashMap()
        for (child in children) {
            val aliases = child.aliases
            if (aliases.isEmpty()) {
                continue  // Unnamable command -- TODO maybe warn?
            }
            val primaryName = aliases[0]
            if (mapping.containsKey(primaryName)) {
                continue  // oh well, we're presented with an ordered collection so hopefully whoever is calling us knows what they're doing
            }
            mapping[primaryName] = child
        }
        for (child in children) {
            val aliases = child.aliases
            for (i in 1 until aliases.size) {
                if (!mapping.containsKey(aliases[i])) {
                    mapping[aliases[i]] = child
                }
            }
        }
        return ChildCommandElement(mapping)
    }

    @JvmStatic
    fun executor(arg: CommandElement): CommandExecutor {
        return ChildCommandExecutor(arg.key.toContextKey(), null)
    }

    fun optionalExecutor(
        arg: CommandElement,
        fallbackExecutor: CommandExecutor?
    ): CommandExecutor {
        return ChildCommandExecutor(arg.key.toContextKey(), fallbackExecutor)
    }

    private class ChildCommandElement internal constructor(children: Map<String, CommandSpec>) :
        CommandElement(+("child" + COUNTER.getAndIncrement())) {
        private val children: Map<String, CommandSpec>

        @Throws(ArgumentParseException::class)
        override fun parse(
            args: CommandArgs,
            context: CommandContext
        ) {
            super.parse(args, context)
            val spec: CommandSpec = context.getOne(key)!!
            spec.parse(args, context)
        }

        @Throws(ArgumentParseException::class)
        override fun parseValue(args: CommandArgs): Any {
            val key = args.next()
            if (!children.containsKey(key.toLowerCase())) {
                throw args.createError(
                    CommonMessages.ERROR_CHILDREN_UNKNOWN.invoke(
                        key
                    )
                )
            }
            return children[key.toLowerCase()]!!
        }

        override fun tabComplete(
            src: Commander,
            args: CommandArgs,
            context: CommandContext
        ): List<String> {
            val commandComponent = args.nextIfPresent()
            return if (commandComponent != null) {
                if (args.hasNext()) {
                    val child = children[commandComponent]
                    if (child != null) {
                        try {
                            child.checkPermission(src)
                            return child.tabComplete<Any>(src, args, context) // todo make this more correct
                        } catch (e: CommandException) {
                        }
                    }
                    listOf()
                } else {
                    filterCommands(src).filter { it.startsWith(commandComponent, ignoreCase = true) }.toList()
                }
            } else {
                children.keys.toList()
            }
        }

        override fun getUsage(context: Commander): Component {
            val args: MutableList<Any> =
                ArrayList(max(0, children.size * 2 - 1))
            val filteredCommands = filterCommands(context).filter { children[it]!!.aliases[0] == it }
            val it: Iterator<String> = filteredCommands.iterator()
            while (it.hasNext()) {
                args.add(it.next())
                if (it.hasNext()) {
                    args.add("|")
                }
            }
            return TextComponent.make { b: TextComponent.Builder ->
                for (arg in args) {
                    b.append(arg.toString())
                }
            }
        }

        private fun filterCommands(src: Commander): Sequence<String> {
            return children.asSequence()
                .filter { (_, v) -> v.hasPermission(src)}
                .map { (k, _) -> k }
        }

        companion object {
            private val COUNTER = AtomicInteger()
        }

        init {
            this.children =
                ImmutableMap.copyOf(
                    children
                )
        }
    }

    private class ChildCommandExecutor internal constructor(
        private val key: String,
        private val fallbackExecutor: CommandExecutor?
    ) : CommandExecutor {
        @Throws(CommandException::class)
        override fun execute(
            src: Commander,
            args: CommandContext
        ) {
            val spec =
                args.getOne<CommandSpec>(key)
                    ?: if (fallbackExecutor != null) {
                        fallbackExecutor.execute(src, args)
                        return
                    } else {
                        throw CommandException(
                            CommonMessages.ERROR_CHILDREN_STATE.invoke(
                                key
                            )
                        )
                    }
            spec.checkPermission(src)
            spec.executor.execute(src, args)
        }

    }
}
