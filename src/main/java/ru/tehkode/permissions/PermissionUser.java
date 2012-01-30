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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.tehkode.permissions.events.PermissionEntityEvent;
import ru.tehkode.permissions.exceptions.RankingException;

/**
 *
 * @author t3hk0d3
 */
public abstract class PermissionUser extends PermissionNode {

	protected Map<String, String> cachedAnwsers = new HashMap<String, String>();

	public PermissionUser(String playerName, PermissionManager manager) {
		super(playerName, manager);
	}

	@Override
	public void initialize() {
		super.initialize();
		
		if (this.manager.getBackend().isCreateUserRecords() && this.isVirtual()) {
			this.setGroups(this.getGroups(null), null);

			this.save();
		}
		
		if (this.isDebug()) {
			Logger.getLogger("Minecraft").info("[PermissionsEx] User " + this.getName() + " initialized");
		}
	}

	
	@Deprecated
	public String getOwnPrefix() {
		return this.getOwnPrefix(null);
	}

	
	@Deprecated
	public final String getOwnSuffix() {
		return this.getOwnSuffix(null);
	}
	
	@Deprecated
	public final String[] getOwnPermissions(String world) {
		return this.getOwnPermissionsList(world).toArray(new String[0]);
	}
	/**
	 * Return non-inherited value of specified option in common space (all worlds).
	 * 
	 * @param option
	 * @return option value or empty string if option is not set
	 */
	@Deprecated
	public String getOwnOption(String option) {
		return this.getOwnOption(option, null, "");
	}

	/**
	 * Get group for this user, global inheritance only
	 * 
	 * @return 
	 */
	

	@Deprecated
	public final PermissionGroup[] getGroups() {
		return this.getGroups(null);
	}

	/**
	 * Get groups for this user for specified world
	 * 
	 * @param worldName Name of world
	 * @return PermissionGroup groups
	 */
	@Deprecated
	public final PermissionGroup[] getGroups(String worldName) {
		return this.getParents(worldName).toArray(new PermissionGroup[0]);
	}
	
	@Deprecated
	public final Map<String, PermissionGroup[]> getAllGroups() {
		return this.convertMap(this.getParentMap(), new PermissionGroup[0]);
	}
	
	/**
	 * Get group names, common space only
	 * 
	 * @return 
	 */
	@Deprecated
	public List<String> getGroupsNamesList(String worldName) {
		return this.getParentNames(worldName);
	}
	
	@Deprecated
	public final String[] getGroupsNames() {
		return this.getGroupsNames(null);
	}

	/**
	 * Get group names in specified world
	 * 
	 * @return String array of user's group names
	 */
	@Deprecated
	public final String[] getGroupsNames(String worldName) {
		return this.getParents(worldName).toArray(new String[0]);
	}

	/**
	 * Set parent groups for user
	 * 
	 * @param groups array of parent group names
	 */	
	@Deprecated
	public void setGroups(String[] groupNames, String worldName) {
		List<PermissionGroup> groups = new ArrayList<PermissionGroup>();

		for (String groupName : groupNames) {
			groups.add(this.manager.getGroup(groupName));
		}

		this.setParents(groups, worldName);
	}

	@Deprecated
	public void setGroups(String[] groups) {
		this.setGroups(groups, null);
	}

	/**
	 * Set parent groups for user
	 * 
	 * @param groups array of parent group objects
	 */
	@Deprecated
	public void setGroups(PermissionGroup[] parentGroups, String worldName) {
		
	}

	@Deprecated
	public void setGroups(PermissionGroup[] parentGroups) {
		this.setGroups(parentGroups, null);
	}

	/**
	 * Add user to group
	 * 
	 * @param groupName group's name as String
	 */	
	@Deprecated
	public void addGroup(String groupName, String worldName) {
		this.addGroup(this.manager.getGroup(groupName), worldName);
	}

	@Deprecated
	public void addGroup(String groupName) {
		this.addGroup(groupName, null);
	}

