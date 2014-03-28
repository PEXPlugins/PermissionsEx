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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import ru.tehkode.permissions.events.PermissionEntityEvent;

/**
 * @author code
 */
public abstract class PermissionEntity {
	public static enum Type {
		USER, GROUP;
	}

	protected PermissionManager manager;
	private String name;
	protected boolean virtual = true;
	protected Map<String, List<String>> timedPermissions = new ConcurrentHashMap<String, List<String>>();
	protected Map<String, Long> timedPermissionsTime = new ConcurrentHashMap<String, Long>();
	protected boolean debugMode = false;

	public PermissionEntity(String name, PermissionManager manager) {
		this.manager = manager;
		this.name = name;
	}

	protected abstract PermissionsData getData();

	/**
	 * This method 100% run after all constructors have been run and entity
	 * object, and entity object are completely ready to operate
	 */
	public void initialize() {
		this.debugMode = this.getOptionBoolean("debug", null, this.debugMode);
	}

	/**
	 * Return name of permission entity (User or Group)
	 * User should be equal to Player's name on the server
	 *
	 * @return name
	 */
	public String getName() {
		return this.name;
	}

	protected void setName(String name) {
		this.name = name;
	}

	public abstract Type getType();

	/**
	 * Returns entity prefix
	 *
	 * @param worldName
	 * @return prefix
	 */
	public String getPrefix(String worldName) {
		return getData().getPrefix(worldName);
	}

	public String getPrefix() {
		return this.getPrefix(null);
	}

	/**
	 * Returns entity prefix
	 *
	 */
	/**
	 * Set prefix to value
	 *
	 * @param prefix new prefix
	 */
	public void setPrefix(String prefix, String worldName) {
		getData().setPrefix(prefix, worldName);
		this.callEvent(PermissionEntityEvent.Action.INFO_CHANGED);
	}

	/**
	 * Return entity suffix
	 *
	 * @return suffix
	 */
	public String getSuffix(String worldName) {
		return getData().getSuffix(worldName);
	}

	public String getSuffix() {
		return getSuffix(null);
	}

	/**
	 * Set suffix to value
	 *
	 * @param suffix new suffix
	 */
	public void setSuffix(String suffix, String worldName) {
		getData().setSuffix(suffix, worldName);
		this.callEvent(PermissionEntityEvent.Action.INFO_CHANGED);
	}

	/**
	 * Checks if entity has specified permission in default world
	 *
	 * @param permission Permission to check
	 * @return true if entity has this permission otherwise false
	 */
	public boolean has(String permission) {
		return this.has(permission, Bukkit.getServer().getWorlds().get(0).getName());
	}

	/**
	 * Check if entity has specified permission in world
	 *
	 * @param permission Permission to check
	 * @param world      World to check permission in
	 * @return true if entity has this permission otherwise false
	 */
	public boolean has(String permission, String world) {
		if (permission != null && permission.isEmpty()) { // empty permission for public access :)
			return true;
		}

		String expression = getMatchingExpression(permission, world);

		if (this.isDebug()) {
			Logger.getLogger("Minecraft").info("User " + this.getName() + " checked for \"" + permission + "\", " + (expression == null ? "no permission found" : "\"" + expression + "\" found"));
		}

		return explainExpression(expression);
	}

	/**
	 * Return all entity permissions in specified world
	 *
	 * @param world World name
	 * @return Array of permission expressions
	 */
	public List<String> getPermissions(String world) {
		return getData().getPermissions(world);
	}

	/**
	 * Return permissions for all worlds
	 * Common permissions stored as "" (empty string) as world.
	 *
	 * @return Map with world name as key and permissions array as value
	 */
	public Map<String, List<String>> getAllPermissions() {
		return getData().getPermissionsMap();
	}

	/**
	 * Add permissions for specified world
	 *
	 * @param permission Permission to add
	 * @param world      World name to add permission to
	 */
	public void addPermission(String permission, String world) {
		throw new UnsupportedOperationException("You shouldn't call this method");
	}

	/**
	 * Add permission in common space (all worlds)
	 *
	 * @param permission Permission to add
	 */
	public void addPermission(String permission) {
		this.addPermission(permission, "");
	}

	/**
	 * Remove permission in world
	 *
	 * @param permission Permission to remove
	 * @param worldName      World name to remove permission for
	 */
	public void removePermission(String permission, String worldName) {
		throw new UnsupportedOperationException("You shouldn't call this method");
	}

