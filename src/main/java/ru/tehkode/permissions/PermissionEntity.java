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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import ru.tehkode.permissions.events.PermissionEntityEvent;
import ru.tehkode.utils.Debug;

/**
 *
 * @author t3hk0d3
 */
public abstract class PermissionEntity {

	/**
	 * Range regualar expression
	 */
	protected static Pattern rangeExpression = Pattern.compile("(\\d+)-(\\d+)");
	/**
	 * PermissionsManager
	 */
	protected PermissionManager manager;
	/**
	 * Entity name (player or group name)
	 */
	private String name;
	/**
	 * Virtual flag (indicate entity is not inmemory only)
	 *
	 * @deprecated
	 */
	@Deprecated
	protected boolean virtual = true;
	/**
	 * Debug mode (if true would log every permission check into console)
	 */
	protected boolean debugMode = false;
	/**
	 * "Clear" storage
	 */
	protected Map<String, List<String>> permissions = null;
	protected Map<String, Map<String, String>> options = null;
	/**
	 * Caches
	 */
	protected Map<String, List<String>> permissionsCache = new HashMap<String, List<String>>();
	protected Map<String, Map<String, String>> optionsCache = new HashMap<String, Map<String, String>>();
	/**
	 * Timed permissions storage
	 */
	protected Map<String, List<String>> timedPermissions = new ConcurrentHashMap<String, List<String>>();
	protected Map<String, Long> timedPermissionsTime = new ConcurrentHashMap<String, Long>();
	/**
	 * Autocommit if set true class would save changes to backend on each change
	 */
	protected boolean autoCommit = true;

	public PermissionEntity(String name, PermissionManager manager) {
		this.manager = manager;
		this.name = name;
	}

	/**
	 * This method 100% run after all constructors have been run and entity
	 * object, and entity object are completely ready to operate
	 */
	public void initialize() {
		this.debugMode = this.getOptionBoolean("debug", null, this.debugMode);
	}

	/**
	 * ********************************
	 * UTILITY SECTION
	 */
	/**
	 * Return name of permission entity (User or Group) User should be equal to
	 * Player's name on the server
	 *
	 * @return name
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * This method is made to implementations could change entity name in some cases
	 * For example actual name of entity is writter in other case (T3hk0d3 into t3hk0d3)
	 * 
	 * @param name
	 * @return 
	 */
	protected void setName(String name) {
		this.name = name;
	}

	/**
	 * Save in-memory data to storage backend
	 */
	protected abstract void save();

	/**
	 * Remove entity data from backend
	 */
	public abstract void remove();

	/**
	 * Clear caches
	 */
	public void clearCache() {
		this.permissionsCache.clear();
		this.optionsCache.clear();
	}

	/**
	 * Return state of entity
	 *
	 * @return true if entity is only in-memory
	 */
	@Deprecated
	public boolean isVirtual() {
		return this.virtual;
	}

	/**
	 * Return world names where entity have permissions/options/etc
	 *
	 * @return
	 */
	public Set<String> getWorldsList() {
		Set<String> hashSet = new HashSet<String>();

		hashSet.addAll(this.permissions.keySet());
		hashSet.addAll(this.options.keySet());

		hashSet.remove(null); // remove common world :D

		return hashSet;
	}

	@Deprecated
	public final String[] getWorlds() {
		return this.getWorldsList().toArray(new String[0]);
	}

	/**
	 * OPTIONS SECTION
	 */
	protected abstract Map<String, Map<String, String>> loadOptions();

	protected void checkOptions() {
		if (this.options == null) {
			this.options = this.loadOptions();
		}
		
		Debug.print("[=========] Option loaded: %1", this.options);
	}

	/**
	 * Returns entity prefix
	 *
	 * @param worldName
	 * @return prefix, null if no suffix
	 */
	public final String getPrefix(String worldName) {
		return this.getOption("prefix", worldName, null);
	}

	@Deprecated
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
	public final void setPrefix(String prefix, String worldName) {
		this.setOption("prefix", prefix, worldName);
	}

	/**
	 * Return entity suffix
	 *
	 * @return suffix, null if not suffix
	 */
	public final String getSuffix(String worldName) {
		return this.getOption("suffix", worldName, null);
	}

	@Deprecated
	public String getSuffix() {
		return getSuffix(null);
	}

