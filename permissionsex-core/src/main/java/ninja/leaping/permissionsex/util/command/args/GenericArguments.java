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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import ninja.leaping.permissionsex.util.StartsWithPredicate;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.Translations;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.Commander;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ninja.leaping.permissionsex.util.Translations.t;

/**
 * Class containing factory methods to combine single-value command elements
 */
public class GenericArguments {
    private GenericArguments() {}

    /**
     * Expects no arguments
     *
     * @return An expectation of no arguments
     */
    public static CommandElement none() {
        return new SequenceCommandElement(ImmutableList.<CommandElement>of());
    }

    private static CommandElement markTrue(String flag) {
        return new MarkTrueCommandElement(flag);
    }

    private static class MarkTrueCommandElement extends CommandElement {
        public MarkTrueCommandElement(String flag) {
            super(Translations.untr(flag));
        }

        @Override
        protected Object parseValue(CommandArgs args) throws ArgumentParseException {
            return true;
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            return Collections.emptyList();
        }
    }

    public static FlagCommandElementBuilder flags() {
        return new FlagCommandElementBuilder();
    }

    public enum UnknownFlagBehavior {
        /**
         * Throw an {@link ArgumentParseException} when an unknown flag is encountered
         */
        ERROR,
        /**
         * Mark the flag as a non-value flag
         */
        ACCEPT_NONVALUE,
        /**
         * Accept the flag with a string-typed value
         */
        ACCEPT_VALUE,
        /**
         * Act as if the unknown flag is an ordinary argument
         */
        IGNORE

    }

    public static class FlagCommandElementBuilder {
        private final Map<List<String>, CommandElement> usageFlags = new HashMap<>();
        private final Map<String, CommandElement> shortFlags = new HashMap<>();
        private final Map<String, CommandElement> longFlags = new HashMap<>();
        private UnknownFlagBehavior unknownLongFlagBehavior = UnknownFlagBehavior.ERROR;
        private UnknownFlagBehavior unknownShortFlagBehavior = UnknownFlagBehavior.ERROR;
        private boolean anchorFlags = false;

        public FlagCommandElementBuilder flag(String... specs) {
            final List<String> availableFlags = new ArrayList<>(specs.length);
            CommandElement el = null;
            for (String spec : specs) {
                if (spec.startsWith("-")) {
                    final String flagKey = spec.substring(1);
                    if (el == null) {
                        el = markTrue(flagKey);
                    }
                    availableFlags.add(flagKey);
                    longFlags.put(flagKey.toLowerCase(), el);
                } else {
                    for (int i = 0; i < spec.length(); ++i) {
                        final String flagKey = spec.substring(i, i + 1);
                        if (el == null) {
                            el = markTrue(flagKey);
                        }
                        availableFlags.add(flagKey);
                        shortFlags.put(flagKey, el);
                    }
                }
            }
            usageFlags.put(availableFlags, el);
            return this;
        }

        public FlagCommandElementBuilder valueFlag(CommandElement value, String... specs) {
            final List<String> availableFlags = new ArrayList<>(specs.length);
            String valueStore = null;
            for (String spec : specs) {
                if (spec.startsWith("-")) {
                    availableFlags.add(spec);
                    final String flagKey = spec.substring(1);
                    if (valueStore == null) {
                        valueStore = flagKey;
                    }
                    longFlags.put(flagKey.toLowerCase(), value);
                } else {
                    for (int i = 0; i < spec.length(); ++i) {
                        final String flagKey = spec.substring(i, i + 1);
                        if (valueStore == null) {
                            valueStore = flagKey;
                        }
                        availableFlags.add(flagKey);
                        shortFlags.put(flagKey, value);
                    }
                }
            }
            usageFlags.put(availableFlags, markTrue(valueStore));
            return this;
        }

        /**
         * If this is true, any long flag (--) will be accepted and added as a flag
         *
         * @param behavior The behavior upon encountering an unknown long flag
         * @return this
         */
        public FlagCommandElementBuilder setUnknownLongFlagBehavior(UnknownFlagBehavior behavior) {
            this.unknownLongFlagBehavior = behavior;
            return this;
        }

