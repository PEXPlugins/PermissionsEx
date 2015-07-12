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

import ninja.leaping.permissionsex.util.command.args.ArgumentParseException;
import ninja.leaping.permissionsex.util.command.args.CommandElement;
import ninja.leaping.permissionsex.util.command.args.ElementResult;
import ninja.leaping.permissionsex.util.command.args.GenericArguments;
import ninja.leaping.permissionsex.util.command.args.QuotedStringParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static ninja.leaping.permissionsex.util.Translations._;
import static ninja.leaping.permissionsex.util.command.args.GenericArguments.*;

import static org.junit.Assert.*;

/**
 * Test for command flags
 */
public class CommandFlagsTest {
    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void testFlaggedCommand() {
        CommandSpec command = CommandSpec.builder()
                .setAliases("pex")
                .setArguments(flags()
                        .flag("a").valueFlag(integer(_("quot")), "q").buildWith(string(_("key"))))
                .setExecutor(new CommandExecutor() {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        assertEquals(true, args.getOne("a"));
                        assertEquals(42, args.getOne("quot"));
                        assertEquals("something", args.getOne("key"));
                    }
                })
                .build();
        command.process(new TestCommander(), "-a -q 42 something");
        command.process(new TestCommander(), "-aq 42 something");
        command.process(new TestCommander(), "-a something -q 42");
    }

    private CommandContext parseWithInput(CommandElement element, String input) throws ArgumentParseException {
        ElementResult res = element.parse(QuotedStringParser.parseFrom(input, false).openChild(element));
        return new CommandContext(null, res);
    }

    @Test
    public void testUnknownFlagBehaviorError() throws ArgumentParseException {
        CommandElement flags = flags()
                .setUnknownLongFlagBehavior(GenericArguments.UnknownFlagBehavior.ERROR)
                .setUnknownShortFlagBehavior(GenericArguments.UnknownFlagBehavior.ERROR)
                .flag("h", "-help")
                .buildWith(none());
        CommandContext context = parseWithInput(flags, "-h");
        assertTrue(context.hasAny("h"));

        this.expected.expect(ArgumentParseException.class);
        parseWithInput(flags, "--another");
    }

    @Test
    public void testUnknownFlagBehaviorIgnore() throws ArgumentParseException {
        CommandElement flags = flags()
                .setUnknownLongFlagBehavior(GenericArguments.UnknownFlagBehavior.IGNORE)
                .setUnknownShortFlagBehavior(GenericArguments.UnknownFlagBehavior.IGNORE)
                .flag("h", "-help")
                .buildWith(none());

        CommandContext context = parseWithInput(flags, "-h --other -q");
        assertTrue(context.hasAny("h"));
        assertFalse(context.hasAny("other"));
        assertFalse(context.hasAny("q"));
    }

    @Test
    public void testUnknownFlagBehaviorAcceptNonValue() throws ArgumentParseException {
        CommandElement flags = flags()
                .setUnknownLongFlagBehavior(GenericArguments.UnknownFlagBehavior.ACCEPT_NONVALUE)
                .setUnknownShortFlagBehavior(GenericArguments.UnknownFlagBehavior.ACCEPT_NONVALUE)
                .flag("h", "-help")
                .buildWith(none());

        CommandContext context = parseWithInput(flags, "-h --other something -q else --forceargs=always");
        assertTrue(context.hasAny("h"));
        assertEquals(true, context.getOne("other"));
        assertEquals(true, context.getOne("q"));
        assertEquals("always", context.getOne("forceargs"));
    }

    @Test
    public void testUnknownFlagBehaviorAcceptValue() throws ArgumentParseException {
        CommandElement flags = flags()
                .setUnknownLongFlagBehavior(GenericArguments.UnknownFlagBehavior.ACCEPT_VALUE)
                .setUnknownShortFlagBehavior(GenericArguments.UnknownFlagBehavior.ACCEPT_VALUE)
                .flag("h", "-help")
                .buildWith(none());

        CommandContext context = parseWithInput(flags, "-h --other something -q else --forceargs=always");
        assertTrue(context.hasAny("h"));
        assertEquals("something", context.getOne("other"));
        assertEquals("else", context.getOne("q"));
        assertEquals("always", context.getOne("forceargs"));
    }
}
