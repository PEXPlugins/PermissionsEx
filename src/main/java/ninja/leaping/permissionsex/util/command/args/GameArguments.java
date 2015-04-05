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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.Commander;

import java.util.List;

/**
 * Contains command elements for parts of the game
 */
public class GameArguments {
    private GameArguments() {

    }

    /**
     * Expect the provided argument to specify a subject. Subject is of one of the forms:
     * <ul>
     *     <li>&lt;type>:&lt;identifier></li>
     *     <li>&lt;type> &ltidentifier></li>
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

            final String type = typeSegment.get();
            final Optional<String> identifierSegment = args.nextIfPresent();
            if (!identifierSegment.isPresent()) {
                System.out.println("Identifier is not present, completing for subject types");
                return ImmutableList.copyOf(Iterables.filter(pex.getRegisteredSubjectTypes(), new GenericArguments.StartsWithPredicate(type)));
            } else {
                System.out.println("Getting completions for subject identifier type " + type);
                List<String> ret = ImmutableList.copyOf(
                        Iterables.filter(
                                Iterables.concat(pex.getSubjects(type).getAllIdentifiers(), pex.getTransientSubjects(type).getAllIdentifiers()),
                                new GenericArguments.StartsWithPredicate(identifierSegment.get())
                        ));
                System.out.println("Suggestions: " + ret);
                return ret;
            }
        }
    }

}
