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
package ca.stellardrift.permissionsex.minecraft.command.argument;

import ca.stellardrift.permissionsex.subject.SubjectType;
import cloud.commandframework.context.CommandContext;

import java.util.function.Function;

/**
 * Parsers used for our commands
 */
public final class Parsers {

    private static final ContextValueParser CONTEXT_VALUE = new ContextValueParser();
    private static final PermissionValueParser<Object> PERMISSION_VALUE = new PermissionValueParser<>();
    private static final RankLadderParser<Object> RANK_LADDER = new RankLadderParser<>();
    private static final SubjectTypeParser<Object> SUBJECT_TYPE = new SubjectTypeParser<>();
    private static final PatternParser<Object> GREEDY_PATTERN = new PatternParser<>(true);
    private static final PatternParser<Object> NON_GREEDY_PATTERN = new PatternParser<>(false);
    private static final OptionValueParser<Object> OPTION_VALUE = new OptionValueParser<>();

    public static ContextValueParser contextValue() {
        return CONTEXT_VALUE;
    }

    @SuppressWarnings("unchecked")
    public static <C> PermissionValueParser<C> permissionValue() {
        return (PermissionValueParser<C>) PERMISSION_VALUE;
    }

    @SuppressWarnings("unchecked")
    public static <C> RankLadderParser<C> rankLadder() {
        return (RankLadderParser<C>) RANK_LADDER;
    }

    @SuppressWarnings("unchecked")
    public static <C> SubjectTypeParser<C> subjectType() {
        return (SubjectTypeParser<C>) SUBJECT_TYPE;
    }

    public static <C, I> SubjectIdentifierParser<C, I> subjectIdentifier(final SubjectType<I> type) {
        return new SubjectIdentifierParser<C, I>(type);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <C> SubjectIdentifierParser<C, Object> subjectIdentifier(final Function<CommandContext<C>, SubjectType<?>> typeExtractor) {
        return new SubjectIdentifierParser<C, Object>((Function) typeExtractor);
    }

    /**
     * Get a parser for a non-greedy pattern
     *
     * @param <C> the sender type
     * @return a parser
     */
    @SuppressWarnings("unchecked")
    public static <C> PatternParser<C> pattern() {
        return (PatternParser<C>) NON_GREEDY_PATTERN;
    }

    /**
     * Get a parser for a greedy pattern
     *
     * @param <C> the sender type
     * @return a parser
     */
    @SuppressWarnings("unchecked")
    public static <C> PatternParser<C> greedyPattern() {
        return (PatternParser<C>) GREEDY_PATTERN;
    }


    /**
     * Get a parser for an option value.
     *
     * <p>This will read a greedy string up until a flag start character is found, or the end of a quoted block is found</p>
     *
     * @param <C> the sender type
     * @return a parser
     */
    @SuppressWarnings("unchecked")
    public static <C> OptionValueParser<C> optionValue() {
        return (OptionValueParser<C>) OPTION_VALUE;
    }

    private Parsers() {

    }

}
