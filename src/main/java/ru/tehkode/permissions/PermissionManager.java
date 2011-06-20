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
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import ru.tehkode.permissions.config.Configuration;

/**
 *
 * @author code
 */
public class PermissionManager {

    protected static final Logger logger = Logger.getLogger("Minecraft");
    protected Map<String, PermissionUser> users = new HashMap<String, PermissionUser>();
    protected Map<String, PermissionGroup> groups = new HashMap<String, PermissionGroup>();
    protected PermissionBackend backend = null;
    protected PermissionGroup defaultGroup = null;
    protected Configuration config;

    public PermissionManager(Configuration config) {
        this.config = config;

        this.initBackend();
    }

    public void reset() {
        this.users.clear();
        this.groups.clear();
        this.defaultGroup = null;

        if (this.backend != null) {
            this.backend.reload();
        }
    }

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

    public boolean has(Player player, String permission) {
        return this.has(player.getName(), permission, player.getWorld().getName());
    }

    public boolean has(Player player, String permission, String world) {
        return this.has(player.getName(), permission, world);
    }

    public boolean has(String playerName, String permission, String world) {
        PermissionUser user = this.getUser(playerName);

        if (user == null) {
            return false;
        }

        return user.has(permission, world);
    }

    public void resetUser(String userName) {
        this.users.put(userName, null);
    }

    public void resetGroup(String groupName) {
        this.groups.put(groupName, null);
    }

    public boolean removeGroup(String groupName) {
        return backend.removeGroup(groupName);
    }

    public PermissionUser[] getUsers(String groupName) {
        return backend.getUsers(groupName);
    }

    public PermissionUser[] getUsers() {
        return backend.getUsers();
    }

    public PermissionGroup[] getGroups(String groupName) {
        return backend.getGroups(groupName);
    }

    public PermissionGroup[] getGroups() {
        return backend.getGroups();
    }

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

    public String[] getWorldInheritance(String worldName) {
        return backend.getWorldInheritance(worldName);
    }

    public PermissionGroup getDefaultGroup() {
        if (this.defaultGroup == null) {
            this.defaultGroup = this.backend.getDefaultGroup();
        }

        return this.defaultGroup;
    }

    public void setBackend(String backendName) {
        this.reset();
        this.backend = PermissionBackend.getBackend(backendName, this, config);
        this.backend.initialize();
    }

    public String getBackend() {
        return PermissionBackend.getBackendAlias(this.backend.getClass());
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
