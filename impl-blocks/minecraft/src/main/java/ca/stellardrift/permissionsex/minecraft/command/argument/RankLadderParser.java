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

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.minecraft.command.PEXCommandPreprocessor;
import ca.stellardrift.permissionsex.rank.RankLadder;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public final class RankLadderParser<C> implements ArgumentParser<C, RankLadder> {

    RankLadderParser() {
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull RankLadder> parse(
            final @NonNull CommandContext<@NonNull C> ctx,
            final @NonNull Queue<@NonNull String> inputQueue) {
        final @Nullable String input = inputQueue.poll();
        if (input == null) {
            return ArgumentParseResult.failure(new NoInputProvidedException(ContextValueParser.class, ctx));
        }
        final PermissionsEngine engine = ctx.get(PEXCommandPreprocessor.PEX_ENGINE);
        return ArgumentParseResult.success(engine.ladders().get(input, null).join());
    }

    @Override
    public @NonNull List<@NonNull String> suggestions(final @NonNull CommandContext<C> ctx, @NonNull String input) {
        final PermissionsEngine engine = ctx.get(PEXCommandPreprocessor.PEX_ENGINE);
        return engine.ladders()
                .names()
                .collect(Collectors.toList());
    }

    @Override
    public boolean isContextFree() {
        return true;
    }
}
