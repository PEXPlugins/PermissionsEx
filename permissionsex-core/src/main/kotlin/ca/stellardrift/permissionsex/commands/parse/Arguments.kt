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

import ca.stellardrift.permissionsex.util.command.ArgumentKeys
import ca.stellardrift.permissionsex.util.command.CommandException
import ca.stellardrift.permissionsex.util.unaryPlus
import kotlinx.coroutines.flow.*
import net.kyori.text.Component
import net.kyori.text.TextComponent
import net.kyori.text.TranslatableComponent
import net.kyori.text.serializer.plain.PlainComponentSerializer
import java.util.*


fun <S: Any> string(description: Component) = argument<S, String>(description) {
    parse { s, a -> a.next()}
}

fun <S: Any> uuid(description: Component, mojangCompatibile: Boolean): ArgumentParser<S, UUID> = argument(description) {
    if (mojangCompatibile) {
        // TODO
    } else {
        parse { _, args ->
            val inp = args.next()
            try {
                UUID.fromString(inp)
            } catch (ex: IllegalArgumentException) {
                throw args.createError(ArgumentKeys.UUID_ERROR_FORMAT(inp))
            }
        }
    }
}

inline fun <S: Any, reified E: Enum<E>> enum(description: Component = +"Members of ${E::class.simpleName}") = argument<S, E>(description) {
    parse { _, args ->
        enumValueOf(args.next())
    }

    tabComplete { _, a ->
        val ret = flowOf(*enumValues<E>())
                .map { it.name }

        if (a.hasNext()) {
            val prefix  = a.next()
            ret.filter { it.startsWith(prefix, ignoreCase = true)}
        } else {
            ret
        }
    }
}

fun <S: Any, V: Any> argument(description: Component, action: ArgumentParserBuilder<S, V>.() -> Unit): ArgumentParser<S, V> {
    val builder = ArgumentParserBuilder<S, V>(description)
    builder.action()
    return builder.build()
}

internal typealias ParseFunction<S, V> = suspend (S, CommandArgs) -> V
internal typealias TabCompletionFunction<S> = (S, CommandArgs) -> Flow<String>

class ArgumentParserBuilder<S: Any, V: Any>(val description: Component) {
    private var parseFunction: ParseFunction<S, V>? = null
    private var tabCompletionFunc: TabCompletionFunction<S> = { _, _ -> emptyFlow() }

    fun parse(func: ParseFunction<S, V>) {
        this.parseFunction = func;
    }

    fun tabComplete(func: TabCompletionFunction<S>) {
        this.tabCompletionFunc = func
    }

    fun build(): ArgumentParser<S, V> {
        return ArgumentParser(description, parseFunction ?: throw CommandException(+"A parse function must be registered!"), tabCompletionFunc)
    }
}

class ArgumentParser<S: Any, V: Any>(val description: Component, val parseF: ParseFunction<S, V>, val tabCompleteF: TabCompletionFunction<S>) {
    suspend fun parse(src: S, args: CommandArgs): V {
        return parseF(src, args)
    }
    fun tabComplete(src: S, args: CommandArgs): Flow<String> = tabCompleteF(src, args)

}

@JvmName("argKey")
fun Component.toKey(): String {
    return when (this) {
        is TextComponent -> content()
        is TranslatableComponent -> key()
        else -> {
            PlainComponentSerializer.INSTANCE.serialize(this)
        }
    }
}
