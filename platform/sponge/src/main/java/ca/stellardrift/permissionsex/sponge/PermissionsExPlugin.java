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
package ca.stellardrift.permissionsex.sponge;

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.exception.PermissionsException;
import ca.stellardrift.permissionsex.impl.logging.WrappingFormattedLogger;
import ca.stellardrift.permissionsex.logging.FormattedLogger;
import ca.stellardrift.permissionsex.minecraft.BaseDirectoryScope;
import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx;
import ca.stellardrift.permissionsex.minecraft.command.CommandRegistrationContext;
import ca.stellardrift.permissionsex.minecraft.command.Formats;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.subject.SubjectTypeCollection;
import cloud.commandframework.arguments.standard.StringArgument;
import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.slf4j.Log4jLogger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.ProvideServiceEvent;
import org.spongepowered.api.event.lifecycle.RefreshGameEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.GameProfileCache;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.scheduler.TaskExecutorService;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.sql.SqlManager;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.jvm.Plugin;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

import static org.spongepowered.configurate.util.UnmodifiableCollections.immutableMapEntry;

/**
 * The Sponge entry point for PermissionsEx.
 */
@Plugin(ProjectData.ARTIFACT_ID)
public class PermissionsExPlugin {

    // Sponge integration
    private final PluginContainer container;
    private final FormattedLogger logger;
    private final Game game;
    private final SqlManager sql;
    private final Path configDir;
    private final TaskExecutorService scheduler;
    private final Path configFile;

    // Manager state
    private @Nullable MinecraftPermissionsEx<?> manager;
    private @Nullable PermissionsExService service;
    private final SubjectType<String> systemSubjects;
    private final SubjectType<String> roleTemplates;
    private final SubjectType<String> commandBlocks;

    @Inject
    PermissionsExPlugin(
        final PluginContainer container,
        final Logger logger,
        final Game game,
        final SqlManager sql,
        final @ConfigDir(sharedRoot = false) Path configDir
    ) {
        this.container = container;
        this.logger = WrappingFormattedLogger.of(new Log4jLogger((ExtendedLogger) logger, logger.getName()), true);
        this.game = game;
        this.sql = sql;
        this.configDir = configDir;
        this.scheduler = game.getAsyncScheduler().createExecutor(container);
        this.configFile = configDir.resolve(container.getMetadata().getId() + ".conf");

        // Subject types defined by Sponge
        this.systemSubjects = SubjectType.stringIdentBuilder(PermissionService.SUBJECTS_SYSTEM)
            .fixedEntries(
                immutableMapEntry("console", this.game::getSystemSubject),
                immutableMapEntry("Recon", () -> null)
            )
            .undefinedValues($ -> true)
            .build();

        this.roleTemplates = SubjectType.stringIdentBuilder(PermissionService.SUBJECTS_ROLE_TEMPLATE)
            .build();

        this.commandBlocks = SubjectType.stringIdentBuilder(PermissionService.SUBJECTS_COMMAND_BLOCK)
            .undefinedValues($ -> true)
            .build();
    }

    /**
     * Get the permissions engine active for this proxy.
     *
     * @return the permissions engine
     * @since 2.0.0
     */
    public PermissionsEngine engine() {
        return this.manager().engine();
    }

    MinecraftPermissionsEx<?> manager() {
        final @Nullable MinecraftPermissionsEx<?> manager = this.manager;
        if (manager == null) {
            throw new IllegalStateException("PermissionsEx is not currently initialized! Check for an earlier error, " +
                "or whether permissions may be checked too early.");
        }
        return manager;
    }

    /**
     * Get a collection of users stored on this server.
     *
     * <p>Not all elements of this collection are cached, so some requests may involve network lookups.</p>
     *
     * @return the user subjects
     * @since 2.0.0
     */
    public SubjectTypeCollection<UUID> users() {
        return this.manager().users();
    }

    /**
     * Get a collection of groups stored on this server.
     *
     * <p>Elements of this collection will be cached on load.</p>
     *
     * @return the group subjects
     * @since 2.0.0
     */
    public SubjectTypeCollection<String> groups() {
        return this.manager().groups();
    }

    /**
     * Get system subjects for non-player command executors.
     *
     * <p>The subjects defined by default are {@code console} for the server console, and {@code Recon} for
     * RCON connections.</p>
     *
     * @return the system subject type
     * @since 2.0.0
     */
    public SubjectTypeCollection<String> system() {
        return this.engine().subjects(this.systemSubjects);
    }

