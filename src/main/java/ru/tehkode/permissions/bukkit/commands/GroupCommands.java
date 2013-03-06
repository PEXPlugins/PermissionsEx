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

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.commands.Command;
import ru.tehkode.utils.DateUtils;
import ru.tehkode.utils.StringUtils;

public class GroupCommands extends PermissionsCommand {

	@Command(name = "pex",
			syntax = "groups list [world]",
			permission = "permissions.manage.groups.list",
			description = "List all registered groups")
	public void groupsList(Plugin plugin, CommandSender sender, Map<String, String> args) {
		PermissionGroup[] groups = PermissionsEx.getPermissionManager().getGroups();
		String worldName = this.autoCompleteWorldName(args.get("world"));

		sender.sendMessage(ChatColor.WHITE + "Registered groups: ");
		for (PermissionGroup group : groups) {
			String rank = "";
			if (group.isRanked()) {
				rank = " (rank: " + group.getRank() + "@" + group.getRankLadder() + ") ";
			}

			sender.sendMessage(String.format("  %s %s %s %s[%s]", group.getName(), " #" + group.getWeight(), rank, ChatColor.DARK_GREEN, StringUtils.implode(group.getParentGroupsNames(worldName), ", ")));
		}
	}

	@Command(name = "pex",
			syntax = "groups",
			permission = "permissions.manage.groups.list",
			description = "List all registered groups (alias)")
	public void groupsListAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
		this.groupsList(plugin, sender, args);
	}

	@Command(name = "pex",
			syntax = "group",
			permission = "permissions.manage.groups.list",
			description = "List all registered groups (alias)")
	public void groupsListAnotherAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
		this.groupsList(plugin, sender, args);
	}

	@Command(name = "pex",
			syntax = "group <group> weight [weight]",
			permission = "permissions.manage.groups.weight.<group>",
			description = "Print or set group weight")
	public void groupPrintSetWeight(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group doesn't exist");
			return;
		}

		if (args.containsKey("weight")) {
			try {
				group.setWeight(Integer.parseInt(args.get("weight")));
			} catch (NumberFormatException e) {
				sender.sendMessage("Error! Weight should be integer value.");
				return;
			}
		}

		sender.sendMessage("Group " + group.getName() + " have " + group.getWeight() + " calories.");
	}

	@Command(name = "pex",
			syntax = "group <group> toggle debug",
			permission = "permissions.manage.groups.debug.<group>",
			description = "Toggle debug mode for group")
	public void groupToggleDebug(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group doesn't exist");
			return;
		}

		group.setDebug(!group.isDebug());

		sender.sendMessage("Debug mode for group " + group.getName() + " have been " + (group.isDebug() ? "enabled" : "disabled") + "!");
	}

	@Command(name = "pex",
			syntax = "group <group> prefix [newprefix] [world]",
			permission = "permissions.manage.groups.prefix.<group>",
			description = "Get or set <group> prefix.")
	public void groupPrefix(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group doesn't exist");
			return;
		}

		if (args.containsKey("newprefix")) {
			group.setPrefix(args.get("newprefix"), worldName);
		}

		sender.sendMessage(group.getName() + "'s prefix = \"" + group.getPrefix(worldName) + "\"");
	}

	@Command(name = "pex",
			syntax = "group <group> suffix [newsuffix] [world]",
			permission = "permissions.manage.groups.suffix.<group>",
			description = "Get or set <group> suffix")
	public void groupSuffix(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group doesn't exist");
			return;
		}

		if (args.containsKey("newsuffix")) {
			group.setSuffix(args.get("newsuffix"), worldName);
		}

		sender.sendMessage(group.getName() + "'s suffix is = \"" + group.getSuffix(worldName) + "\"");
	}

	@Command(name = "pex",
			syntax = "group <group> create [parents]",
			permission = "permissions.manage.groups.create.<group>",
			description = "Create <group> and/or set [parents]")
	public void groupCreate(Plugin plugin, CommandSender sender, Map<String, String> args) {
		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group doesn't exist");
			return;
		}

		if (!group.isVirtual()) {
			sender.sendMessage(ChatColor.RED + "Group " + args.get("group") + " already exists");
			return;
		}

		if (args.get("parents") != null) {
			String[] parents = args.get("parents").split(",");
			List<PermissionGroup> groups = new LinkedList<PermissionGroup>();

			for (String parent : parents) {
				groups.add(PermissionsEx.getPermissionManager().getGroup(parent));
			}

			group.setParentGroups(groups.toArray(new PermissionGroup[0]), null);
		}

		sender.sendMessage(ChatColor.WHITE + "Group " + group.getName() + " created!");

		group.save();
	}

	@Command(name = "pex",
			syntax = "group <group> delete",
			permission = "permissions.manage.groups.remove.<group>",
			description = "Remove <group>")
	public void groupDelete(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group doesn't exist");
			return;
		}

		sender.sendMessage(ChatColor.WHITE + "Group " + group.getName() + " removed!");

		group.remove();
		PermissionsEx.getPermissionManager().resetGroup(group.getName());
		group = null;
	}

	/**
	 * Group inheritance
	 */
	@Command(name = "pex",
			syntax = "group <group> parents [world]",
			permission = "permissions.manage.groups.inheritance.<group>",
			description = "List parents for <group> (alias)")
	public void groupListParentsAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
		this.groupListParents(plugin, sender, args);
	}

	@Command(name = "pex",
			syntax = "group <group> parents list [world]",
			permission = "permissions.manage.groups.inheritance.<group>",
			description = "List parents for <group>")
	public void groupListParents(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group doesn't exist");
			return;
		}

		if (group.getParentGroups(worldName).length == 0) {
			sender.sendMessage(ChatColor.RED + "Group " + group.getName() + " doesn't have parents");
			return;
		}

		sender.sendMessage("Group " + group.getName() + " parents:");

		for (PermissionGroup parent : group.getParentGroups(worldName)) {
			sender.sendMessage("  " + parent.getName());
		}

	}

	@Command(name = "pex",
			syntax = "group <group> parents set <parents> [world]",
			permission = "permissions.manage.groups.inheritance.<group>",
			description = "Set parent(s) for <group> (single or comma-separated list)")
	public void groupSetParents(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group doesn't exist");
			return;
		}

		if (args.get("parents") != null) {
			String[] parents = args.get("parents").split(",");
			List<PermissionGroup> groups = new LinkedList<PermissionGroup>();

			for (String parent : parents) {
				PermissionGroup parentGroup = PermissionsEx.getPermissionManager().getGroup(this.autoCompleteGroupName(parent));

				if (parentGroup != null && !groups.contains(parentGroup)) {
					groups.add(parentGroup);
				}
			}

			group.setParentGroups(groups.toArray(new PermissionGroup[0]), worldName);

			sender.sendMessage(ChatColor.WHITE + "Group " + group.getName() + " inheritance updated!");

			group.save();
		}
	}

	@Command(name = "pex",
			syntax = "group <group> parents add <parents> [world]",
			permission = "permissions.manage.groups.inheritance.<group>",
			description = "Set parent(s) for <group> (single or comma-separated list)")
	public void groupAddParents(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group doesn't exist");
			return;
		}

		if (args.get("parents") != null) {
			String[] parents = args.get("parents").split(",");
			List<PermissionGroup> groups = new LinkedList<PermissionGroup>(Arrays.asList(group.getParentGroups(worldName)));

			for (String parent : parents) {
				PermissionGroup parentGroup = PermissionsEx.getPermissionManager().getGroup(this.autoCompleteGroupName(parent));

				if (parentGroup != null && !groups.contains(parentGroup)) {
					groups.add(parentGroup);
				}
			}

			group.setParentGroups(groups.toArray(new PermissionGroup[0]), worldName);

			sender.sendMessage(ChatColor.WHITE + "Group " + group.getName() + " inheritance updated!");

			group.save();
		}
	}

	@Command(name = "pex",
			syntax = "group <group> parents remove <parents> [world]",
			permission = "permissions.manage.groups.inheritance.<group>",
			description = "Set parent(s) for <group> (single or comma-separated list)")
	public void groupRemoveParents(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group doesn't exist");
			return;
		}

		if (args.get("parents") != null) {
			String[] parents = args.get("parents").split(",");
			List<PermissionGroup> groups = new LinkedList<PermissionGroup>(Arrays.asList(group.getParentGroups(worldName)));

			for (String parent : parents) {
				PermissionGroup parentGroup = PermissionsEx.getPermissionManager().getGroup(this.autoCompleteGroupName(parent));

				groups.remove(parentGroup);
			}

			group.setParentGroups(groups.toArray(new PermissionGroup[groups.size()]), worldName);

			sender.sendMessage(ChatColor.WHITE + "Group " + group.getName() + " inheritance updated!");

			group.save();
		}
	}

	/**
	 * Group permissions
	 */
	@Command(name = "pex",
			syntax = "group <group>",
			permission = "permissions.manage.groups.permissions.<group>",
			description = "List all <group> permissions (alias)")
	public void groupListAliasPermissions(Plugin plugin, CommandSender sender, Map<String, String> args) {
		this.groupListPermissions(plugin, sender, args);
	}

	@Command(name = "pex",
			syntax = "group <group> list [world]",
			permission = "permissions.manage.groups.permissions.<group>",
			description = "List all <group> permissions in [world]")
	public void groupListPermissions(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group doesn't exist");
			return;
		}

		sender.sendMessage("'" + groupName + "' inherits the following groups:");
		printEntityInheritance(sender, group.getParentGroups());

		for (String world : group.getAllParentGroups().keySet()) {
			if (world == null) {
				continue;
			}

			sender.sendMessage("  @" + world + ":");
			printEntityInheritance(sender, group.getAllParentGroups().get(world));
		}

		sender.sendMessage("Group " + group.getName() + "'s permissions:");
		this.sendMessage(sender, this.mapPermissions(worldName, group, 0));

		sender.sendMessage("Group " + group.getName() + "'s Options: ");
		for (Map.Entry<String, String> option : group.getOptions(worldName).entrySet()) {
			sender.sendMessage("  " + option.getKey() + " = \"" + option.getValue() + "\"");
		}
	}

	@Command(name = "pex",
			syntax = "group <group> add <permission> [world]",
			permission = "permissions.manage.groups.permissions.<group>",
			description = "Add <permission> to <group> in [world]")
	public void groupAddPermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group doesn't exist");
			return;
		}

		group.addPermission(args.get("permission"), worldName);

		sender.sendMessage(ChatColor.WHITE + "Permission \"" + args.get("permission") + "\" added to " + group.getName() + " !");

		this.informGroup(plugin, group, "Your permissions have been changed");
	}

	@Command(name = "pex",
			syntax = "group <group> set <option> <value> [world]",
			permission = "permissions.manage.groups.permissions.<group>",
			description = "Set <option> <value> for <group> in [world]")
	public void groupSetOption(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group doesn't exist");
			return;
		}

		group.setOption(args.get("option"), args.get("value"), worldName);

		if (args.containsKey("value") && args.get("value").isEmpty()) {
			sender.sendMessage(ChatColor.WHITE + "Option \"" + args.get("option") + "\" cleared!");
		} else {
			sender.sendMessage(ChatColor.WHITE + "Option \"" + args.get("option") + "\" set!");
		}

		this.informGroup(plugin, group, "Your permissions has been changed");
	}

	@Command(name = "pex",
			syntax = "group <group> remove <permission> [world]",
			permission = "permissions.manage.groups.permissions.<group>",
			description = "Remove <permission> from <group> in [world]")
	public void groupRemovePermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group doesn't exist");
			return;
		}

		String permission = this.autoCompletePermission(group, args.get("permission"), worldName);

		group.removePermission(permission, worldName);
		group.removeTimedPermission(permission, worldName);

		sender.sendMessage(ChatColor.WHITE + "Permission \"" + permission + "\" removed from " + group.getName() + " !");

		this.informGroup(plugin, group, "Your permissions have been changed");
	}

	@Command(name = "pex",
			syntax = "group <group> swap <permission> <targetPermission> [world]",
			permission = "permissions.manage.groups.permissions.<group>",
			description = "Swap <permission> and <targetPermission> in permission list. Could be number or permission itself")
	public void userSwapPermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group doesn't exist");
			return;
		}


		String[] permissions = group.getOwnPermissions(worldName);

		try {
			int sourceIndex = this.getPosition(this.autoCompletePermission(group, args.get("permission"), worldName, "permission"), permissions);
			int targetIndex = this.getPosition(this.autoCompletePermission(group, args.get("targetPermission"), worldName, "targetPermission"), permissions);

			String targetPermission = permissions[targetIndex];

			permissions[targetIndex] = permissions[sourceIndex];
			permissions[sourceIndex] = targetPermission;

			group.setPermissions(permissions, worldName);

			sender.sendMessage("Permissions swapped!");
		} catch (Throwable e) {
			sender.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
		}
	}

	@Command(name = "pex",
			syntax = "group <group> timed add <permission> [lifetime] [world]",
			permission = "permissions.manage.groups.permissions.timed.<group>",
			description = "Add timed <permission> to <group> with [lifetime] in [world]")
	public void groupAddTimedPermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		int lifetime = 0;

		if (args.containsKey("lifetime")) {
			lifetime = DateUtils.parseInterval(args.get("lifetime"));
		}

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group does not exist");
			return;
		}

		group.addTimedPermission(args.get("permission"), worldName, lifetime);

		sender.sendMessage(ChatColor.WHITE + "Timed permission added!");
		this.informGroup(plugin, group, "Your permissions have been changed!");

		logger.info("Group " + groupName + " get timed permission \"" + args.get("permission") + "\" "
				+ (lifetime > 0 ? "for " + lifetime + " seconds " : " ") + "from " + getSenderName(sender));
	}

	@Command(name = "pex",
			syntax = "group <group> timed remove <permission> [world]",
			permission = "permissions.manage.groups.permissions.timed.<group>",
			description = "Remove timed <permissions> for <group> in [world]")
	public void groupRemoveTimedPermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group does not exist");
			return;
		}

		group.removeTimedPermission(args.get("permission"), worldName);

		sender.sendMessage(ChatColor.WHITE + "Timed permission \"" + args.get("permission") + "\" removed!");
		this.informGroup(plugin, group, "Your permissions have been changed!");
	}

	/**
	 * Group users management
	 */
	@Command(name = "pex",
			syntax = "group <group> users",
			permission = "permissions.manage.membership.<group>",
			description = "List all users in <group>")
	public void groupUsersList(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));

		PermissionUser[] users = PermissionsEx.getPermissionManager().getUsers(groupName);

		if (users == null || users.length == 0) {
			sender.sendMessage(ChatColor.RED + "Group doesn't exist or empty");
		}

		sender.sendMessage("Group " + groupName + " users:");

		for (PermissionUser user : users) {
			sender.sendMessage("   " + user.getName());
		}
	}

	@Command(name = "pex",
			syntax = "group <group> user add <user> [world]",
			permission = "permissions.manage.membership.<group>",
			description = "Add <user> (single or comma-separated list) to <group>")
	public void groupUsersAdd(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		String users[];

		if (!args.get("user").contains(",")) {
			users = new String[]{args.get("user")};
		} else {
			users = args.get("user").split(",");
		}

		for (String userName : users) {
			userName = this.autoCompletePlayerName(userName);
			PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

			if (user == null) {
				sender.sendMessage(ChatColor.RED + "User does not exist");
				return;
			}

			user.addGroup(groupName, worldName);

			sender.sendMessage(ChatColor.WHITE + "User " + user.getName() + " added to " + groupName + " !");
			this.informPlayer(plugin, userName, "You are assigned to \"" + groupName + "\" group");
		}
	}

	@Command(name = "pex",
			syntax = "group <group> user remove <user> [world]",
			permission = "permissions.manage.membership.<group>",
			description = "Add <user> (single or comma-separated list) to <group>")
	public void groupUsersRemove(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		String users[];

		if (!args.get("user").contains(",")) {
			users = new String[]{args.get("user")};
		} else {
			users = args.get("user").split(",");
		}

		for (String userName : users) {
			userName = this.autoCompletePlayerName(userName);
			PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

			if (user == null) {
				sender.sendMessage(ChatColor.RED + "User does not exist");
				return;
			}

			user.removeGroup(groupName, worldName);

			sender.sendMessage(ChatColor.WHITE + "User " + user.getName() + " removed from " + args.get("group") + " !");
			this.informPlayer(plugin, userName, "You were removed from \"" + groupName + "\" group");

		}
	}

	@Command(name = "pex",
			syntax = "default group [world]",
			permission = "permissions.manage.groups.inheritance",
			description = "Print default group for specified world")
	public void groupDefaultCheck(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String worldName = this.autoCompleteWorldName(args.get("world"));


		PermissionGroup defaultGroup = PermissionsEx.getPermissionManager().getDefaultGroup(worldName);
		sender.sendMessage("Default group in " + worldName + " world is " + defaultGroup.getName() + " group");
	}

	@Command(name = "pex",
			syntax = "set default group <group> [world]",
			permission = "permissions.manage.groups.inheritance",
			description = "Set default group for specified world")
	public void groupDefaultSet(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));
		String worldName = this.autoCompleteWorldName(args.get("world"));

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

		if (group == null || group.isVirtual()) {
			sender.sendMessage(ChatColor.RED + "Specified group doesn't exist");
			return;
		}

		PermissionsEx.getPermissionManager().setDefaultGroup(group, worldName);
		sender.sendMessage("New default group in " + worldName + " world is " + group.getName() + " group");
	}
}
