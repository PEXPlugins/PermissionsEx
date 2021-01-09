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
import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx;
import ca.stellardrift.permissionsex.minecraft.command.PEXCommandPreprocessor;
import ca.stellardrift.permissionsex.subject.InvalidIdentifierException;
import ca.stellardrift.permissionsex.subject.SubjectType;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Queue;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SubjectIdentifierParser<C, I> implements ArgumentParser<C, I> {
    private final boolean contextFree;
    private final Function<CommandContext<C>, SubjectType<I>> subjectTypeExtractor;

    SubjectIdentifierParser(final SubjectType<I> identifier) {
        this.subjectTypeExtractor = ctx -> identifier;
        this.contextFree = true;
    }

    SubjectIdentifierParser(final Function<CommandContext<C>, SubjectType<I>> subjectTypeExtractor) {
        this.subjectTypeExtractor = subjectTypeExtractor;
        this.contextFree = false;
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull I> parse(
        @NonNull final CommandContext<@NonNull C> commandContext,
        @NonNull final Queue<@NonNull String> inputQueue
    ) {
        final @Nullable String input = inputQueue.peek();
        if (input == null) {
            return ArgumentParseResult.failure(new NoInputProvidedException(
                SubjectIdentifierParser.class,
                commandContext)
            );
        }
        final SubjectType<I> subjectType = this.subjectTypeExtractor.apply(commandContext);
        @Nullable InvalidIdentifierException possibleException = null;
        @Nullable I identifier;
        try {
            identifier = subjectType.parseIdentifier(input);
        } catch (final InvalidIdentifierException ex) {
            possibleException = ex;
            identifier = subjectType.parseOrCoerceIdentifier(input);
        }

        if (identifier == null) {
            return ArgumentParseResult.failure(possibleException);
        }

        inputQueue.remove();
        return ArgumentParseResult.success(identifier);
    }

    @Override
    public @NonNull List<@NonNull String> suggestions(
        @NonNull final CommandContext<C> commandContext,
        @NonNull final String input
    ) {
        final MinecraftPermissionsEx<?> manager = commandContext.get(PEXCommandPreprocessor.PEX_MANAGER);
        final SubjectType<I> type = this.subjectTypeExtractor.apply(commandContext);
        if (type == null) {
            return PCollections.vector();
        }
        // TODO: Include friendly names here?
        // TODO: Do we want to filter this
        return manager.engine().subjects(type).allIdentifiers()
            .limit(100)
            .map(type::serializeIdentifier)
            .collect(Collectors.toList());
    }

    @Override
    public boolean isContextFree() {
        return this.contextFree;
    }

}
