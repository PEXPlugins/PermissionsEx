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

import cloud.commandframework.CommandManager;
import cloud.commandframework.CommandTree;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.plugin.PluginContainer;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A command manager for SpongeAPI 7.
 *
 * @param <C> the command source type
 */
public class SpongeApi7CommandManager<C> extends CommandManager<C> {

    private final PluginContainer owningPlugin;
    private final Function<CommandSource, C> forwardMapper;
    private final Function<C, CommandSource> reverseMapper;

    /**
     * Create a new command manager instance.
     *
     * @param commandExecutionCoordinator Execution coordinator instance. The coordinator is in charge of executing incoming
     *                                    commands. Some considerations must be made when picking a suitable execution coordinator
     *                                    for your platform. For example, an entirely asynchronous coordinator is not suitable
     *                                    when the parsers used in that particular platform are not thread safe. If you have
     *                                    commands that perform blocking operations, however, it might not be a good idea to
     *                                    use a synchronous execution coordinator. In most cases you will want to pick between
     *                                    {@link CommandExecutionCoordinator#simpleCoordinator()} and
     *                                    {@link AsynchronousCommandExecutionCoordinator}
     */
    @SuppressWarnings("unchecked")
    public SpongeApi7CommandManager(
        final @NonNull PluginContainer container,
        final @NonNull Function<CommandTree<C>, CommandExecutionCoordinator<C>> commandExecutionCoordinator,
        final Function<CommandSource, C> forwardMapper,
        final Function<C, CommandSource> reverseMapper
    ) {
        super(commandExecutionCoordinator, new SpongePluginRegistrationHandler<>());
        this.owningPlugin = requireNonNull(container, "container");
        this.forwardMapper = requireNonNull(forwardMapper, "forwardMapper");
        this.reverseMapper = requireNonNull(reverseMapper, "reverseMapper");
        ((SpongePluginRegistrationHandler<C>) this.getCommandRegistrationHandler()).initialize(this);
    }

    @Override
    public boolean hasPermission(final @NonNull C sender, final @NonNull String permission) {
        return this.reverseMapper.apply(sender).hasPermission(permission);
    }

    @Override
    public @NonNull CommandMeta createDefaultCommandMeta() {
        return SimpleCommandMeta.empty();
    }

    public @NonNull Function<CommandSource, C> getCommandSourceMapper() {
        return this.forwardMapper;
    }

    private @NonNull Function<C, CommandSource> getReverseCommandSourceMapper() {
        return this.reverseMapper;
    }

    PluginContainer getOwningPlugin() {
        return this.owningPlugin;
    }

}
