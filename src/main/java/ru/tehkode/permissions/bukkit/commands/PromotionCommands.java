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
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.commands.Command;
import ru.tehkode.utils.StringUtils;

public class PromotionCommands extends PermissionsCommand {

    @Command(name = "pex",
    syntax = "group <group> rank <rank>",
    description = "Promotes user to next group",
    isPrimary = true,
    permission = "permissions.groups.rank")
    public void rankGroup(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String groupName = this.autoCompleteGroupName(args.get("group"));

        PermissionGroup group = PermissionsEx.getPermissionManager().getGroup(args.get("group"));

        if (group == null) {
            sender.sendMessage(ChatColor.RED + "Group \"" + groupName + "\" not found");
            return;
        }

        String newRank = args.get("rank").trim();

        try {
            Integer.parseInt(newRank); // Just to check what this is legal integer number
            group.setOption("rank", newRank);
            sender.sendMessage("Group " + group.getName() + " rank changed to " + newRank);
        } catch (NumberFormatException e) {
            sender.sendMessage("Wrong rank. Make sure it's number.");
        }
    }

    @Command(name = "pex",
    syntax = "promote <user>",
    description = "Promotes user to next group",
    isPrimary = true,
    permission = "permissions.user.promote")
    public void promoteUser(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String userName = this.autoCompletePlayerName(args.get("user"));
        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage("Specified user \"" + args.get("user") + "\" not found!");
            return;
        }

        int srcRank = StringUtils.toInteger(user.getOptionValue("rank"), 0);
        if (srcRank == 0) {
            sender.sendMessage(ChatColor.RED + "User \"" + user.getName() + "\" are not promoteable.");
            return;
        }

        if (sender instanceof Player) {
            PermissionUser promoter = PermissionsEx.getPermissionManager().getUser(((Player) sender).getName());

            if (promoter.getName().equals(user.getName())) {
                sender.sendMessage(ChatColor.RED + "You can't promote yourself!");
                return;
            }

            int promoterRank = StringUtils.toInteger(promoter.getOptionValue("rank"), 0);
            if (srcRank <= promoterRank) {
                sender.sendMessage(ChatColor.RED + "Promoting user have higher or equal rank than you!");
                return;
            }
        }

        // Look for group
        PermissionGroup targetGroup = null;
        int targetGroupRank = 0;

        for (PermissionGroup group : PermissionsEx.getPermissionManager().getGroups()) {
            int groupRank = StringUtils.toInteger(group.getOptionValue("rank"), 0);

            if (groupRank == 0 || groupRank >= srcRank) { // Group arent ranked or have lower rank than user rank
                continue;
            }

            if (targetGroup != null && targetGroupRank > groupRank) {
                continue;
            }

            targetGroup = group;
            targetGroupRank = groupRank;
        }


        if (targetGroup == null) {
            sender.sendMessage(ChatColor.RED + "User \"" + user.getName() + "\" are not promoteable.");
        }

        user.setGroups(new PermissionGroup[]{targetGroup});

        this.informPlayer(plugin, user.getName(), "You have been promoted to " + targetGroup.getName() + " group");
        sender.sendMessage("User " + user.getName() + " promoted to " + targetGroup.getName() + " group");
    }

    @Command(name = "pex",
    syntax = "demote <user>",
    description = "Demotes user to previous group",
    isPrimary = true,
    permission = "permissions.user.rank.demote")
    public void demoteUser(Plugin plugin, CommandSender sender, Map<String, String> args) {
        String userName = this.autoCompletePlayerName(args.get("user"));
        PermissionUser user = PermissionsEx.getPermissionManager().getUser(userName);

        if (user == null) {
            sender.sendMessage("Specified user \"" + args.get("user") + "\" not found!");
            return;
        }

        int srcRank = StringUtils.toInteger(user.getOptionValue("rank"), 0);
        if (srcRank == 0) {
            sender.sendMessage(ChatColor.RED + "User \"" + user.getName() + "\" are not demoteable.");
            return;
        }

        if (sender instanceof Player) {
            PermissionUser demoter = PermissionsEx.getPermissionManager().getUser(((Player) sender).getName());

            if (demoter.getName().equals(user.getName())) {
                sender.sendMessage(ChatColor.RED + "You can't demote yourself!");
                return;
            }

            int promoterRank = StringUtils.toInteger(demoter.getOptionValue("rank"), 0);
            if (srcRank <= promoterRank) {
                sender.sendMessage(ChatColor.RED + "Demoting user have equal or higher rank than you!");
                return;
            }
        }

        // Look for group
        PermissionGroup targetGroup = null;
        int targetGroupRank = 0;

        for (PermissionGroup group : PermissionsEx.getPermissionManager().getGroups()) {
            int groupRank = StringUtils.toInteger(group.getOptionValue("rank"), 0);

            if (groupRank == 0 || groupRank <= srcRank) { // Group arent ranked or have higher rank than user rank
                continue;
            }

            if (targetGroup != null && targetGroupRank < groupRank) {
                continue;
            }

            targetGroup = group;
            targetGroupRank = groupRank;
        }


        if (targetGroup == null) {
            sender.sendMessage(ChatColor.RED + "User \"" + user.getName() + "\" are not demoteable.");
        }

        user.setGroups(new PermissionGroup[]{targetGroup});

        this.informPlayer(plugin, user.getName(), "You have been demoted to " + targetGroup.getName() + " group");

        sender.sendMessage("User " + user.getName() + " demoted to " + targetGroup.getName() + " group");
    }

    @Command(name = "promote",
    syntax = "<user>",
    description = "Promotes user to next group",
    isPrimary = true,
    permission = "permissions.user.rank.promote")
    public void promoteUserAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.promoteUser(plugin, sender, args);
    }

    @Command(name = "demote",
    syntax = "<user>",
    description = "Demotes user to previous group",
    isPrimary = true,
    permission = "permissions.user.demote")
    public void demoteUserAlias(Plugin plugin, CommandSender sender, Map<String, String> args) {
        this.demoteUser(plugin, sender, args);
    }
}
