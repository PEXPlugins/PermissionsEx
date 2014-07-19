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

import com.google.common.util.concurrent.Futures;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import ru.tehkode.permissions.backends.PermissionBackend;
import ru.tehkode.permissions.bukkit.PermissionsExConfig;
import ru.tehkode.permissions.data.Context;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;
import ru.tehkode.permissions.events.MatcherGroupEvent;
import ru.tehkode.permissions.events.PermissionEvent;
import ru.tehkode.permissions.events.PermissionSystemEvent;
import ru.tehkode.permissions.exceptions.PermissionBackendException;
import ru.tehkode.permissions.query.GetQuery;
import ru.tehkode.permissions.query.PermissionQuery;
import ru.tehkode.permissions.query.SetQuery;
import ru.tehkode.utils.PrefixedThreadFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author t3hk0d3
 */
public class PermissionManager {
	public final static int TRANSIENT_PERMISSION = 0;
	protected PermissionBackend backend = null;
	private final PermissionsExConfig config;
	private final NativeInterface nativeI;
	private final Logger logger;
	private ScheduledExecutorService service;
	protected boolean debugMode = false;
	protected boolean allowOps = false;
	protected boolean userAddGroupsLast = false;
	protected PermissionMatcher matcher = new RegExpMatcher();
	private final Set<UUID> debugUsers = new HashSet<>();
	private final ConcurrentMap<PermissionQuery.CacheKey, PermissionQuery.CacheElement> queryCache = new ConcurrentHashMap<>();

	public PermissionManager(PermissionsExConfig config, Logger logger, NativeInterface nativeI) throws PermissionBackendException {
		this.config = config;
		this.logger = logger;
		this.nativeI = nativeI;
		this.debugMode = config.isDebug();
		this.allowOps = config.allowOps();
		this.userAddGroupsLast = config.userAddGroupsLast();
		this.initBackend();
	}

	/**
	 * Gets the (persistent) UUID associated with this server. This may be null if no provider for the UUID is available.
	 * Currently the only provider used for this is the NetEvents plugin.
	 *
	 * @return The UUID of this server
	 */
	UUID getServerUUID() {
		return nativeI.getServerUUID();
	}

	public boolean shouldCreateUserRecords() {
		return config.createUserRecords();
	}

	public PermissionsExConfig getConfiguration() {
		return config;
	}

	/**
	 * Check if specified player has specified permission
	 *
	 * @param player     player object
	 * @param permission permission string to check against
	 * @return true on success false otherwise
	 */
	public boolean has(Player player, String permission) {
		return Futures.getUnchecked(get().userAndWorld(player).has(permission));
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
		return Futures.getUnchecked(get().user(player).world(world).has(permission));
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
		return Futures.getUnchecked(get().user(playerName).world(world).has(permission));
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
		return Futures.getUnchecked(get().user(playerId).world(world).has(permission));
	}

	public GetQuery get() {
		return new GetQuery(this, queryCache);
	}

	public SetQuery set() {
		return new SetQuery(this, queryCache);
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
	 * Set debug mode for a single user.
	 *
	 * @param player The player to set
	 * @param debug The value to set
	 */
	public void setDebug(Player player, boolean debug) {
		if (debug) {
			debugUsers.add(player.getUniqueId());
		} else {
			debugUsers.remove(player.getUniqueId());
		}
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
	 * Returns whether a user has debug mode enabled
	 *
	 * @param player The player to check for debug mode
	 * @return Whether this player has debug mode enabled
	 */
	public boolean isDebug(Player player) {
		return isDebug() || debugUsers.contains(player.getUniqueId());
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
			//this.preloadGroups();
		}

		this.callEvent(PermissionSystemEvent.Action.BACKEND_CHANGED);
	}

	/**
	 * Creates a backend but does not set it as the active backend. Useful for data transfer & such
	 * @param backendName Name of the configuration section which describes this backend
	 */
	public PermissionBackend createBackend(String backendName) throws PermissionBackendException {
		ConfigurationSection config = this.config.getBackendConfig(backendName);
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
		if (service == null || delay == TRANSIENT_PERMISSION) {
			return;
		}

		service.schedule(task, delay, TimeUnit.SECONDS);
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
			if (this.backend != null) {
				this.backend.close();
				this.backend = null;
			}
			reset();
		} catch (PermissionBackendException ignore) {
			// Ignore because we're shutting down so who cares
		}
		service.shutdown();
	}

	public void initTimer() {
		if (service != null) {
			service.shutdown();
		}

		service = Executors.newSingleThreadScheduledExecutor(new PrefixedThreadFactory("PEX-manager"));
	}

	protected void clearCache() {
		queryCache.clear();
		// Close old timed Permission Timer
		this.initTimer();
	}

	private void initBackend() throws PermissionBackendException {
		this.setBackend(config.getDefaultBackend());
	}

