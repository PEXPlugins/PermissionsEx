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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.commands.Command;
import ru.tehkode.utils.DateUtils;
import ru.tehkode.utils.StringUtils;

public class UserCommands extends PermissionsCommand {

	@Command(name = "pex",
			syntax = "users list",
			permission = "permissions.manage.users",
			description = "List all registered users")
	public void usersList(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		Set<PermissionUser> users = plugin.getPermissionsManager().getUsers();

		sender.sendMessage(ChatColor.WHITE + "Currently registered users: ");
		for (PermissionUser user : users) {
			sender.sendMessage(user.getIdentifier() + ChatColor.GRAY + " (Last known username: " + user.getName() + ") "  + ChatColor.DARK_GREEN + "[" + StringUtils.implode(user.getParentIdentifiers(), ", ") + "]");
		}
	}

	@Command(name = "pex",
			syntax = "users",
			permission = "permissions.manage.users",
			description = "List all registered users (alias)",
			isPrimary = true)
	public void userListAlias(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		this.usersList(plugin, sender, args);
	}

	@Command(name = "pex",
			syntax = "user",
			permission = "permissions.manage.users",
			description = "List all registered users (alias)")
	public void userListAnotherAlias(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		this.usersList(plugin, sender, args);
	}

	/**
	 * User permission management
	 */
	@Command(name = "pex",
			syntax = "user <user>",
			permission = "permissions.manage.users.permissions.<user>",
			description = "List user permissions (list alias)")
	public void userListAliasPermissions(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionUser user = plugin.getPermissionsManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}
		userName = user.getName();

		sender.sendMessage("'" + describeUser(user) + "' is a member of:");
		printEntityInheritance(sender, user.getParents());

		Map<String, List<PermissionGroup>> allParents = user.getAllParents();
		for (String world : allParents.keySet()) {
			if (world == null) {
				continue;
			}

			sender.sendMessage("  @" + world + ":");
			printEntityInheritance(sender, allParents.get(world));
		}

		sender.sendMessage(userName + "'s permissions:");

		this.sendMessage(sender, this.mapPermissions(worldName, user, 0));