	/**
	 * Add user to group
	 * 
	 * @param group as PermissionGroup object
	 */
	@Deprecated
	public void addGroup(PermissionGroup group, String worldName) {
		if (group == null) {
			return;
		}

		this.addParent(group, worldName);
	}

	@Deprecated
	public void addGroup(PermissionGroup group) {
		this.addGroup(group, null);
	}

	@Deprecated
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
	@Deprecated
	public void removeGroup(String groupName, String worldName) {
		if (groupName == null || groupName.isEmpty()) {
			return;
		}

		this.removeGroup(this.manager.getGroup(groupName), worldName);
	}

	@Deprecated
	public void removeGroup(String groupName) {
		this.removeGroup(this.manager.getGroup(groupName));
	}

	/**
	 * Remove user from group
	 * 
	 * @param group group as PermissionGroup object
	 */
	@Deprecated
	public void removeGroup(PermissionGroup group, String worldName) {
		if (group == null) {
			return;
		}

		this.removeGroup(group.getName(), worldName);
	}

	@Deprecated
	public void removeGroup(PermissionGroup group) {
		for (String worldName : this.getWorlds()) {
			this.removeGroup(group, worldName);
		}

		this.removeGroup(group, null);
	}

	/**
	 * Check if this user is member of group or one of its descendant groups (optionally)
	 * 
	 * @param group group as PermissionGroup object
	 * @param worldName 
	 * @param deep if true then descendant groups of the given group would be checked too 
	 * @return true on success, false otherwise
	 */
	@Deprecated
	public boolean inGroup(PermissionGroup group, String worldName, boolean deep) {
		return deep ? this.isDescendantOf(group, worldName) : this.isChildOf(group, worldName);
	}

	public boolean inGroup(PermissionGroup group, boolean checkInheritance) {
		for (String worldName : this.getWorldsList()) {
			if (this.inGroup(group, worldName, checkInheritance)) {
				return true;
			}
		}

		return this.inGroup(group, null, checkInheritance);
	}

	/**
	 * Check if this user is member of group or one of its descendant groups (optionally)
	 * 
	 * @param groupName group's name to check
	 * @param worldName 
	 * @param checkInheritance if true than descendant groups of specified group would be checked too 
	 * @return true on success, false otherwise
	 */
	@Deprecated
	public boolean inGroup(String groupName, String worldName, boolean checkInheritance) {
		return this.inGroup(this.manager.getGroup(groupName), worldName, checkInheritance);
	}

	@Deprecated
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
	@Deprecated
	public boolean inGroup(PermissionGroup group, String worldName) {
		return this.inGroup(group, worldName, true);
	}

	@Deprecated
	public boolean inGroup(PermissionGroup group) {
		return this.inGroup(group, true);
	}

	/**
	 * Checks if this user is member of specified group or one of its descendant groups
	 * 
	 * @param group group's name
	 * @return true on success, false otherwise
	 */
	@Deprecated
	public boolean inGroup(String groupName, String worldName) {
		return this.inGroup(this.manager.getGroup(groupName), worldName, true);
	}

