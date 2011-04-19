package ru.tehkode.permissions.bukkit.commands;

import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
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
    public void reload(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
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
    public void userListAliasPermissions(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
        this.userListPermissions(plugin, sender, arguments);
    }

    @Command(name = "permissions",
    syntax = "user <user> list",
    permission = "permissions.manage.user.permissions",
    description = "List user permissions")
    public void userListPermissions(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
    }

    @Command(name = "permissions",
    syntax = "user <user> add <permission> [world]",
    permission = "permissions.manage.user.permissions",
    description = "Add permission to user")
    public void userAddPermission(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
    }

    @Command(name = "permissions",
    syntax = "user <user> set <permission> <value> [world]",
    permission = "permissions.manage.user.permissions",
    description = "Set permission setting to given value")
    public void userSetPermission(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
    }

    @Command(name = "permissions",
    syntax = "user <user> remove <permission> [world]",
    permission = "permissions.manage.user.permissions",
    description = "Remove permission from user")
    public void userRemovePermission(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
    }

    /**
     * User's groups management
     */
    @Command(name = "permissions",
    syntax = "user <user> add group list",
    permission = "permissions.manage.membership",
    description = "List all user groups")
    public void userListGroup(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
    }

    @Command(name = "permissions",
    syntax = "user <user> add group <group>",
    permission = "permissions.manage.membership",
    description = "Add user to specified group")
    public void userAddGroup(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
    }

    @Command(name = "permissions",
    syntax = "user <user> set group <group>",
    permission = "permissions.manage.membership",
    description = "Set leave specified group for user")
    public void userSetGroup(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
    }

    @Command(name = "permissions",
    syntax = "user <user> remove group <group>",
    permission = "permissions.manage.membership",
    description = "Remove user from specified group")
    public void userRemoveGroup(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
    }

    /**
     * Group management
     */

    @Command(name = "permissions",
    syntax = "group <group> create [parents]",
    permission = "permissions.manage.group.create",
    description = "List all group permissions (alias)")
    public void groupCreate(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
        
    }

    @Command(name = "permissions",
    syntax = "group <group> delete",
    permission = "permissions.manage.group.remove",
    description = "Removes group")
    public void groupDelete(Plugin plugin, CommandSender sender, Map<String, String> arguments) {

    }

    @Command(name = "permissions",
    syntax = "group <group> parents <parents>",
    permission = "permissions.manage.group.inheritance",
    description = "Set parents by comma-separated list")
    public void groupSetParents(Plugin plugin, CommandSender sender, Map<String, String> arguments) {

    }

    /**
     * Group permissions
     */
    @Command(name = "permissions",
    syntax = "group <group>",
    permission = "permissions.manage.group.permissions",
    description = "List all group permissions (alias)")
    public void groupListAliasPermissions(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
        this.groupListPermissions(plugin, sender, arguments);
    }

    @Command(name = "permissions",
    syntax = "group <group> list",
    permission = "permissions.manage.group.permissions",
    description = "List all group permissions")
    public void groupListPermissions(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
    }

    @Command(name = "permissions",
    syntax = "group <group> add <permission> [world]",
    permission = "permissions.manage.group.permissions",
    description = "Add permission to group")
    public void groupAddPermission(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
    }

    @Command(name = "permissions",
    syntax = "group <group> set <permission> <value> [world]",
    permission = "permissions.manage.group.permissions",
    description = "Set permission value for group")
    public void groupSetPermission(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
    }

    @Command(name = "permissions",
    syntax = "group <group> remove <permission> <value> [world]",
    permission = "permissions.manage.group.permissions",
    description = "Remove permission from group")
    public void groupRemovePermission(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
    }

    /**
     * Group users management
     */

    @Command(name = "permissions",
    syntax = "group <group> users",
    permission = "permissions.manage.membership",
    description = "List all group users")
    public void groupUsersList(Plugin plugin, CommandSender sender, Map<String, String> arguments) {

    }

    @Command(name = "permissions",
    syntax = "group <group> user add <user>",
    permission = "permissions.manage.membership",
    description = "Add users (one or comma-separated list) to specified group")
    public void groupUsersAdd(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
        
    }

    @Command(name = "permissions",
    syntax = "group <group> user add <user>",
    permission = "permissions.manage.membership",
    description = "Add users (one or comma-separated list) to specified group")
    public void groupUsersRemove(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
        
    }
}
