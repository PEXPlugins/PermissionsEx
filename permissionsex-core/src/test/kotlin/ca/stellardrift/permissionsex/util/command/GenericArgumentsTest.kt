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

import ca.stellardrift.permissionsex.commands.parse.ArgumentParseException
import ca.stellardrift.permissionsex.commands.parse.CommandContext
import ca.stellardrift.permissionsex.commands.parse.CommandElement
import ca.stellardrift.permissionsex.commands.parse.QuotedStringParser
import ca.stellardrift.permissionsex.commands.parse.StructuralArguments
import ca.stellardrift.permissionsex.commands.parse.StructuralArguments.firstParsing
import ca.stellardrift.permissionsex.commands.parse.boolean
import ca.stellardrift.permissionsex.commands.parse.choices
import ca.stellardrift.permissionsex.commands.parse.command
import ca.stellardrift.permissionsex.commands.parse.enum
import ca.stellardrift.permissionsex.commands.parse.int
import ca.stellardrift.permissionsex.commands.parse.none
import ca.stellardrift.permissionsex.commands.parse.remainingJoinedStrings
import ca.stellardrift.permissionsex.commands.parse.string
import ca.stellardrift.permissionsex.commands.parse.uuid
import ca.stellardrift.permissionsex.util.unaryPlus
import java.util.UUID
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Tests for all argument types contained in GenericArguments
 */
class GenericArgumentsTest {
    @Test
    fun testNone() {
        val args = QuotedStringParser.parseFrom("a", false)
        val context = CommandContext(NULL_COMMAND, args.raw)
        none().parse(args, context)
        assertEquals("a", args.next())
    }

    @Test
    @Disabled
    fun testFlags() {
        throw UnsupportedOperationException()
    }

    @Test
    fun testSequence() {
        val one = string() key +"one"
        val two = string() key +"two"
        val three = string() key +"three"
        val el =
            StructuralArguments.seq(one, two, three)
        val context =
            parseForInput("a b c", el)
        assertEquals("a", context[one])
        assertEquals("b", context[two])
        assertEquals("c", context[three])
        assertThrows(ArgumentParseException::class.java) { parseForInput("a b", el) }
    }

    @Test
    fun testChoices() {
        val el = choices(
            mapOf("a" to "one", "b" to "two"), +"a or b"
        ) key +"val"
        val context = parseForInput("a", el)
        assertEquals("one", context[el])
        assertThrows(ArgumentParseException::class.java) { parseForInput("c", el) }
    }

    @Test
    fun testFirstParsing() {
        val el = firstParsing(int() key +"val", string() key +"val")
        var context = parseForInput("word", el)
        assertEquals("word", context.getOne("val"))
        context = parseForInput("42", el)
        assertEquals(42, context.getOne<Int>("val"))
    }

    @Test
    fun testOptional() {
        var el = StructuralArguments.optional(string() key +"val")
        var context = parseForInput("", el)
        Assertions.assertNull(context.getOne("val"))
        // el = StructuralArguments.optional(string() key +"val", "def") // TODO: add back after re-implementation
        // context = parseForInput("", el)
        // assertEquals("def", context.getOne("val"))
        el = StructuralArguments.seq(
            StructuralArguments.optionalWeak(int() key +"val"), string() key +"str"
        )
        context = parseForInput("hello", el)
        assertEquals("hello", context.getOne("str"))
        el = StructuralArguments.seq(
            StructuralArguments.optional(
                int() key +"val"), string() key +"str"
            )
        assertThrows(ArgumentParseException::class.java) {
            parseForInput("hello", el)
        }
    }

    @Test
    @Throws(ArgumentParseException::class)
    fun testRepeated() {
        val context =
            parseForInput(
                "1 1 2 3 5",
                StructuralArguments.repeated(int() key +"key", 5)
            )
        assertEquals(
            listOf(1, 1, 2, 3, 5),
            context.getAll<Any>("key")
        )
    }

    @Test
    @Throws(ArgumentParseException::class)
    fun testAllOf() {
        val context =
            parseForInput(
                "2 4 8 16 32 64 128",
                StructuralArguments.allOf(int() key +"key")
            )
        assertEquals(
            listOf(2, 4, 8, 16, 32, 64, 128),
            context.getAll<Any>("key")
        )
    }

    @Test
    @Throws(ArgumentParseException::class)
    fun testString() {
        val context =
            parseForInput(
                "\"here it is\"",
                string() key +"a value"
            )
        assertEquals("here it is", context.getOne("a value"))
    }

    @Test
    @Throws(ArgumentParseException::class)
    fun testInteger() {
        val context = parseForInput("52", int() key +"a value")
        assertEquals(52, context.getOne<Int>("a value"))
        assertThrows(
            ArgumentParseException::class.java
        ) {
            parseForInput(
                "notanumber",
                int() key +"a value"
            )
        }
    }

    @Test
    @Throws(ArgumentParseException::class)
    fun testUUID() {
        val subject = UUID.randomUUID()
        val ctx =
            parseForInput(
                subject.toString(),
                uuid() key +"a value"
            )
        assertEquals(subject, ctx.getOne("a value"))
        assertThrows(
            ArgumentParseException::class.java
        ) {
            parseForInput(
                "48459",
                uuid() key +"a value"
            )
        }
        assertThrows(
            ArgumentParseException::class.java
        ) {
            parseForInput(
                "words",
                uuid() key +"a value"
            )
        }
    }

    @Test
    @Throws(ArgumentParseException::class)
    fun testBool() {
        val boolEl = boolean() key +"val"
        assertEquals(true, parseForInput("true", boolEl)[boolEl])
        assertEquals(true, parseForInput("t", boolEl)[boolEl])
        assertEquals(false, parseForInput("f", boolEl)[boolEl])
        assertThrows(ArgumentParseException::class.java) {
            parseForInput("notabool", boolEl)
        }
    }

    private enum class TestEnum {
        ONE, TWO, RED
    }

    @Test
    @Throws(ArgumentParseException::class)
    fun testEnumValue() {
        val enumEl = enum<TestEnum>() key +"val"
        assertEquals(
            TestEnum.ONE,
            parseForInput("one", enumEl)[enumEl]
        )
        assertEquals(
            TestEnum.TWO,
            parseForInput("TwO", enumEl)[enumEl]
        )
        assertEquals(
            TestEnum.RED,
            parseForInput("RED", enumEl)[enumEl]
        )
        assertThrows(ArgumentParseException::class.java) {
            parseForInput("notanel", enumEl)
        }
    }

    @Test
    @Throws(ArgumentParseException::class)
    fun testRemainingJoinedStrings() {
        val remainingJoined = remainingJoinedStrings() key +"val"
        assertEquals("one",
            parseForInput("one", remainingJoined)[remainingJoined]
        )
        assertEquals("one big string",
            parseForInput("one big string", remainingJoined)[remainingJoined]
        )
    }
}

private val NULL_COMMAND =
    command("test") { executor { _, _ -> } }

@Throws(ArgumentParseException::class)
private fun parseForInput(
    input: String,
    element: CommandElement
): CommandContext {
    val args = QuotedStringParser.parseFrom(input, false)
    val context = CommandContext(NULL_COMMAND, args.raw)
    element.parse(args, context)
    return context
}
