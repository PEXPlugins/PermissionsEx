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
package ru.tehkode.permissions;

import com.zachsthings.netevents.NetEventsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import ru.tehkode.permissions.backends.PermissionBackend;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.events.PermissionEntityEvent;
import ru.tehkode.permissions.events.PermissionEvent;
import ru.tehkode.permissions.events.PermissionSystemEvent;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author t3hk0d3
 */
public class PermissionManager {

	public final static int TRANSIENT_PERMISSION = 0;
	protected Map<String, PermissionUser> users = new HashMap<>();
	protected Map<String, PermissionGroup> groups = new HashMap<>();
	protected Map<String, PermissionGroup> defaultGroups = new HashMap<>();
	protected PermissionBackend backend = null;
	private final PermissionsEx plugin;
	protected Timer timer;
	protected boolean debugMode = false;
	protected boolean allowOps = false;
	protected boolean userAddGroupsLast = false;
	private NetEventsPlugin netEvents;

	protected PermissionMatcher matcher = new RegExpMatcher();

	public PermissionManager(PermissionsEx plugin) throws PermissionBackendException {
		this.plugin = plugin;
		if (plugin.getConfiguration().useNetEvents()) {
			Plugin netEventsPlugin = plugin.getServer().getPluginManager().getPlugin("NetEvents");
			if (netEventsPlugin != null && netEventsPlugin.isEnabled()) {
				this.netEvents = (NetEventsPlugin) netEventsPlugin;
				plugin.getServer().getPluginManager().registerEvents(new RemoteEventListener(), plugin);
			}
		}
		this.debugMode = plugin.getConfiguration().isDebug();
		this.allowOps = plugin.getConfiguration().allowOps();
		this.userAddGroupsLast = plugin.getConfiguration().userAddGroupsLast();
		this.initBackend();
	}

	UUID getServerUUID() {
		return netEvents != null ? netEvents.getServerUUID() : null;
	}

	public boolean isLocal(PermissionEvent event) {
		return netEvents == null || event.getSourceUUID().equals(netEvents.getServerUUID());

	}

	public boolean shouldCreateUserRecords() {
		return plugin.getConfiguration().createUserRecords();
	}

	public PermissionsEx getPlugin() {
		return plugin;
	}

	private class RemoteEventListener implements Listener {
		@EventHandler(priority = EventPriority.LOWEST)
		public void onEntityEvent(PermissionEntityEvent event) {
			if (isLocal(event)) {
				return;
			}
			final boolean reloadEntity, reloadAll;

			switch (event.getAction()) {
				case DEFAULTGROUP_CHANGED:
				case RANK_CHANGED:
				case INHERITANCE_CHANGED:
					reloadAll = true;
					reloadEntity = false;
					break;
				case SAVED:
				case TIMEDPERMISSION_EXPIRED:
					return;
				default:
					reloadEntity = true;
					reloadAll = false;
					break;
			}

			try {
				if (backend != null) {
					backend.reload();
				}
			} catch (PermissionBackendException e) {
				e.printStackTrace();
			}
			if (reloadEntity) {
				switch (event.getType()) {
					case USER:
						users.remove(event.getEntityIdentifier());
						break;
					case GROUP:
						PermissionGroup group = groups.remove(event.getEntityIdentifier());
						if (group != null) {
							for (Iterator<PermissionUser> it = users.values().iterator(); it.hasNext(); ) {
								if (it.next().inGroup(group, true)) {
									it.remove();
								}
							}
						}

						break;
				}
			} else if (reloadAll) {
				clearCache();
			}
		}

