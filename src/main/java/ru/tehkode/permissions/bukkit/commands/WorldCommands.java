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

import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.commands.Command;
import ru.tehkode.utils.StringUtils;

public class WorldCommands extends PermissionsCommand {

    @Command(name = "pex",
    syntax = "worlds",
    description = "Prints worlds inheritance tree",
    isPrimary = true,
    permission = "permissions.worlds")
    public void worldsTree(Plugin plugin, CommandSender sender, Map<String, String> args) {
        List<World> worlds = Bukkit.getServer().getWorlds();

        PermissionManager manager = PermissionsEx.getPermissionManager();

        sender.sendMessage("Worlds on server: ");
        for (World world : Bukkit.getServer().getWorlds()) {
            String[] parentWorlds = manager.getWorldInheritance(world.getName());
            String output = "  " + world.getName();
            if (parentWorlds.length > 0) {
                output += ChatColor.GREEN + " [" + ChatColor.WHITE + StringUtils.implode(parentWorlds, ", ") + ChatColor.GREEN + "]";
            }
            
            sender.sendMessage(output);
        }
    }
}
