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
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import ru.tehkode.permissions.events.PermissionEntityEvent;

/**
 *
 * @author t3hk0d3
 */
public abstract class PermissionGroup extends PermissionNode implements Comparable<PermissionGroup> {

	protected final static String NON_INHERITABLE_PREFIX = "#";
	protected int weight = 0;
	protected boolean dirtyWeight = true;

	public PermissionGroup(String groupName, PermissionManager manager) {
		super(groupName, manager);
	}

	@Override
	public void initialize() {
		super.initialize();

		if (this.isDebug()) {
			Logger.getLogger("Minecraft").info("[PermissionsEx] Group " + this.getName() + " initialized");
		}
	}

	@Override
	protected List<String> calculatePermissionInheritance(String worldName) {
		List<String> permissionsList = this.calculatePermissions(worldName, true);
		
		for (PermissionGroup parentGroup : this.getParents(worldName)) {
			List<String> parentPermissions = parentGroup.getPermissionsList(worldName);
			
			for (String permission : parentPermissions) {
				
			}
		}
		
		return permissionsList;
	}	
	
	@Override
	protected List<PermissionGroup> calculateParentInheritance(String worldName) {
		List<PermissionGroup> parentGroups = new LinkedList<PermissionGroup>();

		for (PermissionGroup parentGroup : this.getOwnParents(worldName)) {
			List<PermissionGroup> groups = parentGroup.getParents(worldName);

			groups.remove(this); // to prevent cyclic inheritance

			parentGroups.addAll(groups);
		}

		return parentGroups;
	}

	/**
	 * Return non-inherited group prefix. This means if a group don't have has
	 * own prefix then empty string or null would be returned
	 *
	 * @return prefix as string
	 */
	@Deprecated
	public String getOwnPrefix() {
		return this.getOwnPrefix(null);
	}

	/**
	 * Return non-inherited suffix prefix. This means if a group don't has own
	 * suffix then empty string or null would be returned
	 *
	 * @return suffix as string
	 */
	@Deprecated
	public final String getOwnSuffix() {
		return this.getOwnSuffix(null);
	}

	@Deprecated
	public final String[] getOwnPermissions(String world) {
		return this.getOwnPermissionsList(world).toArray(new String[0]);
	}