        public FlagCommandElementBuilder setUnknownShortFlagBehavior(UnknownFlagBehavior behavior) {
            this.unknownShortFlagBehavior = behavior;
            return this;
        }

        /**
         * Whether flags should be anchored to the beginning of the text (so flags will
         * only be picked up if they are at the beginning of the input)
         * @param anchorFlags Whether flags are anchored
         * @return this
         */
        public FlagCommandElementBuilder setAnchorFlags(boolean anchorFlags) {
            this.anchorFlags = anchorFlags;
            return this;
        }

        public CommandElement buildWith(CommandElement wrapped) {
            return new FlagCommandElement(wrapped, usageFlags, shortFlags, longFlags, unknownShortFlagBehavior, unknownLongFlagBehavior, anchorFlags);
        }
    }

    private static class FlagCommandElement extends CommandElement {
        private final CommandElement childElement;
        private final Map<List<String>, CommandElement> usageFlags;
        private final Map<String, CommandElement> shortFlags;
        private final Map<String, CommandElement> longFlags;
        private final UnknownFlagBehavior unknownShortFlagBehavior;
        private final UnknownFlagBehavior unknownLongFlagBehavior;
        private final boolean anchorFlags;

        protected FlagCommandElement(CommandElement childElement, Map<List<String>, CommandElement> usageFlags, Map<String, CommandElement> shortFlags, Map<String, CommandElement> longFlags, UnknownFlagBehavior unknownShortFlagBehavior, UnknownFlagBehavior unknownLongFlagBehavior, boolean anchorFlags) {
            super(null);
            this.childElement = childElement;
            this.usageFlags = usageFlags;
            this.shortFlags = shortFlags;
            this.longFlags = longFlags;
            this.unknownShortFlagBehavior = unknownShortFlagBehavior;
            this.unknownLongFlagBehavior = unknownLongFlagBehavior;
            this.anchorFlags = anchorFlags;
        }

        @Override
        public void parse(CommandArgs args, CommandContext context) throws ArgumentParseException {
            int startIdx = args.getPosition();
            String arg;
            while (args.hasNext()) {
                arg = args.next();
                if (arg.startsWith("-")) {
                    int flagStartIdx = args.getPosition();
                    boolean ignored;
                    if (arg.startsWith("--")) { // Long flag
                        String longFlag = arg.substring(2);
                        ignored = !parseLongFlag(longFlag, args, context);
                    } else {
                        arg = arg.substring(1);
                        ignored = !parseShortFlags(arg, args, context);
                    }
                    if (!ignored) {
                        args.removeArgs(flagStartIdx, args.getPosition());
                    }
                } else if (this.anchorFlags) {
                    break;
                }
            }

            args.setPosition(startIdx);
            if (childElement != null) {
                childElement.parse(args, context);
            }

        }

        private boolean parseLongFlag(String longFlag, CommandArgs args, CommandContext context) throws ArgumentParseException {
            if (longFlag.contains("=")) {
                final String[] flagSplit = longFlag.split("=", 2);
                longFlag = flagSplit[0];
                String value = flagSplit[1];
                CommandElement element = longFlags.get(longFlag.toLowerCase());
                if (element == null) {
                    switch (unknownLongFlagBehavior) {
                        case ERROR:
                            throw args.createError(t("Unknown long flag %s specified", args));
                        case ACCEPT_NONVALUE:
                            context.putArg(longFlag, value);
                            break;
                        case IGNORE:
                            return false;
                    }
                } else {
                    args.insertArg(value);
                    element.parse(args, context);
                }
            } else {
                CommandElement element = longFlags.get(longFlag.toLowerCase());
                if (element == null) {
                    switch (unknownLongFlagBehavior) {
                        case ERROR:
                            throw args.createError(t("Unknown long flag %s specified", args));
                        case ACCEPT_NONVALUE:
                            context.putArg(longFlag, true);
                            break;
                        case IGNORE:
                            return false;
                    }
                    context.putArg(longFlag, true);
                } else {
                    element.parse(args, context);
                }
            }
            return true;
        }

