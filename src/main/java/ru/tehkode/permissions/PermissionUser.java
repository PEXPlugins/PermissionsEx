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

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.tehkode.permissions.events.PermissionEntityEvent;
import ru.tehkode.permissions.exceptions.RankingException;

import java.util.*;

/**
 * @author code
 */
@Deprecated
public class PermissionUser extends PermissionEntity {
	public PermissionUser(String playerName, PermissionManager manager) {
		super(playerName, manager);
	}

	@Override
	public String getName() {
		String name = getOwnOption("name", null, null);
		if (name == null) {
			Player player = getPlayer();
			if (player != null) {
				name = player.getName();
				setOption("name", name);
				return name;
			}
		}
		return super.getName();
	}

	@Override
	public EntityType getType() {
		return EntityType.USER;
	}

	@Deprecated
	public Map<String, List<PermissionGroup>> getAllGroups() {
		return getAllParents();
	}

	/**
	 * Add user to group
	 *
	 * @param groupName group's name as String
	 */
	public void addGroup(String groupName, String worldName) {
		if (groupName == null || groupName.isEmpty()) {
			return;
		}

		List<String> groups = new ArrayList<>(getOwnParentIdentifiers(worldName));

		if (groups.contains(groupName)) {
			return;
		}

		if (this.manager.userAddGroupsLast) {
			groups.add(groupName);
		} else {
			groups.add(0, groupName); //add group to start of list
		}

		this.setParentsIdentifier(groups, worldName);
	}

	public void addGroup(String groupName) {
		this.addGroup(groupName, null);
	}

	/**
	 * Add user to group
	 *
	 * @param group as PermissionGroup object
	 */
	public void addGroup(PermissionGroup group, String worldName) {
		if (group == null) {
			return;
		}

		this.addGroup(group.getIdentifier(), worldName);
	}

	public void addGroup(PermissionGroup group) {
		this.addGroup(group, null);
	}

	public void addGroup(String groupName, String worldName, long lifetime) {
		this.addGroup(groupName, worldName);

		if (lifetime > 0) {
			this.setOption("group-" + groupName + "-until", Long.toString(System.currentTimeMillis() / 1000 + lifetime), worldName);
		}
	}

	/**
	 * Remove user from group
	 *
	 * @param groupName group's name as String
	 */
	public void removeGroup(String groupName, String worldName) {
		if (groupName == null || groupName.isEmpty()) {
			return;
		}

		List<String> groups = new ArrayList<>(getOwnParentIdentifiers(worldName));
		if (!groups.contains(groupName)) {
			return;
		}

		groups.remove(groupName);
		this.setParentsIdentifier(groups, worldName);
	}

	public void removeGroup(String groupName) {
		this.removeGroup(this.manager.getGroup(groupName));
	}

	/**
	 * Remove user from group
	 *
	 * @param group group as PermissionGroup object
	 */
	public void removeGroup(PermissionGroup group, String worldName) {
		if (group == null) {
			return;
		}

		this.removeGroup(group.getIdentifier(), worldName);
	}

	public void removeGroup(PermissionGroup group) {
		for (String worldName : this.getWorlds()) {
			this.removeGroup(group, worldName);
		}

		this.removeGroup(group, null);
	}

	/**
	 * Check if this user is member of group or one of its descendant groups (optionally)
	 *
	 * @param group            group as PermissionGroup object
	 * @param worldName
	 * @param checkInheritance if true then descendant groups of the given group would be checked too
	 * @return true on success, false otherwise
	 */
	public boolean inGroup(PermissionGroup group, String worldName, boolean checkInheritance) {
		for (PermissionGroup parentGroup : this.getParents(worldName)) {
			if (parentGroup.equals(group)) {
				return true;
			}

			if (checkInheritance && parentGroup.isChildOf(group, worldName, true)) {
				return true;
			}
		}

		return false;
	}

	public boolean inGroup(PermissionGroup group, boolean checkInheritance) {
		for (String worldName : this.getWorlds()) {
			if (this.inGroup(group, worldName, checkInheritance)) {
				return true;
			}
		}

		return this.inGroup(group, null, checkInheritance);
	}

	/**
	 * Check if this user is member of group or one of its descendant groups (optionally)
	 *
	 * @param groupName        group's name to check
	 * @param worldName
	 * @param checkInheritance if true than descendant groups of specified group would be checked too
	 * @return true on success, false otherwise
	 */
	public boolean inGroup(String groupName, String worldName, boolean checkInheritance) {
		return this.inGroup(this.manager.getGroup(groupName), worldName, checkInheritance);
	}

	public boolean inGroup(String groupName, boolean checkInheritance) {
		return this.inGroup(this.manager.getGroup(groupName), checkInheritance);
	}

