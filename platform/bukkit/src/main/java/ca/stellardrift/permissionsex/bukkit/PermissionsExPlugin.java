/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2021 zml [at] stellardrift [dot] ca and PermissionsEx contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ca.stellardrift.permissionsex.bukkit;

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.impl.BaseDirectoryScope;
import ca.stellardrift.permissionsex.impl.ImplementationInterface;
import ca.stellardrift.permissionsex.impl.config.FilePermissionsExConfiguration;
import ca.stellardrift.permissionsex.impl.logging.WrappingFormattedLogger;
import ca.stellardrift.permissionsex.logging.FormattedLogger;
import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx;
import ca.stellardrift.permissionsex.minecraft.command.CommandException;
import ca.stellardrift.permissionsex.minecraft.command.CommandRegistrationContext;
import ca.stellardrift.permissionsex.minecraft.command.Commander;
import ca.stellardrift.permissionsex.minecraft.command.Permission;
import ca.stellardrift.permissionsex.sql.hikari.Hikari;
import ca.stellardrift.permissionsex.subject.SubjectTypeCollection;
import cloud.commandframework.CommandManager;
import cloud.commandframework.CommandTree;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.bukkit.arguments.selector.SinglePlayerSelector;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.bukkit.parsers.selector.SinglePlayerSelectorArgument;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import cloud.commandframework.permission.CommandPermission;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.impl.JDK14LoggerAdapter;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class PermissionsExPlugin extends JavaPlugin implements Listener {

    // -- PEX core -- //
    private @MonotonicNonNull FormattedLogger logger;
    private @MonotonicNonNull Path dataPath;
    private @MonotonicNonNull BukkitAudiences adventure;
    private @Nullable MinecraftPermissionsEx<BukkitConfiguration> manager;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // -- Bukkit injections -- //
    private @Nullable PermissionList permissionList;
    private @Nullable PEXPermissionSubscriptionMap subscriptionHandler;

    /**
     * Get the permissions engine active for this proxy.
     *
     * @return the permissions engine
     * @since 2.0.0
     */
    public PermissionsEngine engine() {
        return this.manager().engine();
    }

    MinecraftPermissionsEx<BukkitConfiguration> manager() {
        final @Nullable MinecraftPermissionsEx<BukkitConfiguration> manager = this.manager;
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

    // -- Internals -- //

    private FormattedLogger makeLogger() {
        try {
            final Constructor<JDK14LoggerAdapter> adapter =
                JDK14LoggerAdapter.class.getDeclaredConstructor(java.util.logging.Logger.class);
            adapter.setAccessible(true);
            return WrappingFormattedLogger.of(adapter.newInstance(getLogger()), true);
        } catch (final NoSuchMethodException
            | InstantiationException
            | IllegalAccessException
            | InvocationTargetException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private CommandManager<Commander> createCommandManager(
        Function<CommandTree<Commander>, CommandExecutionCoordinator<Commander>> execCoord
    ) throws RuntimeException {
        final PaperCommandManager<Commander> mgr;
        try {
            mgr = new PaperCommandManager<Commander>(
                this,
                execCoord,
                sender -> new BukkitCommander(this, sender),
                commander -> ((BukkitCommander) commander).source()
            ) {
                @Override
                public boolean hasPermission(final Commander sender, final CommandPermission permission) {
                    if (permission instanceof Permission) {
                        return sender.hasPermission((Permission) permission);
                    }
                    return super.hasPermission(sender, permission);
                }
            };
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }

        if (mgr.queryCapability(CloudBukkitCapabilities.BRIGADIER)) {
            mgr.registerBrigadier();
        }

        if (mgr.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            mgr.registerAsynchronousCompletions();
        }
        return mgr;
    }

    /**
     * Register platform-specific commands.
     *
     * @param reg the registration context
     */
    private void registerBukkitCommands(final CommandRegistrationContext reg) {
        reg.register(builder -> {
            final Permission perm = Permission.pex("bukkit.resendtree");
            final CommandArgument<Commander, SinglePlayerSelector> targetArg = SinglePlayerSelectorArgument.of("target");
            return builder
                .literal("resendtree")
                .argument(targetArg)
                .permission(perm)
                .meta(CommandMeta.DESCRIPTION, "Force resend a player's command tree")
                .meta(CommandMeta.LONG_DESCRIPTION, "Forcibly resend a player's Brigadier command tree\n" +
                    "This can help resolve issues where the completions on a client\n" +
                    "do not match what the server thinks a player has permission for.")
                .handler(ctx -> {
                    final SinglePlayerSelector target = ctx.get(targetArg);
                    if (target.getPlayer() == null) {
                        throw new CommandException(Messages.COMMAND_TREE_NO_TARGET.tr(target.getSelector()));
                    }
                    try {
                        target.getPlayer().updateCommands();
                        ctx.getSender().sendMessage(Messages.COMMAND_TREE_REFRESHED.tr(target.getPlayer().getName()));
                    } catch (final NoSuchMethodError ex) {
                        ctx.getSender().sendMessage(Messages.COMMAND_TREE_UNSUPPORTED.tr(Bukkit.getVersion()));
                    }
                });
        }, "permissionsex");

    }

    @Override
    public void onEnable() {
        this.dataPath = getDataFolder().toPath();
        this.logger = makeLogger();
        this.adventure = BukkitAudiences.create(this);
        final YamlConfigurationLoader configLoader = YamlConfigurationLoader.builder()
            .path(this.dataPath.resolve("config.yml"))
            .nodeStyle(NodeStyle.BLOCK)
            .defaultOptions(FilePermissionsExConfiguration::decorateOptions)
            .build();

        try {
            final ImplementationInterface impl = new BukkitImplementationInterface();
            Files.createDirectories(this.dataPath);
            this.manager = MinecraftPermissionsEx.builder(
                FilePermissionsExConfiguration.fromLoader(
                    configLoader,
                    BukkitConfiguration.class
                ))
                .implementationInterface(impl)
                .opProvider(it -> getServer().getOperators().contains(this.getServer().getOfflinePlayer(it)))
                .cachedUuidResolver(it -> getServer().getOfflinePlayer(it).getUniqueId()) // TODO: is this correct?
                .playerProvider(getServer()::getPlayer)
                .commands(this::createCommandManager)
                .messageFormatter(BukkitMessageFormatter::new)
                .commandContributor(this::registerBukkitCommands)
                .build();
            /*} catch (PEBKACException e) {
            logger.warn(e.getTranslatableMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;*/
        } catch (final Exception ex) {
            logger.error(Messages.ERROR_ON_ENABLE.tr(getDescription().getName()), ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        engine().registerContextDefinitions(
            BukkitContexts.world(),
            BukkitContexts.dimension(),
            BukkitContexts.remoteIp(),
            BukkitContexts.remoteIp(),
            BukkitContexts.localHost(),
            BukkitContexts.localPort()
        );

        getServer().getPluginManager().registerEvents(this, this);
        subscriptionHandler = PEXPermissionSubscriptionMap.inject(this, getServer().getPluginManager());
        permissionList = PermissionList.inject(this);
        injectAllPermissibles();
        PluginIntegrations.detectWorldGuard(this);
        PluginIntegrations.detectVault(this);
    }

    @Override
    public void onDisable() {
        if (this.manager != null) {
            this.manager.close();
            this.manager = null;
        }
        if (this.subscriptionHandler != null) {
            this.subscriptionHandler.uninject();
            this.subscriptionHandler = null;
        }
        if (this.permissionList != null) {
            this.permissionList.uninject();
            this.permissionList = null;
        }
        uninjectAllPermissibles();

        this.executorService.shutdown();
        boolean successful;
        try {
            successful = this.executorService.awaitTermination(20, TimeUnit.SECONDS);
        } catch (final InterruptedException ex) {
            successful = false;
        }

        if (!successful) {
            logger.error(Messages.ERROR_DISABLE_TASK_TIMEOUT.tr());
            executorService.shutdownNow();
        }

        this.adventure.close();
        this.adventure = null;
    }

    @EventHandler
    void onPlayerPreLogin(final AsyncPlayerPreLoginEvent event) {
        users().get(event.getUniqueId())
            .exceptionally(e -> {
                logger.warn(Messages.ERROR_LOAD_PRELOGIN.tr(event.getName(), event.getUniqueId().toString(), e.getMessage()), e);
                return null;
            });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onPlayerLogin(final PlayerLoginEvent event) {
        final UUID identifier = event.getPlayer().getUniqueId();

        // Spigot doesn't seem to store virtual host names, so we have to do it ourselves.
        // Hostnames are provided as <host>:<port>, and we don't need the port.
        this.users().transientData().update(identifier, it -> it.withSegment(
            PermissionsEngine.GLOBAL_CONTEXT,
            segment -> segment.withOption("hostname", StringUtils.substringBeforeLast(event.getHostname(), ":"))
        ));

        this.users().isRegistered(identifier)
            .thenAccept(registered -> {
                if (registered) {
                    users().persistentData().update(
                        identifier,
                        input -> input.withSegment(PermissionsEngine.GLOBAL_CONTEXT, it -> {
                            if (!event.getPlayer().getName().equals(it.options().get("name"))) {
                                return it.withOption("name", event.getPlayer().getName());
                            } else {
                                return it;
                            }
                        }));
                }
            });
        injectPermissible(event.getPlayer());
    }

    /**
     * If the login event is cancelled, we want to make sure we properly uninject the permissible.
     * Because this listener is on priority MONITOR, any plugin that might cancel the event has had a chance to have its say.
     *
     * @param event The login event that may be cancelled.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    void onPlayerLoginDeny(final PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            uninjectPermissible(event.getPlayer());
            this.users().uncache(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR) // Happen last
    void onPlayerQuit(final PlayerQuitEvent event) {
        uninjectPermissible(event.getPlayer());
        final @Nullable MinecraftPermissionsEx<?> manager = this.manager;
        if (manager != null) {
            manager.callbackController().clearOwnedBy(event.getPlayer().getUniqueId());
            manager.users().uncache(event.getPlayer().getUniqueId());
        }
    }

    private void injectPermissible(final Player player) {
        try {
            final PEXPermissible permissible = new PEXPermissible(player, this);
            boolean success = false;
            boolean found = false;
            for (PermissibleInjector injector : PermissibleInjector.INJECTORS) {
                if (injector.isApplicable(player)) {
                    found = true;
                    final @Nullable Permissible oldPerm = injector.inject(player, permissible);
                    if (oldPerm != null) {
                        permissible.previousPermissible = oldPerm;
                        success = true;
                        break;
                    }
                }
            }

            if (!found) {
                this.logger.warn(Messages.SUPERPERMS_INJECT_NO_INJECTOR.tr());
            } else if (!success) {
                this.logger.warn(Messages.SUPERPERMS_INJECT_ERROR_GENERIC.tr(player.getName()));
            }
            permissible.recalculatePermissions();
            if (success && engine().debugMode()) {
                this.logger.info(Messages.SUPERPERMS_INJECT_SUCCESS.tr());
            }
        } catch (final Throwable ex) {
            this.logger.error(Messages.SUPERPERMS_INJECT_ERROR_GENERIC.tr(player.getName()), ex);
        }
    }

    private void injectAllPermissibles() {
        getServer().getOnlinePlayers().forEach(this::injectPermissible);
    }

    private void uninjectPermissible(final Player player) {
        try {
            boolean success = false;
            for (final PermissibleInjector injector : PermissibleInjector.INJECTORS) {
                if (injector.isApplicable(player)) {
                    final Permissible pexPerm = injector.getPermissible(player);
                    if (pexPerm instanceof PEXPermissible) {
                        if (injector.inject(player, ((PEXPermissible) pexPerm).previousPermissible) != null) {
                            success = true;
                            break;
                        }
                    } else {
                        success = true;
                        break;
                    }
                }
            }
            if (!success) {
                this.logger.warn(Messages.SUPERPERMS_UNINJECT_NO_INJECTOR.tr(player.getName()));
            } else if (this.manager != null && this.engine().debugMode()) {
                this.logger.info(Messages.SUPERPERMS_UNINJECT_SUCCESS.tr(player.getName()));
            }
        } catch (final Throwable ex) {
            this.logger.error(Messages.SUPERPERMS_UNINJECT_ERROR.tr(player.getName()), ex);
        }
    }

    private void uninjectAllPermissibles() {
        getServer().getOnlinePlayers().forEach(this::uninjectPermissible);
    }

    @Nullable PermissionList permissionList() {
        return this.permissionList;
    }

    BukkitAudiences adventure() {
        return this.adventure;
    }

    FormattedLogger logger() {
        return this.manager().engine().logger();
    }

    final class BukkitImplementationInterface  implements ImplementationInterface {

        @Override
        public Path baseDirectory(final BaseDirectoryScope scope) {
            switch (scope) {
                case CONFIG: return PermissionsExPlugin.this.dataPath;
                case JAR: return Bukkit.getUpdateFolderFile().toPath();
                case SERVER: return PermissionsExPlugin.this.dataPath.toAbsolutePath().getParent().getParent();
                case WORLDS: return Bukkit.getWorldContainer().toPath();
                default: throw new IllegalArgumentException("Unknown directory scope " + scope);
            }
        }

        @Override
        public DataSource dataSourceForUrl(final String url) throws SQLException {
            return Hikari.createDataSource(url, this.baseDirectory());
        }

        /**
         * Get an executor to run tasks asynchronously on.
         *
         * @return The async executor
         */
        @Override
        public Executor asyncExecutor() {
            return executorService;
        }

        @Override
        public String version() {
            return getDescription().getVersion();
        }

        @Override
        public Logger logger() {
            return PermissionsExPlugin.this.logger;
        }
    }

}
