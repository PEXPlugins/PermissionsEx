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
import ru.tehkode.utils.StringUtils;

public class GroupCommands extends PermissionsCommand {

    @Command(name = "pex",
    syntax = "groups list",
    permission = "permissions.manage.groups",
    description = "List all registered groups")
    public void groupsList(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup[] groups = PermissionsEx.getPermissionManager().getGroups();

        sender.sendMessage(ChatColor.WHITE + "Registred groups: ");
        for (PermissionGroup group : groups) {
            String rank = group.getOption("rank");
            if (rank.isEmpty()) {
                rank = "not ranked";
            } else {
                rank += ":" + group.getRankLadder();
            }

            sender.sendMessage(" " + group.getName() + " (" + rank + ") " + ChatColor.DARK_GREEN + "[" + StringUtils.implode(group.getParentGroupsNames(), ", ") + "]");
        }
    }

    @Command(name = "pex",
    syntax = "groups",
    permission = "permissions.manage.groups",
    description = "List all registered groups (alias)")
    public void groupsListAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.groupsList(plugin, sender, args);
    }

    @Command(name = "pex",
    syntax = "group",
    permission = "permissions.manage.groups",
    description = "List all registered groups (alias)")
    public void groupsListAnotherAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.groupsList(plugin, sender, args);
    }

    @Command(name = "pex",
    syntax = "group <group> prefix [newprefix]",
    permission = "permissions.manage.groups",
    description = "Add permission to user")
    public void groupPrefix(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String groupName = this.autoCompleteGroupName(args.get("group"));

        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "Group doesn't exist");
            return;
        }

        if (args.containsKey("newprefix")) {
            group.setPrefix(args.get("newprefix"));
        }

        sender.sendMessage(group.getName() + "'s prefix = \"" + group.getPrefix() + "\"");
    }

    @Command(name = "pex",
    syntax = "group <group> suffix [newsuffix]",
    permission = "permissions.manage.groups",
    description = "Add permission to user")
    public void groupSuffix(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String groupName = this.autoCompleteGroupName(args.get("group"));

        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "Group doesn't exist");
            return;
        }

        if (args.containsKey("newsuffix")) {
            group.setSuffix(args.get("newsuffix"));
        }

        sender.sendMessage(group.getName() + "'s suffix is = \"" + group.getSuffix() + "\"");
    }

    @Command(name = "pex",
    syntax = "group <group> create [parents]",
    permission = "permissions.manage.groups.create",
    description = "List all group permissions (alias)")
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

            group.setParentGroups(groups.toArray(new PermissionGroup[0]));
        }

        sender.sendMessage(ChatColor.WHITE + "Group " + group.getName() + " created!");

        group.save();
    }

    @Command(name = "pex",
    syntax = "group <group> delete",
    permission = "permissions.manage.groups.remove",
    description = "Removes group")
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
    syntax = "group <group> parents list",
    permission = "permissions.manage.groups.inheritance",
    description = "Set parents by comma-separated list")
    public void groupListParents(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String groupName = this.autoCompleteGroupName(args.get("group"));

        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "Group doesn't exist");
            return;
        }

        if (group.getParentGroups().length == 0) {
            sender.sendMessage(ChatColor.RED + "Group " + group.getName() + " doesn't have parents");
            return;
        }

        sender.sendMessage("Group " + group.getName() + " parents:");

        for (PermissionGroup parent : group.getParentGroups()) {
            sender.sendMessage("  " + parent.getName());
        }

    }

    @Command(name = "pex",
    syntax = "group <group> parents set <parents>",
    permission = "permissions.manage.groups.inheritance",
    description = "Set parents by comma-separated list")
    public void groupSetParents(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String groupName = this.autoCompleteGroupName(args.get("group"));

        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "Group doesn't exist");
            return;
        }

        if (args.get("parents") != null) {
            String[] parents = args.get("parents").split(",");
            List<PermissionGroup> groups = new LinkedList<PermissionGroup>();

            for (String parent : parents) {
                groups.add(PermissionsEx.getPermissionManager().getGroup(parent));
            }

            group.setParentGroups(groups.toArray(new PermissionGroup[0]));

            sender.sendMessage(ChatColor.WHITE + "Group " + group.getName() + " inheritance updated!");

            group.save();
        }
    }

    /**
     * Group permissions
     */
    @Command(name = "pex",
    syntax = "group <group>",
    permission = "permissions.manage.groups.permissions",
    description = "List all group permissions (alias)")
    public void groupListAliasPermissions(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.groupListPermissions(plugin, sender, args);
    }

    @Command(name = "pex",
    syntax = "group <group> list [world]",
    permission = "permissions.manage.groups.permissions",
    description = "List all group permissions")
    public void groupListPermissions(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String groupName = this.autoCompleteGroupName(args.get("group"));
        String worldName = this.autoCompleteWorldName(args.get("world"));

        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "Group doesn't exist");
            return;
        }

        sender.sendMessage("Group " + group.getName() + "'s permissions:\n"
                + this.mapPermissions(worldName, group, 0));

        sender.sendMessage("Group " + group.getName() + "'s Options: ");
        for (Map.Entry<String, String> option : group.getOptions(worldName).entrySet()) {
            sender.sendMessage("  " + option.getKey() + " = \"" + option.getValue() + "\"");
        }
    }

    @Command(name = "pex",
    syntax = "group <group> add <permission> [world]",
    permission = "permissions.manage.groups.permissions",
    description = "Add permission to group")
    public void groupAddPermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String groupName = this.autoCompleteGroupName(args.get("group"));
        String worldName = this.autoCompleteWorldName(args.get("world"));

        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "Group doesn't exist");
            return;
        }

        group.addPermission(args.get("permission"), worldName);

        sender.sendMessage(ChatColor.WHITE + "Permission added to " + group.getName() + " !");

        this.informGroup(plugin, group, "Your permissions have been changed");
    }

    @Command(name = "pex",
    syntax = "group <group> set <option> <value> [world]",
    permission = "permissions.manage.groups.permissions",
    description = "Set permission value for group")
    public void groupSetOption(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String groupName = this.autoCompleteGroupName(args.get("group"));
        String worldName = this.autoCompleteWorldName(args.get("world"));

        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "Group doesn't exist");
            return;
        }

        group.setOption(args.get("option"), args.get("value"), worldName);

        sender.sendMessage(ChatColor.WHITE + "Option set!");

        this.informGroup(plugin, group, "Your permissions has been changed");
    }

    @Command(name = "pex",
    syntax = "group <group> remove <permission> [world]",
    permission = "permissions.manage.groups.permissions",
    description = "Remove permission from group")
    public void groupRemovePermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String groupName = this.autoCompleteGroupName(args.get("group"));
        String worldName = this.autoCompleteWorldName(args.get("world"));

        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "Group doesn't exist");
            return;
        }

        group.removePermission(args.get("permission"), worldName);

        sender.sendMessage(ChatColor.WHITE + "Permission removed from " + group.getName() + " !");

        this.informGroup(plugin, group, "Your permissions have been changed");
    }

    /**
     * Group users management
     */
    @Command(name = "pex",
    syntax = "group <group> users",
    permission = "permissions.manage.membership",
    description = "List all users in the specified group")
    public void groupUsersList(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String groupName = this.autoCompleteGroupName(args.get("group"));

        PermissionUser[] users = PermissionsEx.getPermissionManager().getUsers(groupName);

        if (users == null || users.length == 0) {
            sender.sendMessage(ChatColor.RED + "Group doesn't exist");
        }

        sender.sendMessage("Group " + groupName + " users:");

        for (PermissionUser user : users) {
            sender.sendMessage("   " + user.getName());
        }
    }

    @Command(name = "pex",
    syntax = "group <group> user add <user>",
    permission = "permissions.manage.membership",
    description = "Add users (single or comma-separated list) to specified group")
    public void groupUsersAdd(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String groupName = this.autoCompleteGroupName(args.get("group"));
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

            user.addGroup(groupName);

            sender.sendMessage(ChatColor.WHITE + "User " + user.getName() + " added to " + groupName + " !");
            this.informPlayer(plugin, userName, "You are assigned to \"" + groupName + "\" group");
        }
    }

    @Command(name = "pex",
    syntax = "group <group> user remove <user>",
    permission = "permissions.manage.membership",
    description = "Add users (single or comma-separated list) to specified group")
    public void groupUsersRemove(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String groupName = this.autoCompleteGroupName(args.get("group"));
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

            user.removeGroup(groupName);

            sender.sendMessage(ChatColor.WHITE + "User " + user.getName() + " removed from " + args.get("group") + " !");
            this.informPlayer(plugin, userName, "You were removed from \"" + groupName + "\" group");

        }
    }
}
