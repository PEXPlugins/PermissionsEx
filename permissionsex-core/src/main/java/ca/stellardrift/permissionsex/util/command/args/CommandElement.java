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

package ca.stellardrift.permissionsex.util.command.args;

import ca.stellardrift.permissionsex.commands.commander.Commander;
import ca.stellardrift.permissionsex.util.command.CommandContext;
import ca.stellardrift.permissionsex.util.command.CommandContextKt;
import net.kyori.text.Component;

import java.util.List;

/**
 * Represents a command argument element
 */
public abstract class CommandElement {
    private final Component key;

    protected CommandElement(Component key) {
        this.key = key;
    }

    public Component getKey() {
        return this.key;
    }

    public void parse(CommandArgs args, CommandContext context)  throws ArgumentParseException {
        Object val = parseValue(args);
        if (this.key != null && val != null) {
            context.putArg(CommandContextKt.argKey(this.key), val);
        }
    }

    protected abstract Object parseValue(CommandArgs args) throws ArgumentParseException;

    public abstract List<String> tabComplete(Commander src, CommandArgs args, CommandContext context);

    public Component getUsage(Commander src) {
        return getKey();
    }
}
