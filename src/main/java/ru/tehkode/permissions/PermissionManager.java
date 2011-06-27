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

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import ru.tehkode.permissions.config.Configuration;

/**
 *
 * @author code
 */
public class PermissionManager {

    public final static int TRANSIENT_PERMISSION = 0;
    protected static final Logger logger = Logger.getLogger("Minecraft");
    protected Map<String, PermissionUser> users = new HashMap<String, PermissionUser>();
    protected Map<String, PermissionGroup> groups = new HashMap<String, PermissionGroup>();
    protected PermissionBackend backend = null;
    protected PermissionGroup defaultGroup = null;
    protected Configuration config;
    protected Timer timer = new Timer("PermissionsCleaner");
    protected boolean debugMode = false;

    public PermissionManager(Configuration config) {
        this.config = config;
        this.initBackend();

        this.debugMode = config.getBoolean("permissions.debug", false);
    }

    /**
     * Checks if specified player have specified permission
     * 
     * @param player
     * @param permission
     * @return 
     */
    public boolean has(Player player, String permission) {
        return this.has(player.getName(), permission, player.getWorld().getName());
    }

    /**
     * Checks if player have specified permission in specified world
     * 
     * @param player
     * @param permission
     * @param world
     * @return 
     */
    public boolean has(Player player, String permission, String world) {
        return this.has(player.getName(), permission, world);
    }

    /**
     * Check if player with specified name have permission in specified world
     * 
     * @param playerName
     * @param permission
     * @param world
     * @return 
     */
    public boolean has(String playerName, String permission, String world) {
        PermissionUser user = this.getUser(playerName);

        if (user == null) {
            return false;
        }

        return user.has(permission, world);
    }

    /**
     * Returns user's object
     * 
     * @param username
     * @return 
     */
    public PermissionUser getUser(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }

        PermissionUser user = users.get(username.toLowerCase());

        if (user == null) {
            user = this.backend.getUser(username);
            if (user != null) {
                this.users.put(username.toLowerCase(), user);
            }
        }

        return user;
    }

    /**
     * Returns object of specified player
     * 
     * @param player
     * @return 
     */
    public PermissionUser getUser(Player player) {
        return this.getUser(player.getName());
    }

    /**
     * Returns all registered users objects
     * 
     * @return 
     */
    public PermissionUser[] getUsers() {
        return backend.getUsers();
    }

    /**
     * Returns all users of specified group
     * 
     * @param groupName
     * @return 
     */
    public PermissionUser[] getUsers(String groupName) {
        return backend.getUsers(groupName);
    }

    /**
     * Returns all users of specified group and descendant groups 
     * 
     * @param groupName
     * @param inheritance if true return members of descendant groups of specified group
     * @return 
     */
    public PermissionUser[] getUsers(String groupName, boolean inheritance) {
        return backend.getUsers(groupName, inheritance);
    }

    /**
     * Reset in-memory object of specified group
     * 
     * @param userName 
     */
    public void resetUser(String userName) {
        this.users.remove(userName);
    }

    /**
     * Return object of specified group
     * 
     * @param groupname
     * @return 
     */
    public PermissionGroup getGroup(String groupname) {
        if (groupname == null || groupname.isEmpty()) {
            return null;
        }

        PermissionGroup group = groups.get(groupname.toLowerCase());

        if (group == null) {
            group = this.backend.getGroup(groupname);
            if (group != null) {
                this.groups.put(groupname.toLowerCase(), group);
            }
        }

        return group;
    }

    /**
     * Returns all groups
     * 
     * @return 
     */
    public PermissionGroup[] getGroups() {
        return backend.getGroups();
    }

    /**
     * Returns all child groups of specified group
     * 
     * @param groupName
     * @return 
     */
    public PermissionGroup[] getGroups(String groupName) {
        return backend.getGroups(groupName);
    }

    /**
     * Return all descendant or child groups of specified group
     * 
     * @param groupName
     * @param inheritance If true only direct child groups would be returned
     * @return 
     */
    public PermissionGroup[] getGroups(String groupName, boolean inheritance) {
        return backend.getGroups(groupName, inheritance);
    }

    /**
     * Returns default group object
     * 
     * @return default group object, null if not specified
     */
    public PermissionGroup getDefaultGroup() {
        if (this.defaultGroup == null) {
            this.defaultGroup = this.backend.getDefaultGroup();
        }

        return this.defaultGroup;
    }

    /**
     * Reset in-memory object of specified group
     * 
     * @param groupName 
     */
    public void resetGroup(String groupName) {
        this.groups.remove(groupName);
    }

    /**
     * Toggles debug mode
     * 
     * @param debug Set true to enable debug mode, false to disable
     */
    public void setDebug(boolean debug) {
        this.debugMode = debug;
    }

    /**
     * Returns state of debug mode
     * 
     * @return true if debug mode enabled, false if disabled
     */
    public boolean isDebug() {
        return this.debugMode;
    }

    /**
     * Return groups of specified rank ladder
     * 
     * @param ladderName
     * @return Map of ladder, key - rank of group, value - group object. Empty map if no such ladder exists
     */
    public Map<Integer, PermissionGroup> getRankLadder(String ladderName) {
        Map<Integer, PermissionGroup> ladder = new HashMap<Integer, PermissionGroup>();

        for (PermissionGroup group : this.getGroups()) {
            if (!group.isRanked()) {
                continue;
            }

            if (group.getRankLadder().equalsIgnoreCase(ladderName)) {
                ladder.put(group.getRank(), group);
            }
        }

        return ladder;
    }

    /**
     * Returns array with of world names which specified world inherit
     * 
     * @param world World name
     * @return Array of parent world, if there is no such than just empty array
     */
    public String[] getWorldInheritance(String worldName) {
        return backend.getWorldInheritance(worldName);
    }

    /**
     * Set world inheritance parents for specified world 
     * 
     * @param world world name which inheritance should be set
     * @param parentWorlds array of world names
     */
    public void setWorldInheritance(String world, String[] parentWorlds) {
        backend.setWorldInheritance(world, parentWorlds);
    }

    /**
     * Returns current backend
     * 
     * @return 
     */
    public String getBackend() {
        return PermissionBackend.getBackendAlias(this.backend.getClass());
    }

    /**
     * Set backend to specified backend.
     * This would also lead to resetting.
     * 
     * @param backendName 
     */
    public void setBackend(String backendName) {
        this.reset();
        this.backend = PermissionBackend.getBackend(backendName, this, config);
        this.backend.initialize();
    }

    /**
     * Register new timer task
     * 
     * @param task
     * @param delay 
     */
    public void registerTask(TimerTask task, int delay) {
        if (delay == TRANSIENT_PERMISSION) {
            return;
        }

        timer.schedule(task, delay * 1000);
    }

    /**
     * Reset all in-memory groups and users, clean up runtime stuff, reloads backend
     */
    public void reset() {
        this.users.clear();
        this.groups.clear();
        this.defaultGroup = null;

        // Close old timed Permission Timer
        timer.cancel();
        timer = new Timer("PermissionsCleaner");

        if (this.backend != null) {
            this.backend.reload();
        }
    }

    private void initBackend() {
        String backendName = this.config.getString("permissions.backend");

        if (backendName == null || backendName.isEmpty()) {
            backendName = PermissionBackend.defaultBackend; //Default backend
            this.config.setProperty("permissions.backend", backendName);
            this.config.save();
        }

        this.setBackend(backendName);
    }
}