    /**
     * Get subjects representing declared role templates.
     *
     * <p>Role templates can be declared by registering permissions descriptions.</p>
     *
     * @return role template subjects
     * @since 2.0.0
     */
    public SubjectTypeCollection<String> roleTemplates() {
        return this.engine().subjects(this.roleTemplates);
    }

    /**
     * Get subjects referring to specific named command blocks.
     *
     * <p>The default command block has the name {@code @}.</p>
     *
     * @return command block subjects.
     * @since 2.0.0
     */
    public SubjectTypeCollection<String> commandBlocks() {
        return this.engine().subjects(this.commandBlocks);
    }

    // Internals -- event listeners, accessors, etc //

    PluginContainer container() {
        return this.container;
    }

    PermissionsExService service() {
        final @Nullable PermissionsExService service = this.service;
        if (service == null) {
            throw new IllegalStateException("Tried to access the PEX service while the server was not running");
        }
        return service;
    }

    TaskExecutorService scheduler() {
        return this.scheduler;
    }

    Game game() {
        return this.game;
    }

    @Listener
    public void onPreInit(final ConstructPluginEvent event) {
        this.logger.info(Messages.PLUGIN_INIT_BEGIN.tr(ProjectData.NAME, ProjectData.VERSION));
        try {
            this.convertFromBukkit();
            this.convertFromLegacySpongeName();
            Files.createDirectories(configDir);
            this.manager = MinecraftPermissionsEx.builder()
                .configuration(this.configFile)
                .baseDirectory(this.configDir)
                .logger(this.logger)
                .asyncExecutor(this.scheduler)
                .baseDirectory(BaseDirectoryScope.JAR, game.getGameDirectory().resolve("mods"))
                .baseDirectory(BaseDirectoryScope.SERVER, game.getGameDirectory())
                // .baseDirectory(BaseDirectoryScope.WORLDS, TODO("level container"))
                .databaseProvider(url -> sql.getDataSource(container, url))
                .playerProvider(id -> game.getServer().getPlayer(id).orElse(null))
                .cachedUuidResolver(name -> {
                    final Optional<ServerPlayer> player = game.getServer().getPlayer(name);
                    if (player.isPresent()) {
                        return player.get().getUniqueId();
                    } else {
                        final GameProfileCache res = game.getServer().getGameProfileManager().getCache();
                        return res.streamOfMatches(name)
                            .filter(it -> it.getName().isPresent() && it.getName().get().equalsIgnoreCase(name))
                            .findFirst()
                            .map(Identifiable::getUniqueId)
                            .orElse(null);
                    }
                })
                .commandContributor(this::registerFakeOpCommand)
                .messageFormatter(SpongeMessageFormatter::new)
                .create();
        } catch (final Exception ex) {
            throw new RuntimeException(new PermissionsException(Messages.PLUGIN_INIT_ERROR_GENERAL.tr(ProjectData.NAME), ex));
        }

        this.engine().registerSubjectTypes(
            this.systemSubjects,
            this.roleTemplates,
            this.commandBlocks
        );
    }

    @Listener
    public void registerPermissionService(final ProvideServiceEvent.EngineScoped<PermissionService> event) {
        event.suggest(() -> this.service = new PermissionsExService((Server) event.getEngine(), this));
    }

    private void registerFakeOpCommand(final CommandRegistrationContext reg) {
        doRegister(reg, "op", "minecraft.command.op");
        doRegister(reg, "deop", "minecraft.command.deop");
    }

    private void doRegister(final CommandRegistrationContext reg, final String name, final String permission) {
        reg.register(reg.absoluteBuilder(name)
            .argument(StringArgument.of("user"))
            .permission(permission)
            // .meta(SpongeApi7MetaKeys.RICH_DESCRIPTION, Messages.COMMANDS_FAKE_OP_DESCRIPTION.tr().toSponge())
            .handler(ctx ->
                ctx.getSender().error(Messages.COMMANDS_FAKE_OP_ERROR.tr()))
        );
    }

    @Listener
    public void cacheUserAsync(final ServerSideConnectionEvent.Auth event) {
        final GameProfile profile = event.getProfile();
        try {
            this.users().get(profile.getUniqueId()).exceptionally(err -> {
                logger.warn(Messages.EVENT_CLIENT_AUTH_ERROR.tr(
                    profile.getName(),
                    profile.getUniqueId(),
                    Formats.message(err)
                ), err);
                return null;
            });
        } catch (final Exception ex) {
            logger.warn(Messages.EVENT_CLIENT_AUTH_ERROR.tr(
                profile.getName(),
                profile.getUniqueId(),
                Formats.message(ex)
            ), ex);
        }
    }

