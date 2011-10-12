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
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.ConfigurationNode;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.events.PermissionEntityEvent;
import ru.tehkode.permissions.events.PermissionSystemEvent;

public class BukkitPermissions {
	
	protected static final Logger logger = Logger.getLogger("Minecraft");
	protected Map<Player, PermissionAttachment> attachments = new HashMap<Player, PermissionAttachment>();
	protected Plugin plugin;
	protected boolean dumpAllPermissions = false;
	protected boolean dumpMatchedPermissions = true;
	protected boolean disableByDefault = false;
	protected boolean debugMode = false;
	
	public BukkitPermissions(Plugin plugin, ConfigurationNode config) {
		this.plugin = plugin;
		
		if (!config.getBoolean("enable", true)) {
			logger.info("[PermissionsEx] Superperms disabled. Check \"config.yml\" to enable.");
			return;
		}
		
		this.dumpAllPermissions = config.getBoolean("raw-permissions", dumpAllPermissions);
		this.dumpMatchedPermissions = config.getBoolean("matched-permissions", dumpMatchedPermissions);
		this.disableByDefault = config.getBoolean("disable-unmatched", disableByDefault);
		this.debugMode = config.getBoolean("debug", debugMode);
		
		this.registerEvents();
				
		logger.info("[PermissionsEx] Superperms support enabled.");
	}
	
	private void registerEvents() {
		PluginManager manager = plugin.getServer().getPluginManager();
		
		PlayerEvents playerEventListener = new PlayerEvents();
		
		manager.registerEvent(Event.Type.PLAYER_JOIN, playerEventListener, Event.Priority.Low, plugin);
		manager.registerEvent(Event.Type.PLAYER_KICK, playerEventListener, Event.Priority.Low, plugin);
		manager.registerEvent(Event.Type.PLAYER_QUIT, playerEventListener, Event.Priority.Low, plugin);
		
		manager.registerEvent(Event.Type.PLAYER_RESPAWN, playerEventListener, Event.Priority.Low, plugin);
		manager.registerEvent(Event.Type.PLAYER_TELEPORT, playerEventListener, Event.Priority.Low, plugin);
		manager.registerEvent(Event.Type.PLAYER_PORTAL, playerEventListener, Event.Priority.Low, plugin);
		manager.registerEvent(Event.Type.PLAYER_CHANGED_WORLD, playerEventListener, Event.Priority.Low, plugin);;
        
		manager.registerEvent(Event.Type.CUSTOM_EVENT, new PEXEvents(), Event.Priority.Low, plugin);
        
        
	}
	
	public void updatePermissions(Player player) {
		this.updatePermissions(player, null);
	}
	
	public void updatePermissions(Player player, String world) {
		if (player == null || !this.plugin.isEnabled()) {
			return;
		}
        		
		if (world == null) {
			world = player.getWorld().getName();
		}
        
        // Remove attachment
        if(this.attachments.containsKey(player)){
            this.attachments.get(player).remove();
        }
		
		PermissionAttachment attachment = player.addAttachment(plugin);
        
        this.attachments.put(player, attachment);
        
		PermissionUser user = PermissionsEx.getPermissionManager().getUser(player);
		String permissions[] = user.getPermissions(world);
		
		if (dumpMatchedPermissions) { // find matching permissions		
			for (Permission permission : this.plugin.getServer().getPluginManager().getPermissions()) {
				String matchingExpression = user.getMatchingExpression(permissions, permission.getName());
				
				if (!disableByDefault && matchingExpression == null) { // not found, skip
					continue;
				}
				
				attachment.setPermission(permission, user.explainExpression(matchingExpression));
			}
		}
		
		if (dumpAllPermissions) { // upload raw permissions, now disabled by default
			
			for (String permission : permissions) {
				Boolean value = true;
				if (permission.startsWith("-")) {
					permission = permission.substring(1); // cut off -
					value = false;
				}
				
				if (!attachment.getPermissions().containsKey(permission)) {
					attachment.setPermission(permission, value);
				}
			}
		}
		
		player.recalculatePermissions();
		
		if (user.isDebug() || PermissionsEx.getPermissionManager().isDebug()) {
			logger.info("[PermissionsEx-Dinnerperms] Player " + player.getName() + " for \"" + player.getWorld().getName() + "\" world permissions updated!");
			
			if (this.debugMode) {
				logger.info("Attachment Permissions:");
				for (Map.Entry<String, Boolean> entry : attachment.getPermissions().entrySet()) {
					logger.info("   " + entry.getKey() + " = " + entry.getValue());
				}
				
				logger.info("Effective Permissions:");
				for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
					logger.info("   " + info.getPermission() + " = " + info.getValue());
					
				}
			}
		}
		
	}
	
	public void updateAllPlayers() {
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			updatePermissions(player);
		}
	}
	
	protected class PlayerEvents extends PlayerListener {

        @Override
        public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
            updatePermissions(event.getPlayer());
        }
        
		@Override
		public void onPlayerJoin(PlayerJoinEvent event) {
			updatePermissions(event.getPlayer());
		}
		
		@Override
		public void onPlayerPortal(PlayerPortalEvent event) { // will portal into another world
			if (event.getTo() == null || event.getPlayer().getWorld().equals(event.getTo().getWorld())) { // only if were world actually changed
				return;
			}
			
			updatePermissions(event.getPlayer(), event.getTo().getWorld().getName());
		}
		
		@Override
		public void onPlayerRespawn(PlayerRespawnEvent event) { // can be respawned in another world
			updatePermissions(event.getPlayer(), event.getRespawnLocation().getWorld().getName());
		}
		
		@Override
		public void onPlayerTeleport(PlayerTeleportEvent event) { // can be teleported into another world
			if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) { // only if world actually changed
				updatePermissions(event.getPlayer(), event.getTo().getWorld().getName());
			}
		}
		
		@Override
		public void onPlayerQuit(PlayerQuitEvent event) {
			attachments.remove(event.getPlayer());
		}
		
		@Override
		public void onPlayerKick(PlayerKickEvent event) {
			attachments.remove(event.getPlayer());
		}
	}
	
	protected class PEXEvents extends CustomEventListener {
		
		@Override
		public void onCustomEvent(Event event) {
			if (event instanceof PermissionEntityEvent) {
				PermissionEntityEvent pee = (PermissionEntityEvent) event;
				
				if (pee.getEntity() instanceof PermissionUser) { // update user only
					updatePermissions(Bukkit.getServer().getPlayer(pee.getEntity().getName()));
				} else if (pee.getEntity() instanceof PermissionGroup) { // update all members of group, might be resource hog
					for (PermissionUser user : PermissionsEx.getPermissionManager().getUsers(pee.getEntity().getName(), true)) {
						updatePermissions(Bukkit.getServer().getPlayer(user.getName()));
					}
				}
			} else if (event instanceof PermissionSystemEvent) {
				if (((PermissionSystemEvent) event).getAction().equals(PermissionSystemEvent.Action.DEBUGMODE_TOGGLE)) {
					return;
				}
				
				updateAllPlayers();
			}
		}
	}
}