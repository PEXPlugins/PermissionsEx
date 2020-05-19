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

import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.parse.StructuralArguments.optional
import ca.stellardrift.permissionsex.commands.CommonMessages.ERROR_CHILDREN_STATE
import ca.stellardrift.permissionsex.commands.CommonMessages.ERROR_CHILDREN_UNKNOWN
import ca.stellardrift.permissionsex.util.join
import ca.stellardrift.permissionsex.util.unaryPlus
import net.kyori.text.Component
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * Build the children for a parent command
 */
@CommandDsl
class ChildCommandBuilder {
    internal val children: MutableList<CommandSpec> = mutableListOf()
    private var fallback: CommandAction? = null

    /**
     * Add the already existing command as a child of this one
     */
    fun child(spec: CommandSpec) {
        children.add(spec)
    }

    /**
     * Create a new command that will be added as a child
     */
    fun child(vararg aliases: String, init: CommandBuilder.() -> Unit) {
        val builder = CommandBuilder(aliases.toList())
        builder.init()
        this.children.add(builder.build())
    }

    /**
     * Set an action that will be run if no child command is specified
     *
     * Declaring a fallback makes the child command optional.
     */
    fun fallback(action: CommandAction) {
        this.fallback = action
    }

    /**
     * Build a
     */
    internal fun build(): Triple<Component, CommandElement, CommandAction> {
        val element = buildElement()
        val returnedElement = if (fallback != null) {
            optional(element)
        } else {
            element
        }
        return Triple(element.key, returnedElement, buildAction(element))
    }

    private fun buildElement(): ChildCommandElement {
        val mapping: MutableMap<String, CommandSpec> = mutableMapOf()
        for (child in children) {
            val aliases = child.aliases
            if (aliases.isEmpty()) {
                continue  // Unnamable command -- TODO maybe warn?
            }
            mapping.putIfAbsent(aliases[0], child)
        }
        for (child in children) {
            for (alias in child.aliases) {
                mapping.putIfAbsent(alias, child)
            }
        }
        return ChildCommandElement(mapping)
    }

    private fun buildAction(arg: ChildCommandElement): CommandAction {
        val fallback = this.fallback
        val key = arg.key.toContextKey()
        return outer@ { src, args ->
            val spec =
                args.getOne<CommandSpec>(key)
                    ?: if (fallback != null) {
                        fallback(src, args)
                        return@outer
                    } else {
                        throw CommandException(ERROR_CHILDREN_STATE(key))
                    }
            spec.checkPermission(src)
            spec.executor?.invoke(src, args)
        }

    }
}

class ChildCommandElement internal constructor(private val children: Map<String, CommandSpec>) : CommandElement() {
    val key = +"child${COUNTER.getAndIncrement()}"

    @Throws(ArgumentParseException::class)
    override fun parse(
        args: CommandArgs,
        context: CommandContext
    ) {
        val key = args.next().toLowerCase(Locale.ROOT)
        val spec = children[key] ?: throw args.createError(ERROR_CHILDREN_UNKNOWN(key))
        context.putArg(this.key, spec)
        spec.parse(args, context)
    }

    override fun tabComplete(src: Commander, args: CommandArgs, context: CommandContext): List<String> {
        val commandComponent = args.nextIfPresent()
        return if (commandComponent != null) {
            if (args.hasNext()) {
                val child = children[commandComponent]
                if (child != null) {
                    try {
                        child.checkPermission(src)
                        return child.tabComplete(src, args, context) // todo make this more correct
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

    override fun getUsage(src: Commander): Component {
        return filterCommands(src)
            .filter { children[it]?.aliases?.get(0) == it }
            .map { +it}
            .join(PIPE)
    }

    private fun filterCommands(src: Commander): Sequence<String> {
        return children.asSequence()
            .filter { (_, v) -> v.hasPermission(src) }
            .map { (k, _) -> k }
    }

    companion object {
        private val COUNTER = AtomicInteger()
    }
}