        private boolean parseShortFlags(String shortFlags, CommandArgs args, CommandContext context) throws ArgumentParseException {
            for (int i = 0; i < shortFlags.length(); ++i) {
                final String flagChar = shortFlags.substring(i, i + 1);
                CommandElement element = this.shortFlags.get(flagChar);
                if (element == null) {
                    switch (unknownShortFlagBehavior) {
                        case IGNORE:
                            if (i == 0) {
                                return false;
                            } // fall-through
                        case ERROR:
                            throw args.createError(t("Unknown short flag %s specified", flagChar));
                        case ACCEPT_NONVALUE:
                            context.putArg(flagChar, true);
                    }
                } else {
                    element.parse(args, context);
                }
            }
            return true;
        }

        @Override
        public <TextType> TextType getUsage(Commander<TextType> src) {
            final List<Object> builder = new ArrayList<>();
            for (Map.Entry<List<String>, CommandElement> arg : usageFlags.entrySet()) {
                builder.add("[");
                for (Iterator<String> it = arg.getKey().iterator(); it.hasNext();) {
                    builder.add("-");
                    builder.add(it.next());
                    if (it.hasNext()) {
                        builder.add("|");
                    }
                }
                if (!(arg.getValue() instanceof MarkTrueCommandElement)) { // true flag
                    builder.add(" ");
                    builder.add(arg.getValue().getUsage(src));
                }
                builder.add("]");
                builder.add(" ");
            }

            if (childElement != null) {
                builder.add(childElement.getUsage(src));
            }
            return src.fmt().combined(builder.toArray());
        }

        @Override
        protected Object parseValue(CommandArgs args) throws ArgumentParseException {
            return null;
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            int startIdx = args.getPosition();
            Optional<String> arg;
            while (args.hasNext()) {
                arg = args.nextIfPresent();
                if (arg.get().startsWith("-")) {
                    int flagStartIdx = args.getPosition();
                    if (arg.get().startsWith("--")) { // Long flag
                        String longFlag = arg.get().substring(2);
                        List<String> ret = tabCompleteLongFlag(longFlag, src, args, context);
                        if (ret != null) {
                            return ret;
                        }
                    } else {
                        final String argStr = arg.get().substring(1);
                        List<String> ret = tabCompleteShortFlags(argStr, src, args, context);
                        if (ret != null) {
                            return ret;
                        }
                    }
                    args.removeArgs(flagStartIdx, args.getPosition());
                } else if (this.anchorFlags) {
                    break;
                }
            }

            args.setPosition(startIdx);
            if (childElement != null) {
                return childElement.tabComplete(src, args, context);
            } else {
                return Collections.emptyList();
            }
        }

        private <TextType> List<String> tabCompleteLongFlag(String longFlag, Commander<TextType> src, CommandArgs args, CommandContext context) {
            if (longFlag.contains("=")) {
                final String[] flagSplit = longFlag.split("=", 2);
                longFlag = flagSplit[0];
                String value = flagSplit[1];
                CommandElement element = longFlags.get(longFlag.toLowerCase());
                if (element == null) { // Whole flag is specified, we'll go to value (even though flag is unknown
                    if (unknownLongFlagBehavior == UnknownFlagBehavior.IGNORE) {

                    } else {
                        context.putArg(longFlag, value);
                    }
                } else {
                    args.insertArg(value);
                    final String finalLongFlag = longFlag;
                    int position = args.getPosition();
                    try {
                        element.parse(args, context);
                    } catch (ArgumentParseException ex) {
                        args.setPosition(position);
                        return ImmutableList.copyOf(Iterables.transform(element.tabComplete(src, args, context), input -> "--" + finalLongFlag + "=" + input));
                    }
                }
            } else {
                CommandElement element = longFlags.get(longFlag.toLowerCase());
                if (element == null) {
                    return ImmutableList.copyOf(Iterables.transform(Iterables.filter(longFlags.keySet(), new StartsWithPredicate(longFlag.toLowerCase())), input -> "--" + input));
                } else {
                    boolean complete = false;
                    int position = args.getPosition();
                    try {
                        element.parse(args, context);
                    } catch (ArgumentParseException ex) {
                        complete = true;
                    }
                    if (!args.hasNext()) {
                        complete = true;
                    }
                    if (complete) {
                        args.setPosition(position);
                        return element.tabComplete(src, args, context);
                    }
                }
            }
            return null;
        }

