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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import ru.tehkode.permissions.PermissionBackend;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.commands.Command;
import ru.tehkode.permissions.config.Configuration;

public class UtilityCommands extends PermissionsCommand {

    @Command(name = "pex",
    syntax = "reload",
    permission = "permissions.manage.reload",
    description = "Reload permissions")
    public void reload(Plugin plugin, CommandSender sender, Map<String, String> args) {
        PermissionsEx.getPermissionManager().reset();

        sender.sendMessage(ChatColor.WHITE + "Permissions reloaded");
    }

    @Command(name = "pex",
    syntax = "config <node> [value]",
    permission = "permissions.manage.config",
    description = "Print <node> value from plugin configuration. Specify [value] to set new value.")
    public void config(Plugin plugin, CommandSender sender, Map<String, String> args) {
        if (!(plugin instanceof PermissionsEx)) {
            return;
        }

        String nodeName = args.get("node");
        if (nodeName == null || nodeName.isEmpty()) {
            return;
        }

        Configuration config = ((PermissionsEx) plugin).getConfigurationNode();

        if (args.get("value") != null) {
            config.setProperty(nodeName, this.parseValue(args.get("value")));
            config.save();
        }

        Object node = config.getProperty(nodeName);
        if (node instanceof Map) {
            sender.sendMessage("Node \"" + nodeName + "\": ");
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) node).entrySet()) {
                sender.sendMessage("  " + entry.getKey() + " = " + entry.getValue());
            }
        } else if (node instanceof List) {
            sender.sendMessage("Node \"" + nodeName + "\": ");
            for (String item : ((List<String>) node)) {
                sender.sendMessage(" - " + item);
            }
        } else {
            sender.sendMessage("Node \"" + nodeName + "\" = \"" + node.toString() + "\"");
        }
    }

    @Command(name = "pex",
    syntax = "backend",
    permission = "permissions.manage.backend",
    description = "Print currently used backend")
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
            if(e.getCause() instanceof ClassNotFoundException){
                sender.sendMessage(ChatColor.RED + "Specified backend not found.");
            } else {
                sender.sendMessage(ChatColor.RED + "Error during backend initializtation:");
                e.getCause().printStackTrace();
            }
        }
    }

    @Command(name = "pex",
    syntax = "hierarchy",
    permission = "permissions.manage.users",
    description = "Print complete user/group hierarhy")
    public void printHierarhy(Plugin plugin, CommandSender sender, Map<String, String> args) {
        sender.sendMessage("Permission Inheritance Hierarhy:");
        this.sendMessage(sender, this.printHierarhy(null, 0));
    }

    @Command(name = "pex",
    syntax = "dump <backend> <filename>",
    permission = "permissions.dump",
    description = "Dump users/groups to selected <backend> format")
    public void dumpData(Plugin plugin, CommandSender sender, Map<String, String> args) {
        if (!(plugin instanceof PermissionsEx)) {
            return; // User informing are disabled
        }

        try {
            PermissionBackend backend = PermissionBackend.getBackend(args.get("backend"), PermissionsEx.getPermissionManager(), ((PermissionsEx) plugin).getConfigurationNode(), null);

            File dstFile = new File("plugins/PermissionsEx/", args.get("filename"));
            backend.dumpData(new OutputStreamWriter(new FileOutputStream(dstFile), "UTF-8"));

            sender.sendMessage(ChatColor.WHITE + "[PermissionsEx] Data dumped in \"" + dstFile.getName() + "\" ");
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ClassNotFoundException) {
                sender.sendMessage(ChatColor.RED + "Specified backend not found!");
            } else {
                sender.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
                logger.severe("Error (" + e.getCause().getClass().getName() + "): " + e.getMessage());
            }
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "IO Error: " + e.getMessage());
        }
    }
}
