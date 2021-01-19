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

import cloud.commandframework.CommandTree;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.function.Function;

/**
 * A command manager for registering server-side commands.
 *
 * <p>All commands should be registered within mod initializers. Any registrations occurring after the first call to
 * {@link CommandRegistrationCallback} will be considered <em>unsafe</em>, and will only be permitted when the unsafe
 * registration manager option is enabled.</p>
 *
 * @param <C> the command sender type
 * @since 1.4.0
 */
public final class FabricServerCommandManager<C> extends FabricCommandManager<C, ServerCommandSource> {

    /**
     * A meta attribute specifying which environments a command should be registered in.
     *
     * <p>The default value is {@link CommandManager.RegistrationEnvironment#ALL}.</p>
     */
    public static final CommandMeta.Key<CommandManager.RegistrationEnvironment> META_REGISTRATION_ENVIRONMENT = CommandMeta.Key.of(
            CommandManager.RegistrationEnvironment.class,
            "cloud:registration-environment"
    );

    /**
     * Create a command manager using native source types.
     *
     * @param execCoordinator Execution coordinator instance.
     * @return a new command manager
     * @see #FabricServerCommandManager(Function, Function, Function) for a more thorough explanation
     */
    public static FabricServerCommandManager<ServerCommandSource> createNative(
            final Function<CommandTree<ServerCommandSource>, CommandExecutionCoordinator<ServerCommandSource>> execCoordinator
    ) {
        return new FabricServerCommandManager<>(execCoordinator, Function.identity(), Function.identity());
    }

    /**
     * Create a new command manager instance.
     *
     * @param commandExecutionCoordinator  Execution coordinator instance. The coordinator is in charge of executing incoming
     *                                     commands. Some considerations must be made when picking a suitable execution coordinator
     *                                     for your platform. For example, an entirely asynchronous coordinator is not suitable
     *                                     when the parsers used in that particular platform are not thread safe. If you have
     *                                     commands that perform blocking operations, however, it might not be a good idea to
     *                                     use a synchronous execution coordinator. In most cases you will want to pick between
     *                                     {@link CommandExecutionCoordinator#simpleCoordinator()} and
     *                                     {@link AsynchronousCommandExecutionCoordinator}
     * @param commandSourceMapper          Function that maps {@link ServerCommandSource} to the command sender type
     * @param backwardsCommandSourceMapper Function that maps the command sender type to {@link ServerCommandSource}
     */
    public FabricServerCommandManager(
            final @NonNull Function<@NonNull CommandTree<C>, @NonNull CommandExecutionCoordinator<C>> commandExecutionCoordinator,
            final Function<ServerCommandSource, C> commandSourceMapper,
            final Function<C, ServerCommandSource> backwardsCommandSourceMapper
    ) {
        super(
                commandExecutionCoordinator,
                commandSourceMapper,
                backwardsCommandSourceMapper,
                new FabricCommandRegistrationHandler.Server<>(),
                () -> new ServerCommandSource(
                        CommandOutput.DUMMY,
                        Vec3d.ZERO,
                        Vec2f.ZERO,
                        null,
                        4,
                        "",
                        LiteralText.EMPTY,
                        null,
                        null
                )
        );
    }

}
