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
package ru.tehkode.permissions.bukkit;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.google.common.cache.CacheBuilder;
import com.zachsthings.netevents.NetEventsPlugin;
import net.gravitydevelopment.updater.Updater;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import ru.tehkode.permissions.NativeInterface;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.backends.MultiBackend;
import ru.tehkode.permissions.backends.PermissionBackend;
import ru.tehkode.permissions.backends.file.FileBackend;
import ru.tehkode.permissions.backends.hybrid.HybridBackend;
import ru.tehkode.permissions.backends.memory.MemoryBackend;
import ru.tehkode.permissions.backends.sql.SQLBackend;
import ru.tehkode.permissions.bukkit.commands.*;
import ru.tehkode.permissions.bukkit.regexperms.RegexPermissions;
import ru.tehkode.permissions.commands.CommandsManager;
import ru.tehkode.permissions.events.PermissionEvent;
import ru.tehkode.permissions.exceptions.PermissionBackendException;
import ru.tehkode.permissions.exceptions.PermissionsNotAvailable;
import ru.tehkode.utils.StringUtils;

/**
 * @author code
 */
public class PermissionsEx extends JavaPlugin implements NativeInterface {
	private static final int BUKKITDEV_ID = 31279;
	protected PermissionManager permissionsManager;
	protected CommandsManager commandsManager;
	private PermissionsExConfig config;
	protected SuperpermsListener superms;
	private RegexPermissions regexPerms;
	private NetEventsPlugin netEvents;
	private boolean errored = false;
	private static PermissionsEx instance;
	{
		instance = this;
	}

	public PermissionsEx() {
		super();
		try {
			Field field = JavaPlugin.class.getDeclaredField("logger");
			field.setAccessible(true);
			field.set(this, new PermissionsExLogger(this));
		} catch (Exception e) {
			// Ignore, just hide the joke
		}

		PermissionBackend.registerBackendAlias("sql", SQLBackend.class);
		PermissionBackend.registerBackendAlias("file", FileBackend.class);
		PermissionBackend.registerBackendAlias("memory", MemoryBackend.class);
		PermissionBackend.registerBackendAlias("multi", MultiBackend.class);
		PermissionBackend.registerBackendAlias("hybrid", HybridBackend.class);

	}

	private static class PermissionsExLogger extends PluginLogger {
		/**
		 * Protected method to construct a logger for a named subsystem.
		 * <p/>
		 * The logger will be initially configured with a null Level
		 * and with useParentHandlers set to true.
		 *
		 * @param plugin Plugin to get class info from
		 */
		protected PermissionsExLogger(Plugin plugin) {
			super(plugin);
			try {
				Field replace = PluginLogger.class.getDeclaredField("pluginName");
				replace.setAccessible(true);
				replace.set(this, "");
			} catch (Exception e) {
				// Dispose, if stuff happens the poor server admin just won't get their joke
			}

		}

		public boolean isDay() {
			final Calendar cal = Calendar.getInstance();
			return cal.get(GregorianCalendar.MONTH) == Calendar.APRIL && cal.get(GregorianCalendar.DAY_OF_MONTH) == 1;
		}

		@Override
		public void log(LogRecord record) {
			record.setMessage("[" + (isDay() ? "PermissionSex" : "PermissionsEx") + "] " + record.getMessage());
			super.log(record);
		}
	}

	private void logBackendExc(PermissionBackendException e) {
		getLogger().log(Level.SEVERE, "\n========== UNABLE TO LOAD PERMISSIONS BACKEND =========\n" +
									  "Your configuration must be fixed before PEX will enable\n" +
									  "Details: " + e.getMessage() + "\n" +
									  "=======================================================", e);
	}

