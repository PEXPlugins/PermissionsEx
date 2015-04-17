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

import com.google.common.collect.Maps;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.tehkode.permissions.events.PermissionEntityEvent;
import ru.tehkode.permissions.exceptions.RankingException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * @author code
 */
public class PermissionUser extends PermissionEntity {

	private final static String PERMISSION_NOT_FOUND = "<not found>"; // used replace null for ConcurrentHashMap

	private final PermissionsUserData data;
	protected Map<String, List<PermissionGroup>> cachedGroups = new HashMap<>();
	protected Map<String, List<String>> cachedPermissions = new HashMap<>();
	protected Map<String, String> cachedPrefix = new HashMap<>();
	protected Map<String, String> cachedSuffix = new HashMap<>();
	protected Map<String, String> cachedAnwsers = new ConcurrentHashMap<>();
	protected Map<String, String> cachedOptions = new HashMap<>();

	public PermissionUser(String playerName, PermissionsUserData data, PermissionManager manager) {
		super(playerName, manager);
		this.data = data;
	}

	@Override
	protected PermissionsUserData getData() {
		return data;
	}

	@Override
	public void initialize() {
		super.initialize();

		if (this.manager.shouldCreateUserRecords() && this.isVirtual()) {
			this.getData().setParents(this.getOwnParentIdentifiers(null), null);
		}
		updateTimedGroups();

		if (this.isDebug()) {
			manager.getLogger().info("User " + this.getIdentifier() + " initialized");
		}
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
	public Type getType() {
		return Type.USER;
	}

	@Override
	public String getOption(String optionName, String worldName, String defaultValue) {
		String cacheIndex = worldName + "|" + optionName;

		if (this.cachedOptions.containsKey(cacheIndex)) {
			return this.cachedOptions.get(cacheIndex);
		}

		String value = super.getOption(optionName, worldName, null);
		if (value != null) {
			this.cachedOptions.put(cacheIndex, value);
			return value;
		}

		// Nothing found
		return defaultValue;
	}

	@Override
	protected List<PermissionGroup> getParentsInternal(String worldName) {
		if (!this.cachedGroups.containsKey(worldName)) {
			List<PermissionGroup> groups = super.getParentsInternal(worldName);
			if (groups.isEmpty()) {
				groups.addAll(manager.getDefaultGroups(worldName));
				Collections.sort(groups);
			}
			this.cachedGroups.put(worldName, groups);
		}

		return this.cachedGroups.get(worldName);
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
			updateTimedGroups();
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
			throw new RankingException("User are not promoteable", this, promoter);
		}

		this.swapGroups(sourceGroup, targetGroup);

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

		this.callEvent(PermissionEntityEvent.Action.RANK_CHANGED);

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

	@Override
	public List<String> getPermissions(String worldName) {
		if (!this.cachedPermissions.containsKey(worldName)) {
			this.cachedPermissions.put(worldName, super.getPermissions(worldName));
		}

		return this.cachedPermissions.get(worldName);
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
		Validate.notNull(src);
		Validate.notNull(dst);

		List<PermissionGroup> groups = new ArrayList<>(this.getParents());
		int indexOfSrcGroup = groups.indexOf(src);

		Validate.isTrue(indexOfSrcGroup != -1);

		groups.set(indexOfSrcGroup, dst);

		this.setParents(groups);
	}

	@Override
	public String getPrefix(String worldName) {
		if (!this.cachedPrefix.containsKey(worldName)) {
			this.cachedPrefix.put(worldName, super.getPrefix(worldName));
		}

		return this.cachedPrefix.get(worldName);
	}

	@Override
	public boolean has(String permission) {
		Player player = getPlayer();
		if (player != null) {
			return this.has(permission, player.getWorld().getName());
		}

		return super.has(permission);
	}

	public Player getPlayer() {
		try {
			return Bukkit.getServer().getPlayer(UUID.fromString(getIdentifier()));
		} catch (Throwable ex) { // Not a UUID or method not implemented in server build
			return Bukkit.getServer().getPlayerExact(getIdentifier());
		}
	}

	@Override
	public String getSuffix(String worldName) {
		if (!this.cachedSuffix.containsKey(worldName)) {
			this.cachedSuffix.put(worldName, super.getSuffix(worldName));
		}

		return this.cachedSuffix.get(worldName);
	}

	@Override
	public String getMatchingExpression(String permission, String world) {
		String cacheId = world + ":" + permission;
		if (!this.cachedAnwsers.containsKey(cacheId)) {
			String result = super.getMatchingExpression(permission, world);

			if (result == null) {    // this is actually kinda dirty clutch
				result = PERMISSION_NOT_FOUND;  // ConcurrentHashMap deny storage of null values
			}

			this.cachedAnwsers.put(cacheId, result);
		}

		String result = this.cachedAnwsers.get(cacheId);

		if (PERMISSION_NOT_FOUND.equals(result)) {
			result = null;
		}

		return result;
	}

	protected void clearCache() {
		this.cachedPrefix.clear();
		this.cachedSuffix.clear();

		this.cachedGroups.clear();
		this.cachedPermissions.clear();
		this.cachedAnwsers.clear();
		this.cachedOptions.clear();
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

	protected void updateTimedGroups() {
		long nextExpiration = Long.MAX_VALUE;
		final Set<Map.Entry<String, String>> removeGroups = new HashSet<>();
		for (Map.Entry<String, Map<String, String>> world : getAllOptions().entrySet()) {
			for (Map.Entry<String, String> entry : world.getValue().entrySet()) {
				final String group = getTimedGroupName(entry.getKey());
				if (group == null) { // Not a timed group
					continue;
				}
				long groupLifetime = Long.parseLong(entry.getValue());
				if (groupLifetime > 0 && groupLifetime <= System.currentTimeMillis() / 1000) { // check for expiration
					removeGroups.add(Maps.immutableEntry(group, world.getKey()));
				} else {
					nextExpiration = Math.min(nextExpiration, groupLifetime);
				}
			}
		}

		for (Map.Entry<String, String> ent : removeGroups) {
			this.setOption("group-" + ent.getKey() + "-until", null, ent.getValue()); // remove option
			this.removeGroup(ent.getKey(), ent.getValue()); // remove membership
			if (isDebug()) {
				manager.getLogger().log(Level.INFO, ent.getValue() != null ? "Timed group '{0}' in world '{1}' expired from user {2}/{3}"
						: "Timed group {0} expired from user {2}/{3}", new Object[]{ent.getKey(), ent.getValue(), getIdentifier(), getName()});
			}
		}

		if (nextExpiration < Long.MAX_VALUE) {
			// Schedule the next timed groups check with the permissions manager
			manager.scheduleTimedGroupsCheck(nextExpiration, getIdentifier());
		}
	}

	static String getTimedGroupName(String option) {
		if (!option.startsWith("group-") && !option.endsWith("-until")) {
			return null;
		}
		return option.substring("group-".length(), option.length() - "-until".length());
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