	@Deprecated
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
	 * 
	 * @param promoter null if action is performed from console or by a plugin
	 * @param ladderName Ladder name
	 * @throws RankingException 
	 */
	@Deprecated
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
			throw new RankingException("User are not promoteable", this, promoter);
		}

		this.swapGroups(null, sourceGroup, targetGroup);

		this.callEvent(PermissionEntityEvent.Action.RANK_CHANGED);

		return targetGroup;
	}

	/**
	 * Demotes user in specified ladder.
	 * If user is not member of the ladder RankingException will be thrown
	 * If demoter is not null and he is member of the ladder and 
	 * his rank is lower then user's RankingException will be thrown too.
	 * If there is no group to demote the user to RankingException would be thrown
	 * 
	 * @param promoter Specify null if action performed from console or by plugin
	 * @param ladderName
	 * @throws RankingException 
	 */
	@Deprecated
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

		this.swapGroups(null, sourceGroup, targetGroup);

		this.callEvent(PermissionEntityEvent.Action.RANK_CHANGED);

		return targetGroup;
	}

	/**
	 * Check if the user is in the specified ladder
	 * 
	 * @param ladder Ladder name
	 * @return true on success, false otherwise
	 */
	@Deprecated
	public boolean isRanked(String ladder) {
		return (this.getRank(ladder) > 0);
	}

	/**
	 * Return user rank in specified ladder
	 * 
	 * @param ladder Ladder name
	 * @return rank as int
	 */
	@Deprecated
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
	@Deprecated
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
	@Deprecated
	public Map<String, PermissionGroup> getRankLadders() {
		Map<String, PermissionGroup> ladders = new HashMap<String, PermissionGroup>();

		for (PermissionGroup group : this.getGroups()) {
			if (!group.isRanked()) {
				continue;
			}

			ladders.put(group.getRankLadder(), group);
		}

		return ladders;
	}

	@Override
	public void addTimedPermission(String permission, String world, int lifeTime) {
		super.addTimedPermission(permission, world, lifeTime);

		this.clearCache();
	}

	@Override
	public void removeTimedPermission(String permission, String world) {
		super.removeTimedPermission(permission, world);

		this.clearCache();
	}

	@Deprecated
	protected int getPromoterRankAndCheck(PermissionUser promoter, String ladderName) throws RankingException {
		if (!this.isRanked(ladderName)) { // not ranked
			throw new RankingException("User are not in this ladder", this, promoter);
		}

		int rank = this.getRank(ladderName);
		int promoterRank = 0;

		if (promoter != null && promoter.isRanked(ladderName)) {
			promoterRank = promoter.getRank(ladderName);

			if (promoterRank >= rank) {
				throw new RankingException("Promoter don't have high enough rank to change " + this.getName() + "'s rank", this, promoter);
			}
		}

		return promoterRank;
	}

	protected void swapGroups(String worldName, PermissionGroup src, PermissionGroup dst) {
		List<PermissionGroup> groups = new ArrayList<PermissionGroup>(Arrays.asList(this.getGroups()));

		groups.remove(src);
		groups.add(dst);

		this.setParents(groups, worldName);
	}

	@Override
	public boolean has(String permission) {
		Player player = Bukkit.getServer().getPlayer(this.getName());
		if (player != null) {
			return this.has(permission, player.getWorld().getName());
		}

		return super.has(permission);
	}

	@Override
	public String getMatchingExpression(String permission, String world) {
		String cacheId = world + ":" + permission;
		if (!this.cachedAnwsers.containsKey(cacheId)) {
			this.cachedAnwsers.put(cacheId, super.getMatchingExpression(permission, world));
		}

		return this.cachedAnwsers.get(cacheId);
	}

	@Override
	public void clearCache() {
		super.clearCache();
		this.cachedAnwsers.clear();
	}

	@Override
	public void remove() {
		this.clearCache();

		this.callEvent(PermissionEntityEvent.Action.REMOVED);
	}

	@Override
	public void save() {
		this.clearCache();

		this.callEvent(PermissionEntityEvent.Action.SAVED);
	}

	@Override
	public boolean explainExpression(String expression) {
		if (expression == null && this.manager.allowOps) {
			Player player = Bukkit.getServer().getPlayer(this.getName());
			if (player != null && player.isOp()) {
				return true;
			}
		}

		return super.explainExpression(expression);
	}

	protected boolean checkMembership(PermissionGroup group, String worldName) {
		int groupLifetime = this.getOwnOptionInteger("group-" + group.getName() + "-until", worldName, 0);

		if (groupLifetime > 0 && groupLifetime < System.currentTimeMillis() / 1000) { // check for expiration
			this.setOption("group-" + group.getName() + "-until", null, worldName); // remove option
			this.removeGroup(group, worldName); // remove membership
			// @TODO Make notification of player about expired memebership
			return false;
		}

		return true;
	}
}
