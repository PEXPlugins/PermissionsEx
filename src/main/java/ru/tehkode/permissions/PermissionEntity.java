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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import com.google.common.util.concurrent.Futures;
import org.bukkit.Bukkit;
import ru.tehkode.permissions.data.Qualifier;
import ru.tehkode.permissions.query.GetQuery;
import ru.tehkode.permissions.query.PermissionQuery;
import ru.tehkode.permissions.query.SetQuery;

/**
 * @author code
 */
@Deprecated
public abstract class PermissionEntity {
	protected final static String NON_INHERITABLE_PREFIX = "#";

	protected PermissionManager manager;
	private final String name;
	protected Map<String, List<String>> timedPermissions = new ConcurrentHashMap<>();
	protected Map<String, Long> timedPermissionsTime = new ConcurrentHashMap<>();
	protected boolean debugMode = false;

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
	 * Return name of permission entity (User or Group)
	 * User should be equal to Player's name on the server
	 *
	 * @return name
	 */
	public String getIdentifier() {
		return this.name;
	}

	public String getName() {
		return getOwnOption("name", null, getIdentifier());
	}

	/*protected void setName(String name) {
		setOption("name", name);
	}*/

	public abstract EntityType getType();

	private void applyEntity(PermissionQuery<?> query) {
		switch (getType()) {
			case USER:
				query.user(getIdentifier());
				break;
			case GROUP:
				query.group(getIdentifier());
		}
	}

	protected GetQuery get() {
		GetQuery query = manager.get();
		applyEntity(query);
		return query;
	}

	protected SetQuery set() {
		SetQuery query = manager.set();
		applyEntity(query);
		return query;
	}

	/**
	 * Return non-inherited user prefix.
	 * This means if at entity  don't have has own prefix
	 * then empty string or null would be returned
	 *
	 * @return prefix as string
	 */
	public String getOwnPrefix() {
		return Futures.getUnchecked(get()
				.followInheritance(false)
				.option("prefix"));
	}

	public String getOwnPrefix(String worldName) {
		return Futures.getUnchecked(get()
				.world(worldName)
				.followInheritance(false)
				.option("prefix"));
	}

	/**
	 * Return non-inherited user suffix.
	 * This means if an entity don't has own suffix
	 * then empty string or null would be returned
	 *
	 * @return suffix as string
	 */
	public final String getOwnSuffix() {
		return Futures.getUnchecked(get()
				.followInheritance(false)
				.option("suffix"));
	}

	public String getOwnSuffix(String worldName) {
		return Futures.getUnchecked(get()
				.world(worldName)
				.followInheritance(false)
				.option("suffix"));
	}

	/**
	 * Returns entity prefix
	 *
	 * @param worldName
	 * @return prefix
	 */
	public String getPrefix(String worldName) {
		return Futures.getUnchecked(get()
				.world(worldName)
				.option("prefix"));
	}

	/**
	 * Returns entity prefix
	 *
	 */
	public String getPrefix() {
		return getPrefix(null);
	}


	/**
	 * Set prefix to value
	 *
	 * @param prefix new prefix
	 */
	public void setPrefix(String prefix, String worldName) {
		Futures.getUnchecked(set()
				.world(worldName)
				.setOption("prefix", prefix)
				.perform());
	}

	/**
	 * Return entity suffix
	 *
	 * @return suffix
	 */
	public String getSuffix(String worldName) {
		return Futures.getUnchecked(get()
				.world(worldName)
				.option("suffix"));
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
		Futures.getUnchecked(set()
				.world(worldName)
				.setOption("suffix", suffix)
				.perform());
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

		return Futures.getUnchecked(get().world(world).has(permission));
		/*String expression = getMatchingExpression(permission, world);

		if (this.isDebug()) {
			manager.getLogger().info("User " + this.getIdentifier() + " checked for \"" + permission + "\", " + (expression == null ? "no permission found" : "\"" + expression + "\" found"));
		}

		return explainExpression(expression);*/
	}

	/**
	 * Return all entity permissions in specified world
	 *
	 * @param world World name
	 * @return Array of permission expressions
	 */
	public List<String> getPermissions(String world) {
		return Futures.getUnchecked(get().world(world).permissions());
	}

	/**
	 * Returns own (without inheritance) permissions of group for world
	 *
	 * @param world world's world name
	 * @return Array of permissions for world
	 */
	public List<String> getOwnPermissions(String world) {
		return Futures.getUnchecked(get().world(world).followInheritance(false).permissions());
	}

