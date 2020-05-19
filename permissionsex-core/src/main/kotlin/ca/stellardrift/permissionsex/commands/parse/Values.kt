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
import ca.stellardrift.permissionsex.commands.ArgumentKeys.CHOICES_ERROR_INVALID
import ca.stellardrift.permissionsex.commands.ArgumentKeys.ENUM_ERROR_INVALID
import ca.stellardrift.permissionsex.commands.ArgumentKeys.INTEGER_ERROR_FORMAT
import ca.stellardrift.permissionsex.commands.ArgumentKeys.LITERAL_ERROR_INVALID
import ca.stellardrift.permissionsex.commands.ArgumentKeys.UUID_ERROR_FORMAT
import ca.stellardrift.permissionsex.util.join
import ca.stellardrift.permissionsex.util.unaryPlus
import net.kyori.text.Component
import net.kyori.text.TextComponent.empty
import java.util.UUID

internal val PIPE = +"|"
internal val ELIPSES = +"…"

/**
 * A command element that performs no action, for commands that take no arguments
 */
fun none() = object : CommandElement() {
    override fun parse(args: CommandArgs, context: CommandContext) {}
    override fun tabComplete(src: Commander, args: CommandArgs, context: CommandContext): List<String> = emptyList()
    override fun getUsage(src: Commander): Component = empty()
}

fun <T> choices(values: Map<String, T>, description: Component, valuesInUsage: Boolean = values.size < 5) = object : Value<T>(description) {
    override fun parse(args: CommandArgs): T {
        return values[args.next()]
            ?: throw args.createError(CHOICES_ERROR_INVALID(values.keys.toString()))
    }

    override fun tabComplete(
        src: Commander,
        args: CommandArgs
    ): Sequence<String> {
        val prefix = args.nextIfPresent() ?: ""
        return values.keys.asSequence()
            .filter { it.startsWith(prefix, ignoreCase = true) }
    }

    override fun usage(key: Component): Component {
        return if (valuesInUsage) {
            values.keys.join(PIPE)
        } else {
            key
        }
    }

}

// TODO: Use Configurate ScalarSerializers wherever it makes sense (starting with 4.0)

fun string(): Value<String> = StrVal

private object StrVal : Value<String>(+"any value") {
    override fun parse(args: CommandArgs): String = args.next()
}

fun int(): Value<Int> = IntVal

private object IntVal : Value<Int>(+"any integer, in base 10") {
    override fun parse(args: CommandArgs): Int {
        val input = args.next()
        try {
            return input.toInt()
        } catch (ex: NumberFormatException) {
            throw args.createError(INTEGER_ERROR_FORMAT(input))
        }
    }
}

fun uuid(): Value<UUID> = Uuid

private object Uuid : Value<UUID>(+"a UUID in RFC format") {
    override fun parse(args: CommandArgs): UUID {
        val input = args.next()
        try {
            return UUID.fromString(input)
        } catch (ex: IllegalArgumentException) {
            throw args.createError(UUID_ERROR_FORMAT(input))
        }
    }
}

private val BOOLEAN_CHOICES: Map<String, Boolean> = mapOf(
    "true" to true,
    "t" to true,
    "y" to true,
    "yes" to true,
    "verymuchso" to true,
    "false" to false,
    "f" to false,
    "n" to false,
    "no" to false,
    "notatall" to false
)

/**
 * Require an argument to be a boolean.
 * The recognized true values are:
 *
 * - true
 * - t
 * - yes
 * - y
 * - verymuchso
 *
 * The recognized false values are:
 *
 * - false
 * - f
 * - no
 * - n
 * - notatall
 */
private val BOOLEAN: Value<Boolean> = choices(
    BOOLEAN_CHOICES,
    +"a true or false value",
    false
)

fun boolean(): Value<Boolean> = BOOLEAN


inline fun <reified T: Enum<T>> enum() = object : Value<T>(+"values of ${T::class.simpleName}") {
    override fun parse(args: CommandArgs): T {
        val arg = args.next().toUpperCase()
        try {
            return enumValueOf(arg)
        } catch (ex: IllegalArgumentException) {
            throw args.createError(ENUM_ERROR_INVALID(arg))
        }
    }

    override fun tabComplete(
        src: Commander,
        args: CommandArgs
    ): Sequence<String> {
        val options = enumValues<T>().asSequence().map { it.name }
        args.nextIfPresent()?.run {
            return options.filter { it.startsWith(this, true) }
        }
        return options
    }
}

fun remainingJoinedStrings(raw: Boolean = false): Value<String> = if (raw) RawRemainingArguments else RemainingJoinedStrings

private object RemainingJoinedStrings : Value<String>(+"the entire rest of the arguments") {
    override fun parse(args: CommandArgs): String = buildString {
        append(args.next())
        while (args.hasNext()) {
            append(" ").append(args.next())
        }
    }

    override fun usage(key: Component) = key.append(ELIPSES)

}

private object RawRemainingArguments : Value<String>(+"Raw input, including any quotes") {
    override fun parse(args: CommandArgs): String {
        args.next()
        val ret = args.raw.substring(args.position)
        while (args.hasNext()) { // consume remaining args
            args.next()
        }
        return ret
    }

    override fun usage(key: Component) = key.append(ELIPSES)
}

fun literal(vararg elements: String) =
    literal(*elements, putValue = true)

fun <V> literal(vararg elements: String, putValue: V): Value<V> = object : Value<V>(+"The literal text ${elements.joinToString(" ")}") {
    override fun parse(args: CommandArgs): V {
        for (arg in elements) {
            val current = args.next()
            if (!current.equals(arg, ignoreCase = true)) {
                throw args.createError(LITERAL_ERROR_INVALID(current, arg))
            }
        }
        return putValue
    }

    override fun tabComplete(src: Commander, args: CommandArgs): Sequence<String> {
        for (arg in elements) {
            val next = args.nextIfPresent()
            if (next == null) {
                return sequenceOf(arg)
            } else if (args.hasNext()) {
                if (!next.equals(arg, ignoreCase = true)) {
                    break
                }
            } else {
                if (arg.startsWith(next.toLowerCase(), ignoreCase = true)) { // Case-insensitive compare
                    return sequenceOf(arg) // TODO: Maybe complete all remaining args
                }
            }
        }
        return sequenceOf()
    }
}

fun suggestibleString(completions: () -> Sequence<String>, description: Component = +"a value") = object : Value<String>(description) {
    override fun parse(args: CommandArgs): String = args.next()

    override fun tabComplete(
        src: Commander,
        args: CommandArgs
    ): Sequence<String> {
        val next = args.nextIfPresent()?: return emptySequence()
        return completions().filter { it.startsWith(next, true) }
    }

}
