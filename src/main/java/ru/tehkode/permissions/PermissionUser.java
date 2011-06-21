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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.tehkode.permissions.exceptions.RankingException;

/**
 *
 * @author code
 */
public abstract class PermissionUser extends PermissionEntity {

    protected PermissionGroup[] cachedGroups = null;
    protected HashMap<String, String[]> cachedPermissions = new HashMap<String, String[]>();
    protected String cachedPrefix = null;
    protected String cachedSuffix = null;
    protected HashMap<String, String> cachedAnwsers = new HashMap<String, String>();

    public PermissionUser(String playerName, PermissionManager manager) {
        super(playerName, manager);
    }

    protected abstract String[] getOwnPermissions(String world);

    public abstract String getOwnOption(String option, String world, String defaultValue);

    public String getOwnOption(String option) {
        return this.getOwnOption(option, "", "");
    }

    public String getOwnOption(String option, String world) {
        return this.getOwnOption(option, world, "");
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

    public abstract void setGroups(String[] groups);

    protected abstract String[] getGroupsNamesImpl();

    public void setGroups(PermissionGroup[] parentGroups) {
        List<String> groups = new LinkedList<String>();

        for (PermissionGroup group : parentGroups) {
            groups.add(group.getName());
        }

        this.setGroups(groups.toArray(new String[0]));
    }

    public boolean inGroup(PermissionGroup group, boolean checkInheritance) {
        for (PermissionGroup parentGroup : this.getGroups()) {
            if (parentGroup.equals(group)) {
                return true;
            }

            if (checkInheritance && parentGroup.isChildOf(group, true)) {
                return true;
            }
        }

        return false;
    }

    public boolean inGroup(String groupName, boolean checkInheritance) {
        return this.inGroup(this.manager.getGroup(groupName), checkInheritance);
    }

    public boolean inGroup(PermissionGroup group) {
        return this.inGroup(group, true);
    }

    public boolean inGroup(String groupName) {
        return this.inGroup(this.manager.getGroup(groupName), true);
    }

    public PermissionGroup[] getGroups() {
        if (this.cachedGroups == null) {
            Set<PermissionGroup> groups = new LinkedHashSet<PermissionGroup>();

            for (String group : this.getGroupsNamesImpl()) {
                PermissionGroup parentGroup = this.manager.getGroup(group.trim());
                if (parentGroup != null) {
                    groups.add(parentGroup);
                }
            }

            if (groups.isEmpty()) {
                groups.add(this.manager.getDefaultGroup());
            }

            this.cachedGroups = groups.toArray(new PermissionGroup[]{});
        }

        return this.cachedGroups;
    }

    public String[] getGroupsNames() {
        List<String> groups = new LinkedList<String>();
        for (PermissionGroup group : this.getGroups()) {
            if (group != null) {
                groups.add(group.getName());
            }
        }

        return groups.toArray(new String[0]);
    }

    @Override
    public String[] getPermissions(String world) {
        if (!this.cachedPermissions.containsKey(world)) {
            List<String> permissions = new LinkedList<String>();
            this.getInheritedPermissions(world, permissions, true);

            this.cachedPermissions.put(world, permissions.toArray(new String[0]));
        }

        return this.cachedPermissions.get(world);
    }

    protected void getInheritedPermissions(String world, List<String> permissions, boolean groupInheritance) {
        permissions.addAll(Arrays.asList(this.getOwnPermissions(world)));

        // World inheritance
        for (String parentWorld : this.manager.getWorldInheritance(world)) {
            getInheritedPermissions(parentWorld, permissions, false);
        }

        // Common permissions
        permissions.addAll(Arrays.asList(this.getOwnPermissions(null)));

        // Group inhertance
        if (groupInheritance) {
            for (PermissionGroup group : this.getGroups()) {
                group.getInheritedPermissions(world, permissions, true);
            }
        }
    }

    public void addGroup(String groupName) {
        if (groupName == null || groupName.isEmpty()) {
            return;
        }

        this.addGroup(this.manager.getGroup(groupName));
    }

    public void addGroup(PermissionGroup group) {
        if (group == null) {
            return;
        }

        List<PermissionGroup> groups = new LinkedList<PermissionGroup>(Arrays.asList(this.getGroups()));

        if (this.getGroupsNamesImpl().length == 0 && groups.size() == 1 && groups.contains(this.manager.getDefaultGroup())) {
            groups.clear(); // clean out default group
        }

        if (group.isVirtual()) {
            group.save();
        }

        if (!groups.contains(group)) {
            groups.add(group);
            this.clearCache();
            this.setGroups(groups.toArray(new PermissionGroup[0]));
        }
    }

    public void removeGroup(String groupName) {
        if (groupName == null || groupName.isEmpty()) {
            return;
        }

        this.removeGroup(this.manager.getGroup(groupName));

        this.clearCache();
    }

    public void removeGroup(PermissionGroup group) {
        if (group == null) {
            return;
        }

        List<PermissionGroup> groups = new LinkedList<PermissionGroup>(Arrays.asList(this.getGroups()));

        if (groups.contains(group)) {
            groups.remove(group);
            this.clearCache();
            this.setGroups(groups.toArray(new PermissionGroup[]{}));
        }
    }

    public void promote(PermissionUser promoter, String ladderName) throws RankingException {
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
    }

    public void demote(PermissionUser demoter, String ladderName) throws RankingException {
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
                throw new RankingException("Promoter don't have high enough rank to change " + this.getName() + "'s rank", this, promoter);
            }
        }

