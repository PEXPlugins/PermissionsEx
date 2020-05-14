/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2020 zml [at] stellardrift [dot] ca and PermissionsEx contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ca.stellardrift.permissionsex.bukkit;

import ca.stellardrift.permissionsex.util.command.CommandSpec;
import com.google.common.base.Joiner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;

/**
 * Wrapper class between PEX commands and the Sponge command class
 */
public class PEXBukkitCommand implements CommandExecutor, TabExecutor {
    private final CommandSpec command;
    private final PermissionsExPlugin plugin;

    public PEXBukkitCommand(CommandSpec command, PermissionsExPlugin plugin) {
        this.command = command;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        this.command.process(new BukkitCommander(plugin, commandSender), Joiner.on(" ").join(strings));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return this.command.tabComplete(new BukkitCommander(plugin, commandSender), Joiner.on(" ").join(strings));
    }
}
