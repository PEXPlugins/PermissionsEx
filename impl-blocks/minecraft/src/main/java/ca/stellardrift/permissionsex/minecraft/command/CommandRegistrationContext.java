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
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Context for executing command registrations
 */
public final class CommandRegistrationContext {
    private final String commandPrefix;
    private final CommandManager<Commander> commandManager;
    private final MinecraftPermissionsEx<?> manager;
    private final Deque<Command.Builder<Commander>> builderStack = new ArrayDeque<>();

    public CommandRegistrationContext(
            final String commandPrefix,
            final MinecraftPermissionsEx<?> manager,
            final CommandManager<Commander> commandManager) {
        this.commandPrefix = commandPrefix;
        this.manager = manager;
        this.commandManager = commandManager;
    }

    public PermissionsEngine engine() {
        return this.manager.engine();
    }

    public MinecraftPermissionsEx<?> manager() {
        return this.manager;
    }

    public String commandPrefix() {
        return this.commandPrefix;
    }

    public CommandManager<Commander> commandManager() {
        return this.commandManager;
    }

    /**
     * Get the current command builder at the head of the command stack.
     *
     * @return the head of the stack
     */
    public Command.Builder<Commander> head() {
        final Command.@Nullable Builder<Commander> head = this.builderStack.peek();
        if (head == null) {
            throw new IllegalStateException("Tried to peek command registration while stack was empty");
        } else {
            return head;
        }
    }

    public void push(final Command.Builder<Commander> builder, final Consumer<CommandRegistrationContext> handler) {
        final int startSize = this.builderStack.size();
        this.builderStack.push(builder);
        try {
            handler.accept(this);
        } finally {
            this.builderStack.pop();
            if (this.builderStack.size() != startSize) {
                throw new IllegalStateException("Command registration stack corruption detected while popping " + builder.build());
            }
        }
    }

    public void push(final Consumer<CommandRegistrationContext> handler, final String primaryAlias, final String... aliases) {
        push(this.head().literal(primaryAlias, aliases), handler);
    }

    /**
     * Simple helper to register a subcommand of the PEX base command.
     *
     * @param maker a function that will add arguments to the PEX base command
     * @param primaryAlias the primary alias for this subcommand
     * @param aliases any other aliases
     */
    public Command<Commander> register(final Function<Command.Builder<Commander>, Command.Builder<Commander>> maker, final String primaryAlias, final String... aliases) {
        final Command.@Nullable Builder<Commander> headBuilder = this.builderStack.peek();
        if (headBuilder != null) {
            return register(maker.apply(headBuilder.literal(primaryAlias, aliases)));
        } else {
            return register(maker.apply(absoluteBuilder(primaryAlias, aliases)));
        }
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
     * Create a new command builder at the root of the tree.
     *
     * @param primaryAlias the primary command alias
     * @param aliases the aliases so tadd
     * @return the builder
     */
    public Command.Builder<Commander> absoluteBuilder(String primaryAlias, String... aliases) {
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
