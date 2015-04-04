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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.Commander;

import java.util.ArrayList;
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
     * @return An expectation of arguments
     */
    public static CommandElement none() {
        return new SequenceCommandElement(ImmutableList.<CommandElement>of());
    }

    /**
     * Consumes a series of arguments
     * @param elements
     * @return
     */
    public static CommandElement seq(CommandElement... elements) {
        return new SequenceCommandElement(ImmutableList.copyOf(elements));
    }

    public static CommandElement choices(String key, Map<String, ?> choices) {
        return new ChoicesCommandElement(key, ImmutableMap.copyOf(choices));
    }

    private static class ChoicesCommandElement implements CommandElement {
        private final String key;
        private final Map<String, Object> choices;

        private ChoicesCommandElement(String key, Map<String, Object> choices) {
            this.key = key;
            this.choices = choices;
        }

        @Override
        public void parse(CommandArgs source, CommandContext context) throws ArgumentParseException {
            Object value = choices.get(source.next());
            if (value == null) {
                throw source.createError(tr("Argument was not a valid choice. Valid choices: %s"), choices.keySet().toString());
            }
            context.putArg(key, value);
        }

        @Override
        public List<String> tabComplete(CommandArgs source, CommandContext context) {
            final String prefix = source.nextIfPresent().or("");
            return ImmutableList.copyOf(Iterables.filter(choices.keySet(), new Predicate<String>() {
                @Override
                public boolean apply(String input) {
                    return input.toLowerCase().startsWith(prefix.toLowerCase());
                }
            }));
        }

        @Override
        public <TextType> TextType getUsage(Commander<TextType> commander) {
            return commander.fmt().combined(this.key);
        }
    }

    private static class SequenceCommandElement implements CommandElement {
        private final List<CommandElement> elements;

        private SequenceCommandElement(List<CommandElement> elements) {
            this.elements = elements;
        }

        @Override
        public void parse(CommandArgs source, CommandContext context) throws ArgumentParseException {
            for (CommandElement element : elements) {
                element.parse(source, context);
            }
            if (source.hasNext()) {
                source.next();
                throw source.createError(tr("Too many arguments!"));
            }
        }

        @Override
        public List<String> tabComplete(CommandArgs source, CommandContext context) {
            for (CommandElement element : elements) {
                int startPos = source.getPosition();
                try {
                    element.parse(source, context);
                } catch (ArgumentParseException e) {
                    source.setPosition(startPos);
                    return element.tabComplete(source, context);
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
     * Returns a command element that matches the first of the provided elements that parses
     *
     * @param elements The elements to check against
     * @return The command element matching the first passing of the elements provided
     */
    public static CommandElement firstParsing(CommandElement... elements) {
        return new FirstParsingCommandElement(ImmutableList.copyOf(elements));
    }

    private static class FirstParsingCommandElement implements CommandElement {
        private final List<CommandElement> elements;

        private FirstParsingCommandElement(List<CommandElement> elements) {
            this.elements = elements;
        }

        @Override
        public void parse(CommandArgs source, CommandContext context) throws ArgumentParseException {
            for (CommandElement element : elements) {
                int startIndex = source.getPosition();
                try {
                    element.parse(source, context);
                    return;
                } catch (ArgumentParseException ex) {
                    source.setPosition(startIndex);
                }
            }

        }

        @Override
        public List<String> tabComplete(CommandArgs source, CommandContext context) {
            return null; // TODO: Decide on behavior for this one
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

    public static CommandElement optional(CommandElement element) {
        return new OptionalCommandElement(element, null, null);
    }

    public static CommandElement optional(CommandElement element, String defaultedKey, Object value) {
        return new OptionalCommandElement(element, defaultedKey, value);
    }

    private static class OptionalCommandElement implements CommandElement {
        private final CommandElement element;
        private final String defaultedKey;
        private final Object value;

        private OptionalCommandElement(CommandElement element, String defaultedKey, Object value) {
            this.element = element;
            this.defaultedKey = defaultedKey;
            this.value = value;
        }

        @Override
        public void parse(CommandArgs source, CommandContext context) throws ArgumentParseException {
            int startPos = source.getPosition();
            try {
                element.parse(source, context);
            } catch (ArgumentParseException ex) {
                source.setPosition(startPos);
                if (this.defaultedKey != null) {
                    context.putArg(defaultedKey, value);
                }
            }
        }

        @Override
        public List<String> tabComplete(CommandArgs source, CommandContext context) {
            return element.tabComplete(source, context);
        }

        @Override
        public <TextType> TextType getUsage(Commander<TextType> commander) {
            return commander.fmt().combined("[", this.element.getUsage(commander), "]");
        }
    }

    public static CommandElement repeated(int times, CommandElement element) {
        return null; // TODO: Will this work with how the API is?
    }

    public static CommandElement allOf(CommandElement element) {
        return null;
    }
}
