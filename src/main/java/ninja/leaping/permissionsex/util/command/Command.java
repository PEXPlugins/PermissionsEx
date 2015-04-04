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

import static ninja.leaping.permissionsex.util.Translations.tr;

/**
 * Represents a simple command
 */
public abstract class Command {
    private final CommandSpec spec;

    protected Command(CommandSpec spec) {
        this.spec = spec;
    }

    public <TextType> void process(Commander<TextType> commander, String arguments) {
        try {
            CommandContext args = getSpec().parse(arguments);
            execute(commander, args);
        } catch (CommandException ex) {
            commander.error(ex.getTranslatableMessage());
            commander.error(tr("Usage: %s"), getSpec().getUsage(commander));
        } catch (Throwable t) {
            commander.error(tr("Error occurred while executing command: %s"), String.valueOf(t.getMessage()));
            t.printStackTrace();
        }
    }

    public CommandSpec getSpec() {
        return this.spec;
    }

    protected abstract <TextType> void execute(Commander<TextType> commander, CommandContext args) throws CommandException;
}
