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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import ru.tehkode.permissions.PermissionEntity;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.backends.FileBackend;
import ru.tehkode.permissions.config.ConfigurationNode;

public class FileEntity extends PermissionEntity {

    protected ConfigurationNode node;
    protected FileBackend backend;
    protected String baseNode;

    public FileEntity(String entityName, PermissionManager manager, FileBackend backend, String baseNode) {
        super(entityName, manager);

        this.backend = backend;
        this.baseNode = baseNode + "." + entityName;

        this.node = backend.permissions.getNode(this.baseNode);

        if (node == null) {
            node = new ConfigurationNode();
            this.virtual = true;
        }

        this.prefix = this.node.getString("prefix");
        if (this.prefix == null) {
            this.prefix = "";
        }

        this.suffix = this.node.getString("suffix");
        if (this.suffix == null) {
            this.suffix = "";
        }
    }

    public ConfigurationNode getConfigNode() {
        return this.node;
    }

    @Override
    public String[] getPermissions(String world) {
        String permissionsNode = "permissions";
        if (world != null && !world.isEmpty()) {
            permissionsNode = "worlds." + world + "." + permissionsNode;
        }

        return this.node.getStringList(permissionsNode, new LinkedList<String>()).toArray(new String[0]);
    }

    @Override
    public void addPermission(String permission, String world) {
        String nodePath = "permissions";
        if (world != null && !world.isEmpty()) {
            nodePath = "worlds." + world + "." + nodePath;
        }


        List<String> permissions = this.node.getStringList(nodePath, new LinkedList<String>());
        if (!permissions.contains(permission)) {
            permissions.add(0, permission); // Add permission to begining
        }
        this.node.setProperty(nodePath, permissions);

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
    public void setPermissions(String[] permissions, String world) {
        String nodePath = "permissions";
        if (world != null && !world.isEmpty()) {
            nodePath = "worlds." + world + "." + nodePath;
        }

        this.node.setProperty(nodePath, new LinkedList<String>(Arrays.asList(permissions)));

        this.save();
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
    public String getOption(String permission, String world) {
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

        return "";
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
    public void setPrefix(String prefix) {
        if (prefix != null && !prefix.isEmpty()) {
            this.node.setProperty("prefix", prefix);
        } else {
            this.node.removeProperty("prefix");
        }

        this.save();

        super.setPrefix(prefix);
    }

    @Override
    public void setSuffix(String suffix) {
        if (suffix != null && !suffix.isEmpty()) {
            this.node.setProperty("suffix", suffix);
        } else {
            this.node.removeProperty("suffix");
        }

        this.save();

        super.setSuffix(suffix);
    }

    @Override
    public Map<String, String[]> getAllPermissions() {
        Map<String, String[]> allPermissions = new HashMap<String, String[]>();

        // Common permissions
        List<String> commonPermissions = this.node.getStringList("permissions", null);
        if (commonPermissions != null) {
            allPermissions.put("", commonPermissions.toArray(new String[0]));
        }

        //World-specific permissions
        List<String> worlds = this.node.getKeys("worlds");
        if (worlds != null) {
            for (String world : worlds) {
                List<String> worldPermissions = this.node.getStringList("world." + world + ".permissions", null);
                if (commonPermissions != null) {
                    allPermissions.put(world, worldPermissions.toArray(new String[0]));
                }
            }
        }

        return allPermissions;
    }

    @Override
    public Map<String, Map<String, String>> getAllOptions() {
        Map<String, Map<String, String>> allOptions = new HashMap<String, Map<String, String>>();

        ConfigurationNode commonOptionsNode = this.node.getNode("options");
        if (commonOptionsNode != null) {
            allOptions.put("", FileBackend.collectOptions(commonOptionsNode.getRoot()));
        }

        List<String> worlds = this.node.getKeys("worlds");
        if (worlds != null) {
            for (String world : worlds) {
                ConfigurationNode worldOptionsNode = this.node.getNode("world." + world + ".permissions");
                if (worldOptionsNode != null) {
                    allOptions.put(world, FileBackend.collectOptions(worldOptionsNode.getRoot()));
                }
            }
        }

        return allOptions;
    }

    @Override
    public void save() {
        this.backend.permissions.setProperty(baseNode, node);

        this.backend.permissions.save();
    }

    @Override
    public void remove() {
        this.node.getRoot().clear();
        this.prefix = "";
        this.suffix = "";

        this.backend.permissions.removeProperty(baseNode);

        this.backend.permissions.save();
    }
}
