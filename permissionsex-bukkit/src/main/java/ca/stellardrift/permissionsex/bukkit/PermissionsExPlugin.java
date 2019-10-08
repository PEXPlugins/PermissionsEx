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

package ca.stellardrift.permissionsex.bukkit;

import ca.stellardrift.permissionsex.ImplementationInterface;
import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.config.FilePermissionsExConfiguration;
import ca.stellardrift.permissionsex.logging.TranslatableLogger;
import ca.stellardrift.permissionsex.profile.ProfileKt;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.util.MinecraftProfile;
import ca.stellardrift.permissionsex.util.command.CommandSpec;
import com.google.common.collect.ImmutableSet;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.ServicePriority;
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
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Function;

import static ca.stellardrift.permissionsex.bukkit.BukkitTranslations.t;
import static ca.stellardrift.permissionsex.hikariconfig.HikariConfig.createHikariDataSource;

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

    private PermissionsEx manager;
    private TranslatableLogger logger;
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
    private TranslatableLogger createLogger() {
        try {
            Constructor<JDK14LoggerAdapter> adapter = JDK14LoggerAdapter.class.getDeclaredConstructor(java.util.logging.Logger.class);
            adapter.setAccessible(true);
            return TranslatableLogger.forLogger(adapter.newInstance(getLogger()));
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
            getDataFolder().mkdirs();
            this.manager = new PermissionsEx(FilePermissionsExConfiguration.fromLoader(configLoader), new BukkitImplementationInterface());
        /*} catch (PEBKACException e) {
            logger.warn(e.getTranslatableMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;*/
        } catch (Exception e) {
            logger.error(t("Error occurred while enabling %s", getDescription().getName()), e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        manager.registerContextDefinition(WorldContextDefinition.INSTANCE);
        manager.registerContextDefinition(DimensionContextDefinition.INSTANCE);

        manager.getSubjects(PermissionsEx.SUBJECTS_USER).setTypeInfo(new UserSubjectTypeDescription(PermissionsEx.SUBJECTS_USER, this));
        getServer().getPluginManager().registerEvents(this, this);
        subscriptionHandler = PEXPermissionSubscriptionMap.inject(this, this.getServer().getPluginManager());
        permsList = PermissionList.inject(this);
        injectAllPermissibles();
        if (getServer().getPluginManager().isPluginEnabled("Vault")) {
            final PEXVault vault = new PEXVault(this);
            getServer().getServicesManager().register(Permission.class, vault, this, ServicePriority.High); // Hook into vault
            getServer().getServicesManager().register(Chat.class, new PEXVaultChat(vault), this, ServicePriority.High);
            logger.info(t("Hooked into Vault for Permission and Chat interfaces"));
        }
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
            logger.error(t("Timeout while waiting for PEX tasks to finish!"));
            this.executorService.shutdownNow();
        }
    }

    @EventHandler
    private void onPlayerPreLogin(final AsyncPlayerPreLoginEvent event) {
        getUserSubjects().get(event.getUniqueId().toString()).exceptionally(e -> {
            logger.warn(t("Error while loading data for user %s/%s during prelogin: %s", event.getName(), event.getUniqueId().toString(), e.getMessage()), e);
            return null;
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerLogin(final PlayerLoginEvent event) {
        final String identifier = event.getPlayer().getUniqueId().toString();
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
    public PermissionsEx getManager() {
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
                logger.warn(t("No Permissible injector found for your server implementation!"));
            } else if (!success) {
                logger.warn(t("Unable to inject PEX's permissible for %s", player.getName()));
            }

            permissible.recalculatePermissions();

            if (success && getManager().hasDebugMode()) {
                logger.info(t("Permissions handler for %s successfully injected", player.getName()));
            }
        } catch (Throwable e) {
            logger.error(t("Unable to inject permissible for %s", player.getName()), e);
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
                logger.warn(t("No Permissible injector found for your server implementation (while uninjecting for %s)!", player.getName()));
            } else if (getManager() != null && getManager().hasDebugMode()) {
                logger.info(t("Permissions handler for %s successfully uninjected", player.getName()));
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void uninjectAllPermissibles() {
        getServer().getOnlinePlayers().forEach(this::uninjectPermissible);
    }

    private class BukkitImplementationInterface implements ImplementationInterface {
        @Override
        public Path getBaseDirectory() {
            return dataPath;
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
        public void registerCommand(CommandSpec command) {
            PluginCommand cmd = getCommand(command.getAliases().get(0));
            if (cmd != null) {
                PEXBukkitCommand bukkitCommand = new PEXBukkitCommand(command, PermissionsExPlugin.this);
                cmd.setExecutor(bukkitCommand);
                cmd.setTabCompleter(bukkitCommand);
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
