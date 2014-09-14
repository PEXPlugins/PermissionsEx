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

import ru.tehkode.permissions.backends.PermissionBackend;
import ru.tehkode.permissions.sponge.PermissionsExConfig;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.events.MatcherGroupEvent;
import ru.tehkode.permissions.events.PermissionEvent;
import ru.tehkode.permissions.events.PermissionSystemEvent;
import ru.tehkode.permissions.exceptions.PermissionBackendException;
import ru.tehkode.utils.PrefixedThreadFactory;

import java.util.*;
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
		// Close old timed Permission Timer
		this.initTimer();
	}

	private void initBackend() throws PermissionBackendException {
		this.setBackend(config.getDefaultBackend());
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
}
