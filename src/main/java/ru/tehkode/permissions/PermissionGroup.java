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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author code
 */
public abstract class PermissionGroup extends PermissionEntity {

    public PermissionGroup(String groupName, PermissionManager manager) {
        super(groupName, manager);
    }

    /**
     * Renames group
     * 
     * @param newName 
     */
    @Override
    public void setName(String newName) {
        String oldName = this.getName();


    }

    /**
     * Copy group
     * 
     * @param name
     * @return Copy of group with specifed name
     */
    public PermissionGroup copy(String name) {
        try {
            PermissionGroup copy = (PermissionGroup) this.clone();
            
            copy.setName(name);
            copy.save();
            
            
            
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns own (without inheritance) permissions of group for specified world
     * @param world
     * @return Array of permissions
     */
    public abstract String[] getOwnPermissions(String world);

    /**
     * Returns specified option value in specified world without inheritance
     * This mean option value wouldn't be inherited from parent groups
     * 
     * @param option
     * @param world
     * @param defaultValue
     * @return option value, or defaultValue if option is not found in own options
     */
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

    /**
     * Checks if group participating in ranking system
     * @return 
     */
    public boolean isRanked() {
        return (this.getRank() > 0);
    }

    /**
     * Returns rank in ranking system, 0 if group are not ranked
     * 
     * @return 
     */
    public int getRank() {
        return this.getOwnOptionInteger("rank", "", 0);
    }

    /**
     * Set rank for this group
     * 
     * @param rank Rank for group, specify 0 to remove group from ranking
     */
    public void setRank(int rank) {
        if (rank > 0) {
            this.setOption("rank", Integer.toString(rank));
        } else {
            this.setOption("rank", null);
        }

    }

    /**
     * Returns ranking ladder where this group participating
     * 
     * @return 
     */
    public String getRankLadder() {
        return this.getOption("rank-ladder", "", "default");
    }

    /**
     * Set rank ladder for this group
     * 
     * @param rankLadder 
     */
    public void setRankLadder(String rankLadder) {
        if (rankLadder.isEmpty() || rankLadder.equals("default")) {
            rankLadder = null;
        }

        this.setOption("rank-ladder", rankLadder);
    }

    protected abstract String[] getParentGroupsNamesImpl();

    /**
     * Returns array of parent groups objects
     * 
     * @return array of groups objects
     */
    public PermissionGroup[] getParentGroups() {
        Set<PermissionGroup> parentGroups = new HashSet<PermissionGroup>();

        for (String parentGroup : this.getParentGroupsNamesImpl()) {

            // Yeah horrible thing, i know, that just safety from invoking empty named groups
            parentGroup = parentGroup.trim();
            if (parentGroup.isEmpty()) {
                continue;
            }

            if (parentGroup.equals(this.getName())) {
                continue;
            }

            PermissionGroup group = this.manager.getGroup(parentGroup);
            if (!group.isChildOf(this, true)) { // To prevent cyclic inheritance
                parentGroups.add(group);
            }
        }

        return parentGroups.toArray(new PermissionGroup[0]);
    }

    /**
     * Returns direct parents names of this group
     * 
     * @return array of group names
     */
    public String[] getParentGroupsNames() {
        List<String> groups = new LinkedList<String>();
        for (PermissionGroup group : this.getParentGroups()) {
            groups.add(group.getName());
        }

        return groups.toArray(new String[0]);
    }

    /**
     * Set parent groups
     * 
     * @param parentGroups Array of parent groups names
     */
    public abstract void setParentGroups(String[] parentGroups);

    /**
     * Set parent groups
     * 
     * @param parentGroups Array of parent groups objects
     */
    public void setParentGroups(PermissionGroup[] parentGroups) {
        List<String> groups = new LinkedList<String>();

        for (PermissionGroup group : parentGroups) {
            groups.add(group.getName());
        }

        this.setParentGroups(groups.toArray(new String[0]));
    }

    protected abstract void removeGroup();

    /**
     * Check if this group are descendant of specified group 
     * 
     * @param group group object of parent
     * @param checkInheritance set false to check only the direct inheritance
     * @return true if this group are descendant or direct parent of specified group
     */
    public boolean isChildOf(PermissionGroup group, boolean checkInheritance) {
        if (group == null) {
            return false;
        }

        for (PermissionGroup parentGroup : this.getParentGroups()) {
            if (group.equals(parentGroup)) {
                return true;
            }

            if (checkInheritance && parentGroup.isChildOf(group, checkInheritance)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if this group are descendant of specified group 
     * 
     * @param groupName name of group to check for
     * @param checkInheritance set false to check only the direct inheritance
     * @return 
     */
    public boolean isChildOf(String groupName, boolean checkInheritance) {
        return isChildOf(this.manager.getGroup(groupName), checkInheritance);
    }

    /**
     * Check if specified group are direct parent of this group
     * 
     * @param groupName
     * @return 
     */
    public boolean isChildOf(String groupName) {
        return this.isChildOf(groupName, false);
    }

    /**
     * Return array of direct child group objects
     * 
     * @return 
     */
    public PermissionGroup[] getChildGroups() {
        return this.manager.getGroups(this.getName(), false);
    }

    /**
     * Return array of descendant group objects
     * 
     * @return 
     */
    public PermissionGroup[] getDescendantGroups() {
        return this.manager.getGroups(this.getName(), true);
    }

    /**
     * Return array of direct members (users) of this group
     * 
     * @return 
     */
    public PermissionUser[] getUsers() {
        return this.manager.getUsers(this.getName(), false);
    }

    /**
     * Overriden methods
     */
    @Override
    public String getPrefix() {
        String localPrefix = super.getPrefix();
        if (localPrefix == null || localPrefix.isEmpty()) {
            for (PermissionGroup group : this.getParentGroups()) {
                localPrefix = group.getPrefix();
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
    public String getSuffix() {
        String localSuffix = super.getSuffix();
        if (localSuffix == null || localSuffix.isEmpty()) {
            for (PermissionGroup group : this.getParentGroups()) {
                localSuffix = group.getSuffix();
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
    public String[] getPermissions(String world) {
        List<String> permissions = new LinkedList<String>();
        this.getInheritedPermissions(world, permissions, true);
        return permissions.toArray(new String[0]);
    }

    protected void getInheritedPermissions(String world, List<String> permissions, boolean groupInheritance) {
        permissions.addAll(Arrays.asList(this.getTimedPermissions(world)));
        permissions.addAll(Arrays.asList(this.getOwnPermissions(world)));

        if (world != null) {
            // World inheritance
            for (String parentWorld : this.manager.getWorldInheritance(world)) {
                getInheritedPermissions(parentWorld, permissions, false);
            }
            // Common permission
            this.getInheritedPermissions(null, permissions, false);
        }

        // Group inhertance
        if (groupInheritance) {
            for (PermissionGroup group : this.getParentGroups()) {
                group.getInheritedPermissions(world, permissions, true);
            }
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
        for (PermissionUser user : this.manager.getUsers(this.getName(), true)) {
            user.clearCache();
        }
    }

    @Override
    public final void remove() {
        for (PermissionGroup group : this.manager.getGroups(this.getName())) {
            List<PermissionGroup> parentGroups = new LinkedList<PermissionGroup>(Arrays.asList(group.getParentGroups()));
            parentGroups.remove(this);
            group.setParentGroups(parentGroups.toArray(new PermissionGroup[0]));
        }

        if (this.manager.getGroups(this.getName()).length > 0) {
            return;
        }

        for (PermissionUser user : this.manager.getUsers(this.getName())) {
            user.removeGroup(this);
        }

        this.removeGroup();
    }

    public String getOwnPrefix() {
        return this.prefix;
    }

    public String getOwnSuffix() {
        return this.suffix;
    }
}
