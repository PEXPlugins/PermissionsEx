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

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import ru.tehkode.permissions.PermissionBackend;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.backends.FileBackend;
import ru.tehkode.permissions.backends.SQLBackend;
import ru.tehkode.permissions.bukkit.commands.GroupCommands;
import ru.tehkode.permissions.bukkit.commands.PromotionCommands;
import ru.tehkode.permissions.bukkit.commands.UserCommands;
import ru.tehkode.permissions.bukkit.commands.UtilityCommands;
import ru.tehkode.permissions.bukkit.commands.WorldCommands;
import ru.tehkode.permissions.bukkit.regexperms.RegexPermissions;
import ru.tehkode.permissions.commands.CommandsManager;
import ru.tehkode.permissions.exceptions.PermissionsNotAvailable;

/**
 * @author code
 */
public class PermissionsEx extends JavaPlugin {
	protected static final String CONFIG_FILE = "config.yml";
	protected PermissionManager permissionsManager;
	protected CommandsManager commandsManager;
	protected FileConfiguration config;
	protected SuperpermsListener superms;
	private RegexPermissions regexPerms;
	private static PermissionsEx instance;
	{
		instance = this;
	}

	public PermissionsEx() {
		super();

		PermissionBackend.registerBackendAlias("sql", SQLBackend.class);
		PermissionBackend.registerBackendAlias("file", FileBackend.class);
	}

	@Override
	public void onLoad() {
		try {
			this.config = this.getConfig();
			this.commandsManager = new CommandsManager(this);
			this.permissionsManager = new PermissionManager(this.config);
		} catch (Throwable t) {
			ErrorReport.handleError("In onLoad", t);
			this.setEnabled(false);
		}
	}

	@Override
	public void onEnable() {
		try {
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
			regexPerms = new RegexPermissions(this);
			superms = new SuperpermsListener(this);
			this.getServer().getPluginManager().registerEvents(superms, this);
			this.saveConfig();

			// Start timed permissions cleaner timer
			this.permissionsManager.initTimer();

		} catch (Throwable t) {
			ErrorReport.handleError("Error while enabling: ", t);
			this.getPluginLoader().disablePlugin(this);
		}
	}

	@Override
	public void onDisable() {
		try {
			if (this.permissionsManager != null) {
				this.permissionsManager.end();
			}

			this.getServer().getServicesManager().unregister(PermissionManager.class, this.permissionsManager);
			if (this.regexPerms != null) {
				this.regexPerms.onDisable();
			}
			if (this.superms != null) {
				this.superms.onDisable();
			}

		} catch (Throwable t) {
			ErrorReport.handleError("While disabling", t);
		}
		ErrorReport.shutdown();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		try {
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
		} catch (Throwable t) {
			ErrorReport.handleError("While " + sender.getName() + " was executing /" + command.getName(), t, sender);
			return true;
		}
	}

	public boolean isDebug() {
		return permissionsManager.isDebug();
	}

	public static Plugin getPlugin() {
		return instance;
	}

	public RegexPermissions getRegexPerms() {
		return regexPerms;
	}

	public static boolean isAvailable() {
		Plugin plugin = getPlugin();

		return plugin.isEnabled() && ((PermissionsEx) plugin).permissionsManager != null;
	}

	public static PermissionManager getPermissionManager() {
		if (!isAvailable()) {
			throw new PermissionsNotAvailable();
		}

		return ((PermissionsEx) getPlugin()).permissionsManager;
	}

	public PermissionManager getPermissionsManager() {
		return permissionsManager;
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
			try {
			if (!logLastPlayerLogin) {
				return;
			}

			PermissionUser user = getPermissionManager().getUser(event.getPlayer());
			user.setOption("last-login-time", Long.toString(System.currentTimeMillis() / 1000L));
			// user.setOption("last-login-ip", event.getPlayer().getAddress().getAddress().getHostAddress()); // somehow this won't work
			} catch (Throwable t) {
				ErrorReport.handleError("While login cleanup event", t);
			}
		}

		@EventHandler
		public void onPlayerQuit(PlayerQuitEvent event) {
			try {
			if (logLastPlayerLogin) {
				getPermissionManager().getUser(event.getPlayer()).setOption("last-logout-time", Long.toString(System.currentTimeMillis() / 1000L));
			}

			getPermissionManager().resetUser(event.getPlayer().getName());
			} catch (Throwable t) {
				ErrorReport.handleError("While logout cleanup event", t);
			}
		}
	}
}