	/**
	 * Check if this user is member of group or one of its descendant groups
	 *
	 * @param group
	 * @param worldName
	 * @return true on success, false otherwise
	 */
	public boolean inGroup(PermissionGroup group, String worldName) {
		return this.inGroup(group, worldName, true);
	}

	public boolean inGroup(PermissionGroup group) {
		return this.inGroup(group, true);
	}

	/**
	 * Checks if this user is member of specified group or one of its descendant groups
	 *
	 * @param groupName group's name
	 * @return true on success, false otherwise
	 */
	public boolean inGroup(String groupName, String worldName) {
		return this.inGroup(this.manager.getGroup(groupName), worldName, true);
	}

	public boolean inGroup(String groupName) {
		return this.inGroup(groupName, true);
	}

	/**
	 * Promotes user in specified ladder.
	 * If user is not member of the ladder RankingException will be thrown
	 * If promoter is not null and he is member of the ladder and
	 * his rank is lower then user's RankingException will be thrown too.
	 * If there is no group to promote the user to RankingException would be thrown
	 *
	 * @param promoter   null if action is performed from console or by a plugin
	 * @param ladderName Ladder name
	 * @throws RankingException
	 */
	public PermissionGroup promote(PermissionUser promoter, String ladderName) throws RankingException {
		if (ladderName == null || ladderName.isEmpty()) {
			ladderName = "default";
		}

		int promoterRank = getPromoterRankAndCheck(promoter, ladderName);
		int rank = this.getRank(ladderName);

		PermissionGroup sourceGroup = this.getRankLadders().get(ladderName);
		PermissionGroup targetGroup = null;

		for (Map.Entry<Integer, PermissionGroup> entry : this.manager.getRankLadder(ladderName).entrySet()) {
			int groupRank = entry.getValue().getRank();
			if (groupRank >= rank) { // group have equal or lower than current rank
				continue;
			}

			if (groupRank <= promoterRank) { // group have higher rank than promoter
				continue;
			}

			if (targetGroup != null && groupRank <= targetGroup.getRank()) { // group have higher rank than target group
				continue;
			}

			targetGroup = entry.getValue();
		}

		if (targetGroup == null) {
			throw new RankingException("User is not promotable", this, promoter);
		}

		this.swapGroups(sourceGroup, targetGroup);


		return targetGroup;
	}

	/**
	 * Demotes user in specified ladder.
	 * If user is not member of the ladder RankingException will be thrown
	 * If demoter is not null and he is member of the ladder and
	 * his rank is lower then user's RankingException will be thrown too.
	 * If there is no group to demote the user to RankingException would be thrown
	 *
	 * @param demoter   Specify null if action performed from console or by plugin
	 * @param ladderName
	 * @throws RankingException
	 */
	public PermissionGroup demote(PermissionUser demoter, String ladderName) throws RankingException {
		if (ladderName == null || ladderName.isEmpty()) {
			ladderName = "default";
		}

		int promoterRank = getPromoterRankAndCheck(demoter, ladderName);
		int rank = this.getRank(ladderName);

		PermissionGroup sourceGroup = this.getRankLadders().get(ladderName);
		PermissionGroup targetGroup = null;

		for (Map.Entry<Integer, PermissionGroup> entry : this.manager.getRankLadder(ladderName).entrySet()) {
			int groupRank = entry.getValue().getRank();
			if (groupRank <= rank) { // group have equal or higher than current rank
				continue;
			}

			if (groupRank <= promoterRank) { // group have higher rank than promoter
				continue;
			}

			if (targetGroup != null && groupRank >= targetGroup.getRank()) { // group have lower rank than target group
				continue;
			}

			targetGroup = entry.getValue();
		}

		if (targetGroup == null) {
			throw new RankingException("User are not demoteable", this, demoter);
		}

		this.swapGroups(sourceGroup, targetGroup);

		return targetGroup;
	}

	/**
	 * Check if the user is in the specified ladder
	 *
	 * @param ladder Ladder name
	 * @return true on success, false otherwise
	 */
	public boolean isRanked(String ladder) {
		return (this.getRank(ladder) > 0);
	}

	/**
	 * Return user rank in specified ladder
	 *
	 * @param ladder Ladder name
	 * @return rank as int
	 */
	public int getRank(String ladder) {
		Map<String, PermissionGroup> ladders = this.getRankLadders();

		if (ladders.containsKey(ladder)) {
			return ladders.get(ladder).getRank();
		}

		return 0;
	}

