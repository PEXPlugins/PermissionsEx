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

import org.junit.Test;

import static ninja.leaping.permissionsex.util.Translations.t;
import static ninja.leaping.permissionsex.util.command.args.GenericArguments.*;

import static org.junit.Assert.*;

/**
 * Test for command flags
 */
public class CommandFlagsTest {

    @Test
    public void testFlaggedCommand() {
        CommandSpec command = CommandSpec.builder()
                .setAliases("pex")
                .setArguments(flags()
                        .flag("a").valueFlag(integer(t("quot")), "q").buildWith(string(t("key"))))
                .setExecutor(new CommandExecutor() {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        assertEquals(true, args.getOne("a"));
                        assertEquals((Integer) 42, args.getOne("quot"));
                        assertEquals("something", args.getOne("key"));
                    }
                })
                .build();
        command.process(new TestCommander(), "-a -q 42 something");
        command.process(new TestCommander(), "-aq 42 something");
        command.process(new TestCommander(), "-a something -q 42");
    }
}
