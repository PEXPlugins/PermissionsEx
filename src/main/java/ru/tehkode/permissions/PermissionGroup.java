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

     @Override
    public final String getPrefix() {
        String prefix = super.getPrefix();
        if(prefix == null || prefix.isEmpty()){
            for(PermissionGroup group : this.getParentGroups()){
                prefix = group.getPrefix();
                if(prefix != null && !prefix.isEmpty()){
                    break;
                }
            }
        }

        if(prefix == null){ // just for NPE safety
            prefix = "";
        }

        return prefix;
    }

    @Override
    public final String getSuffix() {
        String suffix = super.getSuffix();
        if(suffix == null || suffix.isEmpty()){
            for(PermissionGroup group : this.getParentGroups()){
                suffix = group.getSuffix();
                if(suffix != null && !suffix.isEmpty()){
                    break;
                }
            }
        }

        if(suffix == null){ // just for NPE safety
            suffix = "";
        }

        return suffix;
    }

    @Override
    protected void getInheritedPermissions(String world, List<String> permissions){
        permissions.addAll(Arrays.asList(this.getOwnPermissions(world)));

        for(PermissionGroup group : this.getParentGroups()){
            group.getInheritedPermissions(world, permissions);
        }
    }
    
    public boolean isChildOf(String groupName, boolean checkInheritance) {
        if (groupName == null || groupName.isEmpty()) {
            return false;
        }

        for (PermissionGroup group : this.getParentGroups()) {
            if (group.getName().equalsIgnoreCase(groupName)) {
                return true;
            }

            if (checkInheritance && group.isChildOf(groupName, checkInheritance)) {
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

            PermissionGroup group = this.manager.getGroup(parentGroup);
            if (!group.isChildOf(this.getName(), true)) {
                parentGroups.add(group);
            }
        }

        return parentGroups.toArray(new PermissionGroup[]{});
    }

    public PermissionGroup[] getChildGroups() {
        return this.manager.getGroups(this.getName());
    }

    public PermissionUser[] getUsers(){
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
        for(PermissionGroup group : this.manager.getGroups(this.getName())){
            List<PermissionGroup> parentGroups = Arrays.asList(group.getParentGroups());
            parentGroups.remove(this);
            group.setParentGroups(parentGroups.toArray(new PermissionGroup[0]));
        }

        if(this.manager.getGroups(this.getName()).length > 0){
            return;
        }

        for(PermissionUser user : this.manager.getUsers(this.getName()) ){
            user.removeGroup(this);
        }

        this.removeGroup();
    }

    protected abstract void removeGroup();


    public abstract void setParentGroups(PermissionGroup[] parentGroups);
}
