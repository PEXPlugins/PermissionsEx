/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2020 zml [at] stellardrift [dot] ca and PermissionsEx contributors
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

import ca.stellardrift.permissionsex.BaseDirectoryScope;
import ca.stellardrift.permissionsex.ImplementationInterface;
import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.config.FilePermissionsExConfiguration;
import ca.stellardrift.permissionsex.logging.FormattedLogger;
import ca.stellardrift.permissionsex.profile.ProfileKt;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.util.MinecraftProfile;
import ca.stellardrift.permissionsex.commands.parse.CommandSpec;
import com.google.common.collect.ImmutableSet;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.impl.JDK14LoggerAdapter;
import org.yaml.snakeyaml.DumperOptions;

import javax.sql.DataSource;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static ca.stellardrift.permissionsex.hikariconfig.HikariConfig.createHikariDataSource;
import static java.util.Objects.requireNonNull;

/**
 * PermissionsEx plugin
 */
public class PermissionsExPlugin extends JavaPlugin implements Listener {
    private static final PermissibleInjector[] INJECTORS = new PermissibleInjector[] {
            new PermissibleInjector.ClassPresencePermissibleInjector("net.glowstone.entity.GlowHumanEntity", "permissions", true),
            new PermissibleInjector.ClassPresencePermissibleInjector("org.getspout.server.entity.SpoutHumanEntity", "permissions", true),
            new PermissibleInjector.ClassNameRegexPermissibleInjector("org.getspout.spout.player.SpoutCraftPlayer", "perm", false, "org\\.getspout\\.spout\\.player\\.SpoutCraftPlayer"),
            new PermissibleInjector.ClassPresencePermissibleInjector(CraftBukkitInterface.getCBClassName("entity.CraftHumanEntity"), "perm", true),
    };

