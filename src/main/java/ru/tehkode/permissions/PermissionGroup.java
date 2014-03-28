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

import ru.tehkode.permissions.events.PermissionEntityEvent;

import java.util.*;

/**
 * @author t3hk0d3
 */
public class PermissionGroup extends PermissionEntity implements Comparable<PermissionGroup> {

	protected final static String NON_INHERITABLE_PREFIX = "#";

	protected int weight = 0;
	protected boolean dirtyWeight = true;
	private final PermissionsGroupData data;

	public PermissionGroup(String groupName, PermissionsGroupData data, PermissionManager manager) {
		super(groupName, manager);
		this.data = data;
	}

	@Override
	protected PermissionsGroupData getData() {
		return data;
	}

	@Override
	public void initialize() {
		super.initialize();

		if (this.isDebug()) {
			manager.getLogger().info("Group " + this.getName() + " initialized");
		}
	}

	public Type getType() {
		return Type.GROUP;
	}

	/**
	 * Return non-inherited group prefix.
	 * This means if a group don't have has own prefix
	 * then empty string or null would be returned
	 *
	 * @return prefix as string
	 */
	public String getOwnPrefix() {
		return this.getOwnPrefix(null);
	}

	public String getOwnPrefix(String worldName) {
		return getData().getPrefix(worldName);
	}

	/**
	 * Return non-inherited suffix prefix.
	 * This means if a group don't has own suffix
	 * then empty string or null would be returned
	 *
	 * @return suffix as string
	 */
	public final String getOwnSuffix() {
		return this.getOwnSuffix(null);
	}

	public String getOwnSuffix(String worldName) {
		return getData().getSuffix(worldName);
	}

	/**
	 * Returns own (without inheritance) permissions of group for world
	 *
	 * @param world world's world name
	 * @return Array of permissions for world
	 */
	public List<String> getOwnPermissions(String world) {
		return getData().getPermissions(world);
	}

	/**
	 * Returns option value in specified world without inheritance
	 * This mean option value wouldn't be inherited from parent groups
	 *
	 * @param option
	 * @param world
	 * @param defaultValue
	 * @return option value or defaultValue if option was not found in own options
	 */
	public String getOwnOption(String option, String world, String defaultValue) {
		String ret = getData().getOption(option, world);
		if (ret == null) {
			ret = defaultValue;
		}
		return ret;
	}

	public String getOwnOption(String option) {
		return this.getOwnOption(option, null, null);
	}

