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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.Commander;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static ninja.leaping.permissionsex.util.Translations.tr;

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
            for (CommandElement element : elements) {
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
        public <TextType> TextType getUsage(Commander<TextType> commander) {
            final List<Object> ret = new ArrayList<>(elements.size() * 2 - 1);
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
     * will only display only the key. To override this behavior, see {@link #choices(String, Map, boolean)}.
     *
     * @param key The key to store the resulting value under
     * @param choices The choices users can choose from
     * @return the element to match the input
     */
    public static CommandElement choices(String key, Map<String, ?> choices) {
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
    public static CommandElement choices(String key, Map<String, ?> choices, boolean choicesInUsage) {
        return new ChoicesCommandElement(key, ImmutableMap.copyOf(choices), choicesInUsage);
    }

    private static class ChoicesCommandElement extends CommandElement {
        private final Map<String, Object> choices;
        private final boolean choicesInUsage;

        private ChoicesCommandElement(String key, Map<String, Object> choices, boolean choicesInUsage) {
            super(key);
            this.choices = choices;
            this.choicesInUsage = choicesInUsage;
        }

        @Override
        public Object parseValue(CommandArgs args) throws ArgumentParseException {
            Object value = choices.get(args.next());
            if (value == null) {
                throw args.createError(tr("Argument was not a valid choice. Valid choices: %s", choices.keySet().toString()));
            }
            return value;
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            final String prefix = args.nextIfPresent().or("");
            return ImmutableList.copyOf(Iterables.filter(choices.keySet(), new Predicate<String>() {
                @Override
                public boolean apply(String input) {
                    return input.toLowerCase().startsWith(prefix.toLowerCase());
                }
            }));
        }

        @Override
        public <TextType> TextType getUsage(Commander<TextType> commander) {
            if (choicesInUsage) {
                List<Object> args = new ArrayList<>(choices.size() * 2 - 1);
                for (Iterator<String> it = choices.keySet().iterator(); it.hasNext();) {
                    args.add(it.next());
                    if (it.hasNext()) {
                        args.add("|");
                    }
                }
                return commander.fmt().combined(args.toArray());
            } else {
                return commander.fmt().combined(getKey());
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
        protected Object parseValue(CommandArgs args) throws ArgumentParseException {
            ArgumentParseException firstException = null;
            for (CommandElement element : elements) {
                int startIndex = args.getPosition();
                try {
                    return element.parseValue(args);
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
            return null;
        }

        @Override
        public <TextType> List<String> tabComplete(final Commander<TextType> src, final CommandArgs args, final CommandContext context) {
            return ImmutableList.copyOf(Iterables.concat(Iterables.transform(elements, new Function<CommandElement, Iterable<String>>() {
                @Nullable
                @Override
                public Iterable<String> apply(CommandElement input) {
                    return input.tabComplete(src, args, context);
                }
            })));
        }

        @Override
        public <TextType> TextType getUsage(Commander<TextType> commander) {
            final List<Object> ret = new ArrayList<>(elements.size() * 2 - 1);
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
        return new OptionalCommandElement(element, null, null, false);
    }

    /**
     * Make the provided command element optional
     * This means the command element is not required. However, if the element is provided with invalid format and there
     * are no more args specified, errors will still be passed on. To suppress errors instead,
     * set {@code considerInvalidFormatEmpty} to true.
     *
     * @param element The element to optionally require
     * @return the element to match the input
     */
    public static CommandElement optional(CommandElement element, boolean considerInvalidFormatEmpty) {
        return new OptionalCommandElement(element, null, null, considerInvalidFormatEmpty);
    }

    /**
     * Make the provided command element optional
     * This means the command element is not required. However, if the element is provided with invalid format and there
     * are no more args specified, any errors will still be passed on. If {@code defaultedKey} and {@code value} are not
     * null, if this element is not provided the default key will be set to the given value.
     *
     * @param element The element to optionally require
     * @param defaultedKey The key to store the default value under if parsing fails
     * @param value The default value to set
     * @return the element to match the input
     */
    public static CommandElement optional(CommandElement element, String defaultedKey, Object value) {
        return new OptionalCommandElement(element, defaultedKey, value, false);
    }

    /**
     * Make the provided command element optional
     * This means the command element is not required. However, if the element is provided with invalid format and there
     * are no more args specified, errors will still be passed on. To suppress errors instead,
     * set {@code considerInvalidFormatEmpty} to true.
     * If {@code defaultedKey} and {@code value} are not null, if this element is not provided the default key will be
     * set to the given value.
     *
     * @param element The element to optionally require
     * @param defaultedKey The key to store the default value under if parsing fails
     * @param value The default value to set
     * @return the element to match the input
     */
    public static CommandElement optional(CommandElement element, String defaultedKey, Object value, boolean considerInvalidFormatEmpty) {
        return new OptionalCommandElement(element, defaultedKey, value, considerInvalidFormatEmpty);
    }

    private static class OptionalCommandElement extends CommandElement {
        private final CommandElement element;
        private final String defaultedKey;
        private final Object value;
        private final boolean considerInvalidFormatEmpty;

        private OptionalCommandElement(CommandElement element, String defaultedKey, Object value, boolean considerInvalidFormatEmpty) {
            super(null);
            this.element = element;
            this.defaultedKey = defaultedKey;
            this.value = value;
            this.considerInvalidFormatEmpty = considerInvalidFormatEmpty;
        }

        @Override
        public void parse(CommandArgs args, CommandContext context) throws ArgumentParseException {
            if (!args.hasNext()) {
                if (this.defaultedKey != null) {
                    context.putArg(defaultedKey, value);
                }
                return;
            }
            int startPos = args.getPosition();
            try {
                element.parse(args, context);
            } catch (ArgumentParseException ex) {
                if (considerInvalidFormatEmpty || args.hasNext()) { // If there are more args, suppress. Otherwise, throw the error
                    args.setPosition(startPos);
                    if (this.defaultedKey != null && this.value != null) {
                        context.putArg(defaultedKey, value);
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
        private KeyElement(String key) {
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
    public static CommandElement string(String key) {
        return new StringElement(key);
    }

    private static class StringElement extends KeyElement {

        private StringElement(String key) {
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
    public static CommandElement integer(String key) {
        return new IntegerElement(key);
    }

    private static class IntegerElement extends KeyElement {

        private IntegerElement(String key) {
            super(key);
        }

        @Override
        public Object parseValue(CommandArgs args) throws ArgumentParseException {
            final String input = args.next();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException ex) {
                throw args.createError(tr("Expected an integer, but input '%s' was not", input));
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
    public static CommandElement bool(String key) {
        return GenericArguments.choices(key, BOOLEAN_CHOICES);
    }

    /**
     * Require the argument to be a key under the provided enum
     * @param key The key to store the matched enum value under
     * @param type The enum class to get enum constants from
     * @param <T> The type of enum
     * @return the element to match the input
     */
    public static <T extends Enum<T>> CommandElement enumValue(String key, Class<T> type) {
        return new EnumValueElement<>(key, type);
    }

    private static class EnumValueElement<T extends Enum<T>> extends CommandElement {
        private final Class<T> type;

        private EnumValueElement(String key, Class<T> type) {
            super(key);
            this.type = type;
        }

        @Override
        public Object parseValue(CommandArgs args) throws ArgumentParseException {
            final String value = args.next().toUpperCase();
            try {
                return Enum.valueOf(type, value);
            } catch (IllegalArgumentException ex) {
                throw args.createError(tr("Enum value %s not valid", value));
            }
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            Iterable<String> validValues = Iterables.transform(Arrays.asList(type.getEnumConstants()), new Function<T, String>() {
                @Nullable
                @Override
                public String apply(T input) {
                    return input.name();
                }
            });

            if (args.hasNext()) {
                try {
                    final String prefix = args.next().toUpperCase();
                    validValues = Iterables.filter(validValues, new Predicate<String>() {
                        @Override
                        public boolean apply(@Nullable String input) {
                            return input.startsWith(prefix);
                        }
                    });
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
    public CommandElement remainingJoinedStrings(String key) {
        return new RemainingJoinedStringsCommandElement(key, false);
    }

    private static class RemainingJoinedStringsCommandElement extends KeyElement {
        private final boolean raw;

        private RemainingJoinedStringsCommandElement(String key, boolean raw) {
            super(key);
            this.raw = raw;
        }

        @Override
        protected Object parseValue(CommandArgs args) throws ArgumentParseException {
            if (raw) {
                args.next();
                ArgumentParseException ex = args.createError(null);
                return args.getRaw().substring(ex.getPosition());
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


}
