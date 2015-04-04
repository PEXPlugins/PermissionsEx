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

import ninja.leaping.permissionsex.util.command.args.CommandSpec;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Context that a command is executed in
 */
public class CommandContext {
    private final CommandSpec spec;
    private final String rawInput;
    private final Map<String, Object> parsedArgs;

    public CommandContext(CommandSpec spec, String rawInput) {
        this.spec = spec;
        this.rawInput = rawInput;
        this.parsedArgs = new HashMap<>();
    }

    public CommandSpec getSpec() {
        return spec;
    }

    @SuppressWarnings("unchecked")
    public <T> T getArg(String key) {
        return (T) parsedArgs.get(key);
    }

    public void putArg(String key, Object value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
        parsedArgs.put(key, value);
    }
}
