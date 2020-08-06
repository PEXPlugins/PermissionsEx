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

package ca.stellardrift.permissionsex.commands.parse;

import ca.stellardrift.permissionsex.commands.commander.Commander;

/**
 * Interface containing the method directing how a certain command will be executed
 */
@FunctionalInterface
public interface CommandExecutor {
    /**
     * Callback for the execution of a command
     *
     * @param src The commander who is executing this command
     * @param args The parsed command arguments for this command
     * @throws CommandException If a user-facing error occurs while executing this command
     */
    void execute(Commander src, CommandContext args) throws CommandException;
}
