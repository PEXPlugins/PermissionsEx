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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.tehkode.permissions.config.Configuration;
import ru.tehkode.permissions.events.PermissionEntityEvent;
import ru.tehkode.permissions.events.PermissionEvent;
import ru.tehkode.permissions.events.PermissionSystemEvent;

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
     * Check if specified player has specified permission
     * 
     * @param player player object
     * @param permission permission string to check against
     * @return true on success false otherwise
     */
    public boolean has(Player player, String permission) {
        return this.has(player.getName(), permission, player.getWorld().getName());
    }

    /**
     * Check if player has specified permission in world
     * 
     * @param player player object
     * @param permission permission as string to check against
     * @param world world's name as string
     * @return true on success false otherwise
     */
    public boolean has(Player player, String permission, String world) {
        return this.has(player.getName(), permission, world);
    }

    /**
     * Check if player with name has permission in world
     * 
     * @param player player object
     * @param permission permission as string to check against
     * @param world world's name as string
     * @return true on success false otherwise
     */
    public boolean has(String playerName, String permission, String world) {
        PermissionUser user = this.getUser(playerName);

        if (user == null) {
            return false;
        }

        return user.has(permission, world);
    }

    /**
     * Return user's object
     * 
     * @param username get PermissionUser with given name
     * @return PermissionUser instance
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
     * Return object of specified player
     * 
     * @param player player object
     * @return PermissionUser instance
     */
    public PermissionUser getUser(Player player) {
        return this.getUser(player.getName());
    }

    /**
     * Return all registered user objects
     * 
     * @return PermissionUser array
     */
    public PermissionUser[] getUsers() {
        return backend.getUsers();
    }

    /**
     * Return all users in group
     * 
     * @param groupName group's name
     * @return PermissionUser array
     */
    public PermissionUser[] getUsers(String groupName, String worldName) {
        return backend.getUsers(groupName, worldName);
    }

    public PermissionUser[] getUsers(String groupName) {
        return backend.getUsers(groupName);
    }

    /**
     * Return all users in group and descendant groups 
     * 
     * @param groupName group's name
     * @param inheritance true return members of descendant groups of specified group
     * @return PermissionUser array for groupnName
     */
    public PermissionUser[] getUsers(String groupName, String worldName, boolean inheritance) {
        return backend.getUsers(groupName, worldName, inheritance);
    }

    public PermissionUser[] getUsers(String groupName, boolean inheritance) {
        return backend.getUsers(groupName, inheritance);
    }

    /**
     * Reset in-memory object of specified group
     * 
     * @param userName user's name
     */
    public void resetUser(String userName) {
        this.users.remove(userName);
    }

    /**
     * Return object for specified group
     * 
     * @param groupname group's name
     * @return PermissionGroup object
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
     * Return all groups
     * 
     * @return PermissionGroup array
     */
    public PermissionGroup[] getGroups() {
        return backend.getGroups();
    }

    /**
     * Return all child groups of specified group
     * 
     * @param groupName group's name
     * @return PermissionGroup array
     */
    public PermissionGroup[] getGroups(String groupName, String worldName) {
        return backend.getGroups(groupName, worldName);
    }

    public PermissionGroup[] getGroups(String groupName) {
        return backend.getGroups(groupName);
    }

    /**
     * Return all descendants or child groups for groupName
     * 
     * @param groupName group's name
     * @param inheritance true: only direct child groups would be returned
     * @return PermissionGroup array for specified groupName
     */
    public PermissionGroup[] getGroups(String groupName, String worldName, boolean inheritance) {
        return backend.getGroups(groupName, worldName, inheritance);
    }

    public PermissionGroup[] getGroups(String groupName, boolean inheritance) {
        return backend.getGroups(groupName, inheritance);
    }

    /**
     * Return default group object
     * 
     * @return default group object. null if not specified
     */
    public PermissionGroup getDefaultGroup() {
        if (this.defaultGroup == null) {
            this.defaultGroup = this.backend.getDefaultGroup();
        }

        return this.defaultGroup;
    }

    /**
     * Set default group to specified group
     * 
     * @param group PermissionGroup group object
     */
    public void setDefaultGroup(PermissionGroup group) {
        if (group == null || group.equals(this.defaultGroup)) {
            return;
        }

        backend.setDefaultGroup(group);
        this.callEvent(PermissionSystemEvent.Action.DEFAULTGROUP_CHANGED);
        this.callEvent(new PermissionEntityEvent(group, PermissionEntityEvent.Action.DEFAULTGROUP_CHANGED));
    }

    /**
     * Reset in-memory object for groupName
     * 
     * @param groupName group's name
     */
    public void resetGroup(String groupName) {
        this.groups.remove(groupName);
    }

    /**
     * Set debug mode
     * 
     * @param debug true enables debug mode, false disables
     */
    public void setDebug(boolean debug) {
        this.debugMode = debug;
        this.callEvent(PermissionSystemEvent.Action.DEBUGMODE_TOGGLE);
    }

    /**
     * Return current state of debug mode
     * 
     * @return true debug is enabled, false if disabled
     */
    public boolean isDebug() {
        return this.debugMode;
    }

    /**
     * Return groups of specified rank ladder
     * 
     * @param ladderName
     * @return Map of ladder, key - rank of group, value - group object. Empty map if ladder does not exist
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
     * Return array of world names who has world inheritance
     * 
     * @param world World name
     * @return Array of parent world, if world does not exist return empty array
     */
    public String[] getWorldInheritance(String worldName) {
        return backend.getWorldInheritance(worldName);
    }

    /**
     * Set world inheritance parents for world 
     * 
     * @param world world name which inheritance should be set
     * @param parentWorlds array of parent world names
     */
    public void setWorldInheritance(String world, String[] parentWorlds) {
        backend.setWorldInheritance(world, parentWorlds);
        this.callEvent(PermissionSystemEvent.Action.WORLDINHERITANCE_CHANGED);
    }

    /**
     * Return current backend
     * 
     * @return current backend object
     */
    public PermissionBackend getBackend() {
        return this.backend;
    }

    /**
     * Set backend to specified backend.
     * This would also cause backend resetting.
     * 
     * @param backendName name of backend to set to
     */
    public void setBackend(String backendName) {
        this.reset();
        this.backend = PermissionBackend.getBackend(backendName, this, config);
        this.backend.initialize();

        this.callEvent(PermissionSystemEvent.Action.BACKEND_CHANGED);
    }

    /**
     * Register new timer task
     * 
     * @param task TimerTask object
     * @param delay delay in seconds
     */
    protected void registerTask(TimerTask task, int delay) {
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
        this.callEvent(PermissionSystemEvent.Action.RELOADED);
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

    protected void callEvent(PermissionEvent event) {
        Bukkit.getServer().getPluginManager().callEvent(event);
    }

    protected void callEvent(PermissionSystemEvent.Action action) {
        this.callEvent(new PermissionSystemEvent(action));
    }
}
