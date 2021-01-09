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
import ca.stellardrift.permissionsex.minecraft.command.CommandException;
import ca.stellardrift.permissionsex.minecraft.command.PEXCommandPreprocessor;
import ca.stellardrift.permissionsex.subject.SubjectType;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Queue;

public final class SubjectTypeParser<C> implements ArgumentParser<C, SubjectType<?>> {

    SubjectTypeParser() {
    }

    @Override
    public @NonNull ArgumentParseResult<SubjectType<?>> parse(@NonNull CommandContext<C> commandContext, @NonNull Queue<String> inputQueue) {
        final @Nullable String input = inputQueue.peek();
        if (input == null) {
            return ArgumentParseResult.failure(new NoInputProvidedException(SubjectTypeParser.class, commandContext));
        }

        final MinecraftPermissionsEx<?> pex = commandContext.get(PEXCommandPreprocessor.PEX_MANAGER);
        final String lowerInput = input.toLowerCase(Locale.ROOT);
        for (final SubjectType<?> type : pex.engine().knownSubjectTypes()) {
            if (type.name().toLowerCase(Locale.ROOT).startsWith(lowerInput)) { // TODO: do we want to use startsWith here?
                inputQueue.remove();
                return ArgumentParseResult.success(type);
            }
        }
        return ArgumentParseResult.failure(new CommandException(Messages.SUBJECTTYPE_ERROR_NOTATYPE.tr(input)));
    }

    @Override
    public @NonNull List<String> suggestions(@NonNull CommandContext<C> commandContext, @NonNull String input) {
        final MinecraftPermissionsEx<?> pex = commandContext.get(PEXCommandPreprocessor.PEX_MANAGER);
        return PCollections.asVector(pex.engine().knownSubjectTypes(), SubjectType::name);
    }

}
