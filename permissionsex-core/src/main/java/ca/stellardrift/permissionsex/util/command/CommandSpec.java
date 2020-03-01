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

package ca.stellardrift.permissionsex.util.command;

import ca.stellardrift.permissionsex.commands.commander.Commander;
import ca.stellardrift.permissionsex.util.Translatable;
import ca.stellardrift.permissionsex.util.command.args.*;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;


/**
 * Specification for how command arguments should be parsed
 */
public class CommandSpec {
    private final CommandElement args;
    private final CommandExecutor executor;
    private final List<String> aliases;
    private final Translatable description;
    private final Translatable extendedDescription;
    private final String permission;

    private CommandSpec(CommandElement args, CommandExecutor executor, List<String> aliases, Translatable description,
                        Translatable extendedDescription, String permission) {
        this.args = args;
        this.executor = executor;
        this.permission = permission;
        this.aliases = aliases;
        this.description = description;
        this.extendedDescription = extendedDescription;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private CommandElement args = GenericArguments.none();
        private List<String> aliases;
        private Translatable description, extendedDescription;
        private String permission;
        private CommandExecutor executor;

        private Builder() {}

        /**
         * Sets the aliases to use for this command.
         * The first of this list is considered the command's primary name
         *
         * @param aliases The aliases to set
         * @return this
         */
        public Builder setAliases(String... aliases) {
            this.aliases = ImmutableList.copyOf(aliases);
            return this;
        }

        /**
         * Set the permission that will be checked before using this command. May be null.
         *
         * @param permission The permission to check
         * @return this
         */
        public Builder setPermission(String permission) {
            this.permission = permission;
            return this;
        }

        /**
         * Set the callback that will handle this command's execution
         *
         * @param executor The executor that will be called with this command's parsed arguments
         * @return this
         */
        public Builder setExecutor(CommandExecutor executor) {
            this.executor = executor;
            return this;
        }

        public Builder setChildren(CommandSpec... children) {
            final CommandElement usage = ChildCommands.args(children);
            setArguments(usage);
            setExecutor(ChildCommands.executor(usage));
            return this;
        }

        /**
         * A short, one-line description of this command's purpose
         *
         * @param description The description to set
         * @return this
         */
        public Builder setDescription(Translatable description) {
            this.description = description;
            return this;
        }

        /**
         * Sets an extended description to use in longer help listings for this command.
         * Will be appended to the short description and the command's usage.
         * Command flag descriptions are already included in this
         *
         * @param extendedDescription The description to set
         * @return this
         */
        public Builder setExtendedDescription(Translatable extendedDescription) {
            this.extendedDescription = extendedDescription;
            return this;
        }

        /**
         * Set the argument specification for this command
         *
         * @see GenericArguments
         * @see GameArguments
         * @param args The arguments object to use
         * @return this
         */
        public Builder setArguments(CommandElement args) {
            this.args = args;
            return this;
        }

        public CommandSpec build() {
            if (this.executor == null) {
                throw new IllegalArgumentException("An executor is required");
            }
            if (this.aliases == null || this.aliases.isEmpty()) {
                throw new IllegalArgumentException("A command may not have no aliases");
            }
            return new CommandSpec(args, executor, aliases, description, extendedDescription, permission);
        }
    }

    public <TextType> void process(Commander<TextType> commander, String arguments) {
        if (executor == null) {
            return;
        }

        try {
            checkPermission(commander);
            CommandContext args = parse(arguments);
            executor.execute(commander, args);
        } catch (CommandException ex) {
            commander.error(ex.getTranslatableMessage());
            commander.error(CommonMessages.USAGE.get(getUsage(commander)));
        } catch (Throwable t) {
            commander.error(CommonMessages.ERROR_GENERAL.get(String.valueOf(t.getMessage())));
            t.printStackTrace();
        }
    }

    public <TextType> void checkPermission(Commander<TextType> commander) throws CommandException {
        if (this.permission != null && !commander.hasPermission(permission)) {
            throw new CommandException(CommonMessages.ERROR_PERMISSION.get());
        }
    }

    public CommandContext parse(String commandLine) throws ArgumentParseException {
        CommandArgs args = argsFor(commandLine, false);
        CommandContext context = new CommandContext(this, commandLine);
        parse(args, context);
        return context;
    }

    void parse(CommandArgs args, CommandContext context) throws ArgumentParseException {
        this.args.parse(args, context);
        if (args.hasNext()) {
            args.next();
            throw args.createError(CommonMessages.ERROR_ARGUMENTS_TOOMANY.get());
        }
    }

    public <TextType> List<String> tabComplete(Commander<TextType> src, String commandLine) {
        try {
            checkPermission(src);
        } catch (CommandException ex) {
            return Collections.emptyList();
        }
        try {
            CommandArgs args = argsFor(commandLine, true);
            CommandContext context = new CommandContext(this, commandLine);
            return tabComplete(src, args, context);
        } catch (ArgumentParseException e) {
            src.debug(e.getTranslatableMessage());
            return Collections.emptyList();
        }

    }

    <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
        return this.args.tabComplete(src, args, context);
    }

    /**
     * Get the active executor for this command. Generally not a good idea to call this directly,
     * unless you are handling arg parsing specially
     *
     * @return The active executor for this command
     */
    public CommandExecutor getExecutor() {
        return executor;
    }

    private CommandArgs argsFor(String commandline, boolean lenient) throws ArgumentParseException {
        return QuotedStringParser.parseFrom(commandline, lenient);
    }

    public List<String> getAliases() {
        return this.aliases;
    }

    public String getPermission() {
        return this.permission;
    }

    public <TextType> TextType getDescription(Commander<TextType> commander) {
        return this.description == null ? null : commander.getFormatter().tr(this.description);
    }

    public <TextType> TextType getUsage(Commander<TextType> commander) {
        return commander.getFormatter().combined("/", getAliases().get(0), " ", args.getUsage(commander));
    }

    public <TextType> TextType getExtendedDescription(Commander<TextType> src) {
        TextType desc = getDescription(src);
        if (desc == null) {
            if (this.extendedDescription == null) {
                return getUsage(src);
            } else {
                return src.getFormatter().combined(getUsage(src), '\n', src.getFormatter().tr(this.extendedDescription));
            }
        } else if (this.extendedDescription == null) {
            return src.getFormatter().combined(desc, '\n', getUsage(src));
        } else {
            return src.getFormatter().combined(desc, '\n', getUsage(src), '\n', src.getFormatter().tr(this.extendedDescription));
        }
    }
}