		sender.sendMessage(userName + "'s options:");
		for (Map.Entry<String, String> option : user.getOptions(worldName).entrySet()) {
			sender.sendMessage("  " + option.getKey() + " = \"" + option.getValue() + "\"");
		}
	}

	@Command(name = "pex",
			syntax = "user <user> list [world]",
			permission = "permissions.manage.users.permissions.<user>",
			description = "List user permissions")
	public void userListPermissions(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionUser user = plugin.getPermissionsManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}

		sender.sendMessage(user.getName() + "'s permissions:");

		for (String permission : user.getPermissions(worldName)) {
			sender.sendMessage("  " + permission);
		}

	}

	@Command(name = "pex",
			syntax = "user <user> superperms",
			permission = "permissions.manage.users.permissions.<user>",
			description = "List user actual superperms")
	public void userListSuperPermissions(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));

		Player player;
		try {
			UUID uid = UUID.fromString(userName);
			player = plugin.getServer().getPlayer(uid);
		} catch (IllegalArgumentException ex) {
			player = plugin.getServer().getPlayerExact(userName);
		}

		if (player == null) {
			sender.sendMessage(ChatColor.RED + "Player not found (offline?)");
			return;
		}

		sender.sendMessage(player.getName() + "'s superperms:");

		for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
			String pluginName = "built-in";

			if (info.getAttachment() != null && info.getAttachment().getPlugin() != null) {
				pluginName = info.getAttachment().getPlugin().getDescription().getName();
			}

			sender.sendMessage(" '" + ChatColor.GREEN + info.getPermission() + ChatColor.WHITE + "' = " + ChatColor.BLUE + info.getValue() + ChatColor.WHITE + " by " + ChatColor.DARK_GREEN + pluginName);
		}
	}

	@Command(name = "pex",
			syntax = "user <user> prefix [newprefix] [world]",
			permission = "permissions.manage.users.prefix.<user>",
			description = "Get or set <user> prefix")
	public void userPrefix(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionUser user = plugin.getPermissionsManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}

		if (args.containsKey("newprefix")) {
			user.setPrefix(args.get("newprefix"), worldName);
			sender.sendMessage(describeUser(user) + "'s prefix" + (worldName != null ? " (in world \"" + worldName + "\") " : "") + " has been set to \"" + user.getPrefix() + "\"");
		} else {
			sender.sendMessage(describeUser(user) + "'s prefix" + (worldName != null ? " (in world \"" + worldName + "\") " : "") + " is \"" + user.getPrefix() + "\"");
		}
	}

	@Command(name = "pex",
			syntax = "user <user> suffix [newsuffix] [world]",
			permission = "permissions.manage.users.suffix.<user>",
			description = "Get or set <user> suffix")
	public void userSuffix(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionUser user = plugin.getPermissionsManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}

		if (args.containsKey("newsuffix")) {
			user.setSuffix(args.get("newsuffix"), worldName);
			sender.sendMessage(user.getName() + "'s suffix" + (worldName != null ? " (in world \"" + worldName + "\")" : "") + " has been set to \"" + user.getSuffix() + "\"");
		} else {
			sender.sendMessage(user.getName() + "'s suffix" + (worldName != null ? " (in world \"" + worldName + "\")" : "") + " is \"" + user.getSuffix() + "\"");
		}
	}

	@Command(name = "pex",
			syntax = "user <user> toggle debug",
			permission = "permissions.manage.<user>",
			description = "Toggle debug only for <user>")
	public void userToggleDebug(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));

		PermissionUser user = plugin.getPermissionsManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}

		user.setDebug(!user.isDebug());

		sender.sendMessage("Debug mode for user " + describeUser(user) + " " + (user.isDebug() ? "enabled" : "disabled") + "!");
	}

	@Command(name = "pex",
			syntax = "user <user> check <permission> [world]",
			permission = "permissions.manage.<user>",
			description = "Checks player for <permission>")
	public void userCheckPermission(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionUser user = plugin.getPermissionsManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}

		worldName = this.getSafeWorldName(worldName, user);

		String permission = user.getMatchingExpression(args.get("permission"), worldName);

		if (permission == null) {
			sender.sendMessage("Permission \"" + permission + "\" has not been set for \"Player \"" + describeUser(user));
		} else {
			sender.sendMessage("Player \"" + describeUser(user) + "\" " + (user.explainExpression(permission) ? "has" : "doesn't have") + " \"" + permission + "\"");
		}
	}

	@Command(name = "pex",
			syntax = "user <user> get <option> [world]",
			permission = "permissions.manage.<user>",
			description = "Toggle debug only for <user>")
	public void userGetOption(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionUser user = plugin.getPermissionsManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}

		worldName = this.getSafeWorldName(worldName, user);

		String value = user.getOption(args.get("option"), worldName, null);

		sender.sendMessage("Player \"" + describeUser(user) + "\" @ " + worldName + " option \"" + args.get("option") + "\" = \"" + value + "\"");
	}

	@Command(name = "pex",
			syntax = "user <user> delete",
			permission = "permissions.manage.users.<user>",
			description = "Remove <user>")
	public void userDelete(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		PermissionUser user = plugin.getPermissionsManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}

		if (user.isVirtual()) {
			sender.sendMessage(ChatColor.RED + "User \"" + describeUser(user) + "\" is virtual.");
		}

		user.remove();

		plugin.getPermissionsManager().resetUser(user.getIdentifier());

		sender.sendMessage(ChatColor.WHITE + "User \"" + describeUser(user) + "\" removed!");
	}

	@Command(name = "pex",
			syntax = "user <user> add <permission> [world]",
			permission = "permissions.manage.users.permissions.<user>",
			description = "Add <permission> to <user> in [world]")
	public void userAddPermission(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionUser user = plugin.getPermissionsManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}

		user.addPermission(args.get("permission"), worldName);

		sender.sendMessage(ChatColor.WHITE + "Permission \"" + args.get("permission") + "\" added!");

		this.informPlayer(plugin, user, "Your permissions have been changed!");
	}

	@Command(name = "pex",
			syntax = "user <user> remove <permission> [world]",
			permission = "permissions.manage.users.permissions.<user>",
			description = "Remove permission from <user> in [world]")
	public void userRemovePermission(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionUser user = plugin.getPermissionsManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}

		String permission = this.autoCompletePermission(user, args.get("permission"), worldName);

		user.removePermission(permission, worldName);
		user.removeTimedPermission(permission, worldName);

		sender.sendMessage(ChatColor.WHITE + "Permission \"" + permission + "\" removed!");
		this.informPlayer(plugin, user, "Your permissions have been changed!");
	}

	@Command(name = "pex",
			syntax = "user <user> swap <permission> <targetPermission> [world]",
			permission = "permissions.manage.users.permissions.<user>",
			description = "Swap <permission> and <targetPermission> in permission list. Could be number or permission itself")
	public void userSwapPermission(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionUser user = plugin.getPermissionsManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}

		List<String> permissions = user.getOwnPermissions(worldName);

		try {
			int sourceIndex = this.getPosition(this.autoCompletePermission(user, args.get("permission"), worldName, "permission"), permissions);
			int targetIndex = this.getPosition(this.autoCompletePermission(user, args.get("targetPermission"), worldName, "targetPermission"), permissions);

			String targetPermission = permissions.get(targetIndex);

			permissions.set(targetIndex, permissions.get(sourceIndex));
			permissions.set(sourceIndex, targetPermission);

			user.setPermissions(permissions, worldName);

			sender.sendMessage("Permissions swapped!");
		} catch (Throwable e) {
			sender.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
		}
	}

	@Command(name = "pex",
			syntax = "user <user> timed add <permission> [lifetime] [world]",
			permission = "permissions.manage.users.permissions.timed.<user>",
			description = "Add timed <permissions> to <user> for [lifetime] seconds in [world]")
	public void userAddTimedPermission(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		int lifetime = 0;

		if (args.containsKey("lifetime")) {
			lifetime = DateUtils.parseInterval(args.get("lifetime"));
		}

		PermissionUser user = plugin.getPermissionsManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}

		String permission = args.get("permission");

		user.addTimedPermission(permission, worldName, lifetime);

		sender.sendMessage(ChatColor.WHITE + "Timed permission \"" + permission + "\" added!");
		this.informPlayer(plugin, user, "Your permissions have been changed!");

		plugin.getLogger().info("User \"" + userName + "\" get timed permission \"" + args.get("permission") + "\" "
				+ (lifetime > 0 ? "for " + lifetime + " seconds " : " ") + "from " + sender.getName());
	}

	@Command(name = "pex",
			syntax = "user <user> timed remove <permission> [world]",
			permission = "permissions.manage.users.permissions.timed.<user>",
			description = "Remove timed <permission> from <user> in [world]")
	public void userRemoveTimedPermission(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		String worldName = this.autoCompleteWorldName(args.get("world"));
		String permission = args.get("permission");

		PermissionUser user = plugin.getPermissionsManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}

		user.removeTimedPermission(args.get("permission"), worldName);

		sender.sendMessage(ChatColor.WHITE + "Timed permission \"" + permission + "\" removed!");
		this.informPlayer(plugin, user, "Your permissions have been changed!");
	}

	@Command(name = "pex",
			syntax = "user <user> set <option> <value> [world]",
			permission = "permissions.manage.users.permissions.<user>",
			description = "Set <option> to <value> in [world]")
	public void userSetOption(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionUser user = plugin.getPermissionsManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}

		user.setOption(args.get("option"), args.get("value"), worldName);


		if (args.containsKey("value") && args.get("value").isEmpty()) {
			sender.sendMessage(ChatColor.WHITE + "Option \"" + args.get("option") + "\" cleared!");
		} else {
			sender.sendMessage(ChatColor.WHITE + "Option \"" + args.get("option") + "\" set!");
		}

		this.informPlayer(plugin, user, "Your permissions have been changed!");
	}

	/**
	 * User's groups management
	 */
	@Command(name = "pex",
			syntax = "user <user> group list [world]",
			permission = "permissions.manage.membership.<user>",
			description = "List all <user> groups")
	public void userListGroup(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionUser user = plugin.getPermissionsManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}

		sender.sendMessage("User \"" + describeUser(user) + "\" @" + worldName + " currently in:");
		for (PermissionGroup group : user.getParents(worldName)) {
			sender.sendMessage("  " + group.getIdentifier());
		}
	}

	@Command(name = "pex",
			syntax = "user <user> group add <group> [world] [lifetime]",
			permission = "permissions.manage.membership.<group>",
			description = "Add <user> to <group>")
	public void userAddGroup(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionUser user = plugin.getPermissionsManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}

		if (args.containsKey("lifetime")) {
			try {
				int lifetime = DateUtils.parseInterval(args.get("lifetime"));

				user.addGroup(groupName, worldName, lifetime);
			} catch (NumberFormatException e) {
				sender.sendMessage(ChatColor.RED + "Group lifetime should be number!");
				return;
			}

		} else {
			user.addGroup(groupName, worldName);
		}


		sender.sendMessage(ChatColor.WHITE + "User \"" + describeUser(user) + "\" added to group \"" + groupName + "\"!");
		this.informPlayer(plugin, user, "You are assigned to group \"" + groupName + "\"");
	}

	@Command(name = "pex",
			syntax = "user <user> group set <group> [world]",
			permission = "",
			description = "Set <group> for <user>")
	public void userSetGroup(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		PermissionManager manager = plugin.getPermissionsManager();
		String groupName = args.get("group");

		List<PermissionGroup> groups;

		if (groupName.contains(",")) {
			String[] groupsNames = groupName.split(",");
			groups = new ArrayList<>(groupsNames.length);

			for (String addName : groupsNames) {
				if (sender instanceof Player && !manager.has((Player) sender, "permissions.manage.membership." + addName.toLowerCase())) {
					sender.sendMessage(ChatColor.RED + "Don't have enough permission for group " + addName);
					return;
				}

				groups.add(manager.getGroup(this.autoCompleteGroupName(addName)));
			}

		} else {
			groupName = this.autoCompleteGroupName(groupName);

			if (groupName != null) {
				groups = Collections.singletonList(manager.getGroup(groupName));

				if (sender instanceof Player && !manager.has((Player) sender, "permissions.manage.membership." + groupName.toLowerCase())) {
					sender.sendMessage(ChatColor.RED + "Don't have enough permission for group " + groupName);
					return;
				}

			} else {
				sender.sendMessage(ChatColor.RED + "No groups set!");
				return;
			}
		}

		String userName = this.autoCompletePlayerName(args.get("user"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionUser user = manager.getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}


		if (!groups.isEmpty()) {
			user.setParents(groups, worldName);
			sender.sendMessage(ChatColor.WHITE + "User groups set!");
		} else {
			sender.sendMessage(ChatColor.RED + "No groups set!");
		}

		this.informPlayer(plugin, user, "You are now only in \"" + groupName + "\" group");
	}

	@Command(name = "pex",
			syntax = "user <user> group remove <group> [world]",
			permission = "permissions.manage.membership.<group>",
			description = "Remove <user> from <group>")
	public void userRemoveGroup(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionUser user = plugin.getPermissionsManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "User \"" + userName + "\" doesn't exist.");
			return;
		}

		user.removeGroup(groupName, worldName);

		sender.sendMessage(ChatColor.WHITE + "User \"" + describeUser(user) + "\" removed from group \"" + groupName + "\"!");

		this.informPlayer(plugin, user, "You were removed from \"" + groupName + "\" group");
	}

	@Command(name = "pex",
			syntax = "users cleanup <group> [threshold]",
			permission = "permissions.manage.users.cleanup",
			description = "Clean users of specified group, which last login was before threshold (in days). By default threshold is 30 days.")
	public void usersCleanup(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		long threshold = 2304000;

		PermissionGroup group = plugin.getPermissionsManager().getGroup(args.get("group"));

		if (args.containsKey("threshold")) {
			try {
				threshold = Integer.parseInt(args.get("threshold")) * 86400; // 86400 - seconds in one day
			} catch (NumberFormatException e) {
				sender.sendMessage(ChatColor.RED + "Threshold should be number (in days)");
				return;
			}
		}

		int removed = 0;

		Long deadline = (System.currentTimeMillis() / 1000L) - threshold;
		for (PermissionUser user : group.getUsers()) {
			int lastLogin = user.getOwnOptionInteger("last-login-time", null, 0);

			if (lastLogin > 0 && lastLogin < deadline) {
				user.remove();
				removed++;
			}
		}

		sender.sendMessage("Cleaned " + removed + " users");
	}
}
