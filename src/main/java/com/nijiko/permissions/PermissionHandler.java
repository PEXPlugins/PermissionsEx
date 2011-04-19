package com.nijiko.permissions;

import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

/**
 * Permissions 2.x
 * Copyright (C) 2011  Matt 'The Yeti' Burnett <admin@theyeticave.net>
 * Original Credit & Copyright (C) 2010 Nijikokun <nijikokun@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Permissions Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Permissions Public License for more details.
 *
 * You should have received a copy of the GNU Permissions Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * Abstract method for multiple permission handlers
 *
 * @author Nijiko
 */
/**
 *            
 * @author code
 */
public abstract class PermissionHandler {

    public abstract void setDefaultWorld(String world);

    public abstract boolean loadWorld(String world);

    public abstract void forceLoadWorld(String world);

    public abstract boolean checkWorld(String world);

    public abstract void load();

    public abstract void load(String world, Configuration config);

    public abstract void reload();

    public abstract boolean reload(String world);

    // Cache
    public abstract void setCache(String world, Map<String, Boolean> Cache);

    public abstract void setCacheItem(String world, String player, String permission, boolean data);

    public abstract Map<String, Boolean> getCache(String world);

    public abstract boolean getCacheItem(String world, String player, String permission);

    public abstract void removeCachedItem(String world, String player, String permission);

    public abstract void clearCache(String world);

    public abstract void clearAllCache();

    /**
     * Simple alias for permission method.
     * Easier to understand / recognize what it does and is checking for.
     *
     * @param player
     * @param permission
     * @return boolean
     */
    public abstract boolean has(Player player, String permission);

    /**
     * Checks to see if a player has permission to a specific tree node.
     * <br /><br />
     * Example usage:
     * <blockquote><pre>
     * boolean canReload = Plugin.Permissions.Security.permission(player, "permission.reload");
     * if(canReload) {
     *	System.out.println("The user can reload!");
     * } else {
     *	System.out.println("The user has no such permission!");
     * }
     * </pre></blockquote>
     *
     * @param player
     * @param permission
     * @return boolean
     */
    public abstract boolean permission(Player player, String permission);

    /**
     * Grabs group name.
     * <br /><br />
     * Namespace: groups.name
     *
     * @param group
     * @return String
     */
    public abstract String getGroup(String world, String name);

    /**
     * Grabs users groups.
     * <br /><br />
     * 
     * @param group
     * @return Array
     */
    public abstract String[] getGroups(String world, String name);

    /**
     * Checks to see if the player is in the requested group. 
     *
     * @param world
     * @param name - Player
     * @param group - Group to be checked upon.
     * @return boolean
     */
    public abstract boolean inGroup(String world, String name, String group);

    /**
     * Checks to see if a player is in a single group.
     * This does not check inheritance.
     * 
     * @param world
     * @param name - Player
     * @param group - Group to be checked
     * @return boolean
     */
    public abstract boolean inSingleGroup(String world, String name, String group);

    /**
     * Grabs group prefix, line that comes before the group.
     * <br /><br />
     * Namespace: groups.name.info.prefix
     *
     * @param world
     * @param group
     * @return String
     */
    public abstract String getGroupPrefix(String world, String group);

    /**
     * Grabs group suffix, line that comes after the group.
     * <br /><br />
     * Namespace: groups.name.info.suffix
     *
     * @param world
     * @param group
     * @return String
     */
    public abstract String getGroupSuffix(String world, String group);

    /**
     * Checks to see if the group has build permission.
     * <br /><br />
     * Namespace: groups.name.info.build
     *
     * @param world
     * @param group
     * @return String
     */
    public abstract boolean canGroupBuild(String world, String group);

    /**
     * Get permission nodes from a group that contain values.
     * <br /><br />
     * Grab Group Permission String values.
     *
     * @param world
     * @param group
     * @param permission
     * @return String. If no string found return "".
     */
    public abstract String getGroupPermissionString(String world, String group, String permission);

    /**
     * Get permission nodes from a group that contain values.
     * <br /><br />
     * Grab Group Permission Integer values.
     *
     * @param world
     * @param group
     * @param permission
     * @return Integer. No integer found return -1.
     */
    public abstract int getGroupPermissionInteger(String world, String group, String permission);

    /**
     * Get permission nodes from a group that contain values.
     * <br /><br />
     * Grab Group Permission String values.
     * 
     * @param group
     * @param permission
     * @return Boolean. No boolean found return false.
     */
    public abstract boolean getGroupPermissionBoolean(String world, String group, String permission);

    /**
     * Get permission nodes from a group that contain values.
     * <br /><br />
     * Grab Group Permission Double values.
     *
     * @param world
     * @param group
     * @param permission
     * @return Double. No value found return -1.0
     */
    public abstract double getGroupPermissionDouble(String world, String group, String permission);

    /**
     * Get permission nodes from a specific user that contain values.
     * <br /><br />
     * Grab User Permission String values.
     *
     * @param world
     * @param group
     * @param permission
     * @return String. If no string found return "".
     */
    public abstract String getUserPermissionString(String world, String name, String permission);

    /**
     * Get permission nodes from a specific user that contain values.
     * <br /><br />
     * Grab User Permission Integer values.
     *
     * @param world
     * @param group
     * @param permission
     * @return Integer. No integer found return -1.
     */
    public abstract int getUserPermissionInteger(String world, String name, String permission);

    /**
     * Get permission nodes from a specific user that contain values.
     * <br /><br />
     * Grab User Permission Boolean values.
     *
     * @param world
     * @param group
     * @param permission
     * @return Boolean. No boolean found return false.
     */
    public abstract boolean getUserPermissionBoolean(String world, String name, String permission);

    /**
     * Get permission nodes from a specific user that contain values.
     * <br /><br />
     * Grab User Permission Double values.
     *
     * @param world
     * @param group
     * @param permission
     * @return Double. No value found return -1.0
     */
    public abstract double getUserPermissionDouble(String world, String name, String permission);

    /**
     * Get permission nodes from a user / group that contain values.
     * <br /><br />
     * Grab User Permission String values.
     *
     * @param world
     * @param group
     * @param permission
     * @return String. If no string found return "".
     */
    public abstract String getPermissionString(String world, String name, String permission);

    /**
     * Get permission nodes from a user / group that contain values.
     * <br /><br />
     * Grab User Permission Integer values.
     *
     * @param world
     * @param group
     * @param permission
     * @return Integer. No integer found return -1.
     */
    public abstract int getPermissionInteger(String world, String name, String permission);

    /**
     * Get permission nodes from a user / group that contain values.
     * <br /><br />
     * Grab User Permission Boolean values.
     *
     * @param world
     * @param group
     * @param permission
     * @return Boolean. No boolean found return false.
     */
    public abstract boolean getPermissionBoolean(String world, String name, String permission);

    /**
     * Get permission nodes from a user / group that contain values.
     * <br /><br />
     * Grab User Permission Double values.
     *
     * @param world
     * @param group
     * @param permission
     * @return Double. No value found return -1.0
     */
    public abstract double getPermissionDouble(String world, String name, String permission);

    public abstract void addGroupInfo(String world, String group, String node, Object data);

    public abstract void removeGroupInfo(String world, String group, String node);

    public abstract void addUserPermission(String world, String user, String node);

    public abstract void removeUserPermission(String world, String user, String node);

    //Addition by rcjrrjcr
    public abstract void save(String world);

    public abstract void saveAll();
    //End of addition by rcjrrjcr
}
