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

import ca.stellardrift.permissionsex.impl.util.PCollections;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import cloud.commandframework.exceptions.parsing.NumberParseException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

public class PermissionValueParser<C> implements ArgumentParser<C, Integer> {
    private static final Map<String, Integer> CONSTANTS = PCollections.map("true", 1)
        .plus("yes", 1)
        .plus("false", -1)
        .plus("no", -1)
        .plus("zero", 0)
        .plus("none", 0)
        .plus("null", 0)
        .plus("unset", 0);

    private static final List<String> KEYS = PCollections.asVector(CONSTANTS.keySet());

    PermissionValueParser() {
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull Integer> parse(
        @NonNull final CommandContext<@NonNull C> commandContext,
        @NonNull final Queue<@NonNull String> inputQueue
    ) {
        final @Nullable String input = inputQueue.peek();
        if (input == null) {
            return ArgumentParseResult.failure(new NoInputProvidedException(
                PermissionValueParser.class,
                commandContext
            ));
        }

        Integer option = CONSTANTS.get(input.toLowerCase(Locale.ROOT));
        if (option == null) {
            try {
                option = Integer.parseInt(input);
            } catch (final IllegalArgumentException ex) {
                return ArgumentParseResult.failure(new PermissionValueParseException(input, commandContext));
            }
        }

        inputQueue.remove();
        return ArgumentParseResult.success(option);
    }

    @Override
    public @NonNull List<@NonNull String> suggestions(
        @NonNull final CommandContext<C> commandContext,
        @NonNull final String input
    ) {
        return KEYS; // only suggest the keys
    }

    @Override
    public boolean isContextFree() {
        return true;
    }

    public static final class PermissionValueParseException extends NumberParseException {

        private static final long serialVersionUID = -3088598808176112429L;

        /**
         * Construct a new number parse exception
         *
         * @param input       Input
         * @param context     Command context
         */
        PermissionValueParseException(
            @NonNull final String input,
            @NonNull final CommandContext<?> context
        ) {
            super(input, Integer.MIN_VALUE, Integer.MAX_VALUE, PermissionValueParser.class, context);
        }

        @Override
        public @NonNull String getNumberType() {
            return "permission value";
        }

        @Override
        public boolean hasMax() {
            return false;
        }

        @Override
        public boolean hasMin() {
            return false;
        }

    }

}
