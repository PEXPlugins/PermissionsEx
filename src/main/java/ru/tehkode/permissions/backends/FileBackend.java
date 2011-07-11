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
package ru.tehkode.permissions.backends;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;
import ru.tehkode.permissions.PermissionBackend;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.config.Configuration;
import ru.tehkode.permissions.config.ConfigurationNode;
import ru.tehkode.permissions.file.FileGroup;
import ru.tehkode.permissions.file.FileUser;

/**
 *
 * @author code
 */
public class FileBackend extends PermissionBackend {

    public Configuration permissions;

    public FileBackend(PermissionManager manager, Configuration config) {
        super(manager, config);
    }

    @Override
    public void initialize() {
        String permissionFilename = config.getString("permissions.backends.file.file");

        // Default settings
        if (permissionFilename == null) {
            permissionFilename = "permissions.yml";
            config.setProperty("permissions.backends.file.file", "permissions.yml");
            config.save();
        }

        String baseDir = config.getString("permissions.basedir");

        if (baseDir.contains("\\") && !"\\".equals(File.separator)) {
            baseDir = baseDir.replace("\\", File.separator);
        }

        File baseDirectory = new File(baseDir);
        if (!baseDirectory.exists()) {
            baseDirectory.mkdirs();
        }

        File permissionFile = new File(baseDir, permissionFilename);

        permissions = new Configuration(permissionFile);

        if (!permissionFile.exists()) {
            try {
                permissionFile.createNewFile();

                // Load default permissions
                permissions.setProperty("groups.default.default", true);


                List<String> defaultPermissions = new LinkedList<String>();
                // Specify here default permissions
                defaultPermissions.add("modifyworld.*");

                permissions.setProperty("groups.default.permissions", defaultPermissions);

                permissions.save();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        permissions.load();
    }

    @Override
    public String[] getWorldInheritance(String world) {
        if (world != null && !world.isEmpty()) {
            return this.permissions.getStringList("worlds." + world + ".inheritance", new LinkedList<String>()).toArray(new String[0]);
        }

        return new String[0];
    }

    @Override
    public void setWorldInheritance(String world, String[] parentWorlds) {
        if (world == null && world.isEmpty()) {
            return;
        }

        this.permissions.setProperty("worlds." + world + ".inheritance", Arrays.asList(parentWorlds));
    }

    @Override
    public PermissionUser getUser(String userName) {
        return new FileUser(userName, manager, this);
    }

    @Override
    public PermissionGroup getGroup(String groupName) {
        return new FileGroup(groupName, manager, this);
    }

    @Override
    public PermissionGroup getDefaultGroup() {
        Map<String, ConfigurationNode> groupsMap = this.permissions.getNodesMap("groups");

        if (groupsMap == null || groupsMap.isEmpty()) {
            throw new RuntimeException("No groups defined. Check your permissions file.");
        }

        for (Map.Entry<String, ConfigurationNode> entry : groupsMap.entrySet()) {
            if (entry.getValue().getBoolean("default", false)) {
                return this.manager.getGroup(entry.getKey());
            }
        }

        throw new RuntimeException("Default user group are not defined. Please select one using \"default: true\" property");
    }

    @Override
    public void setDefaultGroup(PermissionGroup group) {
        Map<String, ConfigurationNode> groupsMap = this.permissions.getNodesMap("groups");

        for (Map.Entry<String, ConfigurationNode> entry : groupsMap.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase(group.getName())
                    && entry.getValue().getBoolean("default", false)) {
                entry.getValue().removeProperty("default");
            }

            if (entry.getKey().equalsIgnoreCase(group.getName())) {
                entry.getValue().setProperty("default", true);
            }
        }
    }

    @Override
    public PermissionGroup[] getGroups() {
        List<PermissionGroup> groups = new LinkedList<PermissionGroup>();
        Map<String, ConfigurationNode> groupsMap = this.permissions.getNodesMap("groups");

        for (Map.Entry<String, ConfigurationNode> entry : groupsMap.entrySet()) {
            groups.add(this.manager.getGroup(entry.getKey()));
        }

        Collections.sort(groups);

        return groups.toArray(new PermissionGroup[0]);
    }

    @Override
    public PermissionUser[] getUsers() {
        List<PermissionUser> users = new LinkedList<PermissionUser>();
        Map<String, ConfigurationNode> userMap = this.permissions.getNodesMap("users");

        if (userMap != null) {
            for (Map.Entry<String, ConfigurationNode> entry : userMap.entrySet()) {
                users.add(this.manager.getUser(entry.getKey()));
            }
        }

        return users.toArray(new PermissionUser[]{});
    }

    @Override
    public void reload() {
        this.permissions.load();
    }

    public static Map<String, String> collectOptions(Map<String, Object> root) {
        return collectOptions(root, "", new HashMap<String, String>());
    }

    protected static Map<String, String> collectOptions(Map<String, Object> root, String baseKey, Map<String, String> collector) {
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            String newKey = entry.getKey();
            if (baseKey != null && !baseKey.isEmpty()) {
                newKey = baseKey + "." + newKey;
            }
            if (entry.getValue() instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) entry.getValue();
                collectOptions(map, newKey, collector);
            } else if (entry.getValue() instanceof ConfigurationNode) {
                collectOptions(((ConfigurationNode) entry.getValue()).getRoot(), newKey, collector);
            } else {
                collector.put(newKey, (String) entry.getValue());
            }
        }