	/**
	 * Set suffix to value
	 *
	 * @param suffix new suffix
	 */
	public final void setSuffix(String suffix, String worldName) {
		this.setOption("suffix", suffix, worldName);
	}

	/**
	 * Get option in world
	 *
	 * @param option Name of option
	 * @param worldName World to look for
	 * @param defaultValue Default value to fallback if no such option was found
	 * @return Value of option as String
	 */
	public final String getOption(String option, String worldName, String defaultValue) {
		Map<String, String> worldOptions = this.getOptions(worldName);

		if (!worldOptions.containsKey(option)) {
			return defaultValue;
		}

		return worldOptions.get(option);
	}

	/**
	 * Return option Option would be looked up in common options
	 *
	 * @param option Option name
	 * @return option value or null if option was not found
	 */
	@Deprecated
	public String getOption(String option) {
		return this.getOption(option, null, null);
	}

	/**
	 * Return option for world
	 *
	 * @param option Option name
	 * @param world World to look in
	 * @return option value or null if option was not found
	 */
	@Deprecated
	public String getOption(String option, String world) {
		// @todo Replace empty string with null
		return this.getOption(option, world, null);
	}

	/**
	 * Return integer value for option
	 *
	 * @param optionName
	 * @param world
	 * @param defaultValue
	 * @return option value or defaultValue if option was not found or is not
	 * integer
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
	 * @return option value or defaultValue if option was not found or is not
	 * double
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
	 * @return option value or defaultValue if option was not found or is not
	 * boolean
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
	 * @param value Value to set, null to remove
	 * @param worldName World name
	 */
	public void setOption(String option, String value, String worldName) {
		this.checkOptions();

		if (!this.options.containsKey(worldName)) {
			this.options.put(worldName, new HashMap<String, String>());
		}

		this.options.get(worldName).put(option, value);
		
		this.commit(false);

		this.callEvent(PermissionEntityEvent.Action.OPTIONS_CHANGED);
	}

	/**
	 * Set option for all worlds. Can be overwritten by world specific option
	 *
	 * @param option Option name
	 * @param value Value to set, null to remove
	 */
	public void setOption(String permission, String value) {
		this.setOption(permission, value, null);
	}

	/**
	 * Get options in world
	 *
	 * @param worldName
	 * @return Option value as string Map
	 */
	public Map<String, String> getOptions(String worldName) {
		if (!this.optionsCache.containsKey(worldName)) {
			this.optionsCache.put(worldName, calculateOptions(worldName, true));
		}

		return this.optionsCache.get(worldName);
	}

	private Map<String, String> calculateOptions(String worldName, boolean worldInheritance) {
		Debug.print("TEST1 %1", this.options);
		this.checkOptions();
		Debug.print("TEST2 %1", this.options);

		Map<String, String> map = new HashMap();

		if (this.options.containsKey(worldName)) {
			map.putAll(this.options.get(worldName));
		}

		for (String parentWorld : this.manager.getWorldInheritanceList(worldName)) {
			this.copyMapNoRewrite(map, this.calculateOptions(parentWorld, false));
		}

		if (worldInheritance) {
			this.copyMapNoRewrite(map, this.calculateOptions(null, false));
		}

		return map;
	}

	/**
	 * Return options for all worlds Common options stored as null as world.
	 *
	 * @return Map with world name as key and map of options as value
	 */
	public Map<String, Map<String, String>> getAllOptions() {
		return this.options;
	}

	/**
	 * ******************************
	 * PERMISSIONS
	 */
	/**
	 * Checks if entity has specified permission in default world
	 *
	 * @param permission Permission to check
	 * @return true if entity has this permission otherwise false
	 */
	@Deprecated
	public boolean has(String permission) {
		return this.has(permission, Bukkit.getServer().getWorlds().get(0).getName());
	}

	/**
	 * Check if entity has specified permission in world
	 *
	 * @param permission Permission to check
	 * @param world World to check permission in
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
	 * @param worldName World name
	 * @return Array of permission expressions
	 */
	public List<String> getPermissionsList(String worldName) {
		if (!this.permissionsCache.containsKey(worldName)) {
			this.permissionsCache.put(worldName, this.calculatePermissions(worldName, true));
		}
		
		return this.permissionsCache.get(worldName);
	}

