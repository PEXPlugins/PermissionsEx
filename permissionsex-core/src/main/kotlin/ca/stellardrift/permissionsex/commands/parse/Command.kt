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

import ca.stellardrift.permissionsex.util.command.CommandContext
import ca.stellardrift.permissionsex.util.unaryPlus
import net.kyori.text.Component
import net.kyori.text.TextComponent

@DslMarker
annotation class CommandDsl

class CommandContext()

class Command<Src: Any>(
    val aliases: List<String>,
    val description: Component?,
    val extendedDescription: Component?,
    val permission: Permission?,
    next: CommandNode<Src>?,
    executor: (suspend (src: Src, args: CommandContext) -> CommandResult<Src>)?
): CommandNode<Src>(next, executor) {

    suspend fun parse(src: Src, args: String): CommandContext {
        return CommandContext(src, args)
    }

    suspend fun execute(src: Src, args: CommandContext) {

    }

    suspend fun complete(src: Src, args: String, completionConsumer: (String)) {

    }

    fun getHelp(src: Src): Sequence<Component> {
        return sequence {
            yield(+"/${aliases[0]}" )
        }
    }

    fun dumpTree() {

    }

    @CommandDsl
    class Builder<Src>(var aliases: List<String>): CommandNode.Builder<Src>() {
        var description: Component? = null
        var permission: Permission? = null
        var extendedDescription: Component? = null
        private var executor: (suspend (src: Src, args: CommandContext) -> CommandResult<Src>)? = null

        fun build(): Command<Src> {
            return Command(aliases.toList(), description, extendedDescription, permission, next, executor)
        }


    }
}

sealed class CommandNode<Src: Any>(val next: CommandNode<Src>?, val executor: (suspend (src: Src, args: CommandContext) -> CommandResult<Src>)?) {
    @CommandDsl
    open class Builder<Src: Any> {
        var next: CommandNode<Src>? = null
        fun executor(exec: (src: Src, args: CommandContext) -> Unit) {

        }

        infix fun <T: Any> ArgumentParser<Src, T>.key(text: String): BoundArgument<Src, T> {
            return this key TextComponent.of(text)
        }

        infix fun <T: Any> ArgumentParser<Src, T>.key(text: Component): BoundArgument<Src, T> {
            return BoundArgument(this, text) // TODO: Actually add this to parse list
        }

        fun choices(init: OneOf.Builder<Src>.() -> Unit) {
            val build = OneOf.Builder<Src>()
            build.init()
            next = build.build()
        }

    }
}

class OneOf<Src>(val choices: List<CommandNode<Src>>) : CommandNode<Src>(null, null) {
    class Builder<Src> {
        private val choices: MutableList<CommandNode<Src>> = mutableListOf()

        fun choice(init: CommandNode.Builder<Src>.() -> Unit) {
            val build = SingleBuilder<Src>()
            build.init()
            choices += build.build()
        }

        fun build(): OneOf<Src> {
            return OneOf(choices)
        }
    }

    class SingleBuilder<Src>: CommandNode.Builder<Src>() {
        fun build(): CommandNode<Src> {
            return next!!
        }
    }
}
class Literal<Src>() : CommandNode<Src>()

fun <Src> command(vararg aliases: String, init: Command.Builder<Src>.() -> Unit): Command<Src> {
    val build = Command.Builder<Src>(aliases.toList())
    build.init()
    return build.build()
}

/**
 * Represents the result of a command execution
 */
sealed class CommandResult<Src: Any>(val endingNode: CommandNode<Src>)

/**
 * A command that has executed successfully
 */
class Success<Src: Any>(endingNode: CommandNode<Src>, val affectedEntities: Int): CommandResult<Src>(endingNode)
class Failure<Src: Any>(endingNode: CommandNode<Src>, val message: Component?, val exception: Throwable?): CommandResult<Src>(endingNode)

data class BoundArgument<Src: Any, V: Any>(val arg: ArgumentParser<Src, V>, val key: Component)