        private <TextType> List<String> tabCompleteShortFlags(String shortFlags, Commander<TextType> src, CommandArgs args, CommandContext context) {
            for (int i = 0; i < shortFlags.length(); ++i) {
                final String flagChar = shortFlags.substring(i, i + 1);
                CommandElement element = this.shortFlags.get(flagChar);
                if (element == null) {
                    continue;
                }
                int start = args.getPosition();
                try {
                    element.parse(args, context);
                } catch (ArgumentParseException ex) {
                    args.setPosition(start);
                    return element.tabComplete(src, args, context);
                }
            }
            return null;
        }
    }

    /**
     * Consumes a series of arguments. Usage is the elements concated
     *
     * @param elements The series of arguments to expect
     * @return the element to match the input
     */
    public static CommandElement seq(CommandElement... elements) {
        return new SequenceCommandElement(ImmutableList.copyOf(elements));
    }

    private static class SequenceCommandElement extends CommandElement {
        private final List<CommandElement> elements;

        private SequenceCommandElement(List<CommandElement> elements) {
            super(null);
            this.elements = elements;
        }

        @Override
        public void parse(CommandArgs args, CommandContext context) throws ArgumentParseException {
            for (CommandElement element : elements) {
                element.parse(args, context);
            }
        }

        @Override
        protected Object parseValue(CommandArgs args) throws ArgumentParseException {
            return null;
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            for (Iterator<CommandElement> it = elements.iterator(); it.hasNext(); ) {
                CommandElement element = it.next();
                int startPos = args.getPosition();
                try {
                    element.parse(args, context);
                    int endPos = args.getPosition();
                    if (!args.hasNext()) {
                        args.setPosition(startPos);
                        List<String> inputs = element.tabComplete(src, args, context);
                        args.setPosition(args.getPosition() - 1);
                        if (!inputs.contains(args.next())) { // Tabcomplete returns results to complete the last word in an argument.
                            // If the last word is one of the completions, the command is most likely complete
                            return inputs;
                        }

                        args.setPosition(endPos);
                    }
                } catch (ArgumentParseException e) {
                    args.setPosition(startPos);
                    return element.tabComplete(src, args, context);
                }

                if (!it.hasNext()) {
                    args.setPosition(startPos);
                }
            }
            return Collections.emptyList();
        }

        @Override
        public <TextType> TextType getUsage(Commander<TextType> commander) {
            final List<Object> ret = new ArrayList<>(Math.max(0, elements.size() * 2 - 1));
            for (Iterator<CommandElement> it = elements.iterator(); it.hasNext();) {
                ret.add(it.next().getUsage(commander));
                if (it.hasNext()) {
                    ret.add(' ');
                }
            }
            return commander.fmt().combined(ret.toArray());
        }
    }

    /**
     * Return an argument that allows selecting from a limited set of values.
     * If there are 5 or fewer choices available, the choices will be shown in the command usage. Otherwise, the usage
     * will only display only the key. To override this behavior, see {@link #choices(Translatable, Map, boolean)}.
     *
     * @param key The key to store the resulting value under
     * @param choices The choices users can choose from
     * @return the element to match the input
     */
    public static CommandElement choices(Translatable key, Map<String, ?> choices) {
        return choices(key, choices, choices.size() <= 5);
    }

    /**
     * Return an argument that allows selecting from a limited set of values.
     * Unless {@code choicesInUsage} is true, general command usage will only display the provided key
     *
     * @param key The key to store the resulting value under
     * @param choices The choices users can choose from
     * @return the element to match the input
     */
    public static CommandElement choices(Translatable key, Map<String, ?> choices, boolean choicesInUsage) {
        return new ChoicesCommandElement(key, ImmutableMap.copyOf(choices), choicesInUsage);
    }