	@Deprecated
	public String getOwnOption(String option) {
		return this.getOwnOption(option, null, "");
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
	@Deprecated
	public boolean isRanked() {
		return (this.getRank() > 0);
	}

	/**
	 * Returns rank in ranking system. 0 if group is not ranked
	 *
	 * @return
	 */
	@Deprecated
	public int getRank() {
		return this.getOwnOptionInteger("rank", "", 0);
	}

	/**
	 * Set rank for this group
	 *
	 * @param rank Rank for group. Specify 0 to remove group from ranking
	 */
	@Deprecated
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
	@Deprecated
	public String getRankLadder() {
		return this.getOption("rank-ladder", null, "default");
	}

	/**
	 * Set rank ladder for this group
	 *
	 * @param rankLadder Name of rank ladder
	 */
	@Deprecated
	public void setRankLadder(String rankLadder) {
		if (rankLadder.isEmpty() || rankLadder.equals("default")) {
			rankLadder = null;
		}

		this.setOption("rank-ladder", rankLadder);

		this.callEvent(PermissionEntityEvent.Action.RANK_CHANGED);
	}

	// some api sugar
	public boolean isParentOf(PermissionNode group, String worldName) {
		return group.isChildOf(this, worldName);
	}

	public boolean isAncestorOf(PermissionNode group, String worldName) {
		return group.isDescendantOf(this, worldName);
	}

	/**
	 * Returns array of parent groups objects
	 *
	 * @return array of groups objects
	 */
	@Deprecated
	public final PermissionGroup[] getParentGroups(String worldName) {
		return this.getParents(worldName).toArray(new PermissionGroup[0]);
	}

	@Deprecated
	public final PermissionGroup[] getParentGroups() {
		return this.getParentGroups(null);
	}

	@Deprecated
	public final Map<String, PermissionGroup[]> getAllParentGroups() {
		return this.convertMap(this.getParentMap(), new PermissionGroup[0]);
	}

	/**
	 * Returns direct parents names of this group
	 *
	 * @return array of parents group names
	 */
	@Deprecated
	public final String[] getParentGroupsNames(String worldName) {
		return this.getParentNames(worldName).toArray(new String[0]);
	}

	@Deprecated
	public final String[] getParentGroupsNames() {
		return this.getParentGroupsNames(null);
	}

	/**
	 * Set parent groups
	 *
	 * @param parentGroups Array of parent groups names to set
	 */
	@Deprecated
	public final void setParentGroups(String[] parentGroups, String worldName) {
		List<PermissionGroup> groups = new ArrayList<PermissionGroup>();

		for (String group : parentGroups) {
			groups.add(manager.getGroup(group));
		}

		this.setParents(groups, worldName);
	}

	@Deprecated
	public void setParentGroups(String[] parentGroups) {
		this.setParentGroups(parentGroups, null);
	}

	/**
	 * Set parent groups
	 *
	 * @param parentGroups Array of parent groups objects to set
	 */
	@Deprecated
	public final void setParentGroups(PermissionGroup[] parentGroups, String worldName) {
		this.setParents(Arrays.asList(parentGroups), worldName);
	}

	@Deprecated
	public final void setParentGroups(PermissionGroup[] parentGroups) {
		this.setParentGroups(parentGroups, null);
	}

	@Deprecated
	public boolean isChildOf(PermissionGroup group, String worldName, boolean deep) {
		return deep ? isChildOf(group, worldName) : isDescendantOf(group, worldName);
	}

	@Deprecated
	public boolean isChildOf(PermissionGroup group, boolean deep) {
		for (String worldName : this.getWorldsList()) {
			if (this.isChildOf(group, worldName, deep)) {
				return true;
			}
		}

		return this.isChildOf(group, null, deep);
	}

	@Deprecated
	public boolean isChildOf(PermissionGroup group) {
		return isChildOf(group, false);
	}

	/**
	 * Check if this group is descendant of specified group
	 *
	 * @param groupName name of group to check against
	 * @param checkInheritance set to false to check only the direct inheritance
	 * @return
	 */
	@Deprecated
	public boolean isChildOf(String groupName, String worldName, boolean deep) {
		return deep ? isChildOf(this.manager.getGroup(groupName), worldName) : isDescendantOf(this.manager.getGroup(groupName), worldName);
	}

	@Deprecated
	public boolean isChildOf(String groupName, boolean checkInheritance) {
		return isChildOf(this.manager.getGroup(groupName), checkInheritance);
	}

	/**
	 * Check if specified group is direct parent of this group
	 *
	 * @param groupName to check against
	 * @return
	 */
	@Deprecated
	public boolean isChildOf(String groupName, String worldName) {
		return this.isChildOf(groupName, worldName, false);
	}

	@Deprecated
	public boolean isChildOf(String groupName) {
		return this.isChildOf(groupName, false);
	}

	/**
	 * Return array of direct child group objects
	 *
	 * @return
	 */
	@Deprecated
	public final PermissionGroup[] getChildGroups(String worldName) {
		return this.getParents(worldName).toArray(new PermissionGroup[0]);
	}

	@Deprecated
	public final PermissionGroup[] getChildGroups() {
		List<PermissionGroup> groups = new LinkedList<PermissionGroup>();

		for (String parentWorld : this.getWorldsList()) {
			groups.addAll(this.getParents(parentWorld));
		}

		return groups.toArray(new PermissionGroup[0]);
	}

	/**
	 * Return array of descendant group objects
	 *
	 * @return
	 */
	public Set<PermissionGroup> getChildList(String worldName) {
		Set<PermissionGroup> groups = new HashSet<PermissionGroup>(this.manager.getGroupsList());

		for (PermissionGroup group : groups) {
			if (!group.isChildOf(this, worldName)) {
				groups.remove(group);
			}
		}

		return groups;
	}

	public Set<PermissionGroup> getChildList() {
		Set<PermissionGroup> groups = new HashSet<PermissionGroup>();

		for (World world : Bukkit.getWorlds()) {
			groups.addAll(this.getChildList(world.getName()));
		}

		return groups;
	}

	public Set<PermissionGroup> getDescendantList(String worldName) {
		Set<PermissionGroup> groups = new HashSet<PermissionGroup>(this.manager.getGroupsList());

		for (PermissionGroup group : groups) {
			if (!group.isChildOf(this, worldName, true)) {
				groups.remove(group);
			}
		}

		return groups;
	}

	public Set<PermissionGroup> getDescendantList() {
		Set<PermissionGroup> groups = new HashSet<PermissionGroup>();

		for (World world : Bukkit.getWorlds()) {
			groups.addAll(this.getDescendantList(world.getName()));
		}

		return groups;
	}

	@Deprecated
	public PermissionGroup[] getDescendantGroups(String worldName) {
		return this.getDescendantList(worldName).toArray(new PermissionGroup[0]);
	}

	@Deprecated
	public PermissionGroup[] getDescendantGroups() {
		return this.getDescendantList().toArray(new PermissionGroup[0]);
	}

	/**
	 * Return array of direct members (users) of this group
	 *
	 * @return
	 */
	public Set<PermissionUser> getUsersList(String worldName) {
		return this.getUsersList(worldName, false);
	}

	public Set<PermissionUser> getUsersList(String worldName, boolean deep) {
		Set<PermissionUser> usersSet = new HashSet<PermissionUser>();

		for (PermissionUser user : this.manager.getUserList()) {
			if (user.inGroup(this, worldName, deep)) {
				usersSet.add(user);
			}
		}

		return usersSet;
	}

	public Set<PermissionUser> getUsersList() {
		return this.getUsersList(false);
	}

	public Set<PermissionUser> getUsersList(boolean deep) {
		Set<PermissionUser> users = new HashSet<PermissionUser>();

		for (World world : Bukkit.getWorlds()) {
			users.addAll(this.getUsersList(world.getName(), deep));
		}

		return users;
	}

	@Deprecated
	public PermissionUser[] getUsers(String worldName) {
		return this.getUsersList(worldName, false).toArray(new PermissionUser[0]);
	}

	@Deprecated
	public PermissionUser[] getUsers() {
		return this.getUsersList(false).toArray(new PermissionUser[0]);
	}

	public boolean isDefault(String worldName) {
		return this.equals(this.manager.getDefaultGroup(worldName));
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
	public void remove() {
		for (String world : this.getWorldsList()) {
			this.clearChildren(world);
		}

		this.clearChildren(null);

		this.callEvent(PermissionEntityEvent.Action.REMOVED);
	}

	private void clearChildren(String worldName) {
		for (PermissionGroup group : this.getChildList(worldName)) {
			List<PermissionGroup> parentGroups = new ArrayList<PermissionGroup>(group.getParents(worldName));
			parentGroups.remove(this);

			group.setParents(parentGroups, worldName);
		}

		for (PermissionUser user : this.getUsers(worldName)) {
			user.removeGroup(this, worldName);
		}
	}

	@Override
	public int compareTo(PermissionGroup o) {
		return this.getWeight() - o.getWeight();
	}
}
