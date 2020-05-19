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
import ca.stellardrift.permissionsex.commands.commander.Permission
import ca.stellardrift.permissionsex.commands.CommonMessages
import ca.stellardrift.permissionsex.util.unaryMinus
import net.kyori.text.Component
import net.kyori.text.TextComponent

typealias CommandAction = (Commander, CommandContext) -> Unit

/**
 * Specification for how command arguments should be parsed
 */
data class CommandSpec internal constructor(
    private val args: CommandElement,
    val executor: CommandAction?,
    val aliases: List<String>,
    val description: Component?,
    private val extendedDescription: Component?,
    val permission: Permission?
) {

    fun process(
        commander: Commander,
        arguments: String
    ) {
        val executor = this.executor ?: return
        try {
            checkPermission(commander)
            val args = parse(arguments)
            executor(commander, args)
        } catch (ex: CommandException) {
            commander.error(ex.component, null)
            commander.error(
                CommonMessages.USAGE(
                    getUsage(
                        commander
                    )
                ), null)
        } catch (t: Throwable) {
            commander.error(CommonMessages.ERROR_GENERAL(t.message!!), t)
            t.printStackTrace()
        }
    }

    @Throws(CommandException::class)
    fun checkPermission(commander: Commander) {
        if (!hasPermission(commander)) {
            throw CommandException(CommonMessages.ERROR_PERMISSION.invoke())
        }
    }

    fun hasPermission(src: Commander): Boolean {
        return permission == null || src.hasPermission(permission)
    }

    @Throws(ArgumentParseException::class)
    fun parse(commandLine: String): CommandContext {
        val args = commandLine.parseToCommand()
        val context = CommandContext(this, commandLine)
        parse(args, context)
        return context
    }

    @Throws(ArgumentParseException::class)
    fun parse(
        args: CommandArgs,
        context: CommandContext
    ) {
        this.args.parse(args, context)
        if (args.hasNext()) {
            args.next()
            throw args.createError(CommonMessages.ERROR_ARGUMENTS_TOOMANY.invoke())
        }
    }

    fun tabComplete(src: Commander, commandLine: String): List<String> {
        try {
            checkPermission(src)
        } catch (ex: CommandException) {
            return emptyList()
        }
        return try {
            val args = commandLine.parseToCommand(true)
            val context =
                CommandContext(this, commandLine)
            tabComplete(src, args, context)
        } catch (e: ArgumentParseException) {
            src.debug(e.component)
            emptyList()
        }
    }

    fun tabComplete(
        src: Commander,
        args: CommandArgs,
        context: CommandContext
    ): List<String> {
        return this.args.tabComplete(src, args, context)
    }

    @Throws(ArgumentParseException::class)
    private fun String.parseToCommand(lenient: Boolean = false) = QuotedStringParser.parseFrom(this, lenient)

    fun getUsage(commander: Commander): Component = (-"/${aliases[0]} ").append(args.getUsage(commander)).build()

    fun getExtendedDescription(src: Commander): Component {
        return if (this.description == null) {
            if (extendedDescription == null) {
                getUsage(src)
            } else {
                TextComponent.join(
                    TextComponent.newline(),
                    getUsage(src),
                    extendedDescription
                )
            }
        } else if (extendedDescription == null) {
            TextComponent.join(TextComponent.newline(), this.description, getUsage(src))
        } else {
            TextComponent.join(
                TextComponent.newline(),
                description,
                getUsage(src),
                extendedDescription
            )
        }
    }

}

/**
 * Mark builder classes that are part of the commands DSL, to prevent scope leakage
 */
@DslMarker
annotation class CommandDsl


@CommandDsl
class CommandBuilder(val aliases: List<String>) {
    init {
        require(!aliases.isEmpty()) { "A command may not have no aliases" }
    }

    var description: Component? = null
    var extendedDescription: Component? = null
    var permission: Permission? = null
    private var executor: CommandAction? = null
    var args: CommandElement = none()

    fun executor(exec: (Commander, CommandContext) -> Unit) {
        this.executor = exec
    }

    fun args(vararg elements: CommandElement) {
        this.args = StructuralArguments.seq(*elements)
    }

    fun children(init: ChildCommandBuilder.() -> Unit) {
        val builder = ChildCommandBuilder()
        builder.init()
        val (_, element, exec) = builder.build()
        this.args = element
        this.executor = exec
    }

    fun executor(exec: CommandExecutor) {
        this.executor = { src, args -> exec.execute(src, args) }
    }

    fun build(): CommandSpec {
        requireNotNull(executor) { "An executor is required" }
        return CommandSpec(
            args,
            this.executor,
            aliases,
            description,
            extendedDescription,
            permission
        )
    }

}


fun command(vararg aliases: String, init: CommandBuilder.() -> Unit): CommandSpec {
    val builder = CommandBuilder(aliases.toList())
    builder.init()
    return builder.build()
}