	/**
	 * Return permissions for all worlds
	 * Common permissions stored as "" (empty string) as world.
	 *
	 * @return Map with world name as key and permissions array as value
	 */
	public Map<String, List<String>> getAllPermissions() {
		Map<String, List<String>> ret = new HashMap<>();

		for (String world : getWorlds()) {
			ret.put(world, getOwnPermissions(world));
		}
		ret.put(null, getOwnPermissions(null));

		return Collections.unmodifiableMap(ret);
	}

	/**
	 * Add permissions for specified world
	 *
	 * @param permission Permission to add
	 * @param worldName      World name to add permission to
	 */
	public void addPermission(String permission, String worldName) {
		set().world(worldName).addPermission(permission).perform();

	}

	/**
	 * Add permission in common space (all worlds)
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
	 * @param worldName      World name to remove permission for
	 */
	public void removePermission(String permission, String worldName) {
		set().world(worldName).removePermission(permission);
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
		set().world(world).setPermissions(permissions).perform();
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
	public String getOption(final String option, String world, String defaultValue) {
		return Futures.getUnchecked(get().world(world).option(option, defaultValue));
	}

	/**
	 * Return option
	 * Option would be looked up in common options
	 *
	 * @param option Option name
	 * @return option value or empty string if option was not found
	 */
	public String getOption(String option) {
		return this.getOption(option, null, null);
	}

	/**
	 * Return option for world
	 *
	 * @param option Option name
	 * @param world  World to look in
	 * @return option value or empty string if option was not found
	 */
	public String getOption(String option, String world) {
		return this.getOption(option, world, null);
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
		} catch (NumberFormatException ignore) {
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
		} catch (NumberFormatException ignore) {
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
		set().world(world).setOption(option, value).perform();
	}

	/**
	 * Set option for all worlds. Can be overwritten by world specific option
	 *
	 * @param option Option name
	 * @param value  Value to set, null to remove
	 */
	public void setOption(String option, String value) {
		this.setOption(option, value, null);
	}

	/**
	 * Get options in world
	 *
	 * @param world
	 * @return Option value as string Map
	 */
	public Map<String, String> getOptions(String world) {
		return Futures.getUnchecked(get().world(world).options());
	}

	/**
	 * Return options for all worlds
	 * Common options stored as null (empty string) as world.
	 *
	 * @return Map with world name as key and map of options as value
	 */
	public Map<String, Map<String, String>> getAllOptions() {
		Map<String, Map<String, String>> ret = new HashMap<>();
		for (String world : getWorlds()) {
			ret.put(world, getOptions(world));
		}
		ret.put(null, getOptions(null));
		return ret;
	}

	/**
	 * Return non-inherited value of specified option for user in world
	 *
	 * @param option       option string
	 * @param world        world's name
	 * @param defaultValue default value
	 * @return option value or defaultValue if option is not set
	 */
	public String getOwnOption(String option, String world, String defaultValue) {
		return Futures.getUnchecked(get().world(world).followInheritance(false).option(option, defaultValue));
	}

	/**
	 * Return non-inherited value of specified option in common space (all worlds).
	 *
	 * @param option
	 * @return option value or empty string if option is not set
	 */
	public String getOwnOption(String option) {
		return Futures.getUnchecked(get().followInheritance(false).option(option));
	}

	public String getOwnOption(String option, String world) {
		return Futures.getUnchecked(get().followInheritance(false).world(world).option(option));
	}

	public int getOwnOptionInteger(String optionName, String world, int defaultValue) {
		String option = this.getOwnOption(optionName, world, Integer.toString(defaultValue));

		try {
			return Integer.parseInt(option);
		} catch (NumberFormatException ignore) {
		}

		return defaultValue;
	}

	public boolean getOwnOptionBoolean(String optionName, String world, boolean defaultValue) {
		String option = this.getOwnOption(optionName, world, Boolean.toString(defaultValue));

		if ("false".equalsIgnoreCase(option)) {
			return false;
		} else if ("true".equalsIgnoreCase(option)) {
			return true;
		}

		return defaultValue;
	}

	public double getOwnOptionDouble(String optionName, String world, double defaultValue) {
		String option = this.getOwnOption(optionName, world, Double.toString(defaultValue));

		try {
			return Double.parseDouble(option);
		} catch (NumberFormatException ignore) {
		}

		return defaultValue;
	}

	/**
	 * Save in-memory data to storage backend
	 */
	public void save() {
	}

	/**
	 * Remove entity data from backend
	 */
	public void remove() {

	}

	/**
	 * Return state of entity
	 *
	 * @return true if entity is only in-memory
	 */
	public boolean isVirtual() {
		try {
			return !manager.getBackend().hasAnyQualifier(getType().getQualifier(), getIdentifier()).get();
		} catch (InterruptedException | ExecutionException e) {
			return true;
		}
	}

	/**
	 * Return world names where entity have permissions/options/etc
	 *
	 * @return
	 */
	public Set<String> getWorlds() {
		// Just get the values for all entities since this is simpler
		try {
			return new HashSet<>(manager.getBackend().getAllValues(Qualifier.WORLD).get());
		} catch (InterruptedException | ExecutionException e) {
			return Collections.emptySet();
		}
	}

	/**
	 * Return entity timed (temporary) permission for world
	 *
	 * @param world
	 * @return Array of timed permissions in that world
	 */
	public List<String> getTimedPermissions(String world) {
		return Collections.emptyList();
	}

	/**
	 * Returns remaining lifetime of specified permission in world
	 *
	 * @param permission Name of permission
	 * @param world
	 * @return remaining lifetime in seconds of timed permission. 0 if permission is transient
	 */
	public int getTimedPermissionLifetime(String permission, String world) {
		return 0;
	}

	/**
	 * Adds timed permission to specified world in seconds
	 *
	 * @param permission
	 * @param world
	 * @param lifeTime   Lifetime of permission in seconds. 0 for transient permission (world disappear only after server reload)
	 */
	public void addTimedPermission(final String permission, String world, int lifeTime) {

	}

	/**
	 * Removes specified timed permission for world
	 *
	 * @param permission
	 * @param world
	 */
	public void removeTimedPermission(String permission, String world) {

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
		return this.getClass().getSimpleName() + "(ident=" + this.getIdentifier() +  ",name=" + getName() + ")";
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

	// -- Inheritance -- //
	public List<PermissionGroup> getOwnParents(String world) {
		List<String> names = Futures.getUnchecked(get().world(world).followInheritance(false).parents());
		List<PermissionGroup> ret = new ArrayList<>(names.size());
		for (String name : names) {
			ret.add(manager.getGroup(name));
		}
		return ret;
	}

	public List<PermissionGroup> getOwnParents() {
		return getOwnParents(null);
	}

	public List<String> getOwnParentIdentifiers(String world) {
		return Futures.getUnchecked(get().world(world).followInheritance(false).parents());
	}

	public List<String> getOwnParentIdentifiers() {
		return getOwnParentIdentifiers(null);
	}

	public final List<PermissionGroup> getParents(String world) {
		List<String> names = Futures.getUnchecked(get().world(world).parents());
		List<PermissionGroup> ret = new ArrayList<>(names.size());
		for (String name : names) {
			ret.add(manager.getGroup(name));
		}
		return ret;
	}

	public final List<PermissionGroup> getParents() {
		return getParents(null);
	}



	public List<String> getParentIdentifiers(String world) {
		return Futures.getUnchecked(get().world(world).parents());
	}

	/**
	 * Return names of parent groups in global scope
	 * @return Names of parent groups in unmodifiable list
	 */
	public List<String> getParentIdentifiers() {
		return getParentIdentifiers(null);
	}

	public Map<String, List<PermissionGroup>> getAllParents() {
		Map<String, List<PermissionGroup>> allGroups = new HashMap<>();

		for (String worldName : this.getWorlds()) {
			allGroups.put(worldName, this.getOwnParents(worldName));
		}
		allGroups.put(null, this.getOwnParents(null));

		return Collections.unmodifiableMap(allGroups);
	}


	public void setParents(List<PermissionGroup> parents, String world) {
		List<String> parentNames = new LinkedList<>();
		for (PermissionGroup group : parents) {
			parentNames.add(group.getIdentifier());
		}
		setParentsIdentifier(parentNames, world);
	}

	/**
	 * Set parents for entity in global namespace
	 * @param parents
	 */
	public void setParents(List<PermissionGroup> parents) {
		setParents(parents, null);
	}

	public void setParentsIdentifier(List<String> parentNames, String world) {
		set().world(world).setParents(parentNames).perform();
	}

	public void setParentsIdentifier(List<String> parentNames) {
		setParentsIdentifier(parentNames, null);
	}
}
