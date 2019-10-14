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

package ca.stellardrift.permissionsex.util.command.args;

import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.util.Translatable;
import ca.stellardrift.permissionsex.util.command.CommandContext;
import ca.stellardrift.permissionsex.util.command.Commander;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ca.stellardrift.permissionsex.util.Translations.t;
import static ca.stellardrift.permissionsex.util.Utilities.caseInsensitiveStartsWith;

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
            if (!pex.getRegisteredSubjectTypes().hasElement(next).defaultIfEmpty(false).block()) {
                throw args.createError(t("Subject type %s was not valid!", next));
            }
            return next;
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            String nextOpt = args.nextIfPresent().orElse("");
            return pex.getRegisteredSubjectTypes().filter(caseInsensitiveStartsWith(nextOpt)).collect(Collectors.toList()).block();
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
            if (!subjType.isRegistered(identifier).block()) { // TODO: Async command elements
                final Optional<String> newIdentifier = subjType.getTypeInfo().getAliasForName(identifier);
                if (newIdentifier.isPresent()) {
                    identifier = newIdentifier.get();
                }
            }

            if (!subjType.getTypeInfo().isNameValid(identifier)) {
                throw args.createError(t("Name '%s' is invalid for subjects of type %s", identifier, type));
            }

            return Maps.immutableEntry(type, identifier);
        }

        @Override
        public <TextType> List<String> tabComplete(Commander<TextType> src, CommandArgs args, CommandContext context) {
            final Optional<String> typeSegment = args.nextIfPresent();
            if (!typeSegment.isPresent()) {
                return pex.getRegisteredSubjectTypes().collect(Collectors.toList()).block();
            }

            String type = typeSegment.get();
            Optional<String> identifierSegment = args.nextIfPresent();
            if (!identifierSegment.isPresent()) { // TODO: Correct tab completion logic
                if (type.contains(":")) {
                    final String[] argSplit = type.split(":", 2);
                    type = argSplit[0];
                    identifierSegment = Optional.of(argSplit[1]);
                    final SubjectType typeObj = pex.getSubjects(type);
                    final Flux<String> allIdents = typeObj.getAllIdentifiers();

                    return allIdents.map(k -> typeObj.getTypeInfo().getAliasForName(k).orElse(k))
                            .concatWith(allIdents)
                            .filter(caseInsensitiveStartsWith(identifierSegment.get()))
                            .map(it -> typeObj.getTypeInfo().getTypeName() + ":" + it)
                            .collect(Collectors.toList()).block();

                } else {
                    return pex.getRegisteredSubjectTypes()
                            .filter(caseInsensitiveStartsWith(type))
                            .collect(Collectors.toList())
                            .block();
                }
            }

            final Flux<String> allIdents = pex.getSubjects(type).getAllIdentifiers();
            final SubjectType typeObj = pex.getSubjects(type);
            return allIdents.map(k -> typeObj.getTypeInfo().getAliasForName(k).orElse(k)).concatWith(allIdents)
                    .filter(caseInsensitiveStartsWith(identifierSegment.get()))
                    .collect(Collectors.toList())
                    .block();
        }
    }

    public static CommandElement context(Translatable key, PermissionsEx pex) {
        return new ContextCommandElement(key, pex);
    }

    private static class ContextCommandElement extends CommandElement {
        private final PermissionsEx pex;

        protected ContextCommandElement(Translatable key, PermissionsEx pex) {
            super(key);
            this.pex = pex;
        }

        @Override
        protected Object parseValue(CommandArgs args) throws ArgumentParseException {
            final String context = args.next(); // TODO: Allow multi-word contexts (<key> <value>)
            final String[] contextSplit = context.split("=", 2);
            if (contextSplit.length != 2) {
                throw args.createError(t("Context must be of the form <key>=<value>!"));
            }
            ContextDefinition<?> def = pex.getContextDefinition(contextSplit[0]);
            if (def == null) {
                throw args.createError(t("Unknown context type %s", contextSplit[0]));
            }
            ContextValue<?> ret = toCtxValue(def, contextSplit[1]);
            if (ret == null) {
                throw args.createError(t("Unable to parse value for context " + context));
            }
            return ret;
        }

        @Nullable
        private <T> ContextValue<T> toCtxValue(ContextDefinition<T> def, String input) {
            T value = def.deserialize(input);
            if (value == null) {
                return null;
            } else {
                return def.createValue(value);
            }
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
            return pex.getLadders().getAll()
                    .filter(caseInsensitiveStartsWith(args.nextIfPresent().orElse("")))
                    .collect(Collectors.toList())
                    .block();
        }
    }

    public static CommandElement permission(Translatable key, PermissionsEx pex) {
        return GenericArguments.suggestibleString(key, () -> pex.getRecordingNotifier().getKnownPermissions());
    }

    public static CommandElement option(Translatable key, PermissionsEx pex) {
        return GenericArguments.suggestibleString(key, () -> pex.getRecordingNotifier().getKnownOptions());
    }

}
