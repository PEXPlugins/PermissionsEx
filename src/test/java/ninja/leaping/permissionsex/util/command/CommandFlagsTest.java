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

import org.junit.Ignore;
import org.junit.Test;

import static ninja.leaping.permissionsex.util.Translations._;
import static ninja.leaping.permissionsex.util.command.args.GenericArguments.*;

/**
 * Test for command flags
 */
public class CommandFlagsTest {

    @Test
    @Ignore("Command flags are not yet implemented")
    public void testFlaggedCommand() {
        CommandSpec command = CommandSpec.builder()
                .setAliases("pex")
                .setArguments(seq(string(_("key"))))
                .setExecutor(new CommandExecutor() {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        System.out.println(args.getAll("key"));
                    }
                })
                .build();
        command.process(new TestCommander(), "-a -q something");
    }
}
