/*
 * PermissionsEx - Permissions plugin for Bukkit
 * Copyright (C) 2011 t3hk0d3 http://www.tehkode.ru
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package ru.tehkode.permissions.bukkit;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;
import ru.tehkode.permissions.*;
import ru.tehkode.permissions.backends.*;
import ru.tehkode.permissions.bukkit.commands.*;
import ru.tehkode.permissions.commands.CommandsManager;

/**
 *
 * @author code
 */
public class PermissionsEx extends JavaPlugin {

    protected static final String configFile = "config.yml";
    protected static final Logger logger = Logger.getLogger("Minecraft");
    protected PermissionManager permissionsManager;
    protected CommandsManager commandsManager;
    protected Configuration config;
    protected BukkitPermissions superms;

    public PermissionsEx() {
        super();

        PermissionBackend.registerBackendAlias("sql", SQLBackend.class);
        PermissionBackend.registerBackendAlias("file", FileBackend.class);

        logger.log(Level.INFO, "[PermissionsEx] PermissionEx plugin initialized.");
    }

    @Override
    public void onLoad() {
        this.config = this.loadConfig(configFile);
        this.commandsManager = new CommandsManager(this);
        this.permissionsManager = new PermissionManager(this.config);
    }

    @Override
    public void onEnable() {
        // Register commands
        this.commandsManager.register(new UtilityCommands());
        this.commandsManager.register(new UserCommands());
        this.commandsManager.register(new GroupCommands());
        this.commandsManager.register(new PromotionCommands());
        this.commandsManager.register(new WorldCommands());

        // Register Player permissions cleaner
        this.getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, new PlayerEventsListener(), Priority.Normal, this);
        // Multiworld cache cleaner
        this.getServer().getPluginManager().registerEvent(Event.Type.PLAYER_RESPAWN, new PlayerEventsListener(), Priority.Lowest, this);
        this.getServer().getPluginManager().registerEvent(Event.Type.PLAYER_TELEPORT, new PlayerEventsListener(), Priority.Lowest, this);
        this.getServer().getPluginManager().registerEvent(Event.Type.PLAYER_PORTAL, new PlayerEventsListener(), Priority.Lowest, this);

        //register service
        this.getServer().getServicesManager().register(PermissionManager.class, this.permissionsManager, this, ServicePriority.Normal);

        // Bukkit permissions
        ConfigurationNode dinnerpermsConfig = this.config.getNode("permissions.superperms");

        if (dinnerpermsConfig == null) {
            this.config.setProperty("permissions.superperms", new HashMap<String, Object>());
            dinnerpermsConfig = this.config.getNode("permissions.superperms");
        }

        this.superms = new BukkitPermissions(this, dinnerpermsConfig);
        
        this.superms.updateAllPlayers();

        this.config.save();

        logger.log(Level.INFO, "[PermissionsEx] v" + this.getDescription().getVersion() + " enabled");
    }

    @Override
    public void onDisable() {
		if(this.permissionsManager != null){
			this.permissionsManager.reset();
		}

        this.getServer().getServicesManager().unregister(PermissionManager.class, this.permissionsManager);

        logger.log(Level.INFO, "[PermissionsEx] v" + this.getDescription().getVersion() + " disabled successfully.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        PluginDescriptionFile pdf = this.getDescription();
        if (args.length > 0) {
            return this.commandsManager.execute(sender, command, args);
        } else {
            if (sender instanceof Player) {
                sender.sendMessage("[" + ChatColor.RED + "PermissionsEx" + ChatColor.WHITE + "] version [" + ChatColor.BLUE + pdf.getVersion() + ChatColor.WHITE + "]");

                return !this.permissionsManager.has((Player) sender, "permissions.manage");
            } else {
                sender.sendMessage("[PermissionsEx] version [" + pdf.getVersion() + "]");

                return false;
            }
        }
    }
    
    public static Plugin getPlugin(){
        return Bukkit.getServer().getPluginManager().getPlugin("PermissionsEx");
    }
    
    public static boolean isAvailable(){
        Plugin plugin = getPlugin();
        
        return !(plugin == null || !(plugin instanceof PermissionsEx) || ((PermissionsEx) plugin).permissionsManager == null);
    }

    public static PermissionManager getPermissionManager() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("PermissionsEx");
        
        if (!isAvailable()) {
            throw new RuntimeException("Permissions manager is not accessable. Is the PermissionsEx plugin enabled?");
        }

        return ((PermissionsEx) plugin).permissionsManager;
    }

    public static PermissionUser getUser(Player player) {
        return getPermissionManager().getUser(player);
    }

    public static PermissionUser getUser(String name) {
        return getPermissionManager().getUser(name);
    }

    public boolean has(Player player, String permission) {
        return this.permissionsManager.has(player, permission);
    }

    public boolean has(Player player, String permission, String world) {
        return this.permissionsManager.has(player, permission, world);
    }

    @Override
    public org.bukkit.util.config.Configuration getConfiguration() {
        return this.config;
    }
    
    protected final Configuration loadConfig(String name) {
        File dataFolder = getDataFolder();

        if (dataFolder == null) {
            dataFolder = new File("plugins/PermissionsEx/");
        }

        File configurationFile = new File(dataFolder, configFile);
        Configuration configuration;
        if (!configurationFile.exists()) {
            try {
                if (!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }
                configurationFile.createNewFile(); // Try to create new one
                configuration = new Configuration(configurationFile);
                configuration.setProperty("permissions.basedir", dataFolder.getPath());
                configuration.save();
            } catch (IOException e) {
                // And if failed (ex.: not enough rights) - catch exception
                throw new RuntimeException(e); // Rethrow exception
            }
        } else {
            configuration = new Configuration(configurationFile);
            configuration.load();
        }
        return configuration;
    }

    public class PlayerEventsListener extends PlayerListener {

        @Override
        public void onPlayerQuit(PlayerQuitEvent event) {
            getPermissionManager().resetUser(event.getPlayer().getName());
        }

        protected void clearUserCache(PlayerEvent event) {
            getPermissionManager().clearUserCache(event.getPlayer());
        }

        @Override
        public void onPlayerPortal(PlayerPortalEvent event) {
            this.clearUserCache(event);
        }

        @Override
        public void onPlayerRespawn(PlayerRespawnEvent event) {
            this.clearUserCache(event);
        }

        @Override
        public void onPlayerTeleport(PlayerTeleportEvent event) {
            this.clearUserCache(event);
        }
    }
}