		@EventHandler(priority = EventPriority.LOWEST)
		public void onSystemEvent(PermissionSystemEvent event) {
			if (isLocal(event)) {
				return;
			}

			switch (event.getAction()) {
				case BACKEND_CHANGED:
				case DEBUGMODE_TOGGLE:
				case REINJECT_PERMISSIBLES:
					return;
			}

			try {
				if (backend != null) {
					backend.reload();
				}
				clearCache();
			} catch (PermissionBackendException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Check if specified player has specified permission
	 *
	 * @param player     player object
	 * @param permission permission string to check against
	 * @return true on success false otherwise
	 */
	public boolean has(Player player, String permission) {
		return this.has(player.getUniqueId(), permission, player.getWorld().getName());
	}

	/**
	 * Check if player has specified permission in world
	 *
	 * @param player     player object
	 * @param permission permission as string to check against
	 * @param world      world's name as string
	 * @return true on success false otherwise
	 */
	public boolean has(Player player, String permission, String world) {
		return this.has(player.getUniqueId(), permission, world);
	}

	/**
	 * Check if player with name has permission in world
	 *
	 * @param playerName player name
	 * @param permission permission as string to check against
	 * @param world      world's name as string
	 * @return true on success false otherwise
	 */
	public boolean has(String playerName, String permission, String world) {
		PermissionUser user = this.getUser(playerName);

		if (user == null) {
			return false;
		}

		return user.has(permission, world);
	}

	/**
	 * Check if player with UUID has permission in world
	 *
	 * @param playerId player name
	 * @param permission permission as string to check against
	 * @param world      world's name as string
	 * @return true on success false otherwise
	 */
	public boolean has(UUID playerId, String permission, String world) {
		PermissionUser user = this.getUser(playerId);

		return user != null && user.has(permission, world);

	}

	/**
	 * Return user's object
	 *
	 * @param username get PermissionUser with given name
	 * @return PermissionUser instance
	 */
	public PermissionUser getUser(String username) {
		if (username == null || username.isEmpty()) {
			throw new IllegalArgumentException("Null or empty name passed! Name must not be empty");
		}

		try {
			if (username.length() != 36) { // Speedup for things def not uuids
				throw new IllegalArgumentException("not a uuid, try stuff");
			}
			return getUser(UUID.fromString(username)); // Username is uuid as string, just use it
		} catch (IllegalArgumentException ex) {
			OfflinePlayer player = plugin.getServer().getOfflinePlayer(username);
			UUID userUUID = null;
			try {
				userUUID = player instanceof Player ? ((Player) player).getUniqueId() : player.getUniqueId();
			} catch (Throwable t) {
				// Handle cases where the plugin is not running on a uuid-aware Bukkit by just not converting here
			}

			if (userUUID != null && (player.isOnline() || backend.hasUser(userUUID.toString()))) {
				return getUser(userUUID.toString(), username, player.isOnline());
			} else {
				// The user is offline and unconverted, so we'll just have to return an unconverted user.
				return getUser(username, null, player.isOnline());
			}
		}
	}

	/**
	 * Return object of specified player
	 *
	 * @param player player object
	 * @return PermissionUser instance
	 */
	public PermissionUser getUser(Player player) {
		return this.getUser(player.getUniqueId().toString(), player.getName(), true);
	}

	public PermissionUser getUser(UUID uid) {
		final String identifier = uid.toString();
		if (users.containsKey(identifier.toLowerCase())) {
			return getUser(identifier, null, false);
		}
		OfflinePlayer ply = null;
		try {
			ply = plugin.getServer().getPlayer(uid); // to make things cheaper, we're just checking online players (can be improved later on)
			// Also, only online players are really necessary to convert to proper names
		} catch (NoSuchMethodError e) {
			// Old craftbukkit, guess we won't have a fallback name. Much shame.
		}
		String fallbackName = null;
		if (ply != null) {
			fallbackName = ply.getName();
		}
		return getUser(identifier, fallbackName, ply != null);
	}

	private PermissionUser getUser(String identifier, String fallbackName, boolean store) {
		PermissionUser user = users.get(identifier.toLowerCase());

		if (user != null) {
			return user;
		}

		PermissionsUserData data = backend.getUserData(identifier);
		if (data != null) {
			if (fallbackName != null) {
				if (data.isVirtual() && backend.hasUser(fallbackName)) {
					if (isDebug()) {
						getLogger().info("Converting user " + fallbackName + " (UUID " + identifier + ") to UUID-based storage");
					}

					PermissionsUserData oldData = backend.getUserData(fallbackName);
					if (oldData.setIdentifier(identifier)) {
						data = oldData;
						data.setOption("name", fallbackName, null);
						resetUser(fallbackName); // In case somebody requested the old user but conversion was previously unsuccessful
					} else {
						throw new IllegalStateException("User already exists with new id " + identifier + " (converting from " + fallbackName + ")");
					}
				}
			}
			user = new PermissionUser(identifier, data, this);
			user.initialize();
			if (store) {
				this.users.put(identifier.toLowerCase(), user);
			}
		} else {
			throw new IllegalStateException("User " + identifier + " is null");
		}

		return user;
	}

	/**
	 * Return all registered user objects
	 *
	 * @return unmodifiable list of users
	 */
	public Set<PermissionUser> getUsers() {
		Set<PermissionUser> users = new HashSet<>();
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			users.add(getUser(p));
		}
		for (String name : backend.getUserIdentifiers()) {
			users.add(getUser(name));
		}
		return Collections.unmodifiableSet(users);
	}

	/**
	 * Return users currently cached in memory
	 *
	 * @return A copy of the list of users cached in memory
	 */
	public Set<PermissionUser> getActiveUsers() {
		return new HashSet<>(users.values());
	}

	public Collection<String> getUserIdentifiers() {
		return backend.getUserIdentifiers();
	}

	public Collection<String> getUserNames() {
		return backend.getUserNames();
	}

	Set<PermissionUser> getActiveUsers(String groupName, boolean inheritance) {
		Set<PermissionUser> users = new HashSet<>();

		for (PermissionUser user : this.getActiveUsers()) {
			if (user.inGroup(groupName, inheritance)) {
				users.add(user);
			}
		}

		return Collections.unmodifiableSet(users);
	}

	Set<PermissionUser> getActiveUsers(String groupName) {
		return getActiveUsers(groupName, false);
	}
	/**
	 * Return all users in group
	 *
	 * @param groupName group's name
	 * @return PermissionUser array
	 */
	public Set<PermissionUser> getUsers(String groupName, String worldName) {
		return getUsers(groupName, worldName, false);
	}

	public Set<PermissionUser> getUsers(String groupName) {
		return getUsers(groupName, false);
	}

	/**
	 * Return all users in group and descendant groups
	 *
	 * @param groupName   group's name
	 * @param inheritance true return members of descendant groups of specified group
	 * @return PermissionUser array for groupnName
	 */
	public Set<PermissionUser> getUsers(String groupName, String worldName, boolean inheritance) {
		Set<PermissionUser> users = new HashSet<>();

		for (PermissionUser user : this.getUsers()) {
			if (user.inGroup(groupName, worldName, inheritance)) {
				users.add(user);
			}
		}

		return Collections.unmodifiableSet(users);
	}

	public Set<PermissionUser> getUsers(String groupName, boolean inheritance) {
		Set<PermissionUser> users = new HashSet<>();

		for (PermissionUser user : this.getUsers()) {
			if (user.inGroup(groupName, inheritance)) {
				users.add(user);
			}
		}

		return Collections.unmodifiableSet(users);
	}

	/**
	 * Reset in-memory object of specified user
	 *
	 * @param userName user's name
	 */
	public void resetUser(String userName) {
		this.users.remove(userName.toLowerCase());
	}

	public void resetUser(Player ply) {
		this.users.remove(ply.getUniqueId().toString());
		resetUser(ply.getName());
	}

	/**
	 * Clear cache for specified user
	 *
	 * @param userName
	 */
	public void clearUserCache(String userName) {
		PermissionUser user = this.getUser(userName);

		if (user != null) {
			user.clearCache();
		}
	}

	public void clearUserCache(UUID uid) {
		PermissionUser user = this.getUser(uid);

		if (user != null) {
			user.clearCache();
		}
	}

	/**
	 * Clear cache for specified player
	 *
	 * @param player
	 */
	public void clearUserCache(Player player) {
		this.clearUserCache(player.getUniqueId());
	}

	/**
	 * Return object for specified group
	 *
	 * @param groupname group's name
	 * @return PermissionGroup object
	 */
	public PermissionGroup getGroup(String groupname) {
		if (groupname == null || groupname.isEmpty()) {
			return null;
		}

		PermissionGroup group = groups.get(groupname.toLowerCase());

		if (group == null) {
			PermissionsGroupData data = this.backend.getGroupData(groupname);
			if (data != null) {
				group = new PermissionGroup(groupname, data, this);
				this.groups.put(groupname.toLowerCase(), group);
				try {
					group.initialize();
				} catch (Exception e) {
					this.groups.remove(groupname.toLowerCase());
					throw new IllegalStateException("Error initializing group " + groupname, e);
				}
			} else {
				throw new IllegalStateException("Group " + groupname + " is null");
			}
		}

		return group;
	}

	/**
	 * Return all groups
	 *
	 * @return PermissionGroup array
	 */
	public List<PermissionGroup> getGroupList() {
		List<PermissionGroup> ret = new LinkedList<>();
		for (String name : backend.getGroupNames()) {
			ret.add(getGroup(name));
		}
		return Collections.unmodifiableList(ret);
	}

	@Deprecated
	public PermissionGroup[] getGroups() {
		return getGroupList().toArray(new PermissionGroup[0]);
	}

	/**
	 * Return all child groups of specified group
	 *
	 * @param groupName group's name
	 * @return PermissionGroup array
	 */
	public List<PermissionGroup> getGroups(String groupName, String worldName) {
		return getGroups(groupName, worldName, false);
	}

	public List<PermissionGroup> getGroups(String groupName) {
		return getGroups(groupName, null);
	}

	/**
	 * Return all descendants or child groups for groupName
	 *
	 * @param groupName   group's name
	 * @param inheritance true: only direct child groups would be returned
	 * @return unmodifiable PermissionGroup list for specified groupName
	 */
	public List<PermissionGroup> getGroups(String groupName, String worldName, boolean inheritance) {
		List<PermissionGroup> groups = new LinkedList<>();

		for (PermissionGroup group : this.getGroupList()) {
			if (!groups.contains(group) && group.isChildOf(groupName, worldName, inheritance)) {
				groups.add(group);
			}
		}

		return Collections.unmodifiableList(groups);
	}

	public List<PermissionGroup> getGroups(String groupName, boolean inheritance) {
		List<PermissionGroup> groups = new ArrayList<>();

		for (World world : Bukkit.getServer().getWorlds()) {
			groups.addAll(getGroups(groupName, world.getName(), inheritance));
		}

		// Common space users
		groups.addAll(getGroups(groupName, null, inheritance));

		Collections.sort(groups);

		return Collections.unmodifiableList(groups);
	}

	/**
	 * Return all known default groups
	 *
	 * @param worldName World to check (will include global scope)
	 * @return All default groups
	 */
	public List<PermissionGroup> getDefaultGroups(String worldName) {
		List<PermissionGroup> defaults = new LinkedList<>();
		for (String name : this.getBackend().getDefaultGroupNames(worldName)) {
			defaults.add(getGroup(name));
		}

		if (worldName != null) {
			for (String name : this.getBackend().getDefaultGroupNames(null)) {
				defaults.add(getGroup(name));
			}
		}

		return Collections.unmodifiableList(defaults);
	}

	/**
	 * Reset in-memory object for groupName
	 *
	 * @param groupName group's name
	 */
	public void resetGroup(String groupName) {
		this.groups.remove(groupName);
	}

	void preloadGroups() {
		for (PermissionGroup group : getGroupList()) {
			group.getData().load();
		}
	}

	/**
	 * Set debug mode
	 *
	 * @param debug true enables debug mode, false disables
	 */
	public void setDebug(boolean debug) {
		this.debugMode = debug;
		this.callEvent(PermissionSystemEvent.Action.DEBUGMODE_TOGGLE);
	}

	/**
	 * Return current state of debug mode
	 *
	 * @return true debug is enabled, false if disabled
	 */
	public boolean isDebug() {
		return debugMode;
	}

	/**
	 * Return groups of specified rank ladder
	 *
	 * @param ladderName
	 * @return Map of ladder, key - rank of group, value - group object. Empty map if ladder does not exist
	 */
	public Map<Integer, PermissionGroup> getRankLadder(String ladderName) {
		Map<Integer, PermissionGroup> ladder = new HashMap<>();

		for (PermissionGroup group : this.getGroupList()) {
			if (!group.isRanked()) {
				continue;
			}

			if (group.getRankLadder().equalsIgnoreCase(ladderName)) {
				ladder.put(group.getRank(), group);
			}
		}

		return ladder;
	}

	/**
	 * Return array of world names who has world inheritance
	 *
	 * @param worldName World name
	 * @return Array of parent world, if world does not exist return empty array
	 */
	public List<String> getWorldInheritance(String worldName) {
		return backend.getWorldInheritance(worldName);
	}

	/**
	 * Set world inheritance parents for world
	 *
	 * @param world        world name which inheritance should be set
	 * @param parentWorlds array of parent world names
	 */
	public void setWorldInheritance(String world, List<String> parentWorlds) {
		backend.setWorldInheritance(world, parentWorlds);
		this.callEvent(PermissionSystemEvent.Action.WORLDINHERITANCE_CHANGED);
	}

	/**
	 * Return current backend
	 *
	 * @return current backend object
	 */
	public PermissionBackend getBackend() {
		return this.backend;
	}

	/**
	 * Set backend to specified backend.
	 * This would also cause backend resetting.
	 *
	 * @param backendName name of backend to set to
	 */
	public void setBackend(String backendName) throws PermissionBackendException {
		synchronized (this) {
			this.clearCache();
			this.backend = createBackend(backendName);
			this.preloadGroups();
		}

		this.callEvent(PermissionSystemEvent.Action.BACKEND_CHANGED);
	}

	/**
	 * Creates a backend but does not set it as the active backend. Useful for data transfer & such
	 * @param backendName Name of the configuration section which describes this backend
	 */
	public PermissionBackend createBackend(String backendName) throws PermissionBackendException {
		ConfigurationSection config = plugin.getConfiguration().getBackendConfig(backendName);
		String backendType = config.getString("type");
		if (backendType == null) {
			config.set("type", backendType = backendName);
		}

		return PermissionBackend.getBackend(backendType, this, config);
	}

	/**
	 * Register new timer task
	 *
	 * @param task  TimerTask object
	 * @param delay delay in seconds
	 */
	protected void registerTask(TimerTask task, int delay) {
		if (timer == null || delay == TRANSIENT_PERMISSION) {
			return;
		}

		timer.schedule(task, delay * 1000);
	}

	/**
	 * Reset all in-memory groups and users, clean up runtime stuff, reloads backend
	 */
	public void reset() throws PermissionBackendException {
		this.clearCache();

		if (this.backend != null) {
			this.backend.reload();
		}
		this.callEvent(PermissionSystemEvent.Action.RELOADED);
	}

	public void end() {
		try {
			this.backend.close();
			this.backend = null;
			reset();
		} catch (PermissionBackendException ignore) {
			// Ignore because we're shutting down so who cares
		}
		timer.cancel();
	}

	public void initTimer() {
		if (timer != null) {
			timer.cancel();
		}

		timer = new Timer("PermissionsEx-Cleaner");
	}

	protected void clearCache() {
		this.users.clear();
		this.groups.clear();
		this.defaultGroups.clear();

		// Close old timed Permission Timer
		this.initTimer();
	}

	private void initBackend() throws PermissionBackendException {
		this.setBackend(plugin.getConfiguration().getDefaultBackend());
	}

	protected void callEvent(PermissionEvent event) {
		if (netEvents != null) {
			netEvents.callEvent(event);
		} else {
			Bukkit.getServer().getPluginManager().callEvent(event);
		}
	}

	protected void callEvent(PermissionSystemEvent.Action action) {
		this.callEvent(new PermissionSystemEvent(getServerUUID(), action));
	}

	public PermissionMatcher getPermissionMatcher() {
		return matcher;
	}

	public void setPermissionMatcher(PermissionMatcher matcher) {
		this.matcher = matcher;
	}

	public Collection<String> getGroupNames() {
		return backend.getGroupNames();
	}

	public Logger getLogger() {
		return plugin.getLogger();
	}
}
