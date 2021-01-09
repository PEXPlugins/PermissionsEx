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
package ca.stellardrift.permissionsex.sponge.command;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.StaticArgument;
import cloud.commandframework.internal.CommandRegistrationHandler;
import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.units.qual.A;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.plugin.PluginContainer;

import java.util.HashMap;
import java.util.Map;

class SpongePluginRegistrationHandler<C> implements CommandRegistrationHandler {

    private @MonotonicNonNull SpongeApi7CommandManager<C> manager;
    private final Map<CommandArgument<?, ?>, CloudCommandCallable<C>> registeredCommands = new HashMap<>();

    void initialize(final SpongeApi7CommandManager<C> manager) {
        this.manager = manager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean registerCommand(final @NonNull Command<?> command) {
        final StaticArgument<?> commandArgument = (StaticArgument<?>) command.getArguments().get(0);
        if (this.registeredCommands.containsKey(commandArgument)) {
            return false;
        }

        final CloudCommandCallable<C> callable = new CloudCommandCallable<>(
            commandArgument,
            (Command<C>) command,
            this.manager);
        this.registeredCommands.put(commandArgument, callable);

        return Sponge.getGame().getCommandManager().register(
            this.manager.getOwningPlugin(),
            callable,
            ImmutableList.copyOf(commandArgument.getAliases())
        ).isPresent();
    }

}
