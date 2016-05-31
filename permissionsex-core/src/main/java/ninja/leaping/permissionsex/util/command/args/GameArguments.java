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
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.subject.SubjectType;
import ninja.leaping.permissionsex.util.GuavaStartsWithPredicate;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.Commander;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static ninja.leaping.permissionsex.util.Translations.t;

/**
 * Contains command elements for parts of the game
 */
public class GameArguments {
    private GameArguments() {

    }

    public static CommandElement subjectType(Translatable key, PermissionsEx pex) {
        return new SubjectTypeElement(key, pex);
    }

    private static class SubjectTypeElement extends CommandElement {
        private final PermissionsEx pex;

        protected SubjectTypeElement(Translatable key, PermissionsEx pex) {
            super(key);
            this.pex = pex;
        }

        @Override
        protected Object parseValue(CommandArgs args) throws ArgumentParseException {
            final String next = args.next();
            Set<String> subjectTypes = pex.getRegisteredSubjectTypes();
            if (!subjectTypes.contains(next)) {
                throw args.createError(t("Subject type %s was not valid!", next));
            }
            return next;
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            String nextOpt = args.nextIfPresent().orElse("");
            return ImmutableList.copyOf(Iterables.filter(pex.getRegisteredSubjectTypes(), new GuavaStartsWithPredicate(nextOpt)));
        }
    }

    /**
     * Expect the provided argument to specify a subject. Subject is of one of the forms:
     * <ul>
     *     <li>&lt;type&gt;:&lt;identifier&gt;</li>
     *     <li>&lt;type&gt; &lt;identifier&gt;</li>
     * </ul>
     * @param key The key to store the parsed argument under
     * @param pex The PermissionsEx instance to fetch known subjects from
     * @return the element to match the input
     */
    public static CommandElement subject(Translatable key, PermissionsEx pex) {
        return new SubjectElement(key, pex, null);
    }

    public static CommandElement subject(Translatable key, PermissionsEx pex, String group) {
        return new SubjectElement(key, pex, group);
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
            SubjectType subjType = pex.getSubjects(type);
            //if (!subjType.isRegistered(identifier)) { // TODO: Async command elements
                final Optional<String> newIdentifier = subjType.getTypeInfo().getAliasForName(identifier);
                if (newIdentifier.isPresent()) {
                    identifier = newIdentifier.get();
                }
            //}

            if (!subjType.getTypeInfo().isNameValid(identifier)) {
                throw args.createError(t("Name '%s' is invalid for subjects of type %s", identifier, type));
            }

            return SubjectRef.of(type, identifier);
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            final Optional<String> typeSegment = args.nextIfPresent();
            if (!typeSegment.isPresent()) {
                return ImmutableList.copyOf(pex.getRegisteredSubjectTypes());
            }

            String type = typeSegment.get();
            Optional<String> identifierSegment = args.nextIfPresent();
            if (!identifierSegment.isPresent()) { // TODO: Correct tab completion logic
                if (type.contains(":")) {
                    final String[] argSplit = type.split(":", 2);
                    type = argSplit[0];
                    identifierSegment = Optional.of(argSplit[1]);
                    final SubjectType typeObj = pex.getSubjects(type);
                    final Iterable<String> allIdents = typeObj.getAllIdentifiers();
                    final Iterable<String> ret = Iterables.filter(Iterables.concat(allIdents, Iterables.filter(Iterables.transform(allIdents, k -> typeObj.getTypeInfo().getAliasForName(k).orElse(k)), v -> v != null)),
                            new GuavaStartsWithPredicate(identifierSegment.get())
                    );

                    return ImmutableList.copyOf(Iterables.transform(ret, input -> typeObj.getTypeInfo().getTypeName() + ":" + input));
                } else {
                    return ImmutableList.copyOf(Iterables.filter(pex.getRegisteredSubjectTypes(), new GuavaStartsWithPredicate(type)));
                }

            }
            final Iterable<String> allIdents = pex.getSubjects(type).getAllIdentifiers();
            final SubjectType typeObj = pex.getSubjects(type);
            final Iterable<String> ret = Iterables.filter(Iterables.concat(allIdents, Iterables.filter(Iterables.transform(allIdents, k -> typeObj.getTypeInfo().getAliasForName(k).orElse(k)), v -> v != null)),
                    new GuavaStartsWithPredicate(identifierSegment.get())
            );

            return ImmutableList.copyOf(ret);
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
                throw args.createError(t("Context must be of the form <key>=<value>!"));
            }
            return Maps.immutableEntry(contextSplit[0], contextSplit[1]);
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            return Collections.emptyList();
        }
    }

    public static CommandElement rankLadder(Translatable key, PermissionsEx pex) {
        return new RankLadderCommandElement(key, pex);
    }

    private static class RankLadderCommandElement extends CommandElement {
        private final PermissionsEx pex;

        protected RankLadderCommandElement(Translatable key, PermissionsEx pex) {
            super(key);
            this.pex = pex;
        }

        @Override
        protected Object parseValue(CommandArgs args) throws ArgumentParseException {
            return pex.getLadders().get(args.next(), null);
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            return ImmutableList.copyOf(Iterables.filter(pex.getLadders().getAll(), new GuavaStartsWithPredicate(args.nextIfPresent().orElse(""))));
        }
    }

    public static CommandElement permission(Translatable key, PermissionsEx pex) {
        return GenericArguments.suggestibleString(key, () -> pex.getRecordingNotifier().getKnownPermissions());
    }

    public static CommandElement option(Translatable key, PermissionsEx pex) {
        return GenericArguments.suggestibleString(key, () -> pex.getRecordingNotifier().getKnownOptions());
    }

}
