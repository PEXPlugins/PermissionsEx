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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import ru.tehkode.permissions.*;
import ru.tehkode.permissions.backends.*;
import ru.tehkode.permissions.bukkit.commands.*;
import ru.tehkode.permissions.bukkit.modifyworld.ModifyworldManager;
import ru.tehkode.permissions.commands.CommandsManager;
import ru.tehkode.permissions.config.Configuration;

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
    protected ModifyworldManager modifyworldManager;

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
        this.modifyworldManager = new ModifyworldManager(this);
    }

    @Override
    public void onEnable() {       
        this.commandsManager.register(new UtilityCommands());
        this.commandsManager.register(new UserCommands());
        this.commandsManager.register(new GroupCommands());
        this.commandsManager.register(new PromotionCommands());
        this.commandsManager.register(new WorldCommands());
        
        this.getServer().getPluginManager().registerEvent(Event.Type.PLAYER_QUIT, new PlayerEventsListener(), Priority.Low, this);

        this.modifyworldManager.registerEvents();
        
        //register service
        this.getServer().getServicesManager().register(PermissionManager.class, this.permissionsManager, this, ServicePriority.Normal);
        
        logger.log(Level.INFO, "[PermissionsEx] v" + this.getDescription().getVersion() + " enabled");
    }

    @Override
    public void onDisable() {
        this.permissionsManager.reset();
        
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

    public static PermissionManager getPermissionManager() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("PermissionsEx");
        if (plugin == null || !(plugin instanceof PermissionsEx) || ((PermissionsEx)plugin).permissionsManager == null) {
            throw new RuntimeException("Permissions manager is not accessable. Is the PermissionsEx plugin enabled?");
        }

        return ((PermissionsEx) plugin).permissionsManager;
    }

    public boolean has(Player player, String permission) {
        return this.permissionsManager.has(player, permission);
    }

    public boolean has(Player player, String permission, String world) {
        return this.permissionsManager.has(player, permission, world);
    }

    public Configuration getConfigurationNode() {
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
    }
}
