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

import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx;
import ca.stellardrift.permissionsex.minecraft.command.CommandException;
import ca.stellardrift.permissionsex.minecraft.command.Commander;
import ca.stellardrift.permissionsex.minecraft.command.PEXCommandPreprocessor;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ContextValueParser implements ArgumentParser<Commander, ContextValue<?>> {
    private static final Pattern CONTEXT_SPLIT = Pattern.compile("=");

    ContextValueParser() {
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull ContextValue<?>> parse(
        final @NonNull CommandContext<@NonNull Commander> ctx,
        final @NonNull Queue<@NonNull String> queue
    ) {
        final @Nullable String provided = queue.peek();
        if (provided == null) {
            return ArgumentParseResult.failure(new NoInputProvidedException(ContextValueParser.class, ctx));
        }
        final String[] contexts = CONTEXT_SPLIT.split(provided, 2);
        if (contexts.length != 2) {
            return ArgumentParseResult.failure(new CommandException(Messages.CONTEXT_ERROR_FORMAT.tr()));
        }

        final @Nullable ContextDefinition<?> definition = ctx.<MinecraftPermissionsEx<?>>get(PEXCommandPreprocessor.PEX_MANAGER).engine()
            .contextDefinition(contexts[0]);

        if (definition == null) {
            return ArgumentParseResult.failure(new CommandException(Messages.CONTEXT_ERROR_TYPE.tr(contexts[0])));
        }

        return toContextValue(definition, contexts[1]);
    }

    private <V> ArgumentParseResult<ContextValue<?>> toContextValue(final ContextDefinition<V> definition, final String input) {
        final @Nullable V value = definition.deserialize(input);

        if (value == null) {
            return ArgumentParseResult.failure(new CommandException(Messages.CONTEXT_ERROR_VALUE.tr(definition.name())));
        } else {
            return ArgumentParseResult.success(definition.createValue(value));
        }
    }

    @Override
    public @NonNull List<@NonNull String> suggestions(final @NonNull CommandContext<Commander> ctx, final @NonNull String input) {
        final MinecraftPermissionsEx<?> manager = ctx.get(PEXCommandPreprocessor.PEX_MANAGER);
        final String[] split = CONTEXT_SPLIT.split(input, 2);
        if (split.length < 2) { // before =
            return manager.engine().registeredContextTypes().stream()
                .map(ContextDefinition::name)
                .collect(Collectors.toList());
        } else { // <fully written type>=<partial value
            final @Nullable ContextDefinition<?> definition = manager.engine().contextDefinition(split[0]);
            final @Nullable SubjectRef<?> senderId = ctx.getSender().subjectIdentifier();
            if (definition == null || senderId == null) {
                return Collections.emptyList();
            }

            return serializeSuggestions(definition, manager.engine().subject(senderId).join());
        }
    }

    private <V> List<String> serializeSuggestions(final ContextDefinition<V> definition, final CalculatedSubject subject) {
        return PCollections.asVector(definition.suggestValues(subject), ent -> definition.name() + "=" + definition.serialize(ent));
    }

    @Override
    public boolean isContextFree() {
        return true;
    }

}
