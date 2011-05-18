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
package ru.tehkode.permissions.file;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.backends.FileBackend;
import ru.tehkode.permissions.config.Configuration;
import ru.tehkode.permissions.config.ConfigurationNode;

/**
 *
 * @author code
 */
public class FilePermissionUser extends PermissionUser {

    protected ConfigurationNode node;
    protected FileBackend backend;

    public FilePermissionUser(String playerName, PermissionManager manager, FileBackend backend) {
        super(playerName, manager);

        this.backend = backend;
        this.node = backend.permissions.getNode("users." + playerName);
        if (this.node == null) {
            this.node = Configuration.getEmptyNode();
            this.virtual = true;
        }

        this.prefix = this.node.getString("prefix",  "");
        this.suffix = this.node.getString("postfix", "");
    }

    @Override
    public String[] getOwnPermissions(String world) {
        Set<String> permissions = new LinkedHashSet<String>();

        if (world != null && !world.isEmpty()) {
            List<String> worldPermissions = this.node.getStringList("worlds." + world + ".permissions", null); // world specific permissions
            if (worldPermissions != null) {
                permissions.addAll(worldPermissions);
            }
        }

        List<String> commonPermissions = this.node.getStringList("permissions", null);
        if (commonPermissions != null) {
            permissions.addAll(commonPermissions);
        }

        return permissions.toArray(new String[]{});
    }

    @Override
    public Map<String, String> getOptions(String world) {
        Map<String, String> result = new HashMap<String, String>();

        ConfigurationNode commonOptions = this.node.getNode("options");
        if (commonOptions != null) {
            result.putAll(FileBackend.collectOptions(commonOptions.getRoot()));
        }

        // Override
        if (world != null && !world.isEmpty()) {
            ConfigurationNode worldNode = this.node.getNode("world." + world + ".options");
            if (worldNode != null) {
                result.putAll(FileBackend.collectOptions(worldNode.getRoot()));
            }
        }
        return result;
    }

    @Override
    protected String[] getGroupsNamesImpl() {
        String groups = this.node.getString("group");
        if (groups == null) {
            return new String[]{};
        } else if (groups.contains(",")) {
            return groups.split(",");
        } else {
            return new String[]{groups};
        }
    }

    @Override
    public String getOption(String permission, String world, boolean inheritance) {
        if (world != null && !world.isEmpty()) {
            String worldPermission = this.node.getString("worlds." + world + ".options." + permission);
            if (worldPermission != null && !worldPermission.isEmpty()) {
                return worldPermission;
            }
        }

        String commonPermission = this.node.getString("options." + permission);
        if (commonPermission != null && !commonPermission.isEmpty()) {
            return commonPermission;
        }

        if (inheritance) {
            for (PermissionGroup group : this.getGroups()) {
                String value = group.getOption(permission, world, inheritance);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }

        return "";
    }

    @Override
    public void addPermission(String permission, String world) {
        String nodePath = "permissions";
        if (world != null && !world.isEmpty()) {
            nodePath = "worlds." + world + "." + nodePath;
        }


        List<String> permissions = this.node.getStringList(nodePath, new LinkedList<String>());
        if (!permissions.contains(permission)) {
            permissions.add(permission);
        }
        this.node.setProperty(nodePath, permissions);

        this.save();
    }

    @Override
    public void setOption(String permission, String value, String world) {
        String nodePath = "options";
        if (world != null && !world.isEmpty()) {
            nodePath = "worlds." + world + "." + nodePath;
        }

        if (value != null && !value.isEmpty()) {
            nodePath += "." + permission;
            this.node.setProperty(nodePath, value);
        } else {
            this.node.removeProperty(nodePath);
        }

        this.save();
    }

    @Override
    public void removePermission(String permission, String world) {
        String nodePath = "permissions";
        if (world != null && !world.isEmpty()) {
            nodePath = "worlds." + world + "." + nodePath;
        }

        List<String> permissions = this.node.getStringList(nodePath, new LinkedList<String>());
        if (permissions.contains(permission)) {
            permissions.remove(permission);
            this.node.setProperty(nodePath, permissions);
        }

        this.save();
    }

    @Override
    public void setGroups(PermissionGroup[] groups) {
        if (groups == null) {
            return;
        }

        String newGroups = "";

        // @TODO: Replace this code with something more graceful
        for (PermissionGroup group : groups) {
            newGroups += "," + group.getName();
        }

        newGroups = newGroups.substring(1);

        this.node.setProperty("groups", newGroups);
    }

    @Override
    public void setPermissions(String[] permissions, String world) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void save() {
        if (this.virtual) {
            if (this.node.getString("group", null) == null) { // Set default group
                this.node.setProperty("group", this.manager.getDefaultGroup().getName());
            }
            this.backend.permissions.setProperty("users." + this.getName(), node);
        }

        this.backend.permissions.save();
    }

    @Override
    public void remove() {
        if (!this.virtual) {
            this.backend.permissions.setProperty("users." + this.getName(), null);
        }

        this.backend.permissions.save();
    }
}