	protected final List<String> calculatePermissions(String worldName, boolean worldInheritance) {
		List<String> permissionsList = new ArrayList<String>();
		
		this.checkPermissions();

		if (this.timedPermissions.containsKey(worldName)) {
			permissionsList.addAll(this.timedPermissions.get(worldName));
		}
		
		if (this.permissions.containsKey(worldName)) {
			permissionsList.addAll(this.permissions.get(worldName));
		}

		for (String parentWorld : this.manager.getWorldInheritanceList(worldName)) {
			permissionsList.addAll(calculatePermissions(parentWorld, false));
		}

		if (worldInheritance) {
			permissionsList.addAll(calculatePermissions(null, false));
		}
		
		return permissionsList;
	}

	@Deprecated
	public final String[] getPermissions(String world) {
		return this.getPermissionsList(world).toArray(new String[0]);
	}

	/**
	 * Return permissions for all worlds Common permissions stored as "" (empty
	 * string) as world.
	 *
	 * @return Map with world name as key and permissions array as value
	 */
	public Map<String, List<String>> getPermissionsMap() {
		this.checkPermissions();

		return this.permissions;
	}

	@Deprecated
	public final Map<String, String[]> getAllPermissions() {
		return this.convertMap(this.getPermissionsMap(), new String[0]);
	}

	/**
	 * Add permissions for specified world
	 *
	 * @param permission Permission to add
	 * @param worldName World name to add permission to
	 */
	public void addPermission(String permission, String worldName) {
		this.checkPermissions();

		if (!this.permissions.containsKey(worldName)) {
			this.permissions.put(name, new LinkedList<String>());
		}

		this.permissions.get(worldName).add(0, permission);

		this.commit(false);

		this.callEvent(PermissionEntityEvent.Action.PERMISSIONS_CHANGED);
	}

	/**
	 * Add permission in common space
	 *
	 * @param permission Permission to add
	 */
	public void addPermission(String permission) {
		this.addPermission(permission, null);
	}

	/**
	 * Remove permission in world
	 *
	 * @param permission Permission to remove
	 * @param world World name to remove permission for
	 */
	public void removePermission(String permission, String worldName) {
		this.checkPermissions();

		if (this.permissions.containsKey(worldName)) {
			this.permissions.get(worldName).remove(permission);
		}

		this.commit(false);

		this.callEvent(PermissionEntityEvent.Action.PERMISSIONS_CHANGED);
	}

	/**
	 * Remove specified permission from any world
	 *
	 * @param permission Permission to remove
	 */
	public void removePermission(String permission) {
		boolean ac = this.isAutoCommit();

		this.setAutoCommit(false); // disable autocommit to prevent commit on each removePermission()
		for (String worldName : this.getWorldsList()) {
			this.removePermission(permission, worldName);
		}
		this.setAutoCommit(ac);

		this.commit(false);

	}

	/**
	 * Set permissions in world
	 *
	 * @param permissions Array of permissions to set
	 * @param worldName World to set permissions for
	 */
	public void setPermissions(List<String> permissions, String worldName) {
		this.checkPermissions();

		this.permissions.put(worldName, permissions);

		this.commit(false);

		this.callEvent(PermissionEntityEvent.Action.PERMISSIONS_CHANGED);
	}

	@Deprecated
	public final void setPermissions(String[] permissions, String world) {
		this.setPermissions(Arrays.asList(permissions), world);
	}

	/**
	 * Set specified permissions in common space (all world)
	 *
	 * @param permission Permission to set
	 */
	@Deprecated
	public final void setPermissions(String[] permission) {
		this.setPermissions(permission, null);
	}

	/**
	 * Return entity timed (temporary) permission for world
	 *
	 * @param world
	 * @return Array of timed permissions in that world
	 */
	public List<String> getTimedPermissionsList(String world) {
		if (world == null) {
			world = "";
		}

		if (!this.timedPermissions.containsKey(world)) {
			return new ArrayList<String>(); //empty list huh?
		}

		return this.timedPermissions.get(world);
	}

	@Deprecated
	public final String[] getTimedPermissions(String world) {
		return this.getTimedPermissionsList(world).toArray(new String[0]);
	}