	/**
	 * Return user's group in specified ladder
	 *
	 * @param ladder Ladder name
	 * @return PermissionGroup object of ranked ladder group
	 */
	public PermissionGroup getRankLadderGroup(String ladder) {
		if (ladder == null || ladder.isEmpty()) {
			ladder = "default";
		}

		return this.getRankLadders().get(ladder);
	}

	/**
	 * Return all ladders the user is participating in
	 *
	 * @return Map, key - name of ladder, group - corresponding group of that ladder
	 */
	public Map<String, PermissionGroup> getRankLadders() {
		Map<String, PermissionGroup> ladders = new HashMap<>();

		for (PermissionGroup group : this.getParents()) {
			if (!group.isRanked()) {
				continue;
			}

			ladders.put(group.getRankLadder(), group);
		}

		return ladders;
	}

	protected int getPromoterRankAndCheck(PermissionUser promoter, String ladderName) throws RankingException {
		if (!this.isRanked(ladderName)) { // not ranked
			throw new RankingException("User are not in this ladder", this, promoter);
		}

		int rank = this.getRank(ladderName);
		int promoterRank = 0;

		if (promoter != null && promoter.isRanked(ladderName)) {
			promoterRank = promoter.getRank(ladderName);

			if (promoterRank >= rank) {
				throw new RankingException("Promoter don't have high enough rank to change " + this.getIdentifier() + "/" + getName() + "'s rank", this, promoter);
			}
		}

		return promoterRank;
	}

	protected void swapGroups(PermissionGroup src, PermissionGroup dst) {
		List<PermissionGroup> groups = new ArrayList<>(this.getParents());

		groups.remove(src);
		groups.add(dst);

		this.setParents(groups);
	}

	public Player getPlayer() {
		try {
			return Bukkit.getServer().getPlayer(UUID.fromString(getIdentifier()));
		} catch (Throwable ex) { // Not a UUID or method not implemented in server build
			return Bukkit.getServer().getPlayerExact(getIdentifier());
		}
	}

	@Override
	public boolean explainExpression(String expression) {
		if (expression == null && this.manager.allowOps) {
			Player player = getPlayer();
			if (player != null && player.isOp()) {
				return true;
			}
		}

		return super.explainExpression(expression);
	}

	protected boolean checkMembership(PermissionGroup group, String worldName) {
		int groupLifetime = this.getOwnOptionInteger("group-" + group.getIdentifier() + "-until", worldName, 0);

		if (groupLifetime > 0 && groupLifetime < System.currentTimeMillis() / 1000) { // check for expiration
			this.setOption("group-" + group.getIdentifier() + "-until", null, worldName); // remove option
			this.removeGroup(group, worldName); // remove membership
			// @TODO Make notification of player about expired memebership
			return false;
		}

		return true;
	}

	// Compatibility methods
	@Deprecated
	public String[] getGroupsNames() {
		return getGroupsNames(null);
	}

	@Deprecated
	public String[] getGroupsNames(String world) {
		return getParentIdentifiers(world).toArray(new String[0]);
	}


	/**
	 * Get group for this user, global inheritance only
	 *
	 * @return
	 */
	@Deprecated
	public PermissionGroup[] getGroups() {
		return getParents().toArray(new PermissionGroup[0]);
	}

	/**
	 * Get groups for this user for specified world
	 *
	 * @param worldName Name of world
	 * @return PermissionGroup groups
	 */
	@Deprecated
	public PermissionGroup[] getGroups(String worldName) {
		return getParents(worldName).toArray(new PermissionGroup[0]);
	}

	/**
	 * Get group names, common space only
	 *
	 * @return
	 */
	@Deprecated
	public String[] getGroupNames() {
		return getParentIdentifiers().toArray(new String[0]);
	}

	/**
	 * Get group names in specified world
	 *
	 * @return String array of user's group names
	 */
	@Deprecated
	public String[] getGroupNames(String worldName) {
		return getParentIdentifiers(worldName).toArray(new String[0]);
	}

	/**
	 * Set parent groups for user
	 *
	 * @param groups array of parent group names
	 */
	@Deprecated
	public void setGroups(String[] groups, String worldName) {
		setParentsIdentifier(Arrays.asList(groups), worldName);
	}

	@Deprecated
	public void setGroups(String[] groups) {
		setParentsIdentifier(Arrays.asList(groups));
	}

	/**
	 * Set parent groups for user
	 *
	 * @param parentGroups array of parent group objects
	 */
	@Deprecated
	public void setGroups(PermissionGroup[] parentGroups, String worldName) {
		setParents(Arrays.asList(parentGroups), worldName);
	}

	@Deprecated
	public void setGroups(PermissionGroup[] parentGroups) {
		setParents(Arrays.asList(parentGroups));
	}
}