        return promoterRank;
    }

    protected void swapGroups(PermissionGroup src, PermissionGroup dst) {
        List<PermissionGroup> groups = new ArrayList<PermissionGroup>(Arrays.asList(this.getGroups()));

        groups.remove(src);
        groups.add(dst);

        this.setGroups(groups.toArray(new PermissionGroup[0]));
    }

    public boolean isRanked(String ladder) {
        return (this.getRank(ladder) > 0);
    }

    public int getRank(String ladder) {
        Map<String, PermissionGroup> ladders = this.getRankLadders();

        if (ladders.containsKey(ladder)) {
            return ladders.get(ladder).getRank();
        }

        return 0;
    }
    
    public PermissionGroup getRankLadderGroup(String ladder){
        if(ladder == null || ladder.isEmpty()){
            ladder = "default";
        }
        
        return this.getRankLadders().get(ladder);
    }

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
    public String getPrefix() {
        if (this.cachedPrefix == null) {
            String localPrefix = super.getPrefix();
            if (localPrefix == null || localPrefix.isEmpty()) {
                for (PermissionGroup group : this.getGroups()) {
                    localPrefix = group.getPrefix();
                    if (localPrefix != null && !localPrefix.isEmpty()) {
                        break;
                    }
                }
            }

            if (localPrefix == null) { // just for NPE safety
                localPrefix = "";
            }

            this.cachedPrefix = localPrefix;
        }

        return this.cachedPrefix;
    }

    @Override
    public String getSuffix() {
        if (this.cachedSuffix == null) {
            String localSuffix = super.getSuffix();
            if (localSuffix == null || localSuffix.isEmpty()) {
                for (PermissionGroup group : this.getGroups()) {
                    localSuffix = group.getSuffix();
                    if (localSuffix != null && !localSuffix.isEmpty()) {
                        break;
                    }
                }
            }

            if (localSuffix == null) { // just for NPE safety
                localSuffix = "";
            }
            this.cachedSuffix = localSuffix;
        }

        return this.cachedSuffix;
    }

    @Override
    protected String getMatchingExpression(String permission, String world) {
        String cacheId = world + ":" + permission;
        if (!this.cachedAnwsers.containsKey(cacheId)) {
            this.cachedAnwsers.put(cacheId, super.getMatchingExpression(permission, world));
        }

        return this.cachedAnwsers.get(cacheId);
    }

    protected void clearCache() {
        this.cachedGroups = null;
        this.cachedPrefix = null;
        this.cachedSuffix = null;

        this.cachedPermissions.clear();
        this.cachedAnwsers.clear();
    }

    public String getOwnPrefix() {
        return this.prefix;
    }

    public String getOwnSuffix() {
        return this.suffix;
    }

    @Override
    public void setPrefix(String prefix) {
        super.setPrefix(prefix);
        this.clearCache();
    }

    @Override
    public void setSuffix(String postfix) {
        super.setSuffix(postfix);
        this.clearCache();
    }

    @Override
    public void remove() {
        this.clearCache();
    }

    @Override
    public void save() {
        this.clearCache();
    }
}
