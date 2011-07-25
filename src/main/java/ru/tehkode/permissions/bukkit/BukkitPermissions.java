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
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import ru.tehkode.permissions.PermissionEntity;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.events.PermissionEntityEvent;
import ru.tehkode.permissions.events.PermissionSystemEvent;

public class BukkitPermissions {

    protected Map<Player, PermissionAttachment> attachments = new HashMap<Player, PermissionAttachment>();
    protected Set<Permission> registeredPermissions = new HashSet<Permission>();
    protected Plugin plugin;

    public BukkitPermissions(Plugin plugin) {
        this.plugin = plugin;

        this.collectPermissions();
        this.registerEvents();

        this.updateAllPlayers();
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

        CustomEventListener customEventListener = new PEXEvents();

        manager.registerEvent(Event.Type.CUSTOM_EVENT, customEventListener, Event.Priority.Normal, plugin);
    }

    private void collectPermissions() {
        for (Plugin plugin : Bukkit.getServer().getPluginManager().getPlugins()) {
            registeredPermissions.addAll(plugin.getDescription().getPermissions());
        }
    }

    protected void updatePermissions(Player player) {
        if (!this.attachments.containsKey(player)) {
            this.attachments.put(player, player.addAttachment(plugin));
        }

        PermissionAttachment attachment = this.attachments.get(player);

        PermissionUser user = PermissionsEx.getPermissionManager().getUser(player);
        String permissions[] = user.getPermissions(player.getWorld().getName());

        attachment.getPermissions().clear();

        for (Permission permission : registeredPermissions) {
            String matchingExpression = user.getMatchingExpression(permissions, permission.getName());

            if (matchingExpression == null) { // Not found, skip this one
                continue;
            }

            attachment.setPermission(permission, PermissionEntity.explainExpression(matchingExpression));
        }
        
        player.recalculatePermissions();        
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
            updatePermissions(event.getPlayer());
        }

        @Override
        public void onPlayerRespawn(PlayerRespawnEvent event) { // can be respawned in another world
            updatePermissions(event.getPlayer());
        }

        @Override
        public void onPlayerTeleport(PlayerTeleportEvent event) { // can be teleported into another world
            updatePermissions(event.getPlayer());
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
            if (event instanceof PermissionSystemEvent) {
                // have to update all players
                updateAllPlayers();
            } else if (event instanceof PermissionEntityEvent) {
                PermissionEntityEvent pee = (PermissionEntityEvent) event;

                if (pee.getEntity() instanceof PermissionUser) { // update user only
                    PermissionUser user = (PermissionUser) pee.getEntity();

                    Player player = Bukkit.getServer().getPlayer(user.getName());

                    if (player != null) {
                        updatePermissions(player);
                    }
                } else if (pee.getEntity() instanceof PermissionGroup) { // update all members of group, might be resource hog
                    PermissionGroup group = (PermissionGroup) pee.getEntity();

                    PermissionUser[] users = PermissionsEx.getPermissionManager().getUsers(group.getName(), true);

                    for (PermissionUser user : users) {
                        Player player = Bukkit.getServer().getPlayer(user.getName());

                        if (player != null) {
                            updatePermissions(player);
                        }
                    }
                }
            }
        }
    }
}
