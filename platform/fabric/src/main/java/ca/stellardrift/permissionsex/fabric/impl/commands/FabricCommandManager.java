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
package ca.stellardrift.permissionsex.fabric.impl.commands;

import cloud.commandframework.CommandManager;
import cloud.commandframework.CommandTree;
import cloud.commandframework.arguments.standard.UUIDArgument;
import cloud.commandframework.brigadier.BrigadierManagerHolder;
import cloud.commandframework.brigadier.CloudBrigadierManager;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import io.leangen.geantyref.TypeToken;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.Modifier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A command manager for either the server or client on Fabric.
 *
 * <p>Commands registered with managers of this type will be registered into a Brigadier command tree.</p>
 *
 * @param <C> the manager's sender type
 * @param <S> the platform sender type
 * @see FabricServerCommandManager for server commands
 * @since 1.4.0
 */
public abstract class FabricCommandManager<C, S extends CommandSource> extends CommandManager<C> implements BrigadierManagerHolder<C> {
    private final Function<S, C> commandSourceMapper;
    private final Function<C, S> backwardsCommandSourceMapper;
    private final CloudBrigadierManager<C, S> brigadierManager;


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
     * @param commandSourceMapper          Function that maps {@link ServerCommandSource} to the command sender type
     * @param backwardsCommandSourceMapper Function that maps the command sender type to {@link ServerCommandSource}
     * @param registrationHandler the handler accepting command registrations
     * @param dummyCommandSourceProvider a provider of a dummy command source, for use with brigadier registration
     */
    @SuppressWarnings("unchecked")
    FabricCommandManager(
            final @NonNull Function<@NonNull CommandTree<C>, @NonNull CommandExecutionCoordinator<C>> commandExecutionCoordinator,
            final Function<S, C> commandSourceMapper,
            final Function<C, S> backwardsCommandSourceMapper,
            final FabricCommandRegistrationHandler<C, S> registrationHandler,
            final Supplier<S> dummyCommandSourceProvider
            ) {
        super(commandExecutionCoordinator, registrationHandler);
        this.commandSourceMapper = commandSourceMapper;
        this.backwardsCommandSourceMapper = backwardsCommandSourceMapper;

        // We're always brigadier
        this.brigadierManager = new CloudBrigadierManager<>(this, () -> new CommandContext<>(
                // This looks ugly, but it's what the server does when loading datapack functions in 1.16+
                // See net.minecraft.server.function.FunctionLoader.reload for reference
                this.commandSourceMapper.apply(dummyCommandSourceProvider.get()),
                this
                ));
        this.registerNativeBrigadierMappings(this.brigadierManager);
        this.registerCommandPreProcessor(new FabricCommandPreprocessor<>(this));

        ((FabricCommandRegistrationHandler<C, S>) this.getCommandRegistrationHandler()).initialize(this);
    }

    private void registerNativeBrigadierMappings(final CloudBrigadierManager<C, S> brigadier) {
        /* Cloud-native argument types */
        brigadier.registerMapping(new TypeToken<UUIDArgument.UUIDParser<C>>() {}, true, arg -> UuidArgumentType.uuid());
    }

    @Override
    public final @NonNull CommandMeta createDefaultCommandMeta() {
        return SimpleCommandMeta.empty();
    }

    /**
     * Gets the mapper from a game {@link ServerCommandSource} to the manager's {@code C} type.
     *
     * @return Command source mapper
     */
    public final @NonNull Function<@NonNull S, @NonNull C> getCommandSourceMapper() {
        return this.commandSourceMapper;
    }

    /**
     * Gets the mapper from the manager's {@code C} type to a game {@link ServerCommandSource}.
     *
     * @return Command source mapper
     */
    public final @NonNull Function<@NonNull C, @NonNull S> getBackwardsCommandSourceMapper() {
        return this.backwardsCommandSourceMapper;
    }

    @Override
    public final @NonNull CloudBrigadierManager<C, S> brigadierManager() {
        return this.brigadierManager;
    }

    /* transition state to prevent further registration */
    final void registrationCalled() {
        this.transitionOrThrow(RegistrationState.REGISTERING, RegistrationState.AFTER_REGISTRATION);
    }

    /**
     * Check if a sender has a certain permission.
     *
     * <p>The current implementation checks op level, pending a full Fabric permissions api.</p>
     *
     * @param sender     Command sender
     * @param permission Permission node
     * @return whether the sender has the specified permission
     */
    @Override
    public final boolean hasPermission(@NonNull final C sender, @NonNull final String permission) {
        final CommandSource source = this.getBackwardsCommandSourceMapper().apply(sender);
        return Permissions.check(source, permission);
    }
}
