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
package ca.stellardrift.permissionsex.util.command;

import ca.stellardrift.permissionsex.exception.PermissionsException;
import ca.stellardrift.permissionsex.util.Translatable;

/**
 * Exception relating to the execution of a command
 */
public class CommandException extends PermissionsException {
    private static final long serialVersionUID = 242536509476461379L;

    public CommandException(Translatable message) {
        super(message);
    }

    public CommandException(Translatable message, Throwable cause) {
        super(message, cause);
    }
}
