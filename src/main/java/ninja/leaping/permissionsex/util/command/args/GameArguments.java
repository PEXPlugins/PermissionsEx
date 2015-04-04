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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static ninja.leaping.permissionsex.util.Translations.tr;

/**
 * Contains command elements for parts of the game
 */
public class GameArguments {
    private GameArguments() {

    }

    private static abstract class KeyElement implements CommandElement {
        protected final String key;

        private KeyElement(String key) {
            this.key = key;
        }

        @Override
        public List<String> tabComplete(CommandArgs source, CommandContext context) {
            return Collections.emptyList();
        }

        @Override
        public <TextType> TextType getUsage(Commander<TextType> commander) {
            return commander.fmt().combined(key);
        }
    }

    public static CommandElement string(String key) {
        return new StringElement(key);
    }

    private static class StringElement extends KeyElement {

        private StringElement(String key) {
            super(key);
        }

        @Override
        public void parse(CommandArgs source, CommandContext context) throws ArgumentParseException {
            context.putArg(key, source.next());
        }
    }


    public static CommandElement integer(String key) {
        return new IntegerElement(key);
    }

    private static class IntegerElement extends KeyElement {

        private IntegerElement(String key) {
            super(key);
        }

        @Override
        public void parse(CommandArgs source, CommandContext context) throws ArgumentParseException {
            final String input = source.next();
            try {
                context.putArg(key, Integer.parseInt(input));
            } catch (NumberFormatException ex) {
                throw source.createError(tr("Expected a number, but input %s was not"), input);
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

    public static CommandElement bool(String key) {
        return GenericArguments.choices(key, BOOLEAN_CHOICES);
    }

    public static CommandElement subject(String type, String key) {
        return null;
    }

    private static class SubjectElement implements CommandElement {

        @Override
        public void parse(CommandArgs source, CommandContext context) throws ArgumentParseException {
            // TODO: Handle this -- gotta provide a manager?
        }

        @Override
        public List<String> tabComplete(CommandArgs source, CommandContext context) {
            return null;
        }

        @Override
        public <TextType> TextType getUsage(Commander<TextType> commander) {
            return commander.fmt().combined("subject");
        }
    }

    public static <T extends Enum<T>> CommandElement enumValue(String key, Class<T> type) {
        return new EnumValueElement<>(key, type);
    }

    private static class EnumValueElement<T extends Enum<T>> implements CommandElement {
        private final String key;
        private final Class<T> type;

        private EnumValueElement(String key, Class<T> type) {
            this.key = key;
            this.type = type;
        }

        @Override
        public void parse(CommandArgs source, CommandContext context) throws ArgumentParseException {
            final String value = source.next().toUpperCase();
            try {
                context.putArg(key, Enum.valueOf(type, value));
            } catch (IllegalArgumentException ex) {
                throw source.createError(tr("Enum value %s not valid"), value);
            }
        }

        @Override
        public List<String> tabComplete(CommandArgs source, CommandContext context) {
            Iterable<String> validValues = Iterables.transform(Arrays.asList(type.getEnumConstants()), new Function<T, String>() {
                @Nullable
                @Override
                public String apply(T input) {
                    return input.name();
                }
            });
            if (source.hasNext()) {
                try {
                    final String prefix = source.next().toUpperCase();
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

        @Override
        public <TextType> TextType getUsage(Commander<TextType> commander) {
            return commander.fmt().combined(key);
        }
    }

    public CommandElement remainingJoinedStrings(String key) {
        return GenericArguments.allOf(string(key));
    }

}
