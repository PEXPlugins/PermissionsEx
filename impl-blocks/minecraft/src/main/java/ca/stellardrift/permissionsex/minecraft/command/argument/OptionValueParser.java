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
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Queue;

public final class OptionValueParser<C> implements ArgumentParser<C, String> {
    private static final char ESCAPE = '\\';
    private static final String FLAG_STARTER = "-";

    OptionValueParser() {
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull String> parse(
        @NonNull final CommandContext<@NonNull C> commandContext,
        @NonNull final Queue<@NonNull String> inputQueue
    ) {
        final @Nullable String input = inputQueue.peek();
        if (input == null || input.startsWith(FLAG_STARTER)) {
            return ArgumentParseResult.failure(new NoInputProvidedException(
                PermissionValueParser.class,
                commandContext
            ));
        }

        final char startChar = input.charAt(0);

        // If quoted string: read until a word ending with a quote
        if (startChar == '\'' || startChar == '\"') {
            final StringBuilder result = new StringBuilder();
            result.append(inputQueue.remove(), 1, input.length());
            @Nullable String next;
            while ((next = inputQueue.peek()) != null) {
                result.append(" ");
                if (next.length() > 0 && next.charAt(next.length() - 1) == startChar) {
                    // We've found the end of a quoted string, maybe
                    // if escaped, append without escape then continue
                    if (next.charAt(next.length() - 1) == ESCAPE) {
                        result.append(inputQueue.remove(), 0, next.length() - 2)
                            .append(startChar);
                        continue;
                    } else {
                        result.append(inputQueue.remove(), 0, next.length() - 1);
                        // then return our full quoted string
                        return ArgumentParseResult.success(result.toString());
                    }
                }
                result.append(inputQueue.remove());
            }

            // If we made it to the end without finding an end quote, throw an error
            return ArgumentParseResult.failure(new CommandException(Messages.OPTION_VALUE_ERROR_QUOTE_UNTERMINATED.tr()));
        } else { // otherwise, read until end of line, or a word starting with '-'
            final StringBuilder result = new StringBuilder();
            result.append(inputQueue.remove());
            @Nullable String next;
            while ((next = inputQueue.peek()) != null) {
                if (next.startsWith(FLAG_STARTER)) {
                    break;
                }
                result.append(" ").append(inputQueue.remove());
            }

            return ArgumentParseResult.success(result.toString());
        }
    }

    @Override
    public boolean isContextFree() {
        return true;
    }

}
