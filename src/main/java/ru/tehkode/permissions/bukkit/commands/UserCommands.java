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

public class UserCommands extends PermissionsCommand {

    @Command(name = "pex",
    syntax = "users list",
    permission = "permissions.manage.users",
    description = "List all registred users")
    public void usersList(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser[] users = PermissionsEx.getPermissionManager().getUsers();

        sender.sendMessage(ChatColor.WHITE + "Currently registred users: ");
        for (PermissionUser user : users) {
            sender.sendMessage(" " + user.getName() + " " + ChatColor.DARK_GREEN + "[" + StringUtils.implode(user.getGroupsNames(), ", ") + "]");
        }
    }

    @Command(name = "pex",
    syntax = "users",
    permission = "permissions.manage.users",
    description = "List all registred users (alias)",
    isPrimary = true)
    public void userListAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.usersList(plugin, sender, args);
    }

    @Command(name = "pex",
    syntax = "user",
    permission = "permissions.manage.users",
    description = "List all registred users (alias)")
    public void userListAnotherAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.usersList(plugin, sender, args);
    }

    /**
     * User's permissions management
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
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        sender.sendMessage(userName + " are member of:");
        for (PermissionGroup group : user.getGroups()) {
            sender.sendMessage("  " + group.getName());
        }

        sender.sendMessage(userName + "'s permissions:\n"
                + this.mapPermissions(args.get("world"), user, 0));
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
    description = "Add permission to user")
    public void userPrefix(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String userName = this.autoCompletePlayerName(args.get("user"));

        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
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
    description = "Add permission to user")
    public void userSuffix(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String userName = this.autoCompletePlayerName(args.get("user"));

        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
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
    description = "Add permission to user")
    public void userDelete(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String userName = this.autoCompletePlayerName(args.get("user"));
        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
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
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        user.addPermission(args.get("permission"), args.get("world"));

        sender.sendMessage(ChatColor.WHITE + "Permission added!");

        this.informPlayer(plugin, userName, "Your permissions has been changed!");
    }

    @Command(name = "pex",
    syntax = "user <user> set <option> <value> [world]",
    permission = "permissions.manage.users.permissions",
    description = "Set permission setting to given value")
    public void userSetOption(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String userName = this.autoCompletePlayerName(args.get("user"));

        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        user.setOption(args.get("option"), args.get("value"), args.get("world"));

        sender.sendMessage(ChatColor.WHITE + "Option set!");

        this.informPlayer(plugin, userName, "Your permissions has been changed!");
    }

    @Command(name = "pex",
    syntax = "user <user> remove <permission> [world]",
    permission = "permissions.manage.users.permissions",
    description = "Remove permission from user")
    public void userRemovePermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String userName = this.autoCompletePlayerName(args.get("user"));

        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        user.removePermission(args.get("permission"), args.get("world"));

        sender.sendMessage(ChatColor.WHITE + "Permission removed!");
        this.informPlayer(plugin, userName, "Your permissions has been changed!");
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
            sender.sendMessage(ChatColor.RED + "No such user found");
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
            sender.sendMessage(ChatColor.RED + "No such user found");
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
        String userName = this.autoCompletePlayerName(args.get("user"));
        String groupName = this.autoCompleteGroupName(args.get("group"));

        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        user.setGroups(new PermissionGroup[]{PermissionsEx.getPermissionManager().getGroup(groupName)});

        sender.sendMessage(ChatColor.WHITE + "User groups set!");

        this.informPlayer(plugin, userName, "You are now only in \"" + groupName + "\" group");
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
            sender.sendMessage(ChatColor.RED + "No such user found");
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

    @Command(name = "pex",
    syntax = "promote <user> <group>",
    permission = "permissions.manage.membership",
    description = "Remove user from specified group")
    public void userPromote(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.userSetGroup(plugin, sender, args);
    }
    
    @Command(name = "pex",
    syntax = "demote <user> <group>",
    permission = "permissions.manage.membership",
    description = "Remove user from specified group")
    public void userDemote(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.userRemoveGroup(plugin, sender, args);
    }
}
