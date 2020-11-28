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

import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx;
import ca.stellardrift.permissionsex.minecraft.command.Commander;
import ca.stellardrift.permissionsex.minecraft.command.PEXCommandPreprocessor;
import ca.stellardrift.permissionsex.subject.SubjectType;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;

public class SubjectTypeArgument extends CommandArgument<Commander, SubjectType<?>> {


    SubjectTypeArgument(final boolean required,
                        final @NonNull String name,
                        final @NonNull String defaultValue,
                        final @NonNull TypeToken<SubjectType<?>> valueType,
                        final @Nullable BiFunction<CommandContext<Commander>, String, List<String>> suggestionsProvider) {
        super(required,
                name,
                new SubjectTypeArgument.Parser(),
                defaultValue,
                valueType,
                suggestionsProvider);
    }

    public static final class Builder extends CommandArgument.Builder<Commander, SubjectType<?>> {
        private static final TypeToken<SubjectType<?>> TYPE = new TypeToken<SubjectType<?>>() {};
        private boolean onlyExisting;

        protected Builder(final @NonNull String name) {
            super(TYPE, name);
        }

        public Builder onlyExisting() {
            this.onlyExisting = true;
            return this;
        }

        @Override
        public @NonNull CommandArgument<Commander, SubjectType<?>> build() {
            return new SubjectTypeArgument(this.isRequired(),
                    this.getName(),
                    this.getDefaultValue(),
                    TYPE,
                    this.getSuggestionsProvider());
        }
    }

    static final class Parser implements ArgumentParser<Commander, SubjectType<?>> {

        @Override
        public @NonNull ArgumentParseResult<SubjectType<?>> parse(@NonNull CommandContext<Commander> commandContext, @NonNull Queue<String> inputQueue) {
            final MinecraftPermissionsEx<?> pex = commandContext.get(PEXCommandPreprocessor.PEX_MANAGER);
            return null;
        }

        @Override
        public @NonNull List<String> suggestions(@NonNull CommandContext<Commander> commandContext, @NonNull String input) {
            final MinecraftPermissionsEx<?> pex = commandContext.get(PEXCommandPreprocessor.PEX_MANAGER);
            return Collections.emptyList();
        }
    }
}
