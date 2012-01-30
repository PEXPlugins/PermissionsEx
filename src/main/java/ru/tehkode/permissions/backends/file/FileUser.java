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
package ru.tehkode.permissions.backends.file;

import ru.tehkode.permissions.GenericPermissionUser;
import ru.tehkode.permissions.PermissionManager;

/**
 *
 * @author code
 */
public class FileUser extends GenericPermissionUser {
	
	
    public FileUser(String playerName, PermissionManager manager, FileBackend backend) {
        super(playerName, manager, backend.getUserDataProvider(playerName));		
    }
	
	/*

    @Override
    protected String[] getGroupsNamesImpl(String worldName) {
        String inheritanceNode = "group";
        
        if(worldName != null && !worldName.isEmpty()){
            inheritanceNode = "worlds.`" + worldName + "`." + inheritanceNode;
        }
        
        Object groups = this.node.getProperty(inheritanceNode);

        if (groups instanceof String) { // old style
            String[] groupsArray;
            String groupsString = ((String) groups);
            if (groupsString.contains(",")) {
                groupsArray = ((String) groups).split(",");
            } else {
                groupsArray = new String[]{groupsString};
            }

            // Now migrate to new system
            this.node.setProperty("group", Arrays.asList(groupsArray));
            this.save();

            return groupsArray;
        } else if (groups instanceof List) {
            return ((List<String>) groups).toArray(new String[0]);
        } else {
            return new String[0];
        }
    }

    @Override
    public void setGroups(String[] groups, String worldName) {
        if (groups == null) {
            return;
        }
        
        String inheritanceNode = "group";
        
        if(worldName != null && !worldName.isEmpty()){
            inheritanceNode = "worlds.`" + worldName + "`." + inheritanceNode;
        }

        this.node.setProperty(inheritanceNode, Arrays.asList(groups));

        this.save();
        this.callEvent(PermissionEntityEvent.Action.INHERITANCE_CHANGED);
    }
	* 
	* */ 
}
