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
import ca.stellardrift.permissionsex.util.component
import ca.stellardrift.permissionsex.util.unaryPlus
import net.kyori.text.Component

/**
 * Represents a command argument element
 */
abstract class CommandElement {

    @Throws(ArgumentParseException::class)
    abstract fun parse(args: CommandArgs, context: CommandContext)

    abstract fun tabComplete(src: Commander, args: CommandArgs, context: CommandContext): List<String>

    abstract fun getUsage(src: Commander): Component
}

class ValueElement<T>(val key: Component, val value: Value<T>) : CommandElement() {
    @Throws(ArgumentParseException::class)
    override fun parse(
        args: CommandArgs,
        context: CommandContext
    ) {
        val value = value.parse(args)
        if (value != null && value is Any) {
            context.putArg(key.toContextKey(), value)
        }
    }

    /**
     * Given
     */
    override fun tabComplete(src: Commander, args: CommandArgs, context: CommandContext): List<String> {
        return value.tabComplete(src, args).toList()
    }

    /**
     * Get a short usage string that can be placed in-line with other arguments
     */
    override fun getUsage(src: Commander): Component = value.usage(key)
}

/**
 * A value extractor, carrying the information necessary to interpret a value from command arguments
 * The [description] should describe the format accepted and/or the value returned, not the purpose of an argument
 */
abstract class Value<T>(val description: Component) {
    @Throws(ArgumentParseException::class)
    abstract fun parse(args: CommandArgs): T

    open fun tabComplete(src: Commander, args: CommandArgs): Sequence<String> = emptySequence()

    /**
     * Get a short usage string that can be placed in-line with other arguments
     */
    open fun usage(key: Component) = key

    /**
     * Associate this value with a key for usage in a command
     */
    infix fun key(key: Component) =
        ValueElement(key, this)

    /**
     * Attempt to parse either this value or the other one. If both fail, the error message from the first format will be shown.
     */
    infix fun or(other: Value<T>): Value<T> =
        OrValue(this, other)

    fun <R> map(mapper: (T) -> R): Value<R> =
        MappedValue(mapper, this)
}

private class MappedValue<T, R>(private val mapper: (T) -> R, private val original: Value<T>) : Value<R>(original.description) {
    override fun parse(args: CommandArgs): R {
        return mapper(original.parse(args))
    }

    override fun tabComplete(src: Commander, args: CommandArgs): Sequence<String> {
        return original.tabComplete(src, args)
    }

    override fun usage(key: Component): Component {
        return original.usage(key)
    }
}

private class OrValue<T>(val a: Value<T>, val b: Value<T>) :
    Value<T>(component { append(a.description).append(" OR ").append(b.description) }) {
    override fun parse(args: CommandArgs): T {
        return try {
            a.parse(args)
        } catch (ex: ArgumentParseException) {
            try {
                b.parse(args)
            } catch (ex2: ArgumentParseException) {
                ex.addSuppressed(ex2)
                throw ex
            }
        }
    }

    override fun tabComplete(src: Commander, args: CommandArgs): Sequence<String> {
        return a.tabComplete(src, args) + b.tabComplete(src, args)
    }

    override fun usage(key: Component): Component {
        val aUsage = a.usage(key)
        val bUsage = b.usage(key)
        if (aUsage == bUsage) {
            return aUsage
        }
        return component { append(aUsage).append(PIPE).append(bUsage) }
    }
}

fun <T> Value<Iterable<T>>.onlyOne(): Value<T> = object : Value<T>(description) {
    override fun parse(args: CommandArgs): T {
        val multiple = this@onlyOne.parse(args).iterator()
        if (!multiple.hasNext()) {
            throw args.createError(+"No arguments were provided, one must be present")
        }
        val first = multiple.next()
        if (multiple.hasNext()) {
            throw args.createError(+"Argument must only return one value")
        }
        return first
    }

    override fun tabComplete(
        src: Commander,
        args: CommandArgs
    ): Sequence<String> =
        this@onlyOne.tabComplete(src, args)
}
