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
package ninja.leaping.permissionsex.util.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ninja.leaping.permissionsex.util.command.args.ArgumentParseException;
import ninja.leaping.permissionsex.util.command.args.CommandArgs;
import ninja.leaping.permissionsex.util.command.args.CommandElement;
import ninja.leaping.permissionsex.util.command.args.QuotedStringParser;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static ninja.leaping.permissionsex.util.command.args.GenericArguments.*;
import static ninja.leaping.permissionsex.util.Translations.untr;
import static org.junit.Assert.*;
/**
 * Tests for all argument types contained in GenericArguments
 */
public class GenericArgumentsTest {
    static final CommandExecutor NULL_EXECUTOR = new CommandExecutor() {
        @Override
        public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
        }
    };

    @Rule
    public ExpectedException expected = ExpectedException.none();

    private static CommandContext parseForInput(String input, CommandElement element) throws ArgumentParseException {
        CommandSpec spec = CommandSpec.builder()
                .setAliases("test")
                .setExecutor(NULL_EXECUTOR)
                .build();
        CommandArgs args = QuotedStringParser.parseFrom(input, false);
        CommandContext context = new CommandContext(spec, args.getRaw());
        element.parse(args, context);
        return context;
    }

    @Test
    public void testNone() throws ArgumentParseException {
        CommandArgs args = QuotedStringParser.parseFrom("a", false);
        CommandContext context = new CommandContext(CommandSpec.builder().setAliases("test").setExecutor(NULL_EXECUTOR).build(), args.getRaw());
        none().parse(args, context);
        assertEquals("a", args.next());
    }

    @Test
    @Ignore
    public void testFlags() {
        throw new UnsupportedOperationException();
    }

    @Test
    public void testSequence() throws ArgumentParseException {
        CommandElement el = seq(string(untr("one")), string(untr("two")), string(untr("three")));
        CommandContext context = parseForInput("a b c", el);
        assertEquals("a", context.getOne("one"));
        assertEquals("b", context.getOne("two"));
        assertEquals("c", context.getOne("three"));

        expected.expect(ArgumentParseException.class);
        parseForInput("a b", el);
    }

    @Test
    public void testChoices() throws ArgumentParseException {
        CommandElement el = choices(untr("val"), ImmutableMap.of("a", "one", "b", "two"));
        CommandContext context = parseForInput("a", el);
        assertEquals("one", context.getOne("val"));

        expected.expect(ArgumentParseException.class);
        parseForInput("c", el);
    }

    @Test
    public void testFirstParsing() throws ArgumentParseException {
        CommandElement el = firstParsing(integer(untr("val")), string(untr("val")));
        CommandContext context = parseForInput("word", el);
        assertEquals("word", context.getOne("val"));

        context = parseForInput("42", el);
        assertEquals((Integer) 42, context.getOne("val"));
    }

    @Test
    public void testOptional() throws ArgumentParseException {
        CommandElement el = optional(string(untr("val")));
        CommandContext context = parseForInput("", el);
        assertNull(context.getOne("val"));

        el = optional(string(untr("val")), "def");
        context = parseForInput("", el);
        assertEquals("def", context.getOne("val"));

        el = seq(optionalWeak(integer(untr("val"))), string(untr("str")));
        context = parseForInput("hello", el);
        assertEquals("hello", context.getOne("str"));

        el = seq(optional(integer(untr("val")), string(untr("str"))));
        expected.expect(ArgumentParseException.class);
        parseForInput("hello", el);
    }

    @Test
    public void testRepeated() throws ArgumentParseException {
        CommandContext context = parseForInput("1 1 2 3 5", repeated(integer(untr("key")), 5));
        assertEquals(ImmutableList.<Object>of(1, 1, 2, 3, 5), context.getAll("key"));
    }

    @Test
    public void testAllOf() throws ArgumentParseException {
        CommandContext context = parseForInput("2 4 8 16 32 64 128", allOf(integer(untr("key"))));
        assertEquals(ImmutableList.<Object>of(2, 4, 8, 16, 32, 64, 128), context.getAll("key"));
    }

    @Test
    public void testString() throws ArgumentParseException {
        CommandContext context = parseForInput("\"here it is\"", string(untr("a value")));
        assertEquals("here it is", context.getOne("a value"));
    }

    @Test
    public void testInteger() throws ArgumentParseException {
        CommandContext context = parseForInput("52", integer(untr("a value")));
        assertEquals((Integer) 52, context.getOne("a value"));

        expected.expect(ArgumentParseException.class);
        parseForInput("notanumber", integer(untr("a value")));
    }

    @Test
    public void testBool() throws ArgumentParseException {
        CommandElement boolEl = bool(untr("val"));
        assertEquals(true, parseForInput("true", boolEl).getOne("val"));
        assertEquals(true, parseForInput("t", boolEl).getOne("val"));
        assertEquals(false, parseForInput("f", boolEl).getOne("val"));

        expected.expect(ArgumentParseException.class);
        parseForInput("notabool", boolEl);
    }

    private enum TestEnum {
        ONE, TWO, RED
    }

    @Test
    public void testEnumValue() throws ArgumentParseException {
        CommandElement enumEl = enumValue(untr("val"), TestEnum.class);
        assertEquals(TestEnum.ONE, parseForInput("one", enumEl).getOne("val"));
        assertEquals(TestEnum.TWO, parseForInput("TwO", enumEl).getOne("val"));
        assertEquals(TestEnum.RED, parseForInput("RED", enumEl).getOne("val"));

        expected.expect(ArgumentParseException.class);
        parseForInput("notanel", enumEl);
    }

    @Test
    public void testRemainingJoinedStrings() throws ArgumentParseException {
        CommandElement remainingJoined = remainingJoinedStrings(untr("val"));
        assertEquals("one", parseForInput("one", remainingJoined).getOne("val"));
        assertEquals("one big string", parseForInput("one big string", remainingJoined).getOne("val"));
    }
}
