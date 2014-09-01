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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.tehkode.permissions.PermissionEntity;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.commands.CommandListener;
import ru.tehkode.permissions.commands.CommandsManager;
import ru.tehkode.permissions.commands.exceptions.AutoCompleteChoicesException;
import ru.tehkode.utils.StringUtils;

public abstract class PermissionsCommand implements CommandListener {
	protected CommandsManager manager;

	@Override
	public void onRegistered(CommandsManager manager) {
		this.manager = manager;
	}

	protected void informGroup(PermissionsEx plugin, PermissionGroup group, String message) {
		for (PermissionUser user : group.getActiveUsers()) {
			this.informPlayer(plugin, user, message);
		}
	}

	protected void informPlayer(PermissionsEx plugin, PermissionUser user, String message) {
		if (!plugin.getConfiguration().informPlayers()) {
			return; // User informing is disabled
		}

		Player player = user.getPlayer();
		if (player == null) {
			return;
		}

		player.sendMessage(ChatColor.BLUE + "[PermissionsEx] " + ChatColor.RESET + message);
	}

	protected String autoCompletePlayerName(String playerName) {
		return autoCompletePlayerName(playerName, "user");
	}

	protected void printEntityInheritance(CommandSender sender, List<PermissionGroup> groups) {
		for (PermissionGroup group : groups) {
			String rank = "not ranked";
			if (group.isRanked()) {
				rank = "rank " + group.getRank() + " @ " + group.getRankLadder();
			}

			sender.sendMessage("   " + group.getIdentifier() + " (" + rank + ")");
		}
	}

	private String nameToUUID(String name) {
		OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(name);
		if (player != null) {
			UUID uid = player.getUniqueId();
			if (uid != null) {
				return uid.toString();
			}
		}
		return name;
	}

	protected String autoCompletePlayerName(String playerName, String argName) {
		if (playerName == null) {
			return null;
		}

		if (playerName.startsWith("#")) {
			return nameToUUID(playerName.substring(1));
		}

		List<String> players = new LinkedList<>();

		// Collect online Player names
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			if (player.getName().equalsIgnoreCase(playerName)) {
				return player.getUniqueId().toString();
			}

			if (player.getName().toLowerCase().startsWith(playerName.toLowerCase()) && !players.contains(player.getUniqueId().toString())) {
				players.add(player.getUniqueId().toString());
			}
		}

		// Collect registered PEX user names
		for (String user : PermissionsEx.getPermissionManager().getUserNames()) {
			if (user.equalsIgnoreCase(playerName)) {
				return nameToUUID(user);
			}

			if (user.toLowerCase().startsWith(playerName.toLowerCase())) {
				final String uid = nameToUUID(user);
				if (!players.contains(uid)) {
					players.add(uid);
				}
			}
		}

		if (players.size() > 1) {
			throw new AutoCompleteChoicesException(players.toArray(new String[0]), argName);
		} else if (players.size() == 1) {
			return players.get(0);
		}

