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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.config.ConfigurationNode;
import ru.tehkode.permissions.events.PermissionEntityEvent;
import ru.tehkode.permissions.events.PermissionSystemEvent;

public class BukkitPermissions {

    protected static final Logger logger = Logger.getLogger("Minecraft");
    protected Map<Player, PermissionAttachment> attachments = new HashMap<Player, PermissionAttachment>();
    protected Set<Permission> registeredPermissions = new HashSet<Permission>();
    protected Plugin plugin;
    protected boolean dumpAllPermissions = true;
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

        this.collectPermissions();
        this.registerEvents();

        this.updateAllPlayers();

        logger.info("[PermissionsEx] Superperms support enabled.");
    }

    private void registerEvents() {
        PluginManager manager = plugin.getServer().getPluginManager();

        PlayerEvents playerEventListener = new PlayerEvents();

        manager.registerEvent(Event.Type.PLAYER_JOIN, playerEventListener, Event.Priority.Normal, plugin);
        manager.registerEvent(Event.Type.PLAYER_KICK, playerEventListener, Event.Priority.Normal, plugin);
        manager.registerEvent(Event.Type.PLAYER_QUIT, playerEventListener, Event.Priority.Normal, plugin);

        manager.registerEvent(Event.Type.PLAYER_RESPAWN, playerEventListener, Event.Priority.Normal, plugin);
        manager.registerEvent(Event.Type.PLAYER_TELEPORT, playerEventListener, Event.Priority.Normal, plugin);
        manager.registerEvent(Event.Type.PLAYER_PORTAL, playerEventListener, Event.Priority.Normal, plugin);

        manager.registerEvent(Event.Type.CUSTOM_EVENT, new PEXEvents(), Event.Priority.Normal, plugin);

        ServerListener serverListener = new BukkitEvents();

        manager.registerEvent(Event.Type.PLUGIN_ENABLE, serverListener, Event.Priority.Normal, plugin);
        manager.registerEvent(Event.Type.PLUGIN_DISABLE, serverListener, Event.Priority.Normal, plugin);
    }

    private void collectPermissions() {
        registeredPermissions.clear();
        for (Plugin bukkitPlugin : Bukkit.getServer().getPluginManager().getPlugins()) {
            registeredPermissions.addAll(bukkitPlugin.getDescription().getPermissions());
        }
    }
    
    protected void updatePermissions(Player player){
        this.updatePermissions(player, null);
    }
    
    protected void updatePermissions(Player player, String world) {
        if (player == null || !this.plugin.isEnabled()) {
            return;
        }

        if (!this.attachments.containsKey(player)) {
            this.attachments.put(player, player.addAttachment(plugin));
        }

        if(world == null){
            world = player.getWorld().getName();
        }
        
        PermissionAttachment attachment = this.attachments.get(player);

        PermissionUser user = PermissionsEx.getPermissionManager().getUser(player);
        String permissions[] = user.getPermissions(world);

        // clear permissions
        for (String permission : attachment.getPermissions().keySet()) {
            attachment.unsetPermission(permission);
        }

        if (dumpMatchedPermissions) {
            // find matching permissions
            for (Permission permission : registeredPermissions) {
                String matchingExpression = user.getMatchingExpression(permissions, permission.getName());
                boolean permissionValue = user.explainExpression(matchingExpression);

                if (!disableByDefault && matchingExpression == null) { // not found, skip
                    continue;
                }

                attachment.setPermission(permission, permissionValue);
            }
        }

        if (dumpAllPermissions) {
            // all permissions
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

        if (PermissionsEx.getPermissionManager().isDebug() || this.debugMode) {
            PermissionsEx.logger.info("[PermissionsEx-Dinnerperms] Player " + player.getName() + " for \"" + player.getWorld().getName() + "\" world permissions updated!");
            
            if(this.debugMode){
                PermissionsEx.logger.info("Attachment Permissions:");
                for(Map.Entry<String, Boolean> entry : attachment.getPermissions().entrySet()){
                    PermissionsEx.logger.info("   " + entry.getKey() + " = " + entry.getValue());
                }

                PermissionsEx.logger.info("Effective Permissions:");
                for(PermissionAttachmentInfo info : player.getEffectivePermissions()){
                    PermissionsEx.logger.info("   " + info.getPermission() + " = " + info.getValue());
                }
            }
        }
    }

    protected void updateAllPlayers() {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            updatePermissions(player);
        }
    }

    protected class PlayerEvents extends PlayerListener {

        @Override
        public void onPlayerJoin(PlayerJoinEvent event) {
            updatePermissions(event.getPlayer());
        }

        @Override
        public void onPlayerPortal(PlayerPortalEvent event) { // will portal into another world
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

    protected class BukkitEvents extends ServerListener {

        @Override
        public void onPluginEnable(PluginEnableEvent event) {
            collectPermissions();
            updateAllPlayers();
        }

        @Override
        public void onPluginDisable(PluginDisableEvent event) {
            collectPermissions();
            updateAllPlayers();
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
                updateAllPlayers();
            }
        }
    }
}
