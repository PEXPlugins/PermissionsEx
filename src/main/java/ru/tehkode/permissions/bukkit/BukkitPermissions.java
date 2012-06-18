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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.superperms.PermissiblePEX;
import ru.tehkode.permissions.events.PermissionEntityEvent;
import ru.tehkode.permissions.events.PermissionSystemEvent;

public class BukkitPermissions {

	protected static final Logger logger = Logger.getLogger("Minecraft");
	protected Map<Player, PermissionAttachment> attachments = new HashMap<Player, PermissionAttachment>();
	protected Plugin plugin;
	protected boolean strictMode = false;
	protected boolean enableParentNodes = true;
	protected Map<String, Map<String, Boolean>> childPermissions = new HashMap<String, Map<String, Boolean>>();
	
	private int permissionsHashCode;

	public BukkitPermissions(Plugin plugin, ConfigurationSection config) {
		this.plugin = plugin;

		if (!config.getBoolean("enable", true)) {
			logger.info("[PermissionsEx] Superperms disabled. Check \"config.yml\" to enable.");
			return;
		}

		this.strictMode = config.getBoolean("strict-mode", strictMode);
		this.enableParentNodes = config.getBoolean("parent-nodes", this.enableParentNodes);

		this.registerEvents();

		this.checkAllParentPermissions(true);

		logger.info("[PermissionsEx] Superperms support enabled.");
	}

	public Map<String, Map<String, Boolean>> getChildPermissions() {
		return childPermissions;
	}

	public boolean isStrictMode() {
		return strictMode;
	}

	public boolean isEnableParentNodes() {
		return enableParentNodes;
	}

	public Plugin getPlugin() {
		return plugin;
	}
	
	public final void checkAllParentPermissions(boolean forced) {
		if (!this.enableParentNodes) {
			return;
		}
		
		Set<Permission> allPermissions = this.plugin.getServer().getPluginManager().getPermissions();
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

		manager.registerEvents(new EventListener(), plugin);
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
			updatePermissions(event.getPlayer());
		}

		@EventHandler(priority = EventPriority.LOW)
		public void onPluginEnable(PluginEnableEvent event) {
			List<Permission> pluginPermissions = event.getPlugin().getDescription().getPermissions();

			for (Permission permission : pluginPermissions) {
				calculatePermissionChildren(permission);
			}
		}

		@EventHandler(priority = EventPriority.LOW)
		public void onEntityEvent(PermissionEntityEvent event) {
			if (event.getEntity() instanceof PermissionUser) { // update user only
				updatePermissions(Bukkit.getServer().getPlayer(event.getEntity().getName()));
			} else if (event.getEntity() instanceof PermissionGroup) { // update all members of group, might be resource hog
				for (PermissionUser user : PermissionsEx.getPermissionManager().getUsers(event.getEntity().getName(), true)) {
					updatePermissions(Bukkit.getServer().getPlayer(user.getName()));
				}
			}
		}

		@EventHandler(priority = EventPriority.LOW)
		public void onSystemEvent(PermissionSystemEvent event) {
			if (event.getAction() == PermissionSystemEvent.Action.DEBUGMODE_TOGGLE) {
				return;
			}

			updateAllPlayers();
		}
	}
}