		// Nothing found
		return playerName;
	}

	protected String describeUser(PermissionUser user) {
		return user.getIdentifier() + "/" + user.getName();
	}

	protected String autoCompleteGroupName(String groupName) {
		return this.autoCompleteGroupName(groupName, "group");
	}

	protected String autoCompleteGroupName(String groupName, String argName) {

		if (groupName.startsWith("#")) {
			return groupName.substring(1);
		}

		List<String> groups = new LinkedList<>();

		for (String group : PermissionsEx.getPermissionManager().getGroupNames()) {
			if (group.equalsIgnoreCase(groupName)) {
				return group;
			}

			if (group.toLowerCase().startsWith(groupName.toLowerCase()) && !groups.contains(group)) {
				groups.add(group);
			}
		}

		if (groups.size() > 1) { // Found several choices
			throw new AutoCompleteChoicesException(groups.toArray(new String[0]), argName);
		} else if (groups.size() == 1) { // Found one name
			return groups.get(0);
		}

		// Nothing found
		return groupName;
	}

	protected String autoCompleteWorldName(String worldName) {
		return this.autoCompleteWorldName(worldName, "world");
	}

	protected String autoCompleteWorldName(String worldName, String argName) {
		if (worldName == null || worldName.isEmpty() || "*".equals(worldName)) {
			return null;
		}

		List<String> worlds = new LinkedList<>();

		for (World world : Bukkit.getServer().getWorlds()) {
			if (world.getName().equalsIgnoreCase(worldName)) {
				return world.getName();
			}

			if (world.getName().toLowerCase().startsWith(worldName.toLowerCase()) && !worlds.contains(world.getName())) {
				worlds.add(world.getName());
			}
		}

		if (worlds.size() > 1) { // Found several choices
			throw new AutoCompleteChoicesException(worlds.toArray(new String[0]), argName);
		} else if (worlds.size() == 1) { // Found one name
			return worlds.get(0);
		}

		return worldName;
	}

	protected String getSafeWorldName(String worldName, PermissionUser user) {
		if (worldName == null) {
			Player player = user.getPlayer();

			if (player != null) {
				worldName = player.getWorld().getName();
			} else {
				worldName = Bukkit.getServer().getWorlds().get(0).getName();
			}
		}

		return worldName;
	}

	protected String autoCompletePermission(PermissionEntity entity, String permission, String worldName) {
		return this.autoCompletePermission(entity, permission, worldName, "permission");
	}

	protected String autoCompletePermission(PermissionEntity entity, String permission, String worldName, String argName) {
		if (permission == null) {
			return null;
		}

		Set<String> permissions = new HashSet<>();
		for (String currentPermission : entity.getPermissions(worldName)) {
			if (currentPermission.equalsIgnoreCase(permission)) {
				return currentPermission;
			}

			if (currentPermission.toLowerCase().startsWith(permission.toLowerCase())) {
				permissions.add(currentPermission);
			}
		}

		if (permissions.size() > 0) {
			String[] permissionArray = permissions.toArray(new String[0]);

			if (permissionArray.length == 1) {
				return permissionArray[0];
			}

			throw new AutoCompleteChoicesException(permissionArray, argName);
		}

		return permission;
	}

	protected int getPosition(String permission, List<String> permissions) {
		try {
			// permission is permission index
			int position = Integer.parseInt(permission) - 1;

			if (position < 0 || position >= permissions.size()) {
				throw new RuntimeException("Wrong permission index specified!");
			}

			return position;
		} catch (NumberFormatException e) {
			// permission is permission text
			for (int i = 0; i < permissions.size(); i++) {
				if (permission.equalsIgnoreCase(permissions.get(i))) {
					return i;
				}
			}
		}

		throw new RuntimeException("Specified permission not found");
	}

	protected String printHierarchy(PermissionGroup parent, String worldName, int level) {
		StringBuilder buffer = new StringBuilder();

		List<PermissionGroup> groups;
		if (parent == null) {
			groups = PermissionsEx.getPermissionManager().getGroupList();
		} else {
			groups = parent.getChildGroups(worldName);
		}

		for (PermissionGroup group : groups) {
			if (parent == null && !group.getParents(worldName).isEmpty()) {
				continue;
			}

			buffer.append(StringUtils.repeat("  ", level)).append(" - ").append(group.getIdentifier()).append("\n");

			// Groups
			buffer.append(printHierarchy(group, worldName, level + 1));

			for (PermissionUser user : group.getUsers(worldName)) {
				buffer.append(StringUtils.repeat("  ", level + 1)).append(" + ").append(describeUser(user)).append("\n");
			}
		}

		return buffer.toString();
	}

	protected String mapPermissions(String worldName, PermissionEntity entity, int level) {
		StringBuilder builder = new StringBuilder();


		int index = 1;
		for (String permission : this.getPermissionsTree(entity, worldName, 0)) {
			if (level > 0) {
				builder.append("   ");
			} else {
				builder.append(index++).append(") ");
			}

			builder.append(permission);
			if (level > 0) {
				builder.append(" (from ").append(entity.getIdentifier()).append(")");
			} else {
				builder.append(" (own)");
			}
			builder.append("\n");
		}

		List<PermissionGroup> parents = entity.getParents(worldName);
		level++; // Just increment level once
		return builder.toString();
	}

	protected List<String> getPermissionsTree(PermissionEntity entity, String world, int level) {
		List<String> permissions = new LinkedList<>();
		Map<String, List<String>> allPermissions = entity.getAllPermissions();

		List<String> worldsPermissions = allPermissions.get(world);
		if (worldsPermissions != null) {
			permissions.addAll(sprintPermissions(world, worldsPermissions));
		}

		for (String parentWorld : PermissionsEx.getPermissionManager().getWorldInheritance(world)) {
			if (parentWorld != null && !parentWorld.isEmpty()) {
				permissions.addAll(getPermissionsTree(entity, parentWorld, level + 1));
			}
		}

		if (level == 0 && world != null && allPermissions.get(null) != null) { // default world permissions
			permissions.addAll(sprintPermissions("common", allPermissions.get(null)));
		}

		return permissions;
	}

	protected List<String> sprintPermissions(String world, List<String> permissions) {
		List<String> permissionList = new LinkedList<>();

		if (permissions == null) {
			return permissionList;
		}

		for (String permission : permissions) {
			permissionList.add(permission + (world != null ? " @" + world : ""));
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
		} catch (NumberFormatException ignore) {
		}

		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException ignore) {
		}

		return value;
	}

	protected void sendMessage(CommandSender sender, String message) {
		for (String messagePart : message.split("\n")) {
			sender.sendMessage(messagePart);
		}
	}
}
