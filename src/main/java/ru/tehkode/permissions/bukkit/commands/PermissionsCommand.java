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
package ru.tehkode.permissions.bukkit.commands;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.tehkode.permissions.PermissionEntity;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.commands.CommandListener;
import ru.tehkode.permissions.commands.exceptions.AutoCompleteChoicesException;
import ru.tehkode.utils.StringUtils;

public abstract class PermissionsCommand implements CommandListener {

    protected static final Logger logger = Logger.getLogger("Minecraft");

    protected void informGroup(Plugin plugin, PermissionGroup group, String message) {
        for (PermissionUser user : group.getUsers()) {
            this.informPlayer(plugin, user.getName(), message);
        }
    }

    protected void informPlayer(Plugin plugin, String playerName, String message) {
        if (!(plugin instanceof PermissionsEx) || !((PermissionsEx) plugin).getConfigurationNode().getBoolean("permissions.informplayers.changes", false)) {
            return; // User informing is disabled
        }

        Player player = Bukkit.getServer().getPlayer(playerName);
        if (player == null) {
            // @todo mb Logger.inform ?
            return;
        }

        player.sendMessage(ChatColor.BLUE + "[PermissionsEx] " + ChatColor.WHITE + message);
    }

    protected String autoCompletePlayerName(String playerName) {
        if (playerName == null) {
            return null;
        }

        List<String> players = new LinkedList<String>();
        
        // Collect online Player names
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(playerName)) {
                return player.getName();
            }

            if (player.getName().toLowerCase().startsWith(playerName.toLowerCase()) && !players.contains(player.getName())) {
                players.add(player.getName());
            }
        }

        // Collect registered PEX user names
        for (PermissionUser user : PermissionsEx.getPermissionManager().getUsers()) {
            if (user.getName().equalsIgnoreCase(playerName)) {
                return user.getName();
            }

            if (user.getName().toLowerCase().startsWith(playerName.toLowerCase()) && !players.contains(user.getName())) {
                players.add(user.getName());
            }
        }

        if (players.size() > 1) {
            throw new AutoCompleteChoicesException(players.toArray(new String[0]), "user");
        } else if (players.size() == 1) {
            return players.get(0);
        } else {
            return playerName;
        }
    }

    protected String autoCompleteGroupName(String groupName) {
        List<String> groups = new LinkedList<String>();

        for (PermissionGroup group : PermissionsEx.getPermissionManager().getGroups()) {
            if (group.getName().equalsIgnoreCase(groupName)) {
                return group.getName();
            }

            if (group.getName().toLowerCase().startsWith(groupName.toLowerCase()) && !groups.contains(group.getName())) {
                groups.add(group.getName());
            }
        }

        if (groups.size() > 1) { // Found several choices
            throw new AutoCompleteChoicesException(groups.toArray(new String[0]), "group");
        } else if (groups.size() == 1) { // Found one name
            return groups.get(0);
        } else { // Nothing found
            return groupName;
        }
    }

    protected String printHierarchy(PermissionGroup parent, int level) {
        StringBuilder buffer = new StringBuilder();

        PermissionGroup[] groups;
        if (parent == null) {
            groups = PermissionsEx.getPermissionManager().getGroups();
        } else {
            groups = parent.getChildGroups();
        }

        Logger.getLogger("Minecraft").info("Childs of " + parent + " " + groups);

        for (PermissionGroup group : groups) {
            if (parent == null && group.getParentGroups().length > 0) {
                continue;
            }

            Logger.getLogger("Minecraft").info("DAT " + group + " " + Arrays.asList(group.getParentGroups()));

            buffer.append(StringUtils.repeat("  ", level)).append(" - ").append(group.getName()).append("\n");

            // Groups
            buffer.append(printHierarchy(group, level + 1));

            for (PermissionUser user : group.getUsers()) {
                buffer.append(StringUtils.repeat("  ", level + 1)).append(" + ").append(user.getName()).append("\n");
            }
        }

        return buffer.toString();
    }

    protected String mapPermissions(String world, PermissionEntity entity, int level) {
        StringBuilder builder = new StringBuilder();


        int index = 1;
        for (String permission : this.getPermissionsTree(entity, world, 0)) {
            if (level > 0) {
                builder.append("   ");
            } else {
                builder.append(index++).append(") ");
            }

            builder.append(permission);
            if (level > 0) {
                builder.append(" (from ").append(entity.getName()).append(")");
            } else {
                builder.append(" (own)");
            }
            builder.append("\n");
        }

        PermissionGroup[] parents;

        if (entity instanceof PermissionUser) {
            parents = ((PermissionUser) entity).getGroups();
        } else if (entity instanceof PermissionGroup) {
            parents = ((PermissionGroup) entity).getParentGroups();
        } else {
            throw new RuntimeException("Unknown class in hierarchy. Nag t3hk0d3 please.");
        }

        level++; // Just increment level once
        for (PermissionGroup group : parents) {
            builder.append(mapPermissions(world, group, level));
        }

        return builder.toString();
    }

    protected List<String> getPermissionsTree(PermissionEntity entity, String world, int level) {
        List<String> permissions = new LinkedList<String>();
        Map<String, String[]> allPermissions = entity.getAllPermissions();

        String[] worldsPermissions = allPermissions.get(world);
        if (worldsPermissions != null) {
            permissions.addAll(sprintPermissions(world, worldsPermissions));
        }

        for (String parentWorld : PermissionsEx.getPermissionManager().getWorldInheritance(world)) {
            if (parentWorld != null && !parentWorld.isEmpty()) {
                permissions.addAll(getPermissionsTree(entity, parentWorld, level + 1));
            }
        }

        if (level == 0 && allPermissions.get("") != null) { // default world permissions
            permissions.addAll(sprintPermissions("common", allPermissions.get("")));
        }

        return permissions;
    }

    protected List<String> sprintPermissions(String world, String[] permissions) {
        List<String> permissionList = new LinkedList<String>();

        if (permissions == null) {
            return permissionList;
        }

        for (String permission : permissions) {
            permissionList.add(permission + " @" + world);
        }

        return permissionList;
    }

    protected Object parseValue(String value) {
        if (value == null) {
            return null;
        }

        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
        }

        return value;
    }

    protected void sendMessage(CommandSender sender, String message) {
        for (String messagePart : message.split("\n")) {
            sender.sendMessage(messagePart);
        }
    }
}
