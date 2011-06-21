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
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.commands.Command;
import ru.tehkode.utils.StringUtils;

public class UserCommands extends PermissionsCommand {

    @Command(name = "pex",
    syntax = "users list",
    permission = "permissions.manage.users",
    description = "List all registered users")
    public void usersList(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser[] users = PermissionsEx.getPermissionManager().getUsers();

        sender.sendMessage(ChatColor.WHITE + "Currently registered users: ");
        for (PermissionUser user : users) {
            sender.sendMessage(" " + user.getName() + " " + ChatColor.DARK_GREEN + "[" + StringUtils.implode(user.getGroupsNames(), ", ") + "]");
        }
    }

    @Command(name = "pex",
    syntax = "users",
    permission = "permissions.manage.users",
    description = "List all registered users (alias)",
    isPrimary = true)
    public void userListAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.usersList(plugin, sender, args);
    }

    @Command(name = "pex",
    syntax = "user",
    permission = "permissions.manage.users",
    description = "List all registered users (alias)")
    public void userListAnotherAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.usersList(plugin, sender, args);
    }

    /**
     * User permission management
     */
    @Command(name = "pex",
    syntax = "user <user>",
    permission = "permissions.manage.users.permissions",
    description = "List user permissions (list alias)")
    public void userListAliasPermissions(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.userListPermissions(plugin, sender, args);
    }

    @Command(name = "pex",
    syntax = "user <user> list [world]",
    permission = "permissions.manage.users.permissions",
    description = "List user permissions")
    public void userListPermissions(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String userName = this.autoCompletePlayerName(args.get("user"));

        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "User does not exist");
            return;
        }

        sender.sendMessage(userName + " are member of:");
        for (PermissionGroup group : user.getGroups()) {
            String rank = "not ranked";
            if (group.isRanked()) {
                rank = "rank " + group.getRank() + " @ " + group.getRankLadder();
            }

            sender.sendMessage("  " + group.getName() + " (" + rank + ")");
        }

        sender.sendMessage(userName + "'s permissions:");

        this.sendMessage(sender, this.mapPermissions(args.get("world"), user, 0));
        /*
        for (String permission : user.getOwnPermissions(args.get("world"))) {
        sender.sendMessage("  " + permission);
        }
         */

        sender.sendMessage(userName + "'s options:");
        for (Map.Entry<String, String> option : user.getOptions(args.get("world")).entrySet()) {
            sender.sendMessage("  " + option.getKey() + " = \"" + option.getValue() + "\"");
        }

    }

    @Command(name = "pex",
    syntax = "user <user> prefix [newprefix]",
    permission = "permissions.manage.users",
    description = "Change user prefix")
    public void userPrefix(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String userName = this.autoCompletePlayerName(args.get("user"));

        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "User does not exist");
            return;
        }

        if (args.containsKey("newprefix")) {
            user.setPrefix(args.get("newprefix"));
        }

        sender.sendMessage(user.getName() + "'s prefix = \"" + user.getPrefix() + "\"");
    }

    @Command(name = "pex",
    syntax = "user <user> suffix [newsuffix]",
    permission = "permissions.manage.users",
    description = "Change user suffix")
    public void userSuffix(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String userName = this.autoCompletePlayerName(args.get("user"));

        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "User does not exist");
            return;
        }

        if (args.containsKey("newsuffix")) {
            user.setSuffix(args.get("newsuffix"));
        }

        sender.sendMessage(user.getName() + "'s suffix = \"" + user.getSuffix() + "\"");
    }

    @Command(name = "pex",
    syntax = "user <user> delete",
    permission = "permissions.manage.users",
    description = "Resets user permissions")
    public void userDelete(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String userName = this.autoCompletePlayerName(args.get("user"));
        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "User does not exist");
            return;
        }

        if (user.isVirtual()) {
            sender.sendMessage(ChatColor.RED + "User is virtual");
        }

        user.remove();

        sender.sendMessage(ChatColor.WHITE + "User \"" + user.getName() + "\" removed!");
    }

    @Command(name = "pex",
    syntax = "user <user> add <permission> [world]",
    permission = "permissions.manage.users.permissions",
    description = "Add permission to user")
    public void userAddPermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String userName = this.autoCompletePlayerName(args.get("user"));

        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "User does not exist");
            return;
        }

        user.addPermission(args.get("permission"), args.get("world"));

        sender.sendMessage(ChatColor.WHITE + "Permission added!");

        this.informPlayer(plugin, userName, "Your permissions have been changed!");
    }

    @Command(name = "pex",
    syntax = "user <user> set <option> <value> [world]",
    permission = "permissions.manage.users.permissions",
    description = "Set permission setting to given value")
    public void userSetOption(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String userName = this.autoCompletePlayerName(args.get("user"));

        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "User does not exist");
            return;
        }

        user.setOption(args.get("option"), args.get("value"), args.get("world"));

        sender.sendMessage(ChatColor.WHITE + "Option set!");

        this.informPlayer(plugin, userName, "Your permissions have been changed!");
    }

    @Command(name = "pex",
    syntax = "user <user> remove <permission> [world]",
    permission = "permissions.manage.users.permissions",
    description = "Remove permission from user")
    public void userRemovePermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String userName = this.autoCompletePlayerName(args.get("user"));

        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "User does not exist");
            return;
        }

        user.removePermission(args.get("permission"), args.get("world"));

        sender.sendMessage(ChatColor.WHITE + "Permission removed!");
        this.informPlayer(plugin, userName, "Your permissions have been changed!");
    }

    /**
     * User's groups management
     */
    @Command(name = "pex",
    syntax = "user <user> group list",
    permission = "permissions.manage.membership",
    description = "List all user groups")
    public void userListGroup(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String userName = this.autoCompletePlayerName(args.get("user"));

        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "User does not exist");
            return;
        }

        sender.sendMessage("User " + args.get("user") + " currently in:");
        for (PermissionGroup group : user.getGroups()) {
            sender.sendMessage("  " + group.getName());
        }
    }

    @Command(name = "pex",
    syntax = "user <user> group add <group>",
    permission = "permissions.manage.membership",
    description = "Add user to specified group")
    public void userAddGroup(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String userName = this.autoCompletePlayerName(args.get("user"));
        String groupName = this.autoCompleteGroupName(args.get("group"));

        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "User does not exist");
            return;
        }

        user.addGroup(groupName);

        sender.sendMessage(ChatColor.WHITE + "User added to group!");

        this.informPlayer(plugin, userName, "You are assigned to \"" + groupName + "\" group");
    }

    @Command(name = "pex",
    syntax = "user <user> group set <group>",
    permission = "permissions.manage.membership",
    description = "Set specified group for user")
    public void userSetGroup(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionManager manager = PermissionsEx.getPermissionManager();

        PermissionUser user = manager.getUser(this.autoCompletePlayerName(args.get("user")));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "User does not exist");
            return;
        }

        String groupName = args.get("group");

        PermissionGroup[] groups;

        if (groupName.contains(",")) {
            String[] groupsNames = groupName.split(",");
            groups = new PermissionGroup[groupsNames.length];

            for (int i = 0; i < groupsNames.length; i++) {
                groups[i] = manager.getGroup(groupsNames[i]);
            }

        } else {
            groupName = this.autoCompleteGroupName(groupName);
            groups = new PermissionGroup[]{manager.getGroup(groupName)};
        }

        user.setGroups(groups);

        sender.sendMessage(ChatColor.WHITE + "User groups set!");

        this.informPlayer(plugin, user.getName(), "You are now only in \"" + groupName + "\" group");
    }

    @Command(name = "pex",
    syntax = "user <user> group remove <group>",
    permission = "permissions.manage.membership",
    description = "Remove user from specified group")
    public void userRemoveGroup(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String userName = this.autoCompletePlayerName(args.get("user"));
        String groupName = this.autoCompleteGroupName(args.get("group"));

        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "User does not exist");
            return;
        }

        List<PermissionGroup> groups = new LinkedList<PermissionGroup>();
        for (PermissionGroup group : user.getGroups()) {
            if (!group.getName().equalsIgnoreCase(groupName)) {
                groups.add(group);
            }
        }

        user.setGroups(groups.toArray(new PermissionGroup[0]));

        sender.sendMessage(ChatColor.WHITE + "User removed from group " + groupName + "!");

        this.informPlayer(plugin, userName, "You were removed from \"" + groupName + "\" group");
    }
}