    private static class ChoicesCommandElement extends CommandElement {
        private final Map<String, Object> choices;
        private final boolean choicesInUsage;

        private ChoicesCommandElement(Translatable key, Map<String, Object> choices, boolean choicesInUsage) {
            super(key);
            this.choices = choices;
            this.choicesInUsage = choicesInUsage;
        }

        @Override
        public Object parseValue(CommandArgs args) throws ArgumentParseException {
            Object value = choices.get(args.next());
            if (value == null) {
                throw args.createError(t("Argument was not a valid choice. Valid choices: %s", choices.keySet().toString()));
            }
            return value;
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            final String prefix = args.nextIfPresent().orElse("");
            return ImmutableList.copyOf(Iterables.filter(choices.keySet(), new StartsWithPredicate(prefix)));
        }

        @Override
        public <TextType> TextType getUsage(Commander<TextType> commander) {
            if (choicesInUsage) {
                final List<Object> args = new ArrayList<>(Math.max(0, choices.size() * 2 - 1));
                for (Iterator<String> it = choices.keySet().iterator(); it.hasNext();) {
                    args.add(it.next());
                    if (it.hasNext()) {
                        args.add("|");
                    }
                }
                return commander.fmt().combined(args.toArray());
            } else {
                return commander.fmt().tr(getKey());
            }
        }
    }


    /**
     * Returns a command element that matches the first of the provided elements that parses
     * Tab completion matches from all options
     *
     * @param elements The elements to check against
     * @return The command element matching the first passing of the elements provided
     */
    public static CommandElement firstParsing(CommandElement... elements) {
        return new FirstParsingCommandElement(ImmutableList.copyOf(elements));
    }

    private static class FirstParsingCommandElement extends CommandElement {
        private final List<CommandElement> elements;

        private FirstParsingCommandElement(List<CommandElement> elements) {
            super(null);
            this.elements = elements;
        }

        @Override
        public void parse(CommandArgs args, CommandContext context) throws ArgumentParseException {
            ArgumentParseException firstException = null;
            for (CommandElement element : elements) {
                int startIndex = args.getPosition();
                try {
                    element.parse(args, context);
                    return;
                } catch (ArgumentParseException ex) {
                    if (firstException == null) {
                        firstException = ex;
                    }
                    args.setPosition(startIndex); // TODO: roll back commandcontext too when parsing fails
                }
            }
            if (firstException != null) {
                throw firstException;
            }
        }

        @Override
        protected Object parseValue(CommandArgs args) throws ArgumentParseException {
            return null;
        }

        @Override
        public <TextType> List<String> tabComplete(final Commander<TextType> src, final CommandArgs args, final CommandContext context) {
            return ImmutableList.copyOf(Iterables.concat(Iterables.transform(elements, input -> {
                    int startIndex = args.getPosition();
                    List<String> ret = input.tabComplete(src, args, context);
                    args.setPosition(startIndex);
                    return ret;
            })));
        }

        @Override
        public <TextType> TextType getUsage(Commander<TextType> commander) {
            final List<Object> ret = new ArrayList<>(Math.max(0, elements.size() * 2 - 1));
            for (Iterator<CommandElement> it = elements.iterator(); it.hasNext();) {
                ret.add(it.next().getUsage(commander));
                if (it.hasNext()) {
                    ret.add('|');
                }
            }
            return commander.fmt().combined(ret.toArray());
        }
    }

    /**
     * Make the provided command element optional
     * This means the command element is not required. However, if the element is provided with invalid format and there
     * are no more args specified, any errors will still be passed on.
     *
     * @param element The element to optionally require
     * @return the element to match the input
     */
    public static CommandElement optional(CommandElement element) {
        return new OptionalCommandElement(element, null, false);
    }

    /**
     * Make the provided command element optional
     * This means the command element is not required.
     * If the argument is provided but of invalid format, it will be skipped.
     *
     * @param element The element to optionally require
     * @return the element to match the input
     */
    public static CommandElement optionalWeak(CommandElement element) {
        return new OptionalCommandElement(element, null, true);
    }