	public void callEvent(PermissionEvent event) {
		nativeI.callEvent(event);
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

	public Logger getLogger() {
		return logger;
	}

	/*
	 * DEPRECATED METHODS FOLLOW. IF THE FUNCTIONALITY THEY PROVIDE IS NOT IN THE NEW API, IT WILL PROBABLY BE ADDED
	 * Javadocs not provided
	 */

	@Deprecated
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
			UUID userUUID = nativeI.nameToUUID(username);

			if (userUUID != null && (nativeI.isOnline(userUUID) || Futures.getUnchecked(backend.hasAnyQualifier(Qualifier.USER, userUUID.toString())))) {
				return getUser(userUUID);
			} else {
				// The user is offline and unconverted, so we'll just have to return an unconverted user.
				return new PermissionUser(username, this);
			}
		}
	}

	@Deprecated
	public PermissionUser getUser(Player player) {
		return this.getUser(player.getUniqueId().toString());
	}

	@Deprecated
	public PermissionUser getUser(UUID uid) {
		return new PermissionUser(uid.toString(), this);
	}

	@Deprecated
	public List<String> getWorldInheritance(String worldName) {
		return Futures.getUnchecked(get()
				.followInheritance(false)
				.world(worldName)
				.worldParents());
	}

	@Deprecated
	public void setWorldInheritance(String world, List<String> parentWorlds) {
		set().world(world).setParents(parentWorlds);
		this.callEvent(PermissionSystemEvent.Action.WORLDINHERITANCE_CHANGED);
	}

	@Deprecated
	public Collection<String> getGroupNames() {
		return Futures.getUnchecked(backend.getAllValues(Qualifier.GROUP));
	}

	@Deprecated
	public Set<PermissionUser> getUsers() {
		Set<PermissionUser> users = new HashSet<>();
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			users.add(getUser(p));
		}
		for (String name : Futures.getUnchecked(backend.getAllValues(Qualifier.USER))) {
			users.add(new PermissionUser(name, this));
		}
		return Collections.unmodifiableSet(users);
	}

	@Deprecated
	public Set<PermissionUser> getActiveUsers() {
		return new HashSet<>();
	}

	@Deprecated
	public Collection<String> getUserIdentifiers() {
		return Futures.getUnchecked(backend.getAllValues(Qualifier.USER));
	}

	public Collection<String> getUserNames() {
		return backend.getUserNames();
	}

	@Deprecated
	Set<PermissionUser> getActiveUsers(String groupName, boolean inheritance) {
		Set<PermissionUser> users = new HashSet<>();

		for (PermissionUser user : this.getActiveUsers()) {
			if (user.inGroup(groupName, inheritance)) {
				users.add(user);
			}
		}

		return Collections.unmodifiableSet(users);
	}

	@Deprecated
	Set<PermissionUser> getActiveUsers(String groupName) {
		return getActiveUsers(groupName, false);
	}

	@Deprecated
	public Set<PermissionUser> getUsers(String groupName, String worldName) {
		return getUsers(groupName, worldName, false);
	}

	@Deprecated
	public Set<PermissionUser> getUsers(String groupName) {
		return getUsers(groupName, false);
	}

	@Deprecated
	public Set<PermissionUser> getUsers(String groupName, String worldName, boolean inheritance) {
		Set<PermissionUser> users = new HashSet<>();

		for (PermissionUser user : this.getUsers()) {
			if (user.inGroup(groupName, worldName, inheritance)) {
				users.add(user);
			}
		}

		return Collections.unmodifiableSet(users);
	}

	@Deprecated
	public Set<PermissionUser> getUsers(String groupName, boolean inheritance) {
		Set<PermissionUser> users = new HashSet<>();

		for (PermissionUser user : this.getUsers()) {
			if (user.inGroup(groupName, inheritance)) {
				users.add(user);
			}
		}

		return Collections.unmodifiableSet(users);
	}

	@Deprecated
	public void resetUser(String userName) {
	}

	@Deprecated
	public void resetUser(Player ply) {
	}

	@Deprecated
	public void clearUserCache(String userName) {
	}

	@Deprecated
	public void clearUserCache(UUID uid) {
	}

	@Deprecated
	public void clearUserCache(Player player) {
	}

	@Deprecated
	public PermissionGroup getGroup(String groupname) {
		if (groupname == null || groupname.isEmpty()) {
			return null;
		}

		return new PermissionGroup(groupname, this);
	}

	@Deprecated
	public List<PermissionGroup> getGroupList() {
		List<PermissionGroup> ret = new LinkedList<>();
		for (String name : Futures.getUnchecked(backend.getAllValues(Qualifier.GROUP))) {
			ret.add(getGroup(name));
		}
		return Collections.unmodifiableList(ret);
	}

	@Deprecated
	public PermissionGroup[] getGroups() {
		return getGroupList().toArray(new PermissionGroup[0]);
	}

	@Deprecated
	public List<PermissionGroup> getGroups(String groupName, String worldName) {
		return getGroups(groupName, worldName, false);
	}

	@Deprecated
	public List<PermissionGroup> getGroups(String groupName) {
		return getGroups(groupName, null);
	}

	@Deprecated
	public List<PermissionGroup> getGroups(String groupName, String worldName, boolean inheritance) {
		List<PermissionGroup> groups = new LinkedList<>();

		for (PermissionGroup group : this.getGroupList()) {
			if (!groups.contains(group) && group.isChildOf(groupName, worldName, inheritance)) {
				groups.add(group);
			}
		}

		return Collections.unmodifiableList(groups);
	}

	@Deprecated
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

	@Deprecated
	public List<PermissionGroup> getDefaultGroups(String worldName) {
		List<PermissionGroup> defaults = new LinkedList<>();
		for (PermissionGroup grp : getGroupList()) {
			if (grp.isDefault(worldName) || (worldName != null && grp.isDefault(null))) {
				defaults.add(grp);
			}
		}

		return Collections.unmodifiableList(defaults);
	}

	@Deprecated
	public PermissionGroup resetGroup(String groupName) {
		return null;
	}

	public void callEvent(MatcherGroup old, MatcherGroup newGroup, MatcherGroupEvent.Action action) {
		callEvent(new MatcherGroupEvent(getServerUUID(), old, newGroup, action));
	}
}
