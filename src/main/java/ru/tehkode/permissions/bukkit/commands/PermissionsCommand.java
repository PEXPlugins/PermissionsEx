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
import ru.tehkode.permissions.bukkit.PermissionsPlugin;
import ru.tehkode.permissions.commands.Command;
import ru.tehkode.permissions.commands.CommandListener;
import ru.tehkode.utils.StringUtils;

public class PermissionsCommand implements CommandListener {

    @Command(name = "pex",
    syntax = "reload",
    permission = "permissions.manage.reload",
    description = "Reload permissions")
    public void reload(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionsPlugin.getPermissionManager().reset();

        sender.sendMessage(ChatColor.WHITE + "Permissions reloaded");
    }

    @Command(name = "pex",
    syntax = "backend",
    permission = "permissions.manage.backend",
    description = "Print currently using backend")
    public void getBackend(Plugin plugin, CommandSender sender, Map<String, String> args) {
        sender.sendMessage("Current backend: " + PermissionsPlugin.getPermissionManager().getBackend());
    }

    @Command(name = "pex",
    syntax = "backend <backend>",
    permission = "permissions.manage.backend",
    description = "Change permission backend on the fly (Use with caution!)")
    public void setBackend(Plugin plugin, CommandSender sender, Map<String, String> args) {
        if (args.get("backend") == null) {
            return;
        }

        try {
            PermissionsPlugin.getPermissionManager().setBackend(args.get("backend"));
            sender.sendMessage(ChatColor.WHITE + "Permission backend changed!");
        } catch (RuntimeException e) {
            sender.sendMessage(ChatColor.RED + "Specified backend not found.");
        }
    }

    /**
     * User management
     */
    @Command(name = "pex",
    syntax = "users list",
    permission = "permissions.manage.user",
    description = "List all registred users")
    public void usersList(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser[] users = PermissionsPlugin.getPermissionManager().getUsers();

        sender.sendMessage(ChatColor.WHITE + "Currently registred users: ");
        for (PermissionUser user : users) {
            sender.sendMessage(" " + user.getName() + " " + ChatColor.DARK_GREEN + "[" + StringUtils.implode(user.getGroupsNames(), ", ") + "]");
        }
    }

