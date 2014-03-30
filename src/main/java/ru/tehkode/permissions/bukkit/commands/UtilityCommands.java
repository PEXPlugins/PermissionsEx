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

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.tehkode.permissions.backends.PermissionBackend;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.ErrorReport;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.commands.Command;
import ru.tehkode.permissions.commands.CommandsManager.CommandBinding;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class UtilityCommands extends PermissionsCommand {

	@Command(name = "pex",
			syntax = "reload",
			permission = "permissions.manage.reload",
			description = "Reload environment")
	public void reload(Plugin plugin, CommandSender sender, Map<String, String> args) {
		try {
			PermissionsEx.getPermissionManager().reset();
			sender.sendMessage(ChatColor.WHITE + "Permissions reloaded");
		} catch (PermissionBackendException e) {
			sender.sendMessage(ChatColor.RED + "Failed to reload permissions! Check configuration!\n" +
							   ChatColor.RED + "Error (see console for full): " + e.getMessage());
			plugin.getLogger().log(Level.WARNING, "Failed to reload permissions when " + sender.getName() + " ran `pex reload`", e);
		}
	}

	@Command(name = "pex",
			syntax = "report",
			permission = "permissions.manage.reportbug",
			description = "Create an issue template to report an issue")
	public void report(Plugin plugin, CommandSender sender, Map<String, String> args) {
		ErrorReport report = ErrorReport.withException("User-requested report", new Exception().fillInStackTrace());
		sender.sendMessage("Fill in the information at " + report.getShortURL() + " to report an issue");
		sender.sendMessage(ChatColor.RED + "NOTE: A GitHub account is necessary to report issues. Create one at https://github.com/");
	}

	@Command(name = "pex",
			syntax = "config <node> [value]",
			permission = "permissions.manage.config",
			description = "Print or set <node> [value]")
	public void config(Plugin plugin, CommandSender sender, Map<String, String> args) {
		if (!(plugin instanceof PermissionsEx)) {
			return;
		}

		String nodeName = args.get("node");
		if (nodeName == null || nodeName.isEmpty()) {
			return;
		}

		FileConfiguration config = plugin.getConfig();

		if (args.get("value") != null) {
			config.set(nodeName, this.parseValue(args.get("value")));
			try {
				config.save(new File(plugin.getDataFolder(), "config.yml"));
			} catch (Throwable e) {
				sender.sendMessage(ChatColor.RED + "[PermissionsEx] Failed to save configuration: " + e.getMessage());
			}
		}

		Object node = config.get(nodeName);
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
			sender.sendMessage("Node \"" + nodeName + "\" = \"" + node + "\"");
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
			if (e.getCause() instanceof ClassNotFoundException) {
				sender.sendMessage(ChatColor.RED + "Specified backend not found.");
			} else {
				sender.sendMessage(ChatColor.RED + "Error during backend initialization.");
				e.printStackTrace();
			}
		} catch (PermissionBackendException e) {
			sender.sendMessage(ChatColor.RED + "Backend initialization failed! Fix your configuration!\n" +
							   ChatColor.RED + "Error (see console for more): " + e.getMessage());
			plugin.getLogger().log(Level.WARNING, "Backend initialization failed when " + sender.getName() + " was initializing " + args.get("backend"), e);
		}
	}

	@Command(name = "pex",
			syntax = "hierarchy [world]",
			permission = "permissions.manage.users",
			description = "Print complete user/group hierarchy")
	public void printHierarchy(Plugin plugin, CommandSender sender, Map<String, String> args) {
		sender.sendMessage("User/Group inheritance hierarchy:");
		this.sendMessage(sender, this.printHierarchy(null, this.autoCompleteWorldName(args.get("world")), 0));
	}

	@Command(name = "pex",
			syntax = "import <backend>",
			permission = "permissions.dump",
			description = "Import data from <backend> as specified in the configuration")
	public void dumpData(Plugin plugin, CommandSender sender, Map<String, String> args) {
		if (!(plugin instanceof PermissionsEx)) {
			return; // User informing is disabled
		}

		try {
			PermissionBackend backend = PermissionBackend.getBackend(args.get("backend"), PermissionsEx.getPermissionManager(), plugin.getConfig(), null);
			backend.reload();
			backend.validate();
			PermissionsEx.getPermissionManager().getBackend().loadFrom(backend);


			sender.sendMessage(ChatColor.WHITE + "[PermissionsEx] Data from \"" + args.get("backend") + "\" loaded into currently active backend");
		} catch (RuntimeException e) {
			if (e.getCause() instanceof ClassNotFoundException) {
				sender.sendMessage(ChatColor.RED + "Specified backend not found!");
			} else {
				sender.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
				logger.severe("Error: " + e.getMessage());
				e.printStackTrace();
			}
		} catch (PermissionBackendException e) {
			sender.sendMessage(ChatColor.RED + "Backend " + args.get("backend") + " was unable to load due to user configuration error. See console for details.");
			plugin.getLogger().log(Level.WARNING, "Import backend unable to load", e);
		}
	}

	@Command(name = "pex",
			syntax = "toggle debug",
			permission = "permissions.debug",
			description = "Enable/disable debug mode")
	public void toggleFeature(Plugin plugin, CommandSender sender, Map<String, String> args) {
		PermissionManager manager = PermissionsEx.getPermissionManager();

		manager.setDebug(!manager.isDebug());

		String debugStatusMessage = "[PermissionsEx] Debug mode " + (manager.isDebug() ? "enabled" : "disabled");

		if (sender instanceof Player) {
			sender.sendMessage(debugStatusMessage);
		}

		logger.warning(debugStatusMessage);
	}

	private static int tryGetInt(CommandSender sender, Map<String, String> args, String key, int def) {
		if (!args.containsKey(key)) {
			return def;
		}

		try {
			return Integer.parseInt(args.get(key));
		} catch (NumberFormatException e) {
			sender.sendMessage(ChatColor.RED + "Invalid " + key + " entered; must be an integer but was '" + args.get(key) + "'");
			return Integer.MIN_VALUE;
		}
	}

	@Command(name = "pex",
			syntax = "help [page] [count]",
			permission = "permissions.manage",
			description = "PermissionsEx commands help")
	public void showHelp(Plugin plugin, CommandSender sender, Map<String, String> args) {
		List<CommandBinding> commands = this.manager.getCommands();

		int count = tryGetInt(sender, args, "count", 4);
		int page = tryGetInt(sender, args, "page", 1);

		if (page == Integer.MIN_VALUE || count == Integer.MIN_VALUE) {
			return; // method already prints error message
		}

		if (page < 1) {
			sender.sendMessage(ChatColor.RED + "Page couldn't be lower than 1");
			return;
		}

		int totalPages = (int) Math.ceil(commands.size() / count);

		sender.sendMessage(ChatColor.BLUE + "PermissionsEx" + ChatColor.WHITE + " commands (page " + ChatColor.GOLD + page + "/" + totalPages + ChatColor.WHITE + "): ");

		int base = count * (page - 1);

		for (int i = base; i < base + count; i++) {
			if (i >= commands.size()) {
				break;
			}

			Command command = commands.get(i).getMethodAnnotation();
			String commandName = String.format("/%s %s", command.name(), command.syntax()).replace("<", ChatColor.BOLD.toString() + ChatColor.RED + "<").replace(">", ">" + ChatColor.RESET + ChatColor.GOLD.toString()).replace("[", ChatColor.BOLD.toString() + ChatColor.BLUE + "[").replace("]", "]" + ChatColor.RESET + ChatColor.GOLD.toString());


			sender.sendMessage(ChatColor.GOLD + commandName);
			sender.sendMessage(ChatColor.AQUA + "    " + command.description());
		}
	}
}
