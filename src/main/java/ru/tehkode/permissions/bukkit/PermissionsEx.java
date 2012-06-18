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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import ru.tehkode.permissions.*;
import ru.tehkode.permissions.backends.*;
import ru.tehkode.permissions.bukkit.commands.*;
import ru.tehkode.permissions.commands.CommandsManager;
import ru.tehkode.permissions.exceptions.PermissionsNotAvailable;

/**
 *
 * @author code
 */
public class PermissionsEx extends JavaPlugin {

	protected static final String configFile = "config.yml";
	protected static final Logger logger = Logger.getLogger("Minecraft");
	protected PermissionManager permissionsManager;
	protected CommandsManager commandsManager;
	protected FileConfiguration config;
	protected BukkitPermissions superms;

	public PermissionsEx() {
		super();

		PermissionBackend.registerBackendAlias("sql", SQLBackend.class);
		PermissionBackend.registerBackendAlias("file", FileBackend.class);

		logger.log(Level.INFO, "[PermissionsEx] PermissionEx plugin initialized.");
	}

	@Override
	public void onLoad() {
		this.config = this.getConfig();
		this.commandsManager = new CommandsManager(this);
		this.permissionsManager = new PermissionManager(this.config);
	}

	@Override
	public void onEnable() {
		if (this.permissionsManager == null) {
			this.permissionsManager = new PermissionManager(this.config);
		}
		
		// Register commands
		this.commandsManager.register(new UserCommands());
		this.commandsManager.register(new GroupCommands());
		this.commandsManager.register(new PromotionCommands());
		this.commandsManager.register(new WorldCommands());
		this.commandsManager.register(new UtilityCommands());

		// Register Player permissions cleaner
		PlayerEventsListener cleaner = new PlayerEventsListener();
		cleaner.logLastPlayerLogin = this.config.getBoolean("permissions.log-players", cleaner.logLastPlayerLogin);
		this.getServer().getPluginManager().registerEvents(cleaner, this);

		//register service
		this.getServer().getServicesManager().register(PermissionManager.class, this.permissionsManager, this, ServicePriority.Normal);

		// Bukkit permissions
		ConfigurationSection dinnerpermsConfig = this.config.getConfigurationSection("permissions.superperms");

		if (dinnerpermsConfig == null) {
			dinnerpermsConfig = this.config.createSection("permissions.superperms");
		}

		this.superms = new BukkitPermissions(this, dinnerpermsConfig);

		this.superms.updateAllPlayers();

		this.saveConfig();

		// Start timed permissions cleaner timer
		this.permissionsManager.initTimer();

		logger.log(Level.INFO, "[PermissionsEx] v" + this.getDescription().getVersion() + " enabled");
	}

	@Override
	public void onDisable() {
		if (this.permissionsManager != null) {
			this.permissionsManager.end();
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

	public static Plugin getPlugin() {
		return Bukkit.getServer().getPluginManager().getPlugin("PermissionsEx");
	}

	public static boolean isAvailable() {
		Plugin plugin = getPlugin();

		return (plugin instanceof PermissionsEx) && ((PermissionsEx) plugin).permissionsManager != null;
	}

	public static PermissionManager getPermissionManager() {
		if (!isAvailable()) {
			throw new PermissionsNotAvailable();
		}

		return ((PermissionsEx) getPlugin()).permissionsManager;
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
	
	public class PlayerEventsListener implements Listener {

		protected boolean logLastPlayerLogin = false;

		@EventHandler
		public void onPlayerLogin(PlayerLoginEvent event) {
			if (!logLastPlayerLogin) {
				return;
			}

			PermissionUser user = getPermissionManager().getUser(event.getPlayer());
			user.setOption("last-login-time", Long.toString(System.currentTimeMillis() / 1000L));
			// user.setOption("last-login-ip", event.getPlayer().getAddress().getAddress().getHostAddress()); // somehow this won't work
		}

		@EventHandler
		public void onPlayerQuit(PlayerQuitEvent event) {
			if (logLastPlayerLogin) {
				getPermissionManager().getUser(event.getPlayer()).setOption("last-logout-time", Long.toString(System.currentTimeMillis() / 1000L));
			}

			getPermissionManager().resetUser(event.getPlayer().getName());
		}
	}
}