    @Command(name = "pex",
    syntax = "users",
    permission = "permissions.manage.user",
    description = "List all registred users (alias)")
    public void userListAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.usersList(plugin, sender, args);
    }

    @Command(name = "pex",
    syntax = "user",
    permission = "permissions.manage.user",
    description = "List all registred users (alias)")
    public void userListAnotherAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.usersList(plugin, sender, args);
    }

    /**
     * User's permissions management
     */
    @Command(name = "pex",
    syntax = "user <user>",
    permission = "permissions.manage.user.permissions",
    description = "List user permissions (list alias)")
    public void userListAliasPermissions(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.userListPermissions(plugin, sender, args);
    }

    @Command(name = "pex",
    syntax = "user <user> list [world]",
    permission = "permissions.manage.user.permissions",
    description = "List user permissions")
    public void userListPermissions(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsPlugin.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        sender.sendMessage(args.get("user") + "'s permissions:");
        for (String permission : user.getOwnPermissions(args.get("world"))) {
            sender.sendMessage("  " + permission);
        }

        user.getOptions(args.get("world"));
    }

    @Command(name = "pex",
    syntax = "user <user> add <permission> [world]",
    permission = "permissions.manage.user.permissions",
    description = "Add permission to user")
    public void userAddPermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsPlugin.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        user.addPermission(args.get("permission"), args.get("world"));

        sender.sendMessage(ChatColor.WHITE + "Permission added!");
    }

    @Command(name = "pex",
    syntax = "user <user> set <permission> <value> [world]",
    permission = "permissions.manage.user.permissions",
    description = "Set permission setting to given value")
    public void userSetPermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsPlugin.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        user.setOption(args.get("permission"), args.get("value"), args.get("world"));

        sender.sendMessage(ChatColor.WHITE + "Permission set!");
    }

    @Command(name = "pex",
    syntax = "user <user> remove <permission> [world]",
    permission = "permissions.manage.user.permissions",
    description = "Remove permission from user")
    public void userRemovePermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsPlugin.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        user.removePermission(args.get("permission"), args.get("world"));

        sender.sendMessage(ChatColor.WHITE + "Permission removed!");
    }

    /**
     * User's groups management
     */
    @Command(name = "pex",
    syntax = "user <user> group list",
    permission = "permissions.manage.membership",
    description = "List all user groups")
    public void userListGroup(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsPlugin.getPermissionManager().getUser(args.get("user"));

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
        PermissionUser user = PermissionsPlugin.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        user.addGroup(args.get("group"));

        sender.sendMessage(ChatColor.WHITE + "User added to group!");
    }

    @Command(name = "pex",
    syntax = "user <user> group set <group>",
    permission = "permissions.manage.membership",
    description = "Set leave specified group for user")
    public void userSetGroup(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsPlugin.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        user.setGroups(new PermissionGroup[]{PermissionsPlugin.getPermissionManager().getGroup(args.get("group"))});

        sender.sendMessage(ChatColor.WHITE + "User groups set!");
    }

    @Command(name = "pex",
    syntax = "user <user> group remove <group>",
    permission = "permissions.manage.membership",
    description = "Remove user from specified group")
    public void userRemoveGroup(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsPlugin.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        List<PermissionGroup> groups = new LinkedList<PermissionGroup>();
        for (PermissionGroup group : user.getGroups()) {
            if (!group.getName().equalsIgnoreCase(args.get("group"))) {
                groups.add(group);
            }
        }

        user.setGroups(groups.toArray(new PermissionGroup[0]));

        sender.sendMessage(ChatColor.WHITE + "User removed from group " + args.get("group") + "!");
    }

    /**
     * Group management
     */
    @Command(name = "pex",
    syntax = "groups list",
    permission = "permissions.manage.group",
    description = "List all registred groups")
    public void groupsList(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup[] groups = PermissionsPlugin.getPermissionManager().getGroups();

        sender.sendMessage(ChatColor.WHITE + "Currently registred groups: ");
        for (PermissionGroup group : groups) {
            sender.sendMessage(" " + group.getName() + " " + ChatColor.DARK_GREEN + "[" + StringUtils.implode(group.getParentGroupsNames(), ", ") + "]");
        }
    }

    @Command(name = "pex",
    syntax = "groups",
    permission = "permissions.manage.group",
    description = "List all registred groups (alias)")
    public void groupsListAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.groupsList(plugin, sender, args);
    }

    @Command(name = "pex",
    syntax = "group",
    permission = "permissions.manage.group",
    description = "List all registred groups (alias)")
    public void groupsListAnotherAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.groupsList(plugin, sender, args);
    }

    @Command(name = "pex",
    syntax = "group <group> create [parents]",
    permission = "permissions.manage.group.create",
    description = "List all group permissions (alias)")
    public void groupCreate(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsPlugin.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
            return;
        }

        if (!group.isVirtual()) {
            sender.sendMessage(ChatColor.RED + "Group " + args.get("group") + " are already exists");
            return;
        }

        if (args.get("parents") != null) {
            String[] parents = args.get("parents").split(",");
            List<PermissionGroup> groups = new LinkedList<PermissionGroup>();

            for (String parent : parents) {
                groups.add(PermissionsPlugin.getPermissionManager().getGroup(parent));
            }

            group.setParentGroups(groups.toArray(new PermissionGroup[0]));
        }

        sender.sendMessage(ChatColor.WHITE + "Group " + group.getName() + " created!");

        group.save();
    }

    @Command(name = "pex",
    syntax = "group <group> delete",
    permission = "permissions.manage.group.remove",
    description = "Removes group")
    public void groupDelete(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsPlugin.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
            return;
        }

        sender.sendMessage(ChatColor.WHITE + "Group " + group.getName() + " removed!");

        group.remove();
        PermissionsPlugin.getPermissionManager().resetGroup(group.getName());
        group = null;
    }

    /**
     * Group inheritance
     */
    @Command(name = "pex",
    syntax = "group <group> parents list",
    permission = "permissions.manage.group.inheritance",
    description = "Set parents by comma-separated list")
    public void groupListParents(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsPlugin.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
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
    permission = "permissions.manage.group.inheritance",
    description = "Set parents by comma-separated list")
    public void groupSetParents(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsPlugin.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
            return;
        }

        if (args.get("parents") != null) {
            String[] parents = args.get("parents").split(",");
            List<PermissionGroup> groups = new LinkedList<PermissionGroup>();

            for (String parent : parents) {
                groups.add(PermissionsPlugin.getPermissionManager().getGroup(parent));
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
    permission = "permissions.manage.group.permissions",
    description = "List all group permissions (alias)")
    public void groupListAliasPermissions(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.groupListPermissions(plugin, sender, args);
    }

    @Command(name = "pex",
    syntax = "group <group> list [world]",
    permission = "permissions.manage.group.permissions",
    description = "List all group permissions")
    public void groupListPermissions(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsPlugin.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
            return;
        }

        sender.sendMessage("Group " + args.get("group") + " own permissions:");
        for (String permission : group.getOwnPermissions(args.get("world"))) {
            sender.sendMessage("  " + permission);
        }

        sender.sendMessage("Options: ");
        for(Map.Entry<String, String> option : group.getOptions(args.get("world")).entrySet()){
            sender.sendMessage("  " + option.getKey() + " = " + option.getValue());
        }
    }

    @Command(name = "pex",
    syntax = "group <group> add <permission> [world]",
    permission = "permissions.manage.group.permissions",
    description = "Add permission to group")
    public void groupAddPermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsPlugin.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
            return;
        }

        group.addPermission(args.get("permission"), args.get("world"));

        sender.sendMessage(ChatColor.WHITE + "Permission added to " + group.getName() + " !");
    }

    @Command(name = "pex",
    syntax = "group <group> set <permission> <value> [world]",
    permission = "permissions.manage.group.permissions",
    description = "Set permission value for group")
    public void groupSetPermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsPlugin.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
            return;
        }

        group.setOption(args.get("permission"), args.get("value"), args.get("world"));

        sender.sendMessage(ChatColor.WHITE + "Permission set for " + group.getName() + " !");
    }

    @Command(name = "pex",
    syntax = "group <group> remove <permission> [world]",
    permission = "permissions.manage.group.permissions",
    description = "Remove permission from group")
    public void groupRemovePermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsPlugin.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
            return;
        }

        group.removePermission(args.get("permission"), args.get("world"));

        sender.sendMessage(ChatColor.WHITE + "Permission removed from " + group.getName() + " !");
    }

    /**
     * Group users management
     */
    @Command(name = "pex",
    syntax = "group <group> users",
    permission = "permissions.manage.membership",
    description = "List all group users")
    public void groupUsersList(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser[] users = PermissionsPlugin.getPermissionManager().getUsers(args.get("group"));

        if (users == null || users.length == 0) {
            sender.sendMessage(ChatColor.RED + "No such group found");
        }

        sender.sendMessage("Group " + args.get("group") + " users:");

        for(PermissionUser user : users){
            sender.sendMessage("   " + user.getName());
        }
    }

    @Command(name = "pex",
    syntax = "group <group> user add <user>",
    permission = "permissions.manage.membership",
    description = "Add users (one or comma-separated list) to specified group")
    public void groupUsersAdd(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsPlugin.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such users found");
            return;
        }

        user.addGroup(args.get("group"));

        sender.sendMessage(ChatColor.WHITE + "User " + user.getName() + " added to " + args.get("group") + " !");
    }

    @Command(name = "pex",
    syntax = "group <group> user remove <user>",
    permission = "permissions.manage.membership",
    description = "Add users (one or comma-separated list) to specified group")
    public void groupUsersRemove(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsPlugin.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such users found");
            return;
        }

        user.removeGroup(args.get("group"));

        sender.sendMessage(ChatColor.WHITE + "User " + user.getName() + " removed from " + args.get("group") + " !");
    }

}