        return collector;
    }

    @Override
    public void dumpData(OutputStreamWriter writer) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setIndent(4);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml yaml = new Yaml(new SafeConstructor(), new Representer(), options);

        ConfigurationNode root = new ConfigurationNode();

        // Users setup
        for (PermissionUser user : this.manager.getUsers()) {
            // Inheritance
            if (user.getGroupsNames().length > 0) {
                root.setProperty("users." + user.getName() + ".group", Arrays.asList(user.getGroupsNames()));
            }

            // Prefix
            if (user.getOwnPrefix() != null && !user.getOwnPrefix().isEmpty()) {
                root.setProperty("users." + user.getName() + ".prefix", user.getOwnPrefix());
            }

            //Suffix
            if (user.getOwnSuffix() != null && !user.getOwnSuffix().isEmpty()) {
                root.setProperty("users." + user.getName() + ".suffix", user.getOwnSuffix());
            }

            // Permissions
            for (Map.Entry<String, String[]> entry : user.getAllPermissions().entrySet()) {
                String nodePath = "users." + user.getName();
                if (!entry.getKey().isEmpty()) {
                    nodePath += ".worlds." + entry.getKey();
                }
                nodePath += ".permissions";

                if (entry.getValue().length > 0) {
                    root.setProperty(nodePath, Arrays.asList(entry.getValue()));
                }
            }

            // Options
            for (Map.Entry<String, Map<String, String>> entry : user.getAllOptions().entrySet()) {
                String nodePath = "users." + user.getName();
                if (!entry.getKey().isEmpty()) {
                    nodePath += "worlds." + entry.getKey();
                }
                nodePath += ".options";

                if (entry.getValue().size() > 0) {
                    root.setProperty(nodePath, entry.getValue());
                }
            }
        }


        PermissionGroup defaultGroup = this.manager.getDefaultGroup();

        // Groups
        for (PermissionGroup group : this.manager.getGroups()) {
            // Inheritance
            if (group.getParentGroupsNames().length > 0) {
                root.setProperty("groups." + group.getName() + ".inheritance", Arrays.asList(group.getParentGroupsNames()));
            }


            // Prefix
            if (group.getOwnPrefix() != null && !group.getOwnPrefix().isEmpty()) {
                root.setProperty("groups." + group.getName() + ".prefix", group.getOwnPrefix());
            }

            //Suffix
            if (group.getOwnSuffix() != null && !group.getOwnSuffix().isEmpty()) {
                root.setProperty("groups." + group.getName() + ".suffix", group.getOwnSuffix());
            }

            if (group.equals(defaultGroup)) {
                root.setProperty("groups." + group.getName() + ".default", true);
            }

            // Permissions
            for (Map.Entry<String, String[]> entry : group.getAllPermissions().entrySet()) {
                String nodePath = "groups." + group.getName();
                if (!entry.getKey().isEmpty()) {
                    nodePath += ".worlds." + entry.getKey();
                }
                nodePath += ".permissions";

                if (entry.getValue().length > 0) {
                    root.setProperty(nodePath, Arrays.asList(entry.getValue()));
                }
            }

            // Options
            for (Map.Entry<String, Map<String, String>> entry : group.getAllOptions().entrySet()) {
                String nodePath = "groups." + group.getName();
                if (!entry.getKey().isEmpty()) {
                    nodePath += "worlds." + entry.getKey();
                }
                nodePath += ".options";

                if (entry.getValue().size() > 0) {
                    root.setProperty(nodePath, entry.getValue());
                }
            }
        }


        yaml.dump(root.getRoot(), writer);
    }
}
