/**
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ninja.leaping.permissionsex.bukkit;

import com.google.common.base.Joiner;
import ninja.leaping.permissionsex.util.command.CommandSpec;
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
