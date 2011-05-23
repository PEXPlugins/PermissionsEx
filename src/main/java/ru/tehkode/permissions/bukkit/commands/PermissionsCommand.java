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
import ru.tehkode.permissions.PermissionEntity;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.commands.Command;
import ru.tehkode.permissions.commands.CommandListener;
import ru.tehkode.utils.StringUtils;

public class PermissionsCommand implements CommandListener {

    @Command(name = "pex",
    syntax = "reload",
    permission = "permissions.manage.reload",
    description = "Reload permissions")
    public void reload(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionsEx.getPermissionManager().reset();

        sender.sendMessage(ChatColor.WHITE + "Permissions reloaded");
    }

    @Command(name = "pex",
    syntax = "backend",
    permission = "permissions.manage.backend",
    description = "Print currently using backend")
    public void getBackend(Plugin plugin, CommandSender sender, Map<String, String> args) {
        sender.sendMessage("Current backend: " + PermissionsEx.getPermissionManager().getBackend());
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
            PermissionsEx.getPermissionManager().setBackend(args.get("backend"));
            sender.sendMessage(ChatColor.WHITE + "Permission backend changed!");
        } catch (RuntimeException e) {
            sender.sendMessage(ChatColor.RED + "Specified backend not found.");
        }
    }

    @Command(name = "pex",
    syntax = "hierarchy",
    permission = "permissions.manage.users",
    description = "Print complete user/group hierarhy")
    public void printHierarhy(Plugin plugin, CommandSender sender, Map<String, String> args) {
        sender.sendMessage("Permission Inheritance Hierarhy:\n" + this.printHierarhy(null, 0));
    }

    /**
     * User management
     */
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
    description = "List all registred users (alias)")
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
        PermissionUser user = PermissionsEx.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        sender.sendMessage(args.get("user") + " are member of:");
        for (PermissionGroup group : user.getGroups()) {
            sender.sendMessage("  " + group.getName());
        }

        sender.sendMessage(args.get("user") + "'s permissions:\n"
                + this.mapPermissions(args.get("world"), user, 0));
        /*
        for (String permission : user.getOwnPermissions(args.get("world"))) {
        sender.sendMessage("  " + permission);
        }
         */

        sender.sendMessage(args.get("user") + "'s options:");
        for (Map.Entry<String, String> option : user.getOptions(args.get("world")).entrySet()) {
            sender.sendMessage("  " + option.getKey() + " = \"" + option.getValue() + "\"");
        }

    }

    @Command(name = "pex",
    syntax = "user <user> prefix [newprefix]",
    permission = "permissions.manage.users",
    description = "Add permission to user")
    public void userPrefix(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsEx.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        if (args.containsKey("newprefix")) {
            user.setPrefix(args.get("newprefix"));
        }

        sender.sendMessage(user.getName() + "'s prefix is: " + user.getPrefix());
    }

    @Command(name = "pex",
    syntax = "user <user> suffix [newsuffix]",
    permission = "permissions.manage.users",
    description = "Add permission to user")
    public void userSuffix(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsEx.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        if (args.containsKey("newsuffix")) {
            user.setSuffix(args.get("newsuffix"));
        }

        sender.sendMessage(user.getName() + "'s suffix is: " + user.getSuffix());
    }


    @Command(name = "pex",
    syntax = "user <user> add <permission> [world]",
    permission = "permissions.manage.users.permissions",
    description = "Add permission to user")
    public void userAddPermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsEx.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        user.addPermission(args.get("permission"), args.get("world"));

        sender.sendMessage(ChatColor.WHITE + "Permission added!");
    }

    @Command(name = "pex",
    syntax = "user <user> set <option> <value> [world]",
    permission = "permissions.manage.users.permissions",
    description = "Set permission setting to given value")
    public void userSetOption(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsEx.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        user.setOption(args.get("permission"), args.get("value"), args.get("world"));

        sender.sendMessage(ChatColor.WHITE + "Option set!");
    }

    @Command(name = "pex",
    syntax = "user <user> remove <permission> [world]",
    permission = "permissions.manage.users.permissions",
    description = "Remove permission from user")
    public void userRemovePermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsEx.getPermissionManager().getUser(args.get("user"));

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
        PermissionUser user = PermissionsEx.getPermissionManager().getUser(args.get("user"));

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
        PermissionUser user = PermissionsEx.getPermissionManager().getUser(args.get("user"));

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
        PermissionUser user = PermissionsEx.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        user.setGroups(new PermissionGroup[]{PermissionsEx.getPermissionManager().getGroup(args.get("group"))});

        sender.sendMessage(ChatColor.WHITE + "User groups set!");
    }

    @Command(name = "pex",
    syntax = "user <user> group remove <group>",
    permission = "permissions.manage.membership",
    description = "Remove user from specified group")
    public void userRemoveGroup(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsEx.getPermissionManager().getUser(args.get("user"));

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
    permission = "permissions.manage.groups",
    description = "List all registred groups")
    public void groupsList(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup[] groups = PermissionsEx.getPermissionManager().getGroups();

        sender.sendMessage(ChatColor.WHITE + "Currently registred groups: ");
        for (PermissionGroup group : groups) {
            sender.sendMessage(" " + group.getName() + " " + ChatColor.DARK_GREEN + "[" + StringUtils.implode(group.getParentGroupsNames(), ", ") + "]");
        }
    }

    @Command(name = "pex",
    syntax = "groups",
    permission = "permissions.manage.groups",
    description = "List all registred groups (alias)")
    public void groupsListAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.groupsList(plugin, sender, args);
    }

    @Command(name = "pex",
    syntax = "group",
    permission = "permissions.manage.groups",
    description = "List all registred groups (alias)")
    public void groupsListAnotherAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.groupsList(plugin, sender, args);
    }

     @Command(name = "pex",
    syntax = "group <group> prefix [newprefix]",
    permission = "permissions.manage.groups",
    description = "Add permission to user")
    public void groupPrefix(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
            return;
        }

        if (args.containsKey("newprefix")) {
            group.setPrefix(args.get("newprefix"));
        }

        sender.sendMessage(group.getName() + "'s prefix is: " + group.getPrefix());
    }

    @Command(name = "pex",
    syntax = "group <group> suffix [newsuffix]",
    permission = "permissions.manage.groups",
    description = "Add permission to user")
    public void groupSuffix(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
            return;
        }

        if (args.containsKey("newsuffix")) {
            group.setSuffix(args.get("newsuffix"));
        }

        sender.sendMessage(group.getName() + "'s suffix is: " + group.getSuffix());
    }

    @Command(name = "pex",
    syntax = "group <group> create [parents]",
    permission = "permissions.manage.groups.create",
    description = "List all group permissions (alias)")
    public void groupCreate(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

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
        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
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
        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

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
    permission = "permissions.manage.groups.inheritance",
    description = "Set parents by comma-separated list")
    public void groupSetParents(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
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
        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
            return;
        }

        sender.sendMessage("Group " + args.get("group") + "'s permissions:\n"
                + this.mapPermissions(args.get("world"), group, 0));

        sender.sendMessage("Group " + args.get("group") + "'s Options: ");
        for (Map.Entry<String, String> option : group.getOptions(args.get("world")).entrySet()) {
            sender.sendMessage("  " + option.getKey() + " = \"" + option.getValue() + "\"");
        }
    }

    @Command(name = "pex",
    syntax = "group <group> add <permission> [world]",
    permission = "permissions.manage.groups.permissions",
    description = "Add permission to group")
    public void groupAddPermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
            return;
        }

        group.addPermission(args.get("permission"), args.get("world"));

        sender.sendMessage(ChatColor.WHITE + "Permission added to " + group.getName() + " !");
    }

    @Command(name = "pex",
    syntax = "group <group> set <option> <value> [world]",
    permission = "permissions.manage.groups.permissions",
    description = "Set permission value for group")
    public void groupSetOption(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
            return;
        }

        group.setOption(args.get("permission"), args.get("value"), args.get("world"));

        sender.sendMessage(ChatColor.WHITE + "Option set!");
    }

    @Command(name = "pex",
    syntax = "group <group> remove <permission> [world]",
    permission = "permissions.manage.groups.permissions",
    description = "Remove permission from group")
    public void groupRemovePermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

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
        PermissionUser[] users = PermissionsEx.getPermissionManager().getUsers(args.get("group"));

        if (users == null || users.length == 0) {
            sender.sendMessage(ChatColor.RED + "No such group found");
        }

        sender.sendMessage("Group " + args.get("group") + " users:");

        for (PermissionUser user : users) {
            sender.sendMessage("   " + user.getName());
        }
    }

    @Command(name = "pex",
    syntax = "group <group> user add <user>",
    permission = "permissions.manage.membership",
    description = "Add users (one or comma-separated list) to specified group")
    public void groupUsersAdd(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsEx.getPermissionManager().getUser(args.get("user"));

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
        PermissionUser user = PermissionsEx.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such users found");
            return;
        }

        user.removeGroup(args.get("group"));

        sender.sendMessage(ChatColor.WHITE + "User " + user.getName() + " removed from " + args.get("group") + " !");
    }

    protected String printHierarhy(PermissionGroup parent, int level) {
        StringBuilder builder = new StringBuilder();

        PermissionGroup[] groups;
        if (parent == null) {
            groups = PermissionsEx.getPermissionManager().getGroups();
        } else {
            groups = parent.getChildGroups();
        }

        for (PermissionGroup group : groups) {
            if (parent == null && group.getParentGroups().length > 0) {
                continue;
            }

            builder.append(StringUtils.repeat("  ", level)).append(" - ").append(group.getName()).append("\n");

            // Groups
            builder.append(printHierarhy(group, level + 1));

            for (PermissionUser user : group.getUsers()) {
                builder.append(StringUtils.repeat("  ", level + 1)).append(" + ").append(user.getName()).append("\n");
            }
        }

        return builder.toString();
    }

    protected String mapPermissions(String world, PermissionEntity entity, int level) {
        StringBuilder builder = new StringBuilder();

        for (String permission : entity.getOwnPermissions(world)) {
            builder.append("  ").append(permission);
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
            throw new RuntimeException("Unknown class in hierarhy. Nag t3hk0d3 pls.");
        }

        level++; // Just increment level once
        for (PermissionGroup group : parents) {
            builder.append(mapPermissions(world, group, level));
        }

        return builder.toString();
    }
}
