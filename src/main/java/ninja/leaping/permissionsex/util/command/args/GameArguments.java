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
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.Commander;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static ninja.leaping.permissionsex.util.Translations._;

/**
 * Contains command elements for parts of the game
 */
public class GameArguments {
    private GameArguments() {

    }

    /**
     * Expect the provided argument to specify a subject. Subject is of one of the forms:
     * <ul>
     *     <li>&lt;type&gt;:&lt;identifier&gt;</li>
     *     <li>&lt;type&gt; &lt;identifier&gt;</li>
     * </ul>
     * TODO: How do we accept pretty names for users?
     * @param key The key to store the parsed argument under
     * @param pex The PermissionsEx instance to fetch known subjects from
     * @return the element to match the input
     */
    public static CommandElement subject(Translatable key, PermissionsEx pex) {
        return new SubjectElement(key, pex, null);
    }

    private static class SubjectElement extends CommandElement {
        private final PermissionsEx pex;
        private final String defaultType;

        protected SubjectElement(Translatable key, PermissionsEx pex, String defaultType) {
            super(key);
            this.pex = pex;
            this.defaultType = defaultType;
        }

        @Override
        protected Object parseValue(CommandArgs args) throws ArgumentParseException {
            String type = args.next();
            String identifier;
            if (type.contains(":")) {
                String[] typeSplit = type.split(":", 2);
                type = typeSplit[0];
                identifier = typeSplit[1];
            } else if (!args.hasNext() && this.defaultType != null) {
                identifier = type;
                type = this.defaultType;
            } else {
                identifier = args.next();
            }
            return Maps.immutableEntry(type, identifier);
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            final Optional<String> typeSegment = args.nextIfPresent();
            if (!typeSegment.isPresent()) {
                return ImmutableList.copyOf(pex.getRegisteredSubjectTypes());
            }

            String type = typeSegment.get();
            Optional<String> identifierSegment = args.nextIfPresent();
            if (!identifierSegment.isPresent()) {
                if (type.contains(":")) {
                    final String[] argSplit = type.split(":", 2);
                    type = argSplit[0];
                    identifierSegment = Optional.of(argSplit[1]);
                    final String finalType = type;
                    return ImmutableList.copyOf(
                            Iterables.transform(
                            Iterables.filter(
                                    Iterables.concat(pex.getSubjects(type).getAllIdentifiers(), pex.getTransientSubjects(type).getAllIdentifiers()),
                                    new GenericArguments.StartsWithPredicate(identifierSegment.get())
                            ), new Function<String, String>() {
                                        @Override
                                        public String apply(String input) {
                                            return finalType + ":" + input;
                                        }
                                    }));
                } else {
                    return ImmutableList.copyOf(Iterables.filter(pex.getRegisteredSubjectTypes(), new GenericArguments.StartsWithPredicate(type)));
                }

            }
            return ImmutableList.copyOf(
                    Iterables.filter(
                            Iterables.concat(pex.getSubjects(type).getAllIdentifiers(), pex.getTransientSubjects(type).getAllIdentifiers()),
                            new GenericArguments.StartsWithPredicate(identifierSegment.get())
                    ));
        }
    }

    public static CommandElement context(Translatable key) {
        return new ContextCommandElement(key);
    }

    private static class ContextCommandElement extends CommandElement {

        protected ContextCommandElement(Translatable key) {
            super(key);
        }

        @Override
        protected Object parseValue(CommandArgs args) throws ArgumentParseException {
            final String context = args.next(); // TODO: Allow multi-word contexts (<key> <value>)
            final String[] contextSplit = context.split("=", 2);
            if (contextSplit.length != 2) {
                throw args.createError(_("Context must be of the form <key>=<value>!"));
            }
            return Maps.immutableEntry(contextSplit[0], contextSplit[1]);
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            return Collections.emptyList();
        }
    }

}
