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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Created by zml on 04.04.15.
 */
public class CommandSpecTest {
    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void testNoArgsFunctional() {
        CommandSpec.builder()
                .setAliases("something")
                .setExecutor(new CommandExecutor() {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        // Run
                    }
                })
                .build()
                .process(new TestCommander(), "");
    }

    @Test
    public void testExecutorRequired() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("An executor is required");
        CommandSpec.builder()
                .setAliases("something")
                .build();

    }

    @Test
    public void testAliasesRequired() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("A command may not have no aliases");
        CommandSpec.builder()
                .setExecutor(new CommandExecutor() {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                    }
                })
                .build();

    }
}
