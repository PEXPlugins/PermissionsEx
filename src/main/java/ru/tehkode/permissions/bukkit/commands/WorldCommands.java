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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.commands.Command;
import ru.tehkode.utils.StringUtils;

public class WorldCommands extends PermissionsCommand {

	@Command(name = "pex",
			syntax = "worlds",
			description = "Print loaded worlds",
			isPrimary = true,
			permission = "permissions.manage.worlds")
	public void worldsTree(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		List<World> worlds = plugin.getServer().getWorlds();

		PermissionManager manager = plugin.getPermissionsManager();

		sender.sendMessage("Worlds on server: ");
		for (World world : worlds) {
			List<String> parentWorlds = manager.getWorldInheritance(world.getName());
			String output = "  " + world.getName();
			if (!parentWorlds.isEmpty()) {
				output += ChatColor.GREEN + " [" + ChatColor.WHITE + StringUtils.implode(parentWorlds, ", ") + ChatColor.GREEN + "]";
			}

			sender.sendMessage(output);
		}
	}

	@Command(name = "pex",
			syntax = "world <world>",
			description = "Print <world> inheritance info",
			permission = "permissions.manage.worlds")
	public void worldPrintInheritance(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String worldName = this.autoCompleteWorldName(args.get("world"));
		PermissionManager manager = plugin.getPermissionsManager();
		if (plugin.getServer().getWorld(worldName) == null) {
			sender.sendMessage("Specified world \"" + args.get("world") + "\" not found.");
			return;
		}

		List<String> parentWorlds = manager.getWorldInheritance(worldName);

		if (parentWorlds.isEmpty()) {
			sender.sendMessage("World \"" + worldName + "\" inherits nothing.");
			return;
		}

		sender.sendMessage("World \"" + worldName + "\" inherits:");

		for (String parentWorld : parentWorlds) {
			List<String> parents = manager.getWorldInheritance(parentWorld);
			String output = "  " + parentWorld;
			if (!parents.isEmpty()) {
				output += ChatColor.GREEN + " [" + ChatColor.WHITE + StringUtils.implode(parents, ", ") + ChatColor.GREEN + "]";
			}

			sender.sendMessage(output);
		}
	}

	@Command(name = "pex",
			syntax = "world <world> inherit <parentWorlds>",
			description = "Set <parentWorlds> for <world>",
			permission = "permissions.manage.worlds.inheritance")
	public void worldSetInheritance(PermissionsEx plugin, CommandSender sender, Map<String, String> args) {
		String worldName = this.autoCompleteWorldName(args.get("world"));
		PermissionManager manager = plugin.getPermissionsManager();
		if (plugin.getServer().getWorld(worldName) == null) {
			sender.sendMessage("Specified world \"" + args.get("world") + "\" not found.");
			return;
		}

		List<String> parents = new ArrayList<>();
		String parentWorlds = args.get("parentWorlds");
		if (parentWorlds.contains(",")) {
			for (String world : parentWorlds.split(",")) {
				world = this.autoCompleteWorldName(world, "parentWorlds");
				if (!parents.contains(world)) {
					parents.add(world.trim());
				}
			}
		} else {
			parents.add(parentWorlds.trim());
		}

		manager.setWorldInheritance(worldName, parents);

		sender.sendMessage("World \"" + worldName + "\" inherits " + StringUtils.implode(parents, ", "));
	}
}
