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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.World;
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
import ru.tehkode.permissions.backends.file.FileGroup;
import ru.tehkode.permissions.backends.file.FileUser;

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

        String baseDir = config.getString("permissions.basedir",  "plugins/PermissionsEx");

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
            return this.permissions.getStringList("worlds.`" + world + "`.inheritance", new LinkedList<String>()).toArray(new String[0]);
        }

        return new String[0];
    }

    @Override
    public void setWorldInheritance(String world, String[] parentWorlds) {
        if (world == null && world.isEmpty()) {
            return;
        }

        this.permissions.setProperty("worlds.`" + world + "`.inheritance", Arrays.asList(parentWorlds));
        this.permissions.save();
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
    public PermissionGroup getDefaultGroup(String worldName) {
        Map<String, ConfigurationNode> groupsMap = this.permissions.getNodesMap("groups");

        if (groupsMap == null || groupsMap.isEmpty()) {
            throw new RuntimeException("No groups defined. Check your permissions file.");
        }

        String defaultGroupProperty = "default";
        if (worldName != null) {
            defaultGroupProperty = "worlds.`" + worldName + "`." + defaultGroupProperty;
        }

        for (Map.Entry<String, ConfigurationNode> entry : groupsMap.entrySet()) {
            Object property = entry.getValue().getProperty(defaultGroupProperty);
            if(property instanceof Boolean && ((Boolean)property)){
                return this.manager.getGroup(entry.getKey());
            }
        }

        if (worldName == null) {
            throw new RuntimeException("Default user group is not defined. Please select one using the \"default: true\" property");
        }

        return null;
    }

    @Override
    public void setDefaultGroup(PermissionGroup group, String worldName) {
        Map<String, ConfigurationNode> groupsMap = this.permissions.getNodesMap("groups");

        String defaultGroupProperty = "default";
        if (worldName != null) {
            defaultGroupProperty = "worlds.`" + worldName + "`." + defaultGroupProperty;
        }

        for (Map.Entry<String, ConfigurationNode> entry : groupsMap.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase(group.getName())
                    && entry.getValue().getProperty(defaultGroupProperty) != null) {
                entry.getValue().removeProperty(defaultGroupProperty);
            }

            if (entry.getKey().equalsIgnoreCase(group.getName())) {
                entry.getValue().setProperty(defaultGroupProperty, true);
            }
        }
        
        this.permissions.save();
    }

    @Override
    public PermissionGroup[] getGroups() {
        List<PermissionGroup> groups = new LinkedList<PermissionGroup>();
        Map<String, ConfigurationNode> groupsMap = this.permissions.getNodesMap("groups");

        for (String groupName : groupsMap.keySet()) {
            groups.add(this.manager.getGroup(groupName));
        }

        Collections.sort(groups);

        return groups.toArray(new PermissionGroup[0]);
    }

    @Override
    public PermissionUser[] getRegisteredUsers() {
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
			ConfigurationNode userNode = new ConfigurationNode();
            // Inheritance
            if (user.getGroupsNames().length > 0) {
                userNode.setProperty("group", Arrays.asList(user.getGroupsNames()));
            }

            // Prefix
            if (user.getOwnPrefix() != null && !user.getOwnPrefix().isEmpty()) {
                userNode.setProperty("prefix", user.getOwnPrefix());
            }

            //Suffix
            if (user.getOwnSuffix() != null && !user.getOwnSuffix().isEmpty()) {
                userNode.setProperty("suffix", user.getOwnSuffix());
            }
			
            // Permissions
            for (Map.Entry<String, String[]> entry : user.getAllPermissions().entrySet()) {
				if (entry.getValue().length == 0) continue;
				
                String nodePath = "permissions";
                if (entry.getKey() != null && !entry.getKey().isEmpty()) {
                    nodePath = "worlds.`" + entry.getKey() + "`." + nodePath;
                }

                userNode.setProperty(nodePath, Arrays.asList(entry.getValue()));
            }

            // Options
            for (Map.Entry<String, Map<String, String>> entry : user.getAllOptions().entrySet()) {
				if(entry.getValue().isEmpty()) continue;
				
                String nodePath = "options";
                if (entry.getKey() != null && !entry.getKey().isEmpty()) {
                    nodePath = "worlds.`" + entry.getKey() + "`." + nodePath;
                }

                userNode.setProperty(nodePath, entry.getValue());
            }
			
			// world-specific inheritance
			for(Map.Entry<String, PermissionGroup[]> entry : user.getAllGroups().entrySet()){		
				if(entry.getKey() == null) continue;
				
				List<String> groups = new ArrayList<String>();
				for(PermissionGroup group : entry.getValue()){
					if(group == null){ continue; }
					
					groups.add(group.getName());
				}
				
				if(groups.isEmpty()) continue;
								
				userNode.setProperty("worlds.`" + entry.getKey() + "`.group", groups);
			}
			
			// world specific stuff
			for (String worldName : user.getWorlds()){
				if(worldName == null) continue;
				
				String worldPath = "worlds.`" + worldName + "`.";
				// world-specific prefix
				String prefix = user.getOwnPrefix(worldName);
				if(prefix != null && !prefix.isEmpty()){
					userNode.setProperty(worldPath + "prefix", prefix);
				}
				
				String suffix = user.getOwnSuffix(worldName);
				if(suffix != null && !suffix.isEmpty()){
					userNode.setProperty(worldPath + "suffix", suffix);
				}
			}
			
			root.setProperty("users.`" + user.getName() + "`", userNode);
        }

		
        // Groups
        for (PermissionGroup group : this.manager.getGroups()) {
			ConfigurationNode groupNode = new ConfigurationNode();
			
            // Inheritance
            if (group.getParentGroupsNames().length > 0) {
                groupNode.setProperty("inheritance", Arrays.asList(group.getParentGroupsNames()));
            }

            // Prefix
            if (group.getOwnPrefix() != null && !group.getOwnPrefix().isEmpty()) {
                groupNode.setProperty("prefix", group.getOwnPrefix());
            }

            //Suffix
            if (group.getOwnSuffix() != null && !group.getOwnSuffix().isEmpty()) {
                groupNode.setProperty("suffix", group.getOwnSuffix());
            }
			
            // Permissions
            for (Map.Entry<String, String[]> entry : group.getAllPermissions().entrySet()) {
				if (entry.getValue().length == 0) continue;
				
                String nodePath = "permissions";
                if (entry.getKey() != null && !entry.getKey().isEmpty()) {
                    nodePath = "worlds.`" + entry.getKey() + "`." + nodePath;
                }

                groupNode.setProperty(nodePath, Arrays.asList(entry.getValue()));
            }

            // Options
            for (Map.Entry<String, Map<String, String>> entry : group.getAllOptions().entrySet()) {
				if(entry.getValue().isEmpty()) continue;
				
                String nodePath = "options";
                if (entry.getKey() != null && !entry.getKey().isEmpty()) {
                    nodePath = "worlds.`" + entry.getKey() + "`." + nodePath;
                }

                groupNode.setProperty(nodePath, entry.getValue());
            }
			
			// world-specific inheritance
			for(Map.Entry<String, PermissionGroup[]> entry : group.getAllParentGroups().entrySet()){
				if(entry.getKey() == null) continue;
				
				List<String> groups = new ArrayList<String>();
				for(PermissionGroup parentGroup : entry.getValue()){
					if(parentGroup == null){ continue; }
					
					groups.add(parentGroup.getName());
				}
				
				if(groups.isEmpty()) continue;
				
				groupNode.setProperty("worlds.`" + entry.getKey() + "`.inheritance", groups);
			}
			
			// world specific stuff
			for (String worldName : group.getWorlds()){
				if(worldName == null) continue;
				
				String worldPath = "worlds.`" + worldName + "`.";
				// world-specific prefix
				String prefix = group.getOwnPrefix(worldName);
				if(prefix != null && !prefix.isEmpty()){
					groupNode.setProperty(worldPath + "prefix", prefix);
				}
				
				String suffix = group.getOwnSuffix(worldName);
				if(suffix != null && !suffix.isEmpty()){
					groupNode.setProperty(worldPath + "suffix", suffix);
				}
			}
			
			root.setProperty("groups.`" + group.getName() + "`", groupNode);
        }

        // World inheritance
        for (World world : Bukkit.getServer().getWorlds()) {
            String[] parentWorlds = manager.getWorldInheritance(world.getName());
            if (parentWorlds.length == 0) {
                continue;
            }

            root.setProperty("worlds.`" + world.getName() + "`.inheritance", Arrays.asList(parentWorlds));
        }

        // Write data to writer
        yaml.dump(root.getRoot(), writer);
    }
}
