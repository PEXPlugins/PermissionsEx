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
package ca.stellardrift.permissionsex.fabric.impl;

import ca.stellardrift.permissionsex.fabric.FabricContexts;
import ca.stellardrift.permissionsex.fabric.impl.bridge.PermissionCommandSourceBridge;
import ca.stellardrift.permissionsex.fabric.impl.commands.FabricServerCommandManager;
import ca.stellardrift.permissionsex.impl.logging.WrappingFormattedLogger;
import ca.stellardrift.permissionsex.impl.util.CachingValue;
import ca.stellardrift.permissionsex.logging.FormattedLogger;
import ca.stellardrift.permissionsex.minecraft.BaseDirectoryScope;
import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx;
import ca.stellardrift.permissionsex.minecraft.command.Commander;
import ca.stellardrift.permissionsex.sql.hikari.Hikari;
import cloud.commandframework.CommandManager;
import cloud.commandframework.CommandTree;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import com.mojang.authlib.GameProfile;
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static ca.stellardrift.permissionsex.context.ContextDefinitionProvider.GLOBAL_CONTEXT;

public final class FabricPermissionsExImpl implements ModInitializer {

    public static final FabricPermissionsExImpl INSTANCE = new FabricPermissionsExImpl();
    public static final String IDENTIFIER_RCON = "rcon";
    private static final String MOD_ID = "permissionsex";

    private final FormattedLogger logger = WrappingFormattedLogger.of(LoggerFactory.getLogger(MOD_ID), false);
    private @MonotonicNonNull ModContainer container;
    private @MonotonicNonNull Path dataDir;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // Tied to engine state
    private @Nullable MinecraftPermissionsEx<?> manager;
    private @Nullable MinecraftServer server;

    // Static helpers //

    public static Identifier id(final String path) {
        return new Identifier(MOD_ID, path);
    }

    public static <V> CachingValue<V> tickCachedValue(final MinecraftServer server, final long maxDelta, final Supplier<V> update) {
        return new CachingValue<>(
            server::getTicks,
            maxDelta,
            update
        );
    }

    public static <S> Predicate<S> commandPermissionCheck(final String permission, final Predicate<S> original) {
        return subject -> {
            if (subject instanceof PermissionCommandSourceBridge<?>) {
                return ((PermissionCommandSourceBridge<?>) subject).hasPermission(permission);
            } else {
                return original.test(subject);
            }
        };
    }

    private FabricPermissionsExImpl() {
    }

    // Engine operations //

    public boolean available() {
        return this.manager != null;
    }

    public @Nullable MinecraftServer server() {
        return this.server;
    }

    public MinecraftPermissionsEx<?> manager() {
        final @Nullable MinecraftPermissionsEx<?> manager = this.manager;
        if (manager == null) {
            throw new IllegalStateException("PermissionsEx is not currently initialized! Check for an earlier error, " +
                "or whether permissions may be checked too early.");
        }
        return manager;
    }

    public FormattedLogger logger() {
        return this.logger;
    }

    @Override
    public void onInitialize() {
        // Load all mixins in development
        if (Boolean.getBoolean("permissionsex.debug.mixinaudit")) {
            MixinEnvironment.getDefaultEnvironment().audit();
        }

        this.dataDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        this.container = FabricLoader.getInstance().getModContainer(MOD_ID)
            .orElseThrow(() -> new IllegalStateException("Mod container for PermissionsEx was not available in init!"));
        this.logger.prefix("[" + container.getMetadata().getName() + "] ");

        this.manager = this.createManager();
        registerEvents();
        this.logger.info(Messages.MOD_ENABLE_SUCCESS.tr(container.getMetadata().getVersion()));
    }