	@Override
	public void onLoad() {
		try {
			this.config = new PermissionsExConfig(this.getConfig(), this);
			this.commandsManager = new CommandsManager(this);

			if (!getServer().getOnlineMode()) {
				getLogger().log(Level.WARNING, "This server is in offline mode. Unless this server is configured to integrate with a supported proxy (see http://dft.ba/-8ous), UUIDs *may not be stable*!");
			}
			//this.permissionsManager = new PermissionManager(this.config);
		/*} catch (PermissionBackendException e) {
			logBackendExc(e);
			errored = true;*/
		} catch (Throwable t) {
			ErrorReport.handleError("In onLoad", t);
			errored = true;
		}
	}

	@Override
	public void onEnable() {
		if (errored) {
			getLogger().severe("==== PermissionsEx could not be enabled due to an earlier error. Look at the previous server log for more info ====");
			this.getPluginLoader().disablePlugin(this);
			return;
		}
		try {
			try {
				CacheBuilder.class.getMethod("maximumSize", long.class);
			} catch (NoSuchMethodException e) {
				getLogger().severe("=================================================================================");
				getLogger().severe("As of version 1.23, PEX is only compatible with versions of Minecraft 1.8 or greater. " +
						"Please downgrade to the most recent 1.22.x series version of PEX to continue.");
				getLogger().severe("=================================================================================");
				getPluginLoader().disablePlugin(this);
				return;
			}

			if (this.permissionsManager == null) {
				this.permissionsManager = new PermissionManager(config, getLogger(), this);
			}

			try {
				OfflinePlayer.class.getMethod("getUniqueId");
			} catch (NoSuchMethodException e) {
				getLogger().severe("=================================================================================");
				getLogger().severe("As of version 1.21, PEX requires a version of Bukkit with UUID support to function (>1.7.5). Please download a non-UUID version of PermissionsEx to continue.");
				getLogger().severe("Beginning reversion of potential invalid UUID conversion");
				getPermissionsManager().getBackend().revertUUID();
				getLogger().severe("Reversion complete, disabling. PermissionsEx will not work until downgrade is complete");
				getLogger().severe("=================================================================================");
				getPluginLoader().disablePlugin(this);
				return;
			}

			// Register commands
			this.commandsManager.register(new UserCommands());
			this.commandsManager.register(new GroupCommands());
			this.commandsManager.register(new PromotionCommands());
			this.commandsManager.register(new WorldCommands());
			this.commandsManager.register(new UtilityCommands());

			// Register Player permissions cleaner
			PlayerEventsListener cleaner = new PlayerEventsListener();
			this.getServer().getPluginManager().registerEvents(cleaner, this);

			// Register service
			this.getServer().getServicesManager().register(PermissionManager.class, this.permissionsManager, this, ServicePriority.Normal);
			regexPerms = new RegexPermissions(this);
			superms = new SuperpermsListener(this);
			this.getServer().getPluginManager().registerEvents(superms, this);
			this.saveConfig();

			// Start timed permissions cleaner timer
			this.permissionsManager.initTimer();
			if (config.updaterEnabled()) {
				final Updater updater = new Updater(this, BUKKITDEV_ID, this.getFile(), Updater.UpdateType.DEFAULT, false) {
					/**
					 * Customized update check function.
					 * If update is only a difference in minor version (supermajor.major.minor)
					 * @param localVerString Local version in string form
					 * @param remoteVerString Remote version in string format
					 * @return
					 */
					@Override
					public boolean shouldUpdate(String localVerString, String remoteVerString) {
						if (localVerString.equals(remoteVerString)) { // Versions are equal
							return false;
						}

						if (config.alwaysUpdate()) {
							return true;
						}

						if (localVerString.endsWith("-SNAPSHOT") || remoteVerString.endsWith("-SNAPSHOT")) { // Don't update when a dev build is involved
							return false;
						}

						String[] localVer = localVerString.split("\\.");
						int localSuperMajor = Integer.parseInt(localVer[0]);
						int localMajor = localVer.length > 1 ? Integer.parseInt(localVer[1]) : 0;
						int localMinor = localVer.length > 2 ? Integer.parseInt(localVer[2]) : 0;
						String[] remoteVer = remoteVerString.split("\\.");
						int remoteSuperMajor = Integer.parseInt(remoteVer[0]);
						int remoteMajor = remoteVer.length > 1 ? Integer.parseInt(remoteVer[1]) : 0;
						int remoteMinor = remoteVer.length > 2 ? Integer.parseInt(remoteVer[2]) : 0;

						if (localSuperMajor > remoteSuperMajor
								|| (localSuperMajor == remoteSuperMajor && localMajor > remoteMajor)
								|| (localSuperMajor == remoteSuperMajor && localMajor == remoteMajor && localMinor >= remoteMinor)) {
							return false; // Local version is newer or same as remote version
						}
						if (localSuperMajor == remoteSuperMajor && localMajor == remoteMajor) {
							// Versions aren't equal but major version is, this is a minor update
							return true;
						} else {
							getLogger().warning("An update to " + getDescription().getName() + " version " + remoteVerString + " is available to download from" +
									" http://dev.bukkit.org/bukkit-plugins/permissionsex/. Please review the changes and update as soon as possible!");
							return false;
						}

					}
				};
				getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
					@Override
					public void run() {
						switch (updater.getResult()) {
							case SUCCESS:
								getLogger().info("An update to " + updater.getLatestName() + " was downloaded and will be applied on next server launch.");
						}
					}
				});
			}
			if (getConfiguration().useNetEvents()) {
				Plugin netEventsPlugin = getServer().getPluginManager().getPlugin("NetEvents");
				if (netEventsPlugin != null && netEventsPlugin.isEnabled()) {
					NetEventsPlugin netEvents = (NetEventsPlugin) netEventsPlugin;
					getServer().getPluginManager().registerEvents(new RemoteEventListener(netEvents, permissionsManager), this);
					this.netEvents = netEvents;
				}
			}
		} catch (PermissionBackendException e) {
			logBackendExc(e);
			this.getPluginLoader().disablePlugin(this);
		} catch (Throwable t) {
			ErrorReport.handleError("Error while enabling: ", t);
			this.getPluginLoader().disablePlugin(this);
		}
	}

	@Override
	public void onDisable() {
		try {
			if (this.permissionsManager != null) {
				this.permissionsManager.end();
				this.getServer().getServicesManager().unregister(PermissionManager.class, this.permissionsManager);
				this.permissionsManager = null;
			}

			if (this.regexPerms != null) {
				this.regexPerms.onDisable();
				this.regexPerms = null;
			}
			if (this.superms != null) {
				this.superms.onDisable();
				this.superms = null;
			}

		} catch (Throwable t) {
			ErrorReport.handleError("While disabling", t);
		}
		ErrorReport.shutdown();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
		try {
			PluginDescriptionFile pdf = this.getDescription();
			if (args.length > 0) {
				return this.commandsManager.execute(sender, command, args);
			} else {
				if (sender instanceof Player) {
					sender.sendMessage("[" + ChatColor.RED + "PermissionsEx" + ChatColor.WHITE + "] version [" + ChatColor.BLUE + pdf.getVersion() + ChatColor.WHITE + "]");

					return this.permissionsManager == null || !this.permissionsManager.has((Player) sender, "permissions.manage");
				} else {
					sender.sendMessage("[PermissionsEx] version [" + pdf.getVersion() + "]");

					return false;
				}
			}
		} catch (Throwable t) {
			ErrorReport.handleError("While " + sender.getName() + " was executing /" + command.getName() + " " + StringUtils.implode(args, " "), t, sender);
			return true;
		}
	}

	public boolean requiresLateUserSetup() {
		return getServer().getPluginManager().isPluginEnabled("LilyPad-Connect");
	}

	public PermissionsExConfig getConfiguration() {
		return config;
	}

	public boolean isDebug() {
		return permissionsManager != null && permissionsManager.isDebug();
	}

	public static Plugin getPlugin() {
		return instance;
	}

	public RegexPermissions getRegexPerms() {
		return regexPerms;
	}

	@Override
	public String UUIDToName(UUID uid) {
		OfflinePlayer ply = null;
		try {
			ply = getServer().getPlayer(uid); // to make things cheaper, we're just checking online players (can be improved later on)
			// Also, only online players are really necessary to convert to proper names
		} catch (NoSuchMethodError e) {
			// Old craftbukkit, guess we won't have a fallback name. Much shame.
		}
		return ply != null ? ply.getName() : null;
	}

	@Override
	public UUID nameToUUID(String name) {
		OfflinePlayer player = getServer().getOfflinePlayer(name);
		UUID userUUID = null;
		try {
			userUUID = player.getUniqueId();
		} catch (Throwable t) {
			// Handle cases where the plugin is not running on a uuid-aware Bukkit by just not converting here
		}
		return userUUID;
	}

	@Override
	public boolean isOnline(UUID uuid) {
		Player player = getServer().getPlayer(uuid);
		return (player != null && player.isOnline());
	}

	@Override
	public UUID getServerUUID() {
		return netEvents == null ? null : netEvents.getServerUUID();
	}

	@Override
	public void callEvent(PermissionEvent event) {
		if (netEvents != null) {
			netEvents.callEvent(event);
		} else {
			getServer().getPluginManager().callEvent(event);
		}
	}

	public static boolean isAvailable() {
		Plugin plugin = getPlugin();

		return plugin.isEnabled() && ((PermissionsEx) plugin).permissionsManager != null;
	}

	public static PermissionManager getPermissionManager() {
		if (!isAvailable()) {
			throw new PermissionsNotAvailable();
		}

		return ((PermissionsEx) getPlugin()).permissionsManager;
	}

	public PermissionManager getPermissionsManager() {
		return permissionsManager;
	}

	public static PermissionUser getUser(Player player) {
		return getPermissionManager().getUser(player);
	}

	public static PermissionUser getUser(String name) {
		return getPermissionManager().getUser(name);
	}

	public boolean has(Player player, String permission) {
		return this.permissionsManager.has(player, permission);
	}

	public boolean has(Player player, String permission, String world) {
		return this.permissionsManager.has(player, permission, world);
	}

	public class PlayerEventsListener implements Listener {
		@EventHandler(priority = EventPriority.MONITOR)
		public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
			if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED && !requiresLateUserSetup()) {
				getPermissionsManager().cacheUser(event.getUniqueId().toString(), event.getName());
			}
		}

		@EventHandler
		public void onPlayerLogin(PlayerJoinEvent event) {
			try {
				PermissionUser user = getPermissionsManager().getUser(event.getPlayer());
				if (!user.isVirtual()) {
					if (!event.getPlayer().getName().equals(user.getOption("name"))) { // Update name only if user exists in config
						user.setOption("name", event.getPlayer().getName());
					}
					if (!config.shouldLogPlayers()) {
						return;
					}
					user.setOption("last-login-time", Long.toString(System.currentTimeMillis() / 1000L));
					// user.setOption("last-login-ip", event.getPlayer().getAddress().getAddress().getHostAddress()); // somehow this won't work
				}
			} catch (Throwable t) {
				ErrorReport.handleError("While login cleanup event", t);
			}
		}

		@EventHandler
		public void onPlayerQuit(PlayerQuitEvent event) {
			try {
				PermissionUser user = getPermissionsManager().getUser(event.getPlayer());
				if (!user.isVirtual()) {
					if (config.shouldLogPlayers()) {
						user.setOption("last-logout-time", Long.toString(System.currentTimeMillis() / 1000L));
					}

					user.getName(); // Set name if user was created during server run
				}
				getPermissionsManager().resetUser(event.getPlayer());
			} catch (Throwable t) {
				ErrorReport.handleError("While logout cleanup event", t);
			}
		}
	}
}
