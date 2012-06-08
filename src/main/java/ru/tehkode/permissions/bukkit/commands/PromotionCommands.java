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

import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.commands.Command;
import ru.tehkode.permissions.exceptions.RankingException;

public class PromotionCommands extends PermissionsCommand {

	@Command(name = "pex",
	syntax = "group <group> rank [rank] [ladder]",
	description = "Get or set <group> [rank] [ladder]",
	isPrimary = true,
	permission = "permissions.groups.rank.<group>")
	public void rankGroup(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String groupName = this.autoCompleteGroupName(args.get("group"));

		PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(groupName);

		if (group == null) {
			sender.sendMessage(ChatColor.RED + "Group \"" + groupName + "\" not found");
			return;
		}

		if (args.get("rank") != null) {
			String newRank = args.get("rank").trim();

			try {
				group.setRank(Integer.parseInt(newRank));
			} catch (NumberFormatException e) {
				sender.sendMessage("Wrong rank. Make sure it's number.");
			}

			if (args.containsKey("ladder")) {
				group.setRankLadder(args.get("ladder"));
			}
		}

		int rank = group.getRank();

		if (rank > 0) {
			sender.sendMessage("Group " + group.getName() + " rank is " + rank + " (ladder = " + group.getRankLadder() + ")");
		} else {
			sender.sendMessage("Group " + group.getName() + " is unranked");
		}
	}

	@Command(name = "pex",
	syntax = "promote <user> [ladder]",
	description = "Promotes <user> to next group on [ladder]",
	isPrimary = true)
	public void promoteUser(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

		if (user == null) {
			sender.sendMessage("Specified user \"" + args.get("user") + "\" not found!");
			return;
		}

		String promoterName = "console";
		String ladder = "default";

		if (args.containsKey("ladder")) {
			ladder = args.get("ladder");
		}

		PermissionUser promoter = null;
		if (sender instanceof Player) {
			promoter = PermissionsEx.getPermissionManager().getUser(((Player) sender).getName());
			if (promoter == null || !promoter.has("permissions.user.promote." + ladder, ((Player) sender).getWorld().getName())) {
				sender.sendMessage(ChatColor.RED + "You don't have enough permissions to promote on this ladder");
				return;
			}

			promoterName = promoter.getName();
		}

		try {
			PermissionGroup targetGroup = user.promote(promoter, ladder);

			this.informPlayer(plugin, user.getName(), "You have been promoted on " + targetGroup.getRankLadder() + " ladder to " + targetGroup.getName() + " group");
			sender.sendMessage("User " + user.getName() + " promoted to " + targetGroup.getName() + " group");
			Logger.getLogger("Minecraft").info("User " + user.getName() + " has been promoted to " + targetGroup.getName() + " group on " + targetGroup.getRankLadder() + " ladder by " + promoterName);
		} catch (RankingException e) {
			sender.sendMessage(ChatColor.RED + "Promotion error: " + e.getMessage());
			Logger.getLogger("Minecraft").severe("Ranking Error (" + promoterName + " > " + e.getTarget().getName() + "): " + e.getMessage());
		}
	}

	@Command(name = "pex",
	syntax = "demote <user> [ladder]",
	description = "Demotes <user> to previous group or [ladder]",
	isPrimary = true)
	public void demoteUser(Plugin plugin, CommandSender sender, Map<String, String> args) {
		String userName = this.autoCompletePlayerName(args.get("user"));
		PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

		if (user == null) {
			sender.sendMessage(ChatColor.RED + "Specified user \"" + args.get("user") + "\" not found!");
			return;
		}

		String demoterName = "console";
		String ladder = "default";

		if (args.containsKey("ladder")) {
			ladder = args.get("ladder");
		}

		PermissionUser demoter = null;
		if (sender instanceof Player) {
			demoter = PermissionsEx.getPermissionManager().getUser(((Player) sender).getName());

			if (demoter == null || !demoter.has("permissions.user.demote." + ladder, ((Player) sender).getWorld().getName())) {
				sender.sendMessage(ChatColor.RED + "You don't have enough permissions to demote on this ladder");
				return;
			}

			demoterName = demoter.getName();
		}

		try {
			PermissionGroup targetGroup = user.demote(demoter, args.get("ladder"));

			this.informPlayer(plugin, user.getName(), "You have been demoted on " + targetGroup.getRankLadder() + " ladder to " + targetGroup.getName() + " group");
			sender.sendMessage("User " + user.getName() + " demoted to " + targetGroup.getName() + " group");
			Logger.getLogger("Minecraft").info("User " + user.getName() + " has been demoted to " + targetGroup.getName() + " group on " + targetGroup.getRankLadder() + " ladder by " + demoterName);
		} catch (RankingException e) {
			sender.sendMessage(ChatColor.RED + "Demotion error: " + e.getMessage());
			Logger.getLogger("Minecraft").severe("Ranking Error (" + demoterName + " demotes " + e.getTarget().getName() + "): " + e.getMessage());
		}
	}

	@Command(name = "promote",
	syntax = "<user>",
	description = "Promotes <user> to next group",
	isPrimary = true,
	permission = "permissions.user.rank.promote")
	public void promoteUserAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
		this.promoteUser(plugin, sender, args);
	}

	@Command(name = "demote",
	syntax = "<user>",
	description = "Demotes <user> to previous group",
	isPrimary = true,
	permission = "permissions.user.rank.demote")
	public void demoteUserAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
		this.demoteUser(plugin, sender, args);
	}
}
