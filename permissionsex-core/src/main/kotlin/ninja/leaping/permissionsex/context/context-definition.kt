/**
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
package ninja.leaping.permissionsex.context

import ninja.leaping.permissionsex.PermissionsEx
import ninja.leaping.permissionsex.config.PermissionsExConfiguration
import ninja.leaping.permissionsex.subject.CalculatedSubject
import java.lang.IllegalArgumentException

typealias ContextSet = MutableSet<ContextValue<*>>

/**
 * A specific type of context, for example world, server-tag, or until
 */
abstract class ContextDefinition<T>(val name: String) {

    fun createContextValue(value: T): ContextValue<T> = ContextValue(this, value)

    /**
     * Given a parsed value, write data out as a string
     */
    abstract fun serialize(userValue: T): String

    /**
     * Given a string (which may be in user format), return a parsed object
     */
    abstract fun deserialize(canonicalValue: String): T

    /**
     * Given a defined context and the active value (provided by [accumulateCurrentValues]),
     * return whether the active value matches the defined value.
     */
    abstract fun matches(ctx: ContextValue<T>, activeValue: T): Boolean

    /**
     * Given a player, calculate active context types
     *
     * @param subject The subject active contexts are being calculated for
     * @param consumer A function that will take the returned value and add it to the active context set
     */
    abstract fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: T) -> Unit)

    override fun toString(): String {
        return "${this.javaClass.simpleName}(name='$name')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContextDefinition<*>) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return 31 * name.hashCode();
    }
}

abstract class EnumContextDefinition<T: Enum<T>>(name: String, private val enumValueOf: (String) -> T) : ContextDefinition<T>(name) {
    override fun serialize(userValue: T): String {
        return userValue.name
    }

    override fun deserialize(canonicalValue: String): T {
        return enumValueOf(canonicalValue.toUpperCase())
    }

    override fun matches(ctx: ContextValue<T>, activeValue: T): Boolean {
        return ctx.getParsedValue(this) == activeValue
    }
}

open class SimpleContextDefinition(name: String) : ContextDefinition<String>(name) {
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: (value: String) -> Unit) {
    }

    override fun matches(ctx: ContextValue<String>, activeValue: String): Boolean {
        return ctx.getParsedValue(this) == activeValue
    }

    override fun serialize(userValue: String): String {
        return userValue
    }

    override fun deserialize(canonicalValue: String): String {
        return canonicalValue
    }

    override fun equals(other: Any?): Boolean {
        return if (other is ContextDefinition<*>) {
            other.name == this.name
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return 7 + this.name.hashCode()
    }
}

abstract class PEXContextDefinition<T> internal constructor(name: String) : ContextDefinition<T>(name) {
    abstract fun update(config: PermissionsExConfiguration)
}

open class ContextValue<Type>(val key: String, val rawValue: String) {
    var definition: ContextDefinition<Type>? protected set
    var parsedValue: Type? protected set
    init {
        definition = null
        parsedValue = null
    }

    internal constructor(definition: ContextDefinition<Type>, value: Type) : this(definition.name, definition.serialize(value)) {
        this.definition = definition
        this.parsedValue = parsedValue
    }

    fun tryResolve(engine: PermissionsEx): Boolean {
        if (this.definition != null) {
            return true
        }
        val definition = engine.getContextDefinition(this.key) as ContextDefinition<Type>?
        if (definition != null) {
            this.definition = definition
            this.parsedValue = definition.deserialize(this.rawValue)
            return true
        }
        return false
    }

    fun getParsedValue(definition: ContextDefinition<Type>): Type {
        if (this.definition != null  && this.definition != definition) {
            throw IllegalArgumentException("The provided context definition does not match the one this context object currently knows about")
        }
        this.definition = definition
        var parsedValue = this.parsedValue
        if (parsedValue == null) {
            parsedValue = definition.deserialize(this.rawValue)
            this.parsedValue = parsedValue
            return parsedValue
        } else {
            return parsedValue;
        }
    }

    fun getParsedValue(engine: PermissionsEx): Type {
        var tempParsed = parsedValue;
        if (tempParsed != null) {
            return tempParsed;
        }
        tempParsed = engine.getContextDefinition(this.key)?.deserialize(this.rawValue) as Type?
        if (tempParsed == null) {
            throw RuntimeException("No definition for context $key")
        }
        parsedValue = tempParsed
        return tempParsed
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContextValue<*>) return false

        if (key != other.key) return false
        if (rawValue != other.rawValue) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + rawValue.hashCode()
        return result
    }

    override fun toString(): String {
        return "ContextValue(key='$key', rawValue='$rawValue', definition=$definition, parsedValue=$parsedValue)"
    }
}

