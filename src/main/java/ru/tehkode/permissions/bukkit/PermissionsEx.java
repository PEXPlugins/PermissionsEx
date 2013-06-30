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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import ru.tehkode.permissions.PermissionMatcher;
import ru.tehkode.permissions.RegExpMatcher;
import ru.tehkode.permissions.bukkit.superperms.PEXPermissionSubscriptionMap;
import ru.tehkode.permissions.bukkit.superperms.PermissibleInjector;
import ru.tehkode.permissions.bukkit.superperms.PermissiblePEX;

import java.util.logging.Level;

import static ru.tehkode.permissions.bukkit.CraftBukkitInterface.getCBClassName;

/**
 * @author code
 */
public final class PermissionsEx extends JavaPlugin {
	protected static final PermissibleInjector[] injectors = new PermissibleInjector[]{
			new PermissibleInjector.ServerNamePermissibleInjector("net.glowstone.entity.GlowHumanEntity", "permissions", true, "Glowstone"),
			new PermissibleInjector.ServerNamePermissibleInjector("org.getspout.server.entity.SpoutHumanEntity", "permissions", true, "Spout"),
			new PermissibleInjector.ClassNameRegexPermissibleInjector("org\\.getspout\\.spout\\.player\\.SpoutCraftPlayer", "perm", false, "Spout"),
			new PermissibleInjector.ServerNamePermissibleInjector(getCBClassName("entity.CraftHumanEntity"), "perm", true, "CraftBukkit"),
			new PermissibleInjector.ServerNamePermissibleInjector(getCBClassName("entity.CraftHumanEntity"), "perm", true, "CraftBukkit++")
	};
	// Permissions subscriptions handling
	private PEXPermissionSubscriptionMap subscriptionHandler;
	private final PermissionMatcher matcher = new RegExpMatcher();
	private boolean debugMode = false;

	@Override
	public void onEnable() {
		subscriptionHandler = PEXPermissionSubscriptionMap.inject(this, getServer().getPluginManager());
		getServer().getPluginManager().registerEvents(new EventListener(), this);
		injectAllPermissibles();
	}

	@Override
	public void onDisable() {
		subscriptionHandler.uninject();
		uninjectAllPermissibles();
	}

	public boolean debugMode() {
		return this.debugMode;
	}

	public void setDebugMode(boolean debug) {
		this.debugMode = debug;
	}

	public PermissionMatcher getMatcher() {
		return this.matcher;
	}

	public void injectPermissible(Player player) {
		if (player.hasPermission("permissionsex.disable")) { // this user shouldn't get permissionsex matching
			return;
		}

		try {
			PermissiblePEX permissible = new PermissiblePEX(player, this);

			boolean success = false, found = false;
			for (PermissibleInjector injector : injectors) {
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
				getLogger().warning("No Permissible injector found for your server implementation!");
			} else if (!success) {
				getLogger().warning("Unable to inject PEX's permissible for " + player.getName());
			}

			permissible.recalculatePermissions();

			if (success && debugMode()) {
				getLogger().info("Permissions handler for " + player.getName() + " successfully injected");
			}
		} catch (Throwable e) {
			getLogger().log(Level.SEVERE, "Unable to inject permissible for " + player.getName(), e);
		}
	}

	private void injectAllPermissibles() {
		for (Player player : getServer().getOnlinePlayers()) {
			injectPermissible(player);
		}
	}

	private void uninjectPermissible(Player player) {
		if (player.hasPermission("permissionsex.disable")) { // this user shouldn't get permissionsex matching
			return;
		}

		try {
			boolean success = false;
			for (PermissibleInjector injector : injectors) {
				if (injector.isApplicable(player)) {
					Permissible pexPerm = injector.getPermissible(player);
					if (pexPerm instanceof PermissiblePEX) {
					if (injector.inject(player, ((PermissiblePEX) pexPerm).getPreviousPermissible()) != null) {
						success = true;
						break;
					}
					}
				}
			}

			if (!success) {
				getLogger().warning("No Permissible injector found for your server implementation (while uninjecting for " + player.getName() + "!");
			} else if (debugMode()) {
				getLogger().info("Permissions handler for " + player.getName() + " successfully uninjected");
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private void uninjectAllPermissibles() {
		for (Player player : getServer().getOnlinePlayers()) {
			injectPermissible(player);
		}
	}

	private class EventListener implements Listener {
		@EventHandler(priority = EventPriority.LOWEST)
		public void onPlayerLogin(PlayerLoginEvent event) {
			injectPermissible(event.getPlayer());
		}

		@EventHandler(priority = EventPriority.MONITOR)
		// Technically not supposed to use MONITOR for this, but we don't want to remove before other plugins are done checking permissions
		public void onPlayerQuit(PlayerQuitEvent event) {
			uninjectPermissible(event.getPlayer());
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		PluginDescriptionFile pdf = this.getDescription();
		if (args.length == 1) {
			if (args[0].equalsIgnoreCase("debug")) {
				setDebugMode(!debugMode());
				sender.sendMessage(ChatColor.AQUA + " Debug mode " + (debugMode() ? "enabled" : "disabled"));
				return true;
			} else if (args[0].equalsIgnoreCase("reload")) {
				uninjectAllPermissibles();
				injectAllPermissibles();
				sender.sendMessage(ChatColor.AQUA + "PEX reloaded");
				return true;
			} else {
				sender.sendMessage(ChatColor.RED + "Unknown command: " + args[0]);
			}
		} else if (args.length > 1) {
			sender.sendMessage(ChatColor.RED + "Too many arguments!");
		}
		// Usage
		sender.sendMessage("[" + ChatColor.RED + "PermissionsEx" + ChatColor.RESET + "] version [" + ChatColor.BLUE + pdf.getVersion() + ChatColor.RESET + "]");
		return false;
	}
}