    private void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> this.server = server);
        ServerLifecycleEvents.SERVER_STOPPED.register($ -> this.server = null);
        ServerPlayConnectionEvents.INIT.register((handler, $) -> handlePlayerJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, $) -> {
            final @Nullable MinecraftPermissionsEx<?> manager = this.manager;
            if (manager == null) {
                return;
            }

            manager.callbackController().clearOwnedBy(handler.player.getUuidAsString());
            manager.users().uncache(handler.player.getUuid());
        });

        PermissionCheckEvent.EVENT.register((source, permission) -> {
            if (source instanceof PermissionCommandSourceBridge<?>) {
                final PermissionCommandSourceBridge<?> permSource = (PermissionCommandSourceBridge<?>) source;
                final int value = permSource.asCalculatedSubject().permission(permSource.activeContexts(), permission);

                if (value > 0) {
                    return TriState.TRUE;
                } else if (value < 0) {
                    return TriState.FALSE;
                }
            }
            return TriState.DEFAULT;
        });
    }

    private CommandManager<Commander> createCommandManager(final Function<CommandTree<Commander>, CommandExecutionCoordinator<Commander>> execCoord) {
        return new FabricServerCommandManager<>(
            execCoord,
            FabricCommander::new,
            commander -> ((FabricCommander) commander).source()
        );
    }

    private MinecraftPermissionsEx<?> createManager() {

        final MinecraftPermissionsEx<?> manager;
        try {
            Files.createDirectories(this.dataDir);
            manager = MinecraftPermissionsEx.builder()
                .configuration(this.dataDir.resolve("$MOD_ID.conf"))
                .asyncExecutor(this.executor)
                .logger(this.logger)
                .databaseProvider(url -> Hikari.createDataSource(url, this.dataDir))
                .baseDirectory(this.dataDir)
                .baseDirectory(BaseDirectoryScope.JAR, FabricLoader.getInstance().getGameDir().resolve("mods"))
                .baseDirectory(BaseDirectoryScope.SERVER, FabricLoader.getInstance().getGameDir())
                // .baseDirectory(BaseDirectoryScope.WORLDS, this.server?.getSavePath(WorldSavePath.ROOT)) // TODO: How to implement this
                .playerProvider(id -> {
                    final @Nullable MinecraftServer server = this.server;
                    return server == null ? null : server.getPlayerManager().getPlayer(id);
                }).cachedUuidResolver(name -> {
                    final @Nullable MinecraftServer server = this.server;
                    final @Nullable GameProfile profile = server == null ? null : server.getUserCache().findByName(name);
                    return profile == null ? null : profile.getId();
                }).opProvider(id -> {
                    final @Nullable MinecraftServer server = this.server;
                    if (server == null) return false;
                    final @Nullable GameProfile profile = server.getUserCache().getByUuid(id);
                    return profile != null && server.getPlayerManager().isOperator(profile);
                })
                .commands(this::createCommandManager)
                .messageFormatter(FabricMessageFormatter::new)
                .create();
        } catch (final Exception ex) {
            this.logger.error(Messages.MOD_ENABLE_ERROR.tr(), ex);
            System.exit(1);
            throw new IllegalStateException(ex);
            // TODO: throw another exception here?
        }

        manager.engine().registerContextDefinitions(
            FabricContexts.world(),
            FabricContexts.dimension(),
            FabricContexts.remoteIp(),
            FabricContexts.localIp(),
            FabricContexts.localHost(),
            FabricContexts.localPort(),
            FabricContexts.function());

        manager.engine().defaults().transientData().update(FabricSubjectTypes.SYSTEM, it ->
            it.withSegment(GLOBAL_CONTEXT, segment -> segment.withFallbackPermission(1))
        );

        manager.engine().registerSubjectTypes(
            FabricSubjectTypes.SYSTEM,
            FabricSubjectTypes.COMMAND_BLOCK,
            FabricSubjectTypes.FUNCTION
        );

        return manager;
    }

    public void shutdown() {
        final @Nullable MinecraftPermissionsEx<?> manager = this.manager;
        this.manager = null;
        if (manager != null) {
            manager.close();
        }

        if (!this.executor.isShutdown()) {
            this.executor.shutdown();
            boolean successful;
            try {
                successful = this.executor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (final InterruptedException ex) {
                successful = false;
            }

            if (!successful) {
                this.logger.error(Messages.MOD_ERROR_SHUTDOWN_TIMEOUT.tr());
                this.executor.shutdownNow();
            }
        }
    }

    private void handlePlayerJoin(final ServerPlayerEntity player) {
        this.manager().users().get(player.getUuid()).thenAccept(it -> {
            // Update name option
            it.data().isRegistered().thenAccept(isReg -> {
                if (isReg) {
                    it.data().update(GLOBAL_CONTEXT, segment -> segment.withOption("name", player.getName().asString()));
                }
            });

            // Add listener to re-send command tree on a permission update
            it.registerListener(newSubj -> {
                final @Nullable Object associated = newSubj.associatedObject();
                if (associated instanceof ServerPlayerEntity) {
                    final ServerPlayerEntity p2 = (ServerPlayerEntity) associated;
                    p2.server.execute(() -> p2.server.getPlayerManager().sendCommandTree(p2));
                }
            });
        });
    }

    public void logUnredirectedPermissionsCheck(final String method) {
        this.logger.warn(Messages.MOD_ERROR_UNREDIRECTED_CHECK.tr(method));
        this.logger.debug(Messages.MOD_ERROR_UNREDIRECTED_CHECK.tr(method), new Exception("call chain"));
    }

}
