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

import ca.stellardrift.permissionsex.minecraft.command.CommandException;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Queue;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A parser for regular expressions.
 *
 * @param <C> the sender type
 */
public final class PatternParser<C> implements ArgumentParser<C, Pattern> {
    private final boolean greedy;

    /**
     * Create a new parser
     * @param greedy Get whether the argument should consume the remaining input, or just one word
     */
    PatternParser(final boolean greedy) {
        this.greedy = greedy;
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull Pattern> parse(
        @NonNull final CommandContext<@NonNull C> commandContext,
        @NonNull final Queue<@NonNull String> inputQueue
    ) {
        if (inputQueue.isEmpty()) {
            return ArgumentParseResult.failure(new NoInputProvidedException(PatternParser.class, commandContext));
        }

        final String input;
        if (this.greedy) {
            input = String.join(" ", inputQueue);
        } else {
            // TODO: Quoted strings
            input = inputQueue.peek();
        }

        try {
            final Pattern result = Pattern.compile(input);
            if (this.greedy) {
                inputQueue.clear();
            } else {
                inputQueue.remove();
            }
            return ArgumentParseResult.success(result);
        } catch (final PatternSyntaxException ex) {
            return ArgumentParseResult.failure(new PatternParseException(ex));
        }
    }

    public boolean greedy() {
        return this.greedy;
    }

    @Override
    public boolean isContextFree() {
        return true;
    }

    public static final class PatternParseException extends CommandException {

        private static final long serialVersionUID = -4380135510811949525L;

        private final String pattern;
        private final String error;
        private final int index;

        public PatternParseException(final PatternSyntaxException ex) {
            super(Messages.PATTERN_ERROR_SYNTAX.tr(ex.getPattern(), ex.getIndex(), ex.getDescription()));
            this.pattern = ex.getPattern();
            this.error = ex.getDescription();
            this.index = ex.getIndex();
        }

        public String pattern() {
            return this.pattern;
        }

        public String errorDescription() {
            return this.error;
        }

        public int index() {
            return this.index;
        }

    }

}