    /**
     * Make the provided command element optional
     * This means the command element is not required. However, if the element is provided with invalid format and there
     * are no more args specified, any errors will still be passed on. If the given element's key and {@code value} are not
     * null and this element is not provided the element's key will be set to the given value.
     *
     * @param element The element to optionally require
     * @param value The default value to set
     * @return the element to match the input
     */
    public static CommandElement optional(CommandElement element, Object value) {
        return new OptionalCommandElement(element, value, false);
    }

    /**
     * Make the provided command element optional
     * This means the command element is not required.
     * If the argument is provided but of invalid format, it will be skipped.
     * If the given element's key and {@code value} are not null and this element is not provided the element's key will
     * be set to the given value.
     *
     * @param element The element to optionally require
     * @param value The default value to set
     * @return the element to match the input
     */
    public static CommandElement optionalWeak(CommandElement element, Object value) {
        return new OptionalCommandElement(element, value, true);
    }

    private static class OptionalCommandElement extends CommandElement {
        private final CommandElement element;
        private final Object value;
        private final boolean considerInvalidFormatEmpty;

        private OptionalCommandElement(CommandElement element, Object value, boolean considerInvalidFormatEmpty) {
            super(null);
            this.element = element;
            this.value = value;
            this.considerInvalidFormatEmpty = considerInvalidFormatEmpty;
        }

        @Override
        public void parse(CommandArgs args, CommandContext context) throws ArgumentParseException {
            if (!args.hasNext()) {
                if (this.element.getKey() != null && this.value != null) {
                    context.putArg(this.element.getKey().getUntranslated(), value);
                }
                return;
            }
            int startPos = args.getPosition();
            try {
                element.parse(args, context);
            } catch (ArgumentParseException ex) {
                if (considerInvalidFormatEmpty || args.hasNext()) { // If there are more args, suppress. Otherwise, throw the error
                    args.setPosition(startPos);
                    if (this.element.getKey() != null && this.value != null) {
                        context.putArg(this.element.getKey().getUntranslated(), value);
                    }
                } else {
                    throw ex;
                }
            }
        }

        @Override
        protected Object parseValue(CommandArgs args) throws ArgumentParseException {
            return args.hasNext() ? null : element.parseValue(args);
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            return element.tabComplete(src, args, context);
        }

        @Override
        public <TextType> TextType getUsage(Commander<TextType> src) {
            return src.fmt().combined("[", this.element.getUsage(src), "]");
        }
    }

    /**
     * Require a given command element to be provided a certain number of times
     * Command values will be stored under their provided keys in the CommandContext
     *
     * @param element The element to repeat
     * @param times The number of times to repeat the element.
     * @return the element to match the input
     */
    public static CommandElement repeated(CommandElement element, int times) {
        return new RepeatedCommandElement(element, times);
    }

    private static class RepeatedCommandElement extends CommandElement {
        private final CommandElement element;
        private final int times;


        protected RepeatedCommandElement(CommandElement element, int times) {
            super(null);
            this.element = element;
            this.times = times;
        }

        @Override
        public void parse(CommandArgs args, CommandContext context) throws ArgumentParseException {
            for (int i = 0; i < times; ++i) {
                element.parse(args, context);
            }
        }

        @Override
        protected Object parseValue(CommandArgs args) throws ArgumentParseException {
            return null;
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            for (int i = 0; i < times; ++i) {
                int startPos = args.getPosition();
                try {
                    element.parse(args, context);
                } catch (ArgumentParseException e) {
                    args.setPosition(startPos);
                    return element.tabComplete(src, args, context);
                }
            }
            return Collections.emptyList();
        }

        @Override
        public <TextType> TextType getUsage(Commander<TextType> src) {
            return src.fmt().combined(times, '*', element.getUsage(src));
        }
    }

    /**
     * Require all remaining args to match as many instances of CommandElement as will fit
     * Command element values will be stored under their provided keys in the CommandContext.
     *
     * @param element The element to repeat
     * @return the element to match the input
     */
    public static CommandElement allOf(CommandElement element) {
        return new AllOfCommandElement(element);
    }