	/**
	 * Remove specified permission from all worlds
	 *
	 * @param permission Permission to remove
	 */
	public void removePermission(String permission) {
		for (String world : this.getAllPermissions().keySet()) {
			this.removePermission(permission, world);
		}
	}

	/**
	 * Set permissions in world
	 *
	 * @param permissions Array of permissions to set
	 * @param world       World to set permissions for
	 */
	public void setPermissions(List<String> permissions, String world) {
		getData().setPermissions(permissions, world);
		this.callEvent(PermissionEntityEvent.Action.PERMISSIONS_CHANGED);
	}

	/**
	 * Set specified permissions in common space (all world)
	 *
	 * @param permission Permission to set
	 */
	public void setPermissions(List<String> permission) {
		this.setPermissions(permission, null);
	}

	/**
	 * Get option in world
	 *
	 * @param option       Name of option
	 * @param world        World to look for
	 * @param defaultValue Default value to fallback if no such option was found
	 * @return Value of option as String
	 */
	public String getOption(String option, String world, String defaultValue) {
		String ret = getData().getOption(option, world);
		if (ret == null) {
			ret = defaultValue;
		}
		return ret;
	}

	/**
	 * Return option
	 * Option would be looked up in common options
	 *
	 * @param option Option name
	 * @return option value or empty string if option was not found
	 */
	public String getOption(String option) {
		// @todo Replace empty string with null
		return this.getOption(option, null, "");
	}

	/**
	 * Return option for world
	 *
	 * @param option Option name
	 * @param world  World to look in
	 * @return option value or empty string if option was not found
	 */
	public String getOption(String option, String world) {
		// @todo Replace empty string with null
		return this.getOption(option, world, "");
	}

	/**
	 * Return integer value for option
	 *
	 * @param optionName
	 * @param world
	 * @param defaultValue
	 * @return option value or defaultValue if option was not found or is not integer
	 */
	public int getOptionInteger(String optionName, String world, int defaultValue) {
		try {
			return Integer.parseInt(this.getOption(optionName, world, Integer.toString(defaultValue)));
		} catch (NumberFormatException e) {
		}

		return defaultValue;
	}

	/**
	 * Returns double value for option
	 *
	 * @param optionName
	 * @param world
	 * @param defaultValue
	 * @return option value or defaultValue if option was not found or is not double
	 */
	public double getOptionDouble(String optionName, String world, double defaultValue) {
		String option = this.getOption(optionName, world, Double.toString(defaultValue));

		try {
			return Double.parseDouble(option);
		} catch (NumberFormatException e) {
		}

		return defaultValue;
	}

	/**
	 * Returns boolean value for option
	 *
	 * @param optionName
	 * @param world
	 * @param defaultValue
	 * @return option value or defaultValue if option was not found or is not boolean
	 */
	public boolean getOptionBoolean(String optionName, String world, boolean defaultValue) {
		String option = this.getOption(optionName, world, Boolean.toString(defaultValue));

		if ("false".equalsIgnoreCase(option)) {
			return false;
		} else if ("true".equalsIgnoreCase(option)) {
			return true;
		}

		return defaultValue;
	}

	/**
	 * Set specified option in world
	 *
	 * @param option Option name
	 * @param value  Value to set, null to remove
	 * @param world  World name
	 */
	public void setOption(String option, String value, String world) {
		getData().setOption(option, value, world);
		this.callEvent(PermissionEntityEvent.Action.OPTIONS_CHANGED);
	}

	/**
	 * Set option for all worlds. Can be overwritten by world specific option
	 *
	 * @param option Option name
	 * @param value  Value to set, null to remove
	 */
	public void setOption(String option, String value) {
		this.setOption(option, value, "");
	}

	/**
	 * Get options in world
	 *
	 * @param world
	 * @return Option value as string Map
	 */
	public Map<String, String> getOptions(String world) {
		return getData().getOptions(world);
	}

	/**
	 * Return options for all worlds
	 * Common options stored as null (empty string) as world.
	 *
	 * @return Map with world name as key and map of options as value
	 */
	public Map<String, Map<String, String>> getAllOptions() {
		return getData().getOptionsMap();
	}

	/**
	 * Save in-memory data to storage backend
	 */
	public void save() {
		getData().save();
		this.callEvent(PermissionEntityEvent.Action.SAVED);
	}

	/**
	 * Remove entity data from backend
	 */
	public void remove() {
		getData().remove();
		this.callEvent(PermissionEntityEvent.Action.REMOVED);
	}

