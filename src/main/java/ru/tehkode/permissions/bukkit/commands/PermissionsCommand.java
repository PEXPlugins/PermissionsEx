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

/**
 *
 * @author code
 */
public class PermissionsCommand implements CommandListener {

    @Command(name = "permissions",
    syntax = "reload",
    permission = "permissions.manage.reload",
    description = "Reload permissions")
    public void reload(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionsPlugin.getPermissionManager().reset();
    }

    /**
     * User management
     */
    /**
     * User's permissions management
     */
    @Command(name = "permissions",
    syntax = "user <user>",
    permission = "permissions.manage.user.permissions",
    description = "List user permissions (list alias)")
    public void userListAliasPermissions(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.userListPermissions(plugin, sender, args);
    }

    @Command(name = "permissions",
    syntax = "user <user> list [world]",
    permission = "permissions.manage.user.permissions",
    description = "List user permissions")
    public void userListPermissions(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsPlugin.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        sender.sendMessage("Current: " + args.get("user") + " permissions:");
        for (String permission : user.getPermissions(args.get("world"))) {
            sender.sendMessage("  " + permission);
        }
    }

    @Command(name = "permissions",
    syntax = "user <user> add <permission> [world]",
    permission = "permissions.manage.user.permissions",
    description = "Add permission to user")
    public void userAddPermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsPlugin.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        user.addPermission(args.get("permission"), null, args.get("world"));
    }

    @Command(name = "permissions",
    syntax = "user <user> set <permission> <value> [world]",
    permission = "permissions.manage.user.permissions",
    description = "Set permission setting to given value")
    public void userSetPermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsPlugin.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        user.setPermission(args.get("permission"), args.get("value"), args.get("world"));
    }

    @Command(name = "permissions",
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
    }

    /**
     * User's groups management
     */
    @Command(name = "permissions",
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

    @Command(name = "permissions",
    syntax = "user <user> add group <group>",
    permission = "permissions.manage.membership",
    description = "Add user to specified group")
    public void userAddGroup(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsPlugin.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        user.addGroup(args.get("group"));
    }

    @Command(name = "permissions",
    syntax = "user <user> set group <group>",
    permission = "permissions.manage.membership",
    description = "Set leave specified group for user")
    public void userSetGroup(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsPlugin.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such user found");
            return;
        }

        user.setGroups(new PermissionGroup[]{PermissionsPlugin.getPermissionManager().getGroup(args.get("group"))});
    }

    @Command(name = "permissions",
    syntax = "user <user> remove group <group>",
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
    }

    /**
     * Group management
     */
    @Command(name = "permissions",
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

        group.save();
    }

    @Command(name = "permissions",
    syntax = "group <group> delete",
    permission = "permissions.manage.group.remove",
    description = "Removes group")
    public void groupDelete(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsPlugin.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
            return;
        }

        group.remove();
    }

    @Command(name = "permissions",
    syntax = "group <group> parents <parents>",
    permission = "permissions.manage.group.inheritance",
    description = "Set parents by comma-separated list")
    public void groupSetParents(Plugin plugin, CommandSender sender, Map<String, String> args) {
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

            group.save();
        }
    }

    /**
     * Group permissions
     */
    @Command(name = "permissions",
    syntax = "group <group>",
    permission = "permissions.manage.group.permissions",
    description = "List all group permissions (alias)")
    public void groupListAliasPermissions(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.groupListPermissions(plugin, sender, args);
    }

    @Command(name = "permissions",
    syntax = "group <group> list [world]",
    permission = "permissions.manage.group.permissions",
    description = "List all group permissions")
    public void groupListPermissions(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsPlugin.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
            return;
        }

        sender.sendMessage("Group " + args.get("user") + " permissions:");
        for (String permission : group.getPermissions(args.get("world"))) {
            sender.sendMessage("  " + permission);
        }
    }

    @Command(name = "permissions",
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
    }

    @Command(name = "permissions",
    syntax = "group <group> set <permission> <value> [world]",
    permission = "permissions.manage.group.permissions",
    description = "Set permission value for group")
    public void groupSetPermission(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionGroup group = PermissionsPlugin.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "No such group found");
            return;
        }

        group.setPermission(args.get("permission"), args.get("value"), args.get("world"));
    }

    @Command(name = "permissions",
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
    }

    /**
     * Group users management
     */
    @Command(name = "permissions",
    syntax = "group <group> users",
    permission = "permissions.manage.membership",
    description = "List all group users")
    public void groupUsersList(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser[] users = PermissionsPlugin.getPermissionManager().getUsers(args.get("group"));

        if (users == null || users.length == 0) {
            sender.sendMessage(ChatColor.RED + "No such group found");
            return;
        }
    }

    @Command(name = "permissions",
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
    }

    @Command(name = "permissions",
    syntax = "group <group> user add <user>",
    permission = "permissions.manage.membership",
    description = "Add users (one or comma-separated list) to specified group")
    public void groupUsersRemove(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionUser user = PermissionsPlugin.getPermissionManager().getUser(args.get("user"));

        if (user == null) {
            sender.sendMessage(ChatColor.RED + "No such users found");
            return;
        }

        user.removeGroup(args.get("group"));
    }
}