    private static class AllOfCommandElement extends CommandElement {
        private final CommandElement element;


        protected AllOfCommandElement(CommandElement element) {
            super(null);
            this.element = element;
        }

        @Override
        public void parse(CommandArgs args, CommandContext context) throws ArgumentParseException {
            while (args.hasNext()) {
                element.parse(args, context);
            }
        }

        @Override
        protected Object parseValue(CommandArgs args) throws ArgumentParseException {
            return null;
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            while (args.hasNext()) {
                int startPos = args.getPosition();
                try {
                    element.parse(args, context);
                } catch (ArgumentParseException e) {
                    args.setPosition(startPos);
                    return element.tabComplete(src, args, context);
                }
            }
            return Collections.emptyList();
        }

        @Override
        public <TextType> TextType getUsage(Commander<TextType> context) {
            return context.fmt().combined(element.getUsage(context), '+');
        }
    }

    // -- Argument types for basic java types

    /**
     * Parent class that specifies elemenents as having no tab completions. Useful for inputs with a very large domain, like strings and integers
     */
    private static abstract class KeyElement extends CommandElement {
        private KeyElement(Translatable key) {
            super(key);
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            return Collections.emptyList();
        }
    }

    /**
     * Require an argument to be a string. Any provided argument will fit in under this argument
     *
     * @param key The key to store the parsed argument under
     * @return the element to match the input
     */
    public static CommandElement string(Translatable key) {
        return new StringElement(key);
    }

    private static class StringElement extends KeyElement {

        private StringElement(Translatable key) {
            super(key);
        }

        @Override
        public Object parseValue(CommandArgs args) throws ArgumentParseException {
            return args.next();
        }
    }


    /**
     * Require an argument to be an integer (base 10).
     *
     * @param key The key to store the parsed argument under
     * @return the element to match the input
     */
    public static CommandElement integer(Translatable key) {
        return new IntegerElement(key);
    }

    private static class IntegerElement extends KeyElement {

        private IntegerElement(Translatable key) {
            super(key);
        }

