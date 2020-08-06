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
import ca.stellardrift.permissionsex.util.GuavaStartsWithPredicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static ca.stellardrift.permissionsex.commands.ArgumentKeys.FLAG_ERROR_UNKNOWNLONG;
import static ca.stellardrift.permissionsex.commands.ArgumentKeys.FLAG_ERROR_UNKNOWNSHORT;
import static net.kyori.text.TextComponent.space;

public class StructuralArguments {
    private static CommandElement markTrue(String flag) {
        return new MarkTrueCommandElement().key(flag);
    }

    public static FlagCommandElementBuilder flags() {
        return new FlagCommandElementBuilder();
    }

    /**
     * Consumes a series of arguments. Usage is the elements concatenated
     *
     * @param elements The series of arguments to expect
     * @return the element to match the input
     */
    public static CommandElement seq(CommandElement... elements) {
        return new SequenceCommandElement(ImmutableList.copyOf(elements));
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

    private static class MarkTrueCommandElement extends Value<Boolean> {
        static final MarkTrueCommandElement INSTANCE = new MarkTrueCommandElement();
        private MarkTrueCommandElement() {
            super(TextComponent.of("true"));
        }

        @Override
        public Boolean parse(@NotNull CommandArgs args) throws ArgumentParseException {
            return true;
        }

        public ValueElement<Boolean> key(String key) {
            return this.key(TextComponent.of(key));
        }
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
            if (longFlag.isEmpty()) {
                return false;
            }

            if (longFlag.contains("=")) {
                final String[] flagSplit = longFlag.split("=", 2);
                longFlag = flagSplit[0];
                String value = flagSplit[1];
                CommandElement element = longFlags.get(longFlag.toLowerCase());
                if (element == null) {
                    switch (unknownLongFlagBehavior) {
                        case ERROR:
                            throw args.createError(FLAG_ERROR_UNKNOWNLONG.toComponent(longFlag));
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
                            throw args.createError(FLAG_ERROR_UNKNOWNLONG.toComponent(longFlag));
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
            if (shortFlags.isEmpty()) {
                return false;
            }

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
                            throw args.createError(FLAG_ERROR_UNKNOWNSHORT.toComponent(flagChar));
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
        public Component getUsage(Commander src) {
            return TextComponent.make(builder -> {
                for (Map.Entry<List<String>, CommandElement> arg : usageFlags.entrySet()) {
                    builder.append("[");
                    for (Iterator<String> it = arg.getKey().iterator(); it.hasNext();) {
                        builder.append("-");
                        builder.append(it.next());
                        if (it.hasNext()) {
                            builder.append("|");
                        }
                    }
                    if (!(arg.getValue() instanceof ValueElement<?>)
                            || !(((ValueElement<?>) arg.getValue()).getValue() instanceof MarkTrueCommandElement)) {
                        builder.append(" ");
                        builder.append(arg.getValue().getUsage(src));
                    }
                    builder.append("]");
                    builder.append(" ");
                }

                if (childElement != null) {
                    builder.append(childElement.getUsage(src));
                }
            });
        }

        @Override
        public List<String> tabComplete(Commander src, CommandArgs args, CommandContext context) {
            int startIdx = args.getPosition();
            String arg;
            while (args.hasNext()) {
                arg = args.nextIfPresent();
                if (arg.startsWith("-")) {
                    int flagStartIdx = args.getPosition();
                    if (arg.startsWith("--")) { // Long flag
                        String longFlag = arg.substring(2);
                        List<String> ret = tabCompleteLongFlag(longFlag, src, args, context);
                        if (ret != null) {
                            return ret;
                        }
                    } else {
                        final String argStr = arg.substring(1);
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

        private List<String> tabCompleteLongFlag(String longFlag, Commander src, CommandArgs args, CommandContext context) {
            if (longFlag.isEmpty()) {
                return null;
            }

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
                    return ImmutableList.copyOf(Iterables.transform(Iterables.filter(longFlags.keySet(), new GuavaStartsWithPredicate(longFlag.toLowerCase())), input -> "--" + input));
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

        private List<String> tabCompleteShortFlags(String shortFlags, Commander src, CommandArgs args, CommandContext context) {
            if (shortFlags.isEmpty()) {
                return null;
            }

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

    static class SequenceCommandElement extends CommandElement {
        private final List<CommandElement> elements;

        SequenceCommandElement(List<CommandElement> elements) {
            this.elements = elements;
        }

        @Override
        public void parse(CommandArgs args, CommandContext context) throws ArgumentParseException {
            for (CommandElement element : elements) {
                element.parse(args, context);
            }
        }

        @Override
        public List<String> tabComplete(Commander src, CommandArgs args, CommandContext context) {
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
        public Component getUsage(Commander commander) {
            return TextComponent.make(builder -> {
                for (Iterator<CommandElement> it = elements.iterator(); it.hasNext();) {
                    builder.append(it.next().getUsage(commander));
                    if (it.hasNext()) {
                        builder.append(space());
                    }
                }
            });
        }
    }

    private static class FirstParsingCommandElement extends CommandElement {
        private final List<CommandElement> elements;

        private FirstParsingCommandElement(List<CommandElement> elements) {
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
        public  List<String> tabComplete(final Commander src, final CommandArgs args, final CommandContext context) {
            return ImmutableList.copyOf(Iterables.concat(Iterables.transform(elements, input -> {
                    int startIndex = args.getPosition();
                    List<String> ret = input.tabComplete(src, args, context);
                    args.setPosition(startIndex);
                    return ret;
            })));
        }

        @Override
        public  Component getUsage(Commander commander) {
            return TextComponent.make(builder -> {
                for (Iterator<CommandElement> it = elements.iterator(); it.hasNext();) {
                    builder.append(it.next().getUsage(commander));
                    if (it.hasNext()) {
                        builder.append("|");
                    }
                }
            });
        }
    }

    private static class OptionalCommandElement extends CommandElement {
        private final CommandElement element;
        private final Object value;
        private final boolean considerInvalidFormatEmpty;

        private OptionalCommandElement(CommandElement element, Object value, boolean considerInvalidFormatEmpty) {
            this.element = element;
            this.value = value;
            this.considerInvalidFormatEmpty = considerInvalidFormatEmpty;
        }

        @Override
        public void parse(CommandArgs args, CommandContext context) throws ArgumentParseException {
            if (!args.hasNext()) {
                /*if (this.element.getKey() != null && this.value != null) { // TODO: Handle defaults using a better design
                    context.putArg(this.element.getKey(), value);
                }*/
                return;
            }
            int startPos = args.getPosition();
            try {
                element.parse(args, context);
            } catch (ArgumentParseException ex) {
                if (considerInvalidFormatEmpty || args.hasNext()) { // If there are more args, suppress. Otherwise, throw the error
                    args.setPosition(startPos);
                    /*if (this.element.getKey() != null && this.value != null) {
                        context.putArg(this.element.getKey(), value);
                    }*/
                } else {
                    throw ex;
                }
            }
        }

        @Override
        public  List<String> tabComplete(Commander src, CommandArgs args, CommandContext context) {
            return element.tabComplete(src, args, context);
        }

        @Override
        public  Component getUsage(Commander src) {
            return TextComponent.make(builder -> {
                builder.append("[");
                builder.append(this.element.getUsage(src));
                builder.append("]");
            });
        }
    }

    private static class RepeatedCommandElement extends CommandElement {
        private final CommandElement element;
        private final int times;


        protected RepeatedCommandElement(CommandElement element, int times) {
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
        public List<String> tabComplete(Commander src, CommandArgs args, CommandContext context) {
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
        public Component getUsage(Commander src) {
            return TextComponent.builder(String.valueOf(times)).append("*").append(element.getUsage(src)).build();
        }
    }

    private static class AllOfCommandElement extends CommandElement {
        private final CommandElement element;


        protected AllOfCommandElement(CommandElement element) {
            this.element = element;
        }

        @Override
        public void parse(CommandArgs args, CommandContext context) throws ArgumentParseException {
            while (args.hasNext()) {
                element.parse(args, context);
            }
        }

        @Override
        public List<String> tabComplete(Commander src, CommandArgs args, CommandContext context) {
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
        public Component getUsage(Commander context) {
            return element.getUsage(context).append(TextComponent.of("+"));
        }
    }
}