	/**
	 * Returns remaining lifetime of specified permission in world
	 *
	 * @param permission Name of permission
	 * @param world
	 * @return remaining lifetime in seconds of timed permission. 0 if
	 * permission is transient
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
	 * @param lifeTime Lifetime of permission in seconds. 0 for transient
	 * permission (world disappear only after server reload)
	 */
	public void addTimedPermission(final String permission, String world, int lifeTime) {
		if (world == null) {
			world = "";
		}

		if (!this.timedPermissions.containsKey(world)) {
			this.timedPermissions.put(world, new ArrayList<String>());
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

	protected void checkPermissions() {
		if (this.permissions == null) {
			this.permissions = this.loadPermissions();
		}
	}

	protected abstract Map<String, List<String>> loadPermissions();

	protected void callEvent(PermissionEntityEvent event) {
		manager.callEvent(event);
	}

	protected void callEvent(PermissionEntityEvent.Action action) {
		this.callEvent(new PermissionEntityEvent(this, action));
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
		return this.getMatchingExpression(this.getPermissionsList(world), permission);
	}

	public String getMatchingExpression(List<String> permissions, String permission) {
		for (String expression : permissions) {
			if (isMatches(expression, permission, true)) {
				return expression;
			}
		}

		return null;
	}

	protected static String prepareRegexp(String expression) {
		String regexp = expression.replace(".", "\\.").replace("*", "(.*)");

		try {
			Matcher rangeMatcher = rangeExpression.matcher(regexp);
			while (rangeMatcher.find()) {
				StringBuilder range = new StringBuilder();
				int from = Integer.parseInt(rangeMatcher.group(1));
				int to = Integer.parseInt(rangeMatcher.group(2));

				if (from > to) {
					int temp = from;
					from = to;
					to = temp;
				} // swap them

				range.append("(");

				for (int i = from; i <= to; i++) {
					range.append(i);
					if (i < to) {
						range.append("|");
					}
				}

				range.append(")");

				regexp = regexp.replace(rangeMatcher.group(0), range);
			}
		} catch (Throwable e) {
		}

		return regexp;
	}
	/**
	 * Pattern cache
	 */
	protected static HashMap<String, Pattern> patternCache = new HashMap<String, Pattern>();

	/**
	 * Checks if specified permission matches specified permission expression
	 *
	 * @param expression permission expression - what user have in his
	 * permissions list (permission.nodes.*)
	 * @param permission permission which are checking for
	 * (permission.node.some.subnode)
	 * @param additionalChecks check for parent node matching
	 * @return
	 */
	public static boolean isMatches(String expression, String permission, boolean additionalChecks) {
		String localExpression = expression;

		if (localExpression.startsWith("-")) {
			localExpression = localExpression.substring(1);
		}

		if (localExpression.startsWith("#")) {
			localExpression = localExpression.substring(1);
		}

		if (!patternCache.containsKey(localExpression)) {
			patternCache.put(localExpression, Pattern.compile(prepareRegexp(localExpression)));
		}

		if (patternCache.get(localExpression).matcher(permission).matches()) {
			return true;
		}

		//if (additionalChecks && localExpression.endsWith(".*") && isMatches(localExpression.substring(0, localExpression.length() - 2), permission, false)) {
		//	return true;
		//}
		/*
		 * if (additionalChecks && !expression.endsWith(".*") &&
		 * isMatches(expression + ".*", permission, false)) { return true; }
		 */
		return false;
	}

	public boolean explainExpression(String expression) {
		if (expression == null || expression.isEmpty()) {
			return false;
		}

		return !expression.startsWith("-"); // If expression have - (minus) before then that mean expression are negative
	}

	public boolean commit(boolean force) {
		if (this.autoCommit || force) {
			this.save();
			
			this.clearCache();

			return true;
		}

		return false;
	}

	public boolean isAutoCommit() {
		return autoCommit;
	}

	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit = autoCommit;
	}

	public boolean isDebug() {
		return this.debugMode || this.manager.isDebug();
	}

	public void setDebug(boolean debug) {
		this.debugMode = debug;
	}

	protected <T> Map<String, T[]> convertMap(Map<String, List<T>> oldMap, T[] sampleArray) {
		Map<String, T[]> map = new HashMap<String, T[]>();

		for (Map.Entry<String, List<T>> entry : oldMap.entrySet()) {
			map.put(entry.getKey(), entry.getValue().toArray(sampleArray));
		}

		return map;
	}

	protected <K, V> Map<K, V> copyMapNoRewrite(Map<K, V> dst, Map<K, V> src) {
		for (Map.Entry<K, V> entry : src.entrySet()) {
			if (!dst.containsKey(entry.getKey())) {
				dst.put(entry.getKey(), entry.getValue());
			}
		}

		return dst;
	}
}
