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
package ninja.leaping.permissionsex.util.command.args;

import com.google.common.collect.ImmutableList;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.Commander;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Specification for how command arguments should be parsed
 */
public class CommandSpec {
    private final CommandElement args;
    private final List<String> aliases;
    private final Translatable description;
    private final Translatable extendedDescription;
    private final boolean rawArgs;
    private final boolean parseQuotedArgs;
    private final boolean parseLenient;
    private final OptionParser flags;

    private CommandSpec(CommandElement args, List<String> aliases, Translatable description, Translatable extendedDescription, boolean rawArgs, boolean parseQuotedArgs, boolean parseLenient, OptionParser flags) {
        this.args = args;
        this.aliases = aliases;
        this.description = description;
        this.extendedDescription = extendedDescription;
        this.rawArgs = rawArgs;
        this.parseQuotedArgs = parseQuotedArgs;
        this.parseLenient = parseLenient;
        this.flags = flags;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private CommandElement args;
        private List<String> aliases;
        private Translatable description, extendedDescription;
        private String permission;
        private boolean parseQuotedArgs = true, parseLenient = false, rawArgs = false;
        private OptionParser parser = defaultParser();

        private static OptionParser defaultParser() {
            OptionParser ret = new OptionParser();
            ret.allowsUnrecognizedOptions();
            return ret;
        }

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
         * Set the permission that will be checked before using this command
         * @param permission The permission to check
         * @return this
         */
        public Builder setPermission(String permission) {
            this.permission = permission;
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
         * @see ninja.leaping.permissionsex.util.command.args.GenericArguments
         * @see ninja.leaping.permissionsex.util.command.args.GameArguments
         * @param args The arguments object to use
         * @return this
         */
        public Builder setArguments(CommandElement args) {
            this.args = args;
            return this;
        }


        /**
         * Sets whether raw/unparsed arguments are used. If this is true, the parsed arguments object will contain a
         * single string with the entire command input.
         *
         * @param rawArgs Whether raw args are used
         * @return this
         */
        public Builder setUsesRawArgs(boolean rawArgs) {
            this.rawArgs = false;
            return this;
        }

        /**
         * Sets whether the flags attempts to join quoted strings. If this is false, quotes will not be treated as special characters.
         * The quotation characters are ' and "
         *
         * @param parsesQuotedArgs Whether quoted args are used
         * @return this
         */
        public Builder setParsesQuotedArgs(boolean parsesQuotedArgs) {
            this.parseQuotedArgs = parsesQuotedArgs;
            return this;
        }

        /**
         * Set whether the flags is in lenient mode. If lenient is true, the following apply:
         *
         * <ul>
         *     <li>Unclosed quotations are treated as a single string from the opening quotation to the end of the arguments rather than throwing an debug </li>
         * </ul>
         * @param parsesLeniently Whether the flags is in lenient mode.
         * @return this
         */
        public Builder setParsesLeniently(boolean parsesLeniently) {
           this.parseLenient = parsesLeniently;
            return this;
        }

        /**
         * Set the OptionParser to use for parsing flag options.
         *
         * @see joptsimple.OptionParser for details on how to use the option flags
         * @param parser the option flags to use
         * @return this
         */
        public Builder setParser(OptionParser parser) {
            this.parser = parser;
            return this;
        }

        public CommandSpec build() {
            return new CommandSpec(args, aliases, description, extendedDescription, rawArgs, parseQuotedArgs, parseLenient, parser);
        }
    }

    public boolean isRawArgs() {
        return rawArgs;
    }

    public boolean parsesQuotedArgs() {
        return parseQuotedArgs;
    }

    public boolean parsesLeniently() {
        return parseLenient;
    }

    public CommandContext parse(String commandLine) throws ArgumentParseException {
        CommandArgs args = argsFor(commandLine);
        return parse(args);
    }

    public CommandContext parse(CommandArgs args) throws ArgumentParseException {
        CommandContext context = new CommandContext(this, args.getRaw());
        List<String> strArgs = args.getAll();
        OptionSet options = flags.parse(strArgs.toArray(new String[strArgs.size()]));
        List<?> deFlaggedArgs = options.nonOptionArguments();

        /*for (Map.Entry<OptionSpec<?>, List<?>> ent : options.asMap().entrySet()) {
            final String key = ent.getKey().options().iterator().next();
            context.putArg(key, ent.getValue());
        }*/

        this.args.parse(args, context);
        return context;

    }

    private CommandArgs argsFor(String commandline) throws ArgumentParseException {
        if (isRawArgs()) {
            return new CommandArgs(commandline, Collections.singletonList(new CommandArgs.SingleArg(commandline, 0, commandline.length() - 1)));
            //return new CommandArgs(new OptionSet())
        } else {
            return QuotedStringParser.parseFrom(commandline, this);
        }
    }

    public List<String> getAliases() {
        return this.aliases;
    }

    public <TextType> TextType getDescription(Commander<TextType> commander) {
        return commander.fmt().translated(this.description);
    }

    public <TextType> TextType getUsage(Commander<TextType> commander) {
        return commander.fmt().combined("/", getAliases().get(0), " ", args.getUsage(commander));
    }

    public <TextType> TextType getExtendedDescription(Commander<TextType> commander) {
        return commander.fmt().combined(getDescription(commander), '\n', getUsage(commander), '\n', commander.fmt().translated(this.extendedDescription));
    }
}