	/**
	 * Return state of entity
	 *
	 * @return true if entity is only in-memory
	 */
	public boolean isVirtual() {
		return this.virtual;
	}

	/**
	 * Return world names where entity have permissions/options/etc
	 *
	 * @return
	 */
	public Set<String> getWorlds() {
		return getData().getWorlds();
	}

	/**
	 * Return entity timed (temporary) permission for world
	 *
	 * @param world
	 * @return Array of timed permissions in that world
	 */
	public List<String> getTimedPermissions(String world) {
		if (world == null) {
			world = "";
		}

		if (!this.timedPermissions.containsKey(world)) {
			return Collections.emptyList();
		}

		return Collections.unmodifiableList(this.timedPermissions.get(world));
	}

	/**
	 * Returns remaining lifetime of specified permission in world
	 *
	 * @param permission Name of permission
	 * @param world
	 * @return remaining lifetime in seconds of timed permission. 0 if permission is transient
	 */
	public int getTimedPermissionLifetime(String permission, String world) {
		if (world == null) {
			world = "";
		}

		if (!this.timedPermissionsTime.containsKey(world + ":" + permission)) {
			return 0;
		}

		return (int) (this.timedPermissionsTime.get(world + ":" + permission).longValue() - (System.currentTimeMillis() / 1000L));
	}

	/**
	 * Adds timed permission to specified world in seconds
	 *
	 * @param permission
	 * @param world
	 * @param lifeTime   Lifetime of permission in seconds. 0 for transient permission (world disappear only after server reload)
	 */
	public void addTimedPermission(final String permission, String world, int lifeTime) {
		if (world == null) {
			world = "";
		}

		if (!this.timedPermissions.containsKey(world)) {
			this.timedPermissions.put(world, new LinkedList<String>());
		}

		this.timedPermissions.get(world).add(permission);

		final String finalWorld = world;

		if (lifeTime > 0) {
			TimerTask task = new TimerTask() {

				@Override
				public void run() {
					removeTimedPermission(permission, finalWorld);
				}
			};

			this.manager.registerTask(task, lifeTime);

			this.timedPermissionsTime.put(world + ":" + permission, (System.currentTimeMillis() / 1000L) + lifeTime);
		}

		this.callEvent(PermissionEntityEvent.Action.PERMISSIONS_CHANGED);
	}

	/**
	 * Removes specified timed permission for world
	 *
	 * @param permission
	 * @param world
	 */
	public void removeTimedPermission(String permission, String world) {
		if (world == null) {
			world = "";
		}

		if (!this.timedPermissions.containsKey(world)) {
			return;
		}

		this.timedPermissions.get(world).remove(permission);
		this.timedPermissions.remove(world + ":" + permission);

		this.callEvent(PermissionEntityEvent.Action.PERMISSIONS_CHANGED);
	}

	protected void callEvent(PermissionEntityEvent event) {
		manager.callEvent(event);
	}

	protected void callEvent(PermissionEntityEvent.Action action) {
		this.callEvent(new PermissionEntityEvent(manager.getServerUUID(), this, action));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!getClass().equals(obj.getClass())) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		final PermissionEntity other = (PermissionEntity) obj;
		return this.name.equals(other.name);
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
		return hash;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + this.getName() + ")";
	}

	public String getMatchingExpression(String permission, String world) {
		return this.getMatchingExpression(this.getPermissions(world), permission);
	}

	public String getMatchingExpression(List<String> permissions, String permission) {
		for (String expression : permissions) {
			if (isMatches(expression, permission, true)) {
				return expression;
			}
		}

		return null;
	}

	/**
	 * Checks if specified permission matches specified permission expression
	 *
	 * @param expression       permission expression - what user have in his permissions list (permission.nodes.*)
	 * @param permission       permission which are checking for (permission.node.some.subnode)
	 * @param additionalChecks check for parent node matching
	 * @return
	 */
	public boolean isMatches(String expression, String permission, boolean additionalChecks) {
		return this.manager.getPermissionMatcher().isMatches(expression, permission);
	}

	public boolean explainExpression(String expression) {
		if (expression == null || expression.isEmpty()) {
			return false;
		}

		return !expression.startsWith("-"); // If expression have - (minus) before then that mean expression are negative
	}

	public boolean isDebug() {
		return this.debugMode || this.manager.isDebug();
	}

	public void setDebug(boolean debug) {
		this.debugMode = debug;
	}
}