    private PermissionsEx<BukkitConfiguration> manager;
    private FormattedLogger logger;
    // Injections into superperms
    private PermissionList permsList;
    // Permissions subscriptions handling
    private PEXPermissionSubscriptionMap subscriptionHandler;
    // Location of plugin configuration data
    private Path dataPath;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Because of Bukkit's special logging fun, we have to get an slf4j wrapper using specifically the logger that Bukkit provides us...
     *
     * @return Our wrapper of Bukkit's logger
     */
    private FormattedLogger createLogger() {
        try {
            Constructor<JDK14LoggerAdapter> adapter = JDK14LoggerAdapter.class.getDeclaredConstructor(java.util.logging.Logger.class);
            adapter.setAccessible(true);
            return FormattedLogger.forLogger(adapter.newInstance(getLogger()), true);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public void onEnable() {
        this.dataPath = getDataFolder().toPath();
        logger = createLogger();
        ConfigurationLoader<ConfigurationNode> configLoader = YAMLConfigurationLoader.builder()
                .setFile(new File(getDataFolder(), "config.yml"))
                .setFlowStyle(DumperOptions.FlowStyle.BLOCK)
                .build();


        try {
            BukkitImplementationInterface impl = new BukkitImplementationInterface();
            getDataFolder().mkdirs();
            this.manager = new PermissionsEx<>(FilePermissionsExConfiguration.fromLoader(configLoader, BukkitConfiguration.class), impl);
            impl.registerCommandsNow();
        /*} catch (PEBKACException e) {
            logger.warn(e.getTranslatableMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;*/
        } catch (Exception e) {
            logger.error(Messages.ERROR_ON_ENABLE.toComponent(getDescription().getName()), e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        manager.registerContextDefinitions(WorldContextDefinition.INSTANCE,
                DimensionContextDefinition.INSTANCE,
                RemoteIpContextDefinition.INSTANCE,
                LocalIpContextDefinition.INSTANCE,
                LocalHostContextDefinition.INSTANCE,
                LocalPortContextDefiniiton.INSTANCE);

        manager.getSubjects(PermissionsEx.SUBJECTS_USER).setTypeInfo(new UserSubjectTypeDescription(PermissionsEx.SUBJECTS_USER, this));
        getServer().getPluginManager().registerEvents(this, this);
        subscriptionHandler = PEXPermissionSubscriptionMap.inject(this, this.getServer().getPluginManager());
        permsList = PermissionList.inject(this);
        injectAllPermissibles();
        PEXPluginIntegrations.detectWorldGuard(this);
        PEXPluginIntegrations.detectVault(this);
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.close();
            manager = null;
        }
        if (subscriptionHandler != null) {
            subscriptionHandler.uninject();
            subscriptionHandler = null;
        }
        if (permsList != null) {
            permsList.uninject();
        }
        uninjectAllPermissibles();
        this.executorService.shutdown();
        try {
            this.executorService.awaitTermination(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error(Messages.ERROR_DISABLE_TASK_TIMEOUT.toComponent());
            this.executorService.shutdownNow();
        }
    }

    @EventHandler
    private void onPlayerPreLogin(final AsyncPlayerPreLoginEvent event) {
        getUserSubjects().get(event.getUniqueId().toString()).exceptionally(e -> {
            logger.warn(Messages.ERROR_LOAD_PRELOGIN.toComponent(event.getName(), event.getUniqueId().toString(), e.getMessage()), e);
            return null;
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerLogin(final PlayerLoginEvent event) {
        final String identifier = event.getPlayer().getUniqueId().toString();

        // Spigot doesn't seem to store virtual host names, so we have to do it ourselves.
        // Hostnames are provided as <host>:<port>, and we don't need the port.
        getUserSubjects().transientData().update(identifier, subj ->
                subj.setOption(PermissionsEx.GLOBAL_CONTEXT, "hostname", StringUtils.substringBeforeLast(event.getHostname(), ":")));
        getUserSubjects().isRegistered(identifier).thenAccept(registered -> {
            if (registered) {
                getUserSubjects().persistentData().update(identifier, input -> {
                    if (!event.getPlayer().getName().equals(input.getOptions(PermissionsEx.GLOBAL_CONTEXT).get("name"))) {
                        return input.setOption(PermissionsEx.GLOBAL_CONTEXT, "name", event.getPlayer().getName());
                    } else {
                        return input;
                    }
                });
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
    private void onPlayerLoginDeny(final PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            uninjectPermissible(event.getPlayer());
            getUserSubjects().uncache(event.getPlayer().getUniqueId().toString());
        }
    }


    @EventHandler(priority = EventPriority.MONITOR) // Happen last
    private void onPlayerQuit(PlayerQuitEvent event) {
        uninjectPermissible(event.getPlayer());
        getManager().getCallbackController().clearOwnedBy(event.getPlayer().getUniqueId());
        getUserSubjects().uncache(event.getPlayer().getUniqueId().toString());
    }

    PermissionList getPermissionList() {
        return permsList;
    }

    /**
     * Access the PEX engine
     *
     * @return The engine
     */
    public PermissionsEx<BukkitConfiguration> getManager() {
        return this.manager;
    }

    /**
     * Access user subjects
     *
     * @return The user subject collection
     */
    public SubjectType getUserSubjects() {
        return getManager().getSubjects(PermissionsEx.SUBJECTS_USER);
    }

    /**
     * Access group subjects
     *
     * @return The group subject collection
     */
    public SubjectType getGroupSubjects() {
        return getManager().getSubjects(PermissionsEx.SUBJECTS_GROUP);
    }

    private void injectPermissible(Player player) {
        try {
            PEXPermissible permissible = new PEXPermissible(player, this);

            boolean success = false, found = false;
            for (PermissibleInjector injector : INJECTORS) {
                if (injector.isApplicable(player)) {
                    found = true;
                    Permissible oldPerm = injector.inject(player, permissible);
                    if (oldPerm != null) {
                        permissible.setPreviousPermissible(oldPerm);
                        success = true;
                        break;
                    }
                }
            }

            if (!found) {
                logger.warn(Messages.SUPERPERMS_INJECT_NO_INJECTOR.toComponent());
            } else if (!success) {
                logger.warn(Messages.SUPERPERMS_INJECT_ERROR_GENERIC.toComponent(player.getName()));
            }

            permissible.recalculatePermissions();

            if (success && getManager().hasDebugMode()) {
                logger.info(Messages.SUPERPERMS_INJECT_SUCCESS.toComponent());
            }
        } catch (Throwable e) {
            logger.error(Messages.SUPERPERMS_INJECT_ERROR_GENERIC.toComponent(player.getName()), e);
        }
    }

    private void injectAllPermissibles() {
        getServer().getOnlinePlayers().forEach(this::injectPermissible);
    }

    private void uninjectPermissible(Player player) {
        try {
            boolean success = false;
            for (PermissibleInjector injector : INJECTORS) {
                if (injector.isApplicable(player)) {
                    Permissible pexPerm = injector.getPermissible(player);
                    if (pexPerm instanceof PEXPermissible) {
                        if (injector.inject(player, ((PEXPermissible) pexPerm).getPreviousPermissible()) != null) {
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
                logger.warn(Messages.SUPERPERMS_UNINJECT_NO_INJECTOR.toComponent(player.getName()));
            } else if (getManager() != null && getManager().hasDebugMode()) {
                logger.info(Messages.SUPERPERMS_UNINJECT_SUCCESS.toComponent(player.getName()));
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void uninjectAllPermissibles() {
        getServer().getOnlinePlayers().forEach(this::uninjectPermissible);
    }

    private class BukkitImplementationInterface implements ImplementationInterface {
        private Queue<Supplier<Set<CommandSpec>>> stagedCommands = new ConcurrentLinkedQueue<>();
        @Override
        public Path getBaseDirectory(BaseDirectoryScope scope) {
            switch (scope) {
                case CONFIG:
                    return dataPath;
                case JAR:
                    return Bukkit.getUpdateFolderFile().toPath();
                case SERVER:
                    return dataPath.toAbsolutePath().getParent().getParent();
                case WORLDS:
                    return Bukkit.getWorldContainer().toPath();
                default:
                    throw new IllegalArgumentException("Unknown directory scope" + scope);
            }
        }

        @Override
        public Logger getLogger() {
            return logger;
        }

        @Override
        public DataSource getDataSourceForURL(String url) throws SQLException {
            return createHikariDataSource(url, this.getBaseDirectory());
        }

        /**
         * Get an executor to run tasks asynchronously on.
         *
         * @return The async executor
         */
        @Override
        public Executor getAsyncExecutor() {
            return executorService;
        }

        @Override
        public void registerCommands(Supplier<Set<CommandSpec>> commands) {
            stagedCommands.add(commands);
            registerCommandsNow();
        }

        boolean registerCommandsNow() {
            if (manager == null) {
                return false;
            }
            Supplier<Set<CommandSpec>> supply;
            while ((supply = stagedCommands.poll()) != null) {
                registerCommandsNow(supply);
            }

            return true;
        }

        void registerCommandsNow(Supplier<Set<CommandSpec>> commandSupplier) {
            requireNonNull(manager, "Manager must be initialized to register commands!");
            for (CommandSpec command : commandSupplier.get()) {
                PluginCommand cmd = getCommand(command.getAliases().get(0));
                if (cmd != null) {
                    PEXBukkitCommand bukkitCommand = new PEXBukkitCommand(command, PermissionsExPlugin.this);
                    cmd.setExecutor(bukkitCommand);
                    cmd.setTabCompleter(bukkitCommand);
                }
            }
        }

        @Override
        public Set<CommandSpec> getImplementationCommands() {
            return ImmutableSet.of();
        }

        @Override
        public String getVersion() {
            return getDescription().getVersion();
        }

        @Override
        public CompletableFuture<Integer> lookupMinecraftProfilesByName(Iterable<String> names, Function<MinecraftProfile, CompletableFuture<Void>> action) {
            return ProfileKt.lookupMinecraftProfilesByName(names, action::apply);
        }
    }
}
