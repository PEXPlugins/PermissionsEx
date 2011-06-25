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

    public abstract String[] getOwnPermissions(String world);

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

    protected abstract void removeGroup();

    public abstract void setParentGroups(String[] parentGroups);

    public void setParentGroups(PermissionGroup[] parentGroups) {
        List<String> groups = new LinkedList<String>();

        for (PermissionGroup group : parentGroups) {
            groups.add(group.getName());
        }

        this.setParentGroups(groups.toArray(new String[0]));
    }

    public boolean isRanked() {
        return (this.getRank() > 0);
    }

    public int getRank() {
        return this.getOwnOptionInteger("rank", "", 0);
    }

    public void setRank(int rank) {
        if (rank > 0) {
            this.setOption("rank", Integer.toString(rank));
        } else {
            this.setOption("rank", null);
        }

    }

    public String getRankLadder() {
        return this.getOption("rank-ladder", "", "default");
    }

    public void setRankLadder(String rankGroup) {
        if (rankGroup.isEmpty() || rankGroup.equals("default")) {
            rankGroup = null;
        }

        this.setOption("rank-ladder", rankGroup);
    }

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

    public boolean isChildOf(String groupName, boolean checkInheritance) {
        return isChildOf(this.manager.getGroup(groupName), checkInheritance);
    }

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

    public PermissionGroup[] getChildGroups() {
        return this.manager.getGroups(this.getName());
    }

    public PermissionUser[] getUsers() {
        return this.manager.getUsers(this.getName());
    }

    public String[] getParentGroupsNames() {
        List<String> groups = new LinkedList<String>();
        for (PermissionGroup group : this.getParentGroups()) {
            groups.add(group.getName());
        }

        return groups.toArray(new String[0]);
    }

    public boolean isChildOf(String groupName) {
        return this.isChildOf(groupName, false);
    }

    protected abstract String[] getParentGroupsNamesImpl();

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