    @Listener
    public void disable(final StoppingEngineEvent<Server> event) {
        // TODO: This needs to move to a shutdown hook
        logger.debug(Messages.PLUGIN_SHUTDOWN_BEGIN.tr(ProjectData.NAME));
        this.service = null;
        final @Nullable MinecraftPermissionsEx<?> manager = this.manager;
        if (manager != null) {
            manager.close();
            this.manager = null;
        }
    }

    @Listener
    public void onReload(final RefreshGameEvent event) {
        final @Nullable MinecraftPermissionsEx<?> manager = this.manager;
        if (manager != null) {
            manager.engine().reload();
        }
    }

    @Listener
    public void onPlayerJoin(final ServerSideConnectionEvent.Join event) {
        final UUID identifier = event.getPlayer().getUniqueId();
        final SubjectTypeCollection<UUID> cache = this.users();
        cache.get(identifier).thenAccept(subj -> {
            // Update name option
            subj.data().isRegistered().thenAccept(isReg -> {
                if (isReg) {
                    subj.data().update(
                        PermissionsEngine.GLOBAL_CONTEXT,
                        data -> data.withOption("name", event.getPlayer().getName())
                    );
                }
            });

            // Add listener to re-send command tree on a permission update
            subj.registerListener(newSubj -> {
                final @Nullable Object associated = newSubj.associatedObject();
                if (associated instanceof ServerPlayer) {
                    ((ServerPlayer) associated).getWorld().getEngine().getScheduler().submit(Task.builder()
                        .plugin(container)
                        .execute(() -> game.getCommandManager().updateCommandTreeForPlayer((ServerPlayer) associated))
                        .build());
                }
            });
        });
    }

    @Listener
    public void onPlayerQuit(final ServerSideConnectionEvent.Disconnect event) {
        final @Nullable MinecraftPermissionsEx<?> pex = this.manager;
        if (pex != null) {
            pex.callbackController().clearOwnedBy(event.getPlayer().getUniqueId());
        }

        final @Nullable PermissionsExService service = this.service;
        if (service != null) {
            service.getUserSubjects().suggestUnload(event.getPlayer().getIdentifier());
        }
    }

    private void convertFromBukkit() throws IOException {
        final Path bukkitConfigPath = Paths.get("plugins/PermissionsEx");
        if (Files.isDirectory(bukkitConfigPath) && isEmptyDirectory(configDir)) {
            logger.info(Messages.MIGRATION_BUKKIT_BEGIN.tr());
            Files.move(bukkitConfigPath, configDir, StandardCopyOption.REPLACE_EXISTING);
        }
        final Path bukkitConfigFile = configDir.resolve("config.yml");
        if (Files.exists(bukkitConfigFile)) {
            final ConfigurationNode bukkitConfig = YamlConfigurationLoader.builder()
                .path(bukkitConfigFile)
                .build()
                .load();
            HoconConfigurationLoader.builder()
                .path(configFile)
                .build()
                .save(bukkitConfig);
            Files.move(bukkitConfigFile, configDir.resolve("config.yml.bukkit"));
        }
    }

    private void convertFromLegacySpongeName() throws IOException {
        final Path oldPath = configDir.resolveSibling("ninja.leaping.permissionsex"); // Old plugin ID
        if (Files.exists(oldPath) && isEmptyDirectory(configDir)) {
            Files.move(oldPath, configDir, StandardCopyOption.REPLACE_EXISTING);
            Files.move(
                configDir.resolve("ninja.leaping.permissionsex.conf"),
                configDir.resolve("${ProjectData.ARTIFACT_ID}.conf")
            );
            logger.info(Messages.MIGRATION_LEGACY_SPONGE_SUCCESS.tr(configDir.toString()));
        }
    }

    private static boolean isEmptyDirectory(final Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return true;
        }
        try (final DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }

    /*override fun lookupMinecraftProfilesByName(
        names: Iterable<String>,
        action: Function<MinecraftProfile, CompletableFuture<Void>>
    ): CompletableFuture<Int> {
        return game.server.gameProfileManager.getAllByName(names, true)
            .thenComposeAsync { profiles ->
                CompletableFuture.allOf(*profiles.filterValues { it.isPresent }
                    .map { action.apply(SpongeMinecraftProfile(it.value.get())) }
                    .toTypedArray())
                    .thenApply { profiles.size }
            }
    }*/

}