	public String getOwnOption(String option, String world) {
		return this.getOwnOption(option, world, null);
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

	public int getOwnOptionInteger(String optionName, String world, int defaultValue) {
		String option = this.getOwnOption(optionName, world, Integer.toString(defaultValue));

		try {
			return Integer.parseInt(option);
		} catch (NumberFormatException e) {
		}

		return defaultValue;
	}

	public double getOwnOptionDouble(String optionName, String world, double defaultValue) {
		String option = this.getOwnOption(optionName, world, Double.toString(defaultValue));

		try {
			return Double.parseDouble(option);
		} catch (NumberFormatException e) {
		}

		return defaultValue;
	}

	public int getWeight() {
		if (this.dirtyWeight) {
			this.weight = this.getOptionInteger("weight", null, 0);
			this.dirtyWeight = false;
		}

		return this.weight;
	}

	public void setWeight(int weight) {
		this.setOption("weight", Integer.toString(weight));

		this.dirtyWeight = true;

		this.clearMembersCache();
		this.callEvent(PermissionEntityEvent.Action.WEIGHT_CHANGED);
	}

	/**
	 * Checks if group is participating in ranking system
	 *
	 * @return
	 */
	public boolean isRanked() {
		return (this.getRank() > 0);
	}

	/**
	 * Returns rank in ranking system. 0 if group is not ranked
	 *
	 * @return
	 */
	public int getRank() {
		return this.getOwnOptionInteger("rank", null, 0);
	}

	/**
	 * Set rank for this group
	 *
	 * @param rank Rank for group. Specify 0 to remove group from ranking
	 */
	public void setRank(int rank) {
		if (rank > 0) {
			this.setOption("rank", Integer.toString(rank));
		} else {
			this.setOption("rank", null);
		}

		this.callEvent(PermissionEntityEvent.Action.RANK_CHANGED);
	}

	/**
	 * Returns ranking ladder where this group is participating in
	 *
	 * @return Name of rank ladder as String
	 */
	public String getRankLadder() {
		return this.getOption("rank-ladder", "", "default");
	}

	/**
	 * Set rank ladder for this group
	 *
	 * @param rankLadder Name of rank ladder
	 */
	public void setRankLadder(String rankLadder) {
		if (rankLadder.isEmpty() || rankLadder.equals("default")) {
			rankLadder = null;
		}

		this.setOption("rank-ladder", rankLadder);

		this.callEvent(PermissionEntityEvent.Action.RANK_CHANGED);
	}

	/**
	 * Returns array of parent groups objects
	 *
	 * @return unmodifiable list of parent group objects
	 */
	public List<PermissionGroup> getParentGroups(String worldName) {
		List<PermissionGroup> parentGroups = new LinkedList<PermissionGroup>();

		for (String parentGroup : getData().getParents(worldName)) {

			// Yeah horrible thing, i know, that just safety from invoking empty named groups
			parentGroup = parentGroup.trim();
			if (parentGroup.isEmpty()) {
				continue;
			}

			if (parentGroup.equals(this.getName())) {
				continue;
			}

			PermissionGroup group = this.manager.getGroup(parentGroup);
			if (!parentGroups.contains(group)) { // To prevent cyclic inheritance
				parentGroups.add(group);
			}
		}

		if (worldName != null) {
			// World Inheritance
			for (String parentWorld : this.manager.getWorldInheritance(worldName)) {
				parentGroups.addAll(getParentGroups(parentWorld));
			}

			parentGroups.addAll(getParentGroups(null));
		}

		Collections.sort(parentGroups);

		return Collections.unmodifiableList(parentGroups);
	}

	public List<PermissionGroup> getParentGroups() {
		return this.getParentGroups(null);
	}

	public Map<String, List<PermissionGroup>> getAllParentGroups() {
		Map<String, List<PermissionGroup>> allGroups = new HashMap<String, List<PermissionGroup>>();

		for (String worldName : this.getWorlds()) {
			allGroups.put(worldName, this.getWorldGroups(worldName));
		}

		allGroups.put(null, this.getWorldGroups(null));

		return allGroups;
	}

	protected List<PermissionGroup> getWorldGroups(String worldName) {
		List<PermissionGroup> groups = new LinkedList<PermissionGroup>();

		for (String groupName : getData().getParents(worldName)) {
			if (groupName == null || groupName.isEmpty() || groupName.equalsIgnoreCase(this.getName())) {
				continue;
			}

			PermissionGroup group = this.manager.getGroup(groupName);

			if (!groups.contains(group)) {
				groups.add(group);
			}
		}

		Collections.sort(groups);

		return Collections.unmodifiableList(groups);
	}

	/**
	 * Returns direct parents names of this group
	 *
	 * @return array of parents group names
	 */
	public String[] getParentGroupsNames(String worldName) {
		List<String> groups = new LinkedList<String>();
		for (PermissionGroup group : this.getParentGroups(worldName)) {
			groups.add(group.getName());
		}

		return groups.toArray(new String[0]);
	}

	public String[] getParentGroupsNames() {
		return this.getParentGroupsNames(null);
	}

	/**
	 * Set parent groups
	 *
	 * @param parentGroups Array of parent groups names to set
	 */
	public void setParentGroups(List<String> parentGroups, String worldName) {
		getData().setParents(parentGroups, worldName);
	}

	public void setParentGroups(List<String> parentGroups) {
		this.setParentGroups(parentGroups, null);
	}

	/**
	 * Set parent groups
	 *
	 * @param parentGroups Array of parent groups objects to set
	 */
	public void setParentGroupObjects(List<PermissionGroup> parentGroups, String worldName) {
		List<String> groups = new LinkedList<String>();

		for (PermissionGroup group : parentGroups) {
			groups.add(group.getName());
		}

		this.setParentGroups(groups, worldName);

		this.callEvent(PermissionEntityEvent.Action.INHERITANCE_CHANGED);
	}

	public void setParentGroupObjects(List<PermissionGroup> parentGroups) {
		this.setParentGroupObjects(parentGroups, null);
	}

	/**
	 * Check if this group is descendant of specified group
	 *
	 * @param group            group object of parent
	 * @param checkInheritance set to false to check only the direct inheritance
	 * @return true if this group is descendant or direct parent of specified group
	 */
	public boolean isChildOf(PermissionGroup group, String worldName, boolean checkInheritance) {
		return isChildOf(group, worldName, checkInheritance ? new HashSet<String>() : null);
	}

	private boolean isChildOf(PermissionGroup group, String worldName, Set<String> visitedParents) {
		if (group == null) {
			return false;
		}

		if (visitedParents != null) {
			visitedParents.add(this.getName());
		}

		for (String parentGroup : getData().getParents(worldName)) {
			if (visitedParents != null && visitedParents.contains(parentGroup)) {
				continue;
			}

			if (group.getName().equals(parentGroup)) {
				return true;
			}

			if (visitedParents != null && manager.getGroup(parentGroup).isChildOf(group, worldName, visitedParents)) {
				return true;
			}
		}

		return false;
	}

	public boolean isChildOf(PermissionGroup group, boolean checkInheritance) {
		for (String worldName : this.getWorlds()) {
			if (this.isChildOf(group, worldName, checkInheritance)) {
				return true;
			}
		}

		return this.isChildOf(group, null, checkInheritance);
	}

	public boolean isChildOf(PermissionGroup group, String worldName) {
		return isChildOf(group, worldName, false);
	}

	public boolean isChildOf(PermissionGroup group) {
		return isChildOf(group, false);
	}

	/**
	 * Check if this group is descendant of specified group
	 *
	 * @param groupName        name of group to check against
	 * @param checkInheritance set to false to check only the direct inheritance
	 * @return
	 */
	public boolean isChildOf(String groupName, String worldName, boolean checkInheritance) {
		return isChildOf(this.manager.getGroup(groupName), worldName, checkInheritance);
	}

	public boolean isChildOf(String groupName, boolean checkInheritance) {
		return isChildOf(this.manager.getGroup(groupName), checkInheritance);
	}

	/**
	 * Check if specified group is direct parent of this group
	 *
	 * @param groupName to check against
	 * @return
	 */
	public boolean isChildOf(String groupName, String worldName) {
		return this.isChildOf(groupName, worldName, false);
	}

	public boolean isChildOf(String groupName) {
		return this.isChildOf(groupName, false);
	}

	/**
	 * Return array of direct child group objects
	 *
	 * @return
	 */
	public List<PermissionGroup> getChildGroups(String worldName) {
		return this.manager.getGroups(this.getName(), worldName, false);
	}

	public List<PermissionGroup> getChildGroups() {
		return this.manager.getGroups(this.getName(), false);
	}

	/**
	 * Return array of descendant group objects
	 *
	 * @return
	 */
	public List<PermissionGroup> getDescendantGroups(String worldName) {
		return this.manager.getGroups(this.getName(), worldName, true);
	}

	public List<PermissionGroup> getDescendantGroups() {
		return this.manager.getGroups(this.getName(), true);
	}

	/**
	 * Return array of direct members (users) of this group
	 *
	 * @return
	 */
	public Set<PermissionUser> getUsers(String worldName) {
		return this.manager.getUsers(this.getName(), worldName, false);
	}

	public Set<PermissionUser> getUsers() {
		return this.manager.getUsers(this.getName());
	}

	public boolean isDefault(String worldName) {
		return getData().isDefault(worldName);
	}

	public void setDefault(boolean def, String worldName) {
		getData().setDefault(def, worldName);
		callEvent(PermissionEntityEvent.Action.DEFAULTGROUP_CHANGED);
	}

	/**
	 * Overriden methods
	 */
	@Override
	public String getPrefix(String worldName) {
		// @TODO This method should be refactored

		String localPrefix = this.getOwnPrefix(worldName);

		if (worldName != null && (localPrefix == null || localPrefix.isEmpty())) {
			// World-inheritance
			for (String parentWorld : this.manager.getWorldInheritance(worldName)) {
				String prefix = this.getOwnPrefix(parentWorld);
				if (prefix != null && !prefix.isEmpty()) {
					localPrefix = prefix;
					break;
				}
			}

			// Common space
			if (localPrefix == null || localPrefix.isEmpty()) {
				localPrefix = this.getOwnPrefix(null);
			}
		}

		if (localPrefix == null || localPrefix.isEmpty()) {
			for (PermissionGroup group : this.getParentGroups(worldName)) {
				localPrefix = group.getPrefix(worldName);
				if (localPrefix != null && !localPrefix.isEmpty()) {
					break;
				}
			}
		}

		if (localPrefix == null) { // NPE safety
			localPrefix = "";
		}

		return localPrefix;
	}

	@Override
	public String getSuffix(String worldName) {
		// @TODO This method should be refactored

		String localSuffix = this.getOwnSuffix(worldName);

		if (worldName != null && (localSuffix == null || localSuffix.isEmpty())) {
			// World-inheritance
			for (String parentWorld : this.manager.getWorldInheritance(worldName)) {
				String suffix = this.getOwnSuffix(parentWorld);
				if (suffix != null && !suffix.isEmpty()) {
					localSuffix = suffix;
					break;
				}
			}

			// Common space
			if (localSuffix == null || localSuffix.isEmpty()) {
				localSuffix = this.getOwnSuffix(null);
			}
		}

		if (localSuffix == null || localSuffix.isEmpty()) {
			for (PermissionGroup group : this.getParentGroups(worldName)) {
				localSuffix = group.getSuffix(worldName);
				if (localSuffix != null && !localSuffix.isEmpty()) {
					break;
				}
			}
		}

		if (localSuffix == null) { // NPE safety
			localSuffix = "";
		}

		return localSuffix;
	}

	@Override
	public List<String> getPermissions(String world) {
		List<String> permissions = new LinkedList<String>();
		this.getInheritedPermissions(world, permissions, true, false, new HashSet<PermissionGroup>());
		return Collections.unmodifiableList(permissions);
	}

	@Override
	public void addPermission(String permission, String worldName) {
		List<String> permissions = new LinkedList<String>(this.getOwnPermissions(worldName));

		if (permissions.contains(permission)) {
			permissions.remove(permission);
		}

		permissions.add(0, permission);

		this.setPermissions(permissions, worldName);
	}

	@Override
	public void removePermission(String permission, String worldName) {
		List<String> permissions = new LinkedList<String>(this.getOwnPermissions(worldName));
		permissions.remove(permission);
		this.setPermissions(permissions, worldName);
	}

	protected void getInheritedPermissions(String worldName, List<String> permissions, boolean groupInheritance, boolean worldInheritance, Set<PermissionGroup> visitedGroups) {
		if (visitedGroups.size() == 0) {
			permissions.addAll(this.getTimedPermissions(worldName));
			permissions.addAll(this.getOwnPermissions(worldName));
		} else { // filter permissions for ancestors groups
			this.copyFilterPermissions(NON_INHERITABLE_PREFIX, permissions, this.getTimedPermissions(worldName));
			this.copyFilterPermissions(NON_INHERITABLE_PREFIX, permissions, this.getOwnPermissions(worldName));
		}

		if (worldName != null) {
			// World inheritance
			for (String parentWorld : this.manager.getWorldInheritance(worldName)) {
				getInheritedPermissions(parentWorld, permissions, false, true, visitedGroups);
			}
			// Common permission
			if (!worldInheritance) {
				this.getInheritedPermissions(null, permissions, false, true, visitedGroups);
			}
		}

		// Group inhertance
		if (groupInheritance && !visitedGroups.contains(this)) {
			visitedGroups.add(this);

			for (PermissionGroup group : this.getParentGroups(worldName)) {
				group.getInheritedPermissions(worldName, permissions, true, false, visitedGroups);
			}
		}
	}

	protected void copyFilterPermissions(String filterPrefix, List<String> to, List<String> from) {
		for (String permission : from) {
			if (permission.startsWith(filterPrefix)) {
				continue;
			}
			to.add(permission);
		}
	}

	@Override
	public void addTimedPermission(String permission, String world, int lifeTime) {
		super.addTimedPermission(permission, world, lifeTime);

		this.clearMembersCache();
	}

	@Override
	public void removeTimedPermission(String permission, String world) {
		super.removeTimedPermission(permission, world);

		this.clearMembersCache();
	}

	protected void clearMembersCache() {
		for (PermissionUser user : this.getUsers()) {
			user.clearCache();
		}
	}

	@Override
	public final void remove() {
		for (String world : this.getWorlds()) {
			this.clearChildren(world);
		}

		this.clearChildren(null);
		super.remove();
	}

	private void clearChildren(String worldName) {
		for (PermissionGroup group : this.getChildGroups(worldName)) {
			List<PermissionGroup> parentGroups = new LinkedList<PermissionGroup>(group.getParentGroups(worldName));
			parentGroups.remove(this);

			group.setParentGroupObjects(parentGroups, worldName);
		}

		for (PermissionUser user : this.getUsers(worldName)) {
			user.removeGroup(this, worldName);
		}
	}

	@Override
	public String getOption(String optionName, String worldName, String defaultValue) {
		return getOption(optionName, worldName, defaultValue, new HashSet<PermissionGroup>());
	}

	protected String getOption(String optionName, String worldName, String defaultValue, Set<PermissionGroup> alreadyVisited) {
		String value = this.getOwnOption(optionName, worldName, null);
		if (value != null) {
			return value;
		}

		if (worldName != null) { // world inheritance
			for (String world : manager.getWorldInheritance(worldName)) {
				value = this.getOption(optionName, world, null);
				if (value != null) {
					return value;
				}
			}

			// Check common space
			value = this.getOption(optionName, null, null);
			if (value != null) {
				return value;
			}
		}

		// Inheritance
		if (!alreadyVisited.contains(this)) {
			alreadyVisited.add(this);
			for (PermissionGroup group : this.getParentGroups(worldName)) {
				value = group.getOption(optionName, worldName, null, alreadyVisited);
				if (value != null) {
					return value;
				}
			}
		}

		// Nothing found
		return defaultValue;
	}


	@Override
	public int compareTo(PermissionGroup o) {
		return this.getWeight() - o.getWeight();
	}

}
