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

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

/**
 * Tests for child commands
 */
public class ChildCommandsTest {
    @Test
    public void testSimpleChildCommand() {
        final AtomicBoolean childExecuted = new AtomicBoolean();
        CommandSpec.builder()
                .setAliases("parent")
                .setChildren(CommandSpec.builder()
                    .setAliases("child")
                    .setExecutor(new CommandExecutor() {
                        @Override
                        public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                            childExecuted.set(true);
                        }
                    })
                    .build())
                .build()
                .process(new TestCommander(), "child");

        assertTrue(childExecuted.get());
    }
}
