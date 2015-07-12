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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import ninja.leaping.permissionsex.util.command.args.ElementResult;

import java.util.Collection;
import java.util.Collections;

import static ninja.leaping.permissionsex.util.Translations._;

/**
 * Context that a command is executed in
 */
public class CommandContext {
    private final CommandSpec spec;
    private final ListMultimap<String, Object> parsedArgs;

    public CommandContext(CommandSpec spec, ElementResult tail) {
        this.spec = spec;
        this.parsedArgs = ArrayListMultimap.create();
        do {
            if (tail.getKey() != null && tail.getKey().getKey() != null) {
                this.parsedArgs.putAll(tail.getKey().getKey().getUntranslated(), tail.getValues());
            }
        } while ((tail = tail.getHolder()) != null);
        for (String key : parsedArgs.keySet()) {
            Collections.reverse(parsedArgs.get(key));
        }
    }

    public CommandSpec getSpec() {
        return spec;
    }

    @SuppressWarnings("unchecked")
    public <T> Collection<T> getAll(String key) {
        return (Collection) parsedArgs.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOne(String key) {
        Collection<Object> values = parsedArgs.get(key);
        if (values.size() != 1) {
            return null;
        } else {
            return (T) values.iterator().next();
        }
    }

    public void checkPermission(Commander<?> commander, String permission) throws CommandException {
        if (!commander.hasPermission(permission)) {
            throw new CommandException(_("You do not have permission to use this command!"));
        }
    }

    public boolean hasAny(String key) {
        return parsedArgs.containsKey(key);
    }
}