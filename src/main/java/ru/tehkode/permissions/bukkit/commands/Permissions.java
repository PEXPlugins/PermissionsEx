/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.tehkode.permissions.bukkit.commands;

import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import ru.tehkode.permissions.commands.Command;
import ru.tehkode.permissions.commands.CommandListener;

/**
 *
 * @author code
 */
public class Permissions implements CommandListener {

    @Command(name = "permissions",
    syntax = "reload",
    permission = "permission.manage.reload",
    description = "Reload permissions")
    public void reload(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
        Logger.getLogger("Minecraft").info("WORKING YAY");
        sender.sendMessage("YAHOO");
    }

    @Command(name = "permissions",
    syntax = "user <user>",
    permission = "permission.manage.users",
    description = "List user's permissions")
    public void userListPermissions(Plugin plugin, CommandSender sender, Map<String, String> arguments) {
        Logger.getLogger("Minecraft").info("WORKING YAY - " + arguments);
        sender.sendMessage("User name: " + arguments.get("user"));
    }
}
