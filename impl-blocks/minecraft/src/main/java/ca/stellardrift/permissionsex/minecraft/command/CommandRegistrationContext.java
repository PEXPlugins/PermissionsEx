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
package ca.stellardrift.permissionsex.minecraft.command;

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx;
import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.meta.CommandMeta;

import java.util.Arrays;
import java.util.function.Function;

/**
 * Context for executing command registrations
 */
public final class CommandRegistrationContext {
    private final String commandPrefix;
    private final CommandManager<Commander> commandManager;
    private final MinecraftPermissionsEx<?> manager;
    private final Command.Builder<Commander> pexRoot;

    public CommandRegistrationContext(
            final String commandPrefix,
            final MinecraftPermissionsEx<?> manager,
            final CommandManager<Commander> commandManager) {
        this.commandPrefix = commandPrefix;
        this.manager = manager;
        this.commandManager = commandManager;

        // TODO: Make the active base command pushable
        this.pexRoot = this.rootBuilder("permissionsex", "pex")
                .meta(CommandMeta.DESCRIPTION, "The command for controlling PermissionsEx");
    }

    public PermissionsEngine engine() {
        return this.manager.engine();
    }

    public MinecraftPermissionsEx<?> manager() {
        return this.manager;
    }

    /**
     * Get the base command for PEX operations.
     *
     * @return the command
     */
    public Command.Builder<Commander> pex() {
        return this.pexRoot;
    }

    public CommandManager<Commander> commandManager() {
        return this.commandManager;
    }

    /**
     * Register a command with the manager.
     *
     * @param builder the builder to register
     * @return the built command
     */
    public Command<Commander> register(final Command.Builder<Commander> builder) {
        final Command<Commander> built = builder.manager(this.commandManager).build();
        this.commandManager.command(built);
        return built;
    }

    /**
     * Simple helper to register a subcommand of the PEX base command.
     *
     * @param maker a function that will add arguments to the PEX base command
     * @param primaryAlias the primary alias for this subcommand
     * @param aliases any other aliases
     */
    public Command<Commander> register(final Function<Command.Builder<Commander>, Command.Builder<Commander>> maker, final String primaryAlias, final String... aliases) {
        return register(maker.apply(this.pexRoot.literal(primaryAlias, aliases)));
    }

    /**
     * Create a new command builder at the root of the tree.
     *
     * @param primaryAlias the primary command alias
     * @param aliases the aliases so tadd
     * @return the builder
     */
    public Command.Builder<Commander> rootBuilder(String primaryAlias, String... aliases) {
        if (!this.commandPrefix.isEmpty()) {
            primaryAlias = this.commandPrefix + primaryAlias;
            aliases = Arrays.copyOf(aliases, aliases.length);
            for (int i = 0, length = aliases.length; i < length; ++i) {
                aliases[i] = this.commandPrefix + aliases[i];
            }
        }
        return this.commandManager.commandBuilder(primaryAlias, aliases);
    }
}
