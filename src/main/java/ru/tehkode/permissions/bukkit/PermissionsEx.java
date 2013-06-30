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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import ru.tehkode.permissions.bukkit.superperms.PEXPermissionSubscriptionMap;
import ru.tehkode.permissions.bukkit.superperms.PermissiblePEX;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author code
 */
public class PermissionsEx extends JavaPlugin {
	// Parent-child permissions stuff
	private final Map<String, Map<String, Boolean>> childPermissions = new HashMap<String, Map<String, Boolean>>();
	private int permissionsHashCode;

	// Permissions subscriptions handling
	private PEXPermissionSubscriptionMap subscriptionHandler;

	@Override
	public void onEnable() {
		subscriptionHandler = PEXPermissionSubscriptionMap.inject(this, getServer().getPluginManager());
		getServer().getPluginManager().registerEvents(new EventListener(), this);
		checkAllParentPermissions(true);
	}

	@Override
	public void onDisable() {
		subscriptionHandler.uninject();
	}

	public Map<String, Map<String, Boolean>> getChildPermissions() {
		return childPermissions;
	}

	public final void checkAllParentPermissions(boolean forced) {
		Set<Permission> allPermissions = getServer().getPluginManager().getPermissions();
		int hashCode = allPermissions.hashCode();

		if (forced || hashCode != permissionsHashCode) {
			calculateParentPermissions(allPermissions);

			permissionsHashCode = hashCode;
		}
	}

	protected void calculateParentPermissions(Set<Permission> permissions) {
		for (Permission permission : permissions) {
			this.calculatePermissionChildren(permission);
		}
	}

	protected void calculatePermissionChildren(Permission permission) {
		for (Map.Entry<String, Boolean> child : permission.getChildren().entrySet()) {
			Map<String, Boolean> map = this.childPermissions.get(child.getKey().toLowerCase());
			if (map == null) {
				this.childPermissions.put(child.getKey().toLowerCase(), map = new HashMap<String, Boolean>());
			}

			map.put(permission.getName(), child.getValue());
		}
	}

	private void registerEvents() {
		PluginManager manager = plugin.getServer().getPluginManager();

		manager.registerEvents(new EventListener(), this);
	}

	public void updatePermissions(Player player) {
		if (player == null || !this.plugin.isEnabled()) {
			return;
		}

		PermissiblePEX.inject(player, this);

		player.recalculatePermissions();
	}

	public void updateAllPlayers() {
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			updatePermissions(player);
		}
	}

	protected class EventListener implements Listener {

		@EventHandler(priority = EventPriority.LOWEST)
		public void onPlayerLogin(PlayerLoginEvent event) {
			try {
				updatePermissions(event.getPlayer());
			} catch (Throwable t) {
				ErrorReport.handleError("Superperms event login", t);
			}
		}

		@EventHandler(priority = EventPriority.LOW)
		public void onPluginEnable(PluginEnableEvent event) {
			try {
				List<Permission> pluginPermissions = event.getPlugin().getDescription().getPermissions();

				for (Permission permission : pluginPermissions) {
					calculatePermissionChildren(permission);
				}
			} catch (Throwable t) {
				ErrorReport.handleError("Superperms event plugin enable", t);
			}
		}

		@EventHandler(priority = EventPriority.LOW)
		public void onEntityEvent(PermissionEntityEvent event) {
			try {
				if (event.getEntity() instanceof PermissionUser) { // update user only
					updatePermissions(Bukkit.getServer().getPlayer(event.getEntity().getName()));
				} else if (event.getEntity() instanceof PermissionGroup) { // update all members of group, might be resource hog
					for (PermissionUser user : PermissionsEx.getPermissionManager().getUsers(event.getEntity().getName(), true)) {
						updatePermissions(Bukkit.getServer().getPlayer(user.getName()));
					}
				}
			} catch (Throwable t) {
				ErrorReport.handleError("Superperms event permission entity", t);
			}
		}

		@EventHandler(priority = EventPriority.LOW)
		public void onSystemEvent(PermissionSystemEvent event) {
			try {
				if (event.getAction() == PermissionSystemEvent.Action.DEBUGMODE_TOGGLE) {
					return;
				}

				updateAllPlayers();
			} catch (Throwable t) {
				ErrorReport.handleError("Superperms event permission system event", t);
			}
		}
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
			ErrorReport report = ErrorReport.withException("While " + sender.getName() + " was executing /" + command.getName(), t);
			String msg = report.buildUserErrorMessage();
			if (!(sender instanceof ConsoleCommandSender)) {
				getLogger().severe(msg);
			}
			sender.sendMessage(ChatColor.RED + msg);
			return true;
		}
	}
}