        @Override
        public Object parseValue(CommandArgs args) throws ArgumentParseException {
            final String input = args.next();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                throw args.createError(t("Expected an integer, but input '%s' was not", input));
            }
        }
    }

    private static final Map<String, Boolean> BOOLEAN_CHOICES = ImmutableMap.<String, Boolean>builder()
            .put("true", true)
            .put("t", true)
            .put("y", true)
            .put("yes", true)
            .put("verymuchso", true)
            .put("false", false)
            .put("f", false)
            .put("n", false)
            .put("no", false)
            .put("notatall", false)
            .build();

    /**
     * Require an argument to be a boolean.
     * The recognized true values are:
     * <ul>
     *     <li>true</li>
     *     <li>t</li>
     *     <li>yes</li>
     *     <li>y</li>
     *     <li>verymuchso</li>
     * </ul>
     * The recognized false values are:
     * <ul>
     *     <li>false</li>
     *     <li>f</li>
     *     <li>no</li>
     *     <li>n</li>
     *     <li>notatall</li>
     * </ul>
     *
     * @param key The key to store the parsed argument under
     * @return the element to match the input
     */
    public static CommandElement bool(Translatable key) {
        return GenericArguments.choices(key, BOOLEAN_CHOICES);
    }

    /**
     * Require the argument to be a key under the provided enum
     * @param key The key to store the matched enum value under
     * @param type The enum class to get enum constants from
     * @param <T> The type of enum
     * @return the element to match the input
     */
    public static <T extends Enum<T>> CommandElement enumValue(Translatable key, Class<T> type) {
        return new EnumValueElement<>(key, type);
    }

    private static class EnumValueElement<T extends Enum<T>> extends CommandElement {
        private final Class<T> type;

        private EnumValueElement(Translatable key, Class<T> type) {
            super(key);
            this.type = type;
        }

        @Override
        public Object parseValue(CommandArgs args) throws ArgumentParseException {
            final String value = args.next().toUpperCase();
            try {
                return Enum.valueOf(type, value);
            } catch (IllegalArgumentException ex) {
                throw args.createError(t("Enum value %s not valid", value));
            }
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            Iterable<String> validValues = Iterables.transform(Arrays.asList(type.getEnumConstants()), Enum::name);

            if (args.hasNext()) {
                try {
                    final String prefix = args.next();
                    validValues = Iterables.filter(validValues, new StartsWithPredicate(prefix));
                } catch (ArgumentParseException ignore) {
                }
            }
            return ImmutableList.copyOf(validValues);
        }
    }

    /**
     * Require one or more strings, which are combined into a single, space-separated string.
     *
     * @param key The key to store the parsed argument under
     * @return the element to match the input
     */
    public static CommandElement remainingJoinedStrings(Translatable key) {
        return new RemainingJoinedStringsCommandElement(key, false);
    }

    private static class RemainingJoinedStringsCommandElement extends KeyElement {
        private final boolean raw;

        private RemainingJoinedStringsCommandElement(Translatable key, boolean raw) {
            super(key);
            this.raw = raw;
        }

        @Override
        protected Object parseValue(CommandArgs args) throws ArgumentParseException {
            if (raw) {
                args.next();
                ArgumentParseException ex = args.createError(null);
                String ret = args.getRaw().substring(ex.getPosition());
                while (args.hasNext()) {
                    args.next();
                }
                return ret;
            } else {
                final StringBuilder ret = new StringBuilder(args.next());
                while (args.hasNext()) {
                    ret.append(' ').append(args.next());
                }
                return ret.toString();
            }
        }

        @Override
        public <TextType> TextType getUsage(Commander<TextType> src) {
            return src.fmt().combined(super.getUsage(src), "...");
        }
    }

    /**
     * Expect a literal sequence of arguments. This element matches the input against a predefined array of arguments expected to be present,
     * case-insensitively.
     *
     * @param key The key to add to the context. Will be set to a value of true if this element matches
     * @param expectedArgs The sequence of arguments expected
     * @return the appropriate command element
     */
    public static CommandElement literal(Translatable key, String... expectedArgs) {
        return new LiteralCommandElement(key, ImmutableList.copyOf(expectedArgs), true);
    }

    /**
     * Expect a literal sequence of arguments. This element matches the input against a predefined array of arguments expected to be present,
     * case-insensitively.
     *
     * @param key The key to store this argument as
     * @param putValue The value to put at key if this argument matches. May be null
     * @param expectedArgs The sequence of arguments expected
     * @return the appropriate command element
     */
    public static CommandElement literal(Translatable key, Object putValue, String... expectedArgs) {
        return new LiteralCommandElement(key, ImmutableList.copyOf(expectedArgs), putValue);
    }

    private static class LiteralCommandElement extends CommandElement {
        private final List<String> expectedArgs;
        private final Object putValue;

        protected LiteralCommandElement(@Nullable Translatable key, List<String> expectedArgs, Object putValue) {
            super(key);
            this.expectedArgs = ImmutableList.copyOf(expectedArgs);
            this.putValue = putValue;
        }

        @Nullable
        @Override
        protected Object parseValue(CommandArgs args) throws ArgumentParseException {
            for (String arg : this.expectedArgs) {
                String current;
                if (!(current = args.next()).equalsIgnoreCase(arg)) {
                    throw args.createError(t("Argument %s did not match expected next argument %s", current, arg));
                }
            }
            return this.putValue;
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext ctx) {
            for (String arg : this.expectedArgs) {
                final Optional<String> next = args.nextIfPresent();
                if (!next.isPresent()) {
                    break;
                } else if (args.hasNext()) {
                    if (!next.get().equalsIgnoreCase(arg)) {
                        break;
                    }
                } else {
                    if (arg.toLowerCase().startsWith(next.get().toLowerCase())) { // Case-insensitive compare
                        return ImmutableList.of(arg); // TODO: Possibly complete all remaining args? Does that even work
                    }
                }
            }
            return ImmutableList.of();
        }

        @Override
        public <TextType> TextType getUsage(Commander<TextType> src) {
            return src.fmt().combined(Joiner.on(' ').join(this.expectedArgs));
        }
    }


}
