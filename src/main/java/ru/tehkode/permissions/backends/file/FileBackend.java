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

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.config.ConfigurationFactory;
import ru.tehkode.permissions.PermissionBackend;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.backends.GroupDataProvider;
import ru.tehkode.permissions.backends.UserDataProvider;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.config.ConfigurationNode;
import ru.tehkode.permissions.backends.file.data.FileUserDataProvider;
import ru.tehkode.permissions.backends.file.data.FileGroupDataProvider;
import org.apache.commons.configuration.AbstractHierarchicalFileConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import ru.tehkode.utils.Debug;

/**
 *
 * @author t3hk0d3
 */
public class FileBackend extends PermissionBackend {

	protected Map<String, FileGroupDataProvider> groups = new HashMap<String, FileGroupDataProvider>();
	protected Map<String, FileUserDataProvider> users = new HashMap<String, FileUserDataProvider>();
	protected Map<String, List<String>> worldInheritanceMap = new HashMap<String, List<String>>();
	protected Map<String, String> defaultGroups = new HashMap<String, String>();
	// File containing permissions
	protected Map<File, HierarchicalConfiguration> permissionFiles = new HashMap<File, HierarchicalConfiguration>();
	// Configuration file where PEX would save new players
	protected HierarchicalConfiguration mainUserConfig = null;
	// Configuration file where PEX would save new groups
	protected HierarchicalConfiguration mainGroupConfig = null;

	public FileBackend(PermissionManager manager, ConfigurationSection config) {
		super(manager, config);
	}

	@Override
	public void initialize() {
		File baseDirectory = PermissionsEx.getPlugin().getDataFolder();
		if (!baseDirectory.exists()) {
			baseDirectory.mkdirs();
		}

		if (this.config.isList("file.files")) {
			this.loadPermissions(baseDirectory, this.config.getStringList("file.files"));
		}

		if (this.config.isString("file.file")) {
			this.loadPermissions(new File(baseDirectory, this.config.getString("file.file", "permissions.yml")));
		}

		if (this.permissionFiles.isEmpty()) { // deploy default file
			Logger.getLogger("Minecraft").warning("[PermissionsEx] Valid permissions files not found! Deploying default one.");
			PermissionsEx.getPlugin().saveResource("permissions.yml", false);
			this.loadPermissions(new File(baseDirectory, "permissions.yml"));
		}

		if (this.permissionFiles.size() > 0) {
			if (this.mainUserConfig == null) {
				this.mainUserConfig = findConfiguration();
			}

			if (this.mainGroupConfig == null) {
				this.mainGroupConfig = findConfiguration();
			}
		}

		assert this.mainUserConfig != null && this.mainGroupConfig != null;
	}

	protected HierarchicalConfiguration findConfiguration() {
		// I know, this is looking silly
		for (HierarchicalConfiguration fileConfig : this.permissionFiles.values()) {
			return fileConfig;
		}
		return null;
	}

	protected void loadPermissions(File file) {
		// add file type detection
		try {
            Debug.print("LOADING %1", file);
            HierarchicalConfiguration fileConfig = ConfigurationFactory.loadFile(file);
			
			this.loadUsers(fileConfig);
			this.loadGroups(fileConfig);
			this.loadWorlds(fileConfig);
			
			this.permissionFiles.put(file, fileConfig);
		} catch (Throwable e) {
			Logger.getLogger("Minecraft").warning("[PermissionsEx] Error during loading " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
		}
	}

	protected void loadPermissions(File baseDirectory, List<String> files) {
		for (String fileName : files) {
			this.loadPermissions(new File(baseDirectory, fileName));
		}
	}

	protected void loadUsers(HierarchicalConfiguration fileConfig) {
        for (Object obj : fileConfig.getList("users.user")) {
            Debug.print("USER: %1", obj);
        }
        /*
		if (!fileConfig.isConfigurationSection("users")) { // this file don't have user information
			return;
		}

		if (this.mainUserConfig == null) {
			this.mainUserConfig = fileConfig;
		}

		for (Map.Entry<String, Object> entry : fileConfig.getConfigurationSection("users").getValues(false).entrySet()) {
			if (entry.getValue() instanceof ConfigurationSection) {
				Debug.print("LOADING USER %1", entry.getKey());
				this.users.put(entry.getKey().toLowerCase(), new FileUserDataProvider(this, (ConfigurationSection) entry.getValue()));
			}
		}
		*/
	}

	protected void loadGroups(HierarchicalConfiguration fileConfig) {
        for (Object obj : fileConfig.getList("groups.group")) {
            Debug.print("GROUP: %1", obj);
        }
        /*
		if (!fileConfig.isConfigurationSection("groups")) { // this file doesn't have group information
			return;
		}

		if (this.mainGroupConfig == null) {
			this.mainGroupConfig = fileConfig;
		}

		for (Map.Entry<String, Object> entry : fileConfig.getConfigurationSection("groups").getValues(false).entrySet()) {
			if (entry.getValue() instanceof ConfigurationSection) {
				Debug.print("LOADING GROUP %1", entry.getKey());
				ConfigurationSection groupSection = (ConfigurationSection) entry.getValue();
				FileGroupDataProvider data = new FileGroupDataProvider(this, groupSection);

				this.groups.put(entry.getKey().toLowerCase(), data);

				// default group
				if (groupSection.getBoolean("default", false)) { // default group
					this.setupDefaultGroup(entry.getKey().toLowerCase(), null);
				}

				// world specific default group
				if (groupSection.isConfigurationSection("worlds")) {
					ConfigurationSection worlds = groupSection.getConfigurationSection("worlds");

					for (String world : worlds.getKeys(false)) {
						if (worlds.isConfigurationSection(world) && worlds.getConfigurationSection(world).getBoolean("default", false)) {
							this.setupDefaultGroup(entry.getKey(), world);
						}
					}
				}
			}
		}
		*/
	}

	protected void setupDefaultGroup(String groupName, String worldName) {
		if (groupName == null || groupName.isEmpty()) {
			return;
		}

		if (this.defaultGroups.containsKey(worldName)) {
			Logger.getLogger("Minecraft").warning("[PermissionsEx] Several default groups for one world is not allowed!");
			return;
		}

		this.defaultGroups.put(worldName, groupName);
	}

	protected void loadWorlds(HierarchicalConfiguration fileConfig) {
        for (Object obj : fileConfig.getList("worlds.world")) {
            Debug.print("WORLD: %1", obj);
        }

        /*
		for (Map.Entry<String, Object> entry : fileConfig.getConfigurationSection("worlds").getValues(false).entrySet()) {
			if (entry.getValue() instanceof ConfigurationSection) {
				Debug.print("LOADING WORLD %1", entry.getKey());
				ConfigurationSection worldNode = ((ConfigurationSection) entry.getValue());

				if (worldNode.isList("inheritance")) {
					this.worldInheritanceMap.put(entry.getKey(), worldNode.getStringList("inheritance"));
				}

				if (worldNode.isString("defaultGroup")) {
					this.setupDefaultGroup(worldNode.getString("defaultGroup"), entry.getKey());
				}
			}
		}
		*/
	}

	@Override
	public List<String> getWorldInheritance(String worldName) {
		if (this.worldInheritanceMap.containsKey(worldName)) {
			return this.worldInheritanceMap.get(worldName);
		}

		return new ArrayList<String>();
	}

	@Override
	public void setWorldInheritance(String world, List<String> parentWorlds) {
		if (world == null && world.isEmpty()) {
			return;
		}

		this.worldInheritanceMap.put(world, parentWorlds);

		this.saveWorldInheritance();
		this.save();
	}

	protected void saveWorldInheritance() {
        HierarchicalConfiguration worldConfig = null;
        /*
		for (HierarchicalConfiguration fileConfig : this.permissionFiles.values()) {
			if (fileConfig.isConfigurationSection("worlds")) {
				if (worldConfig != null) {
					fileConfig.set("worlds", null);
					continue;
				}

				worldConfig = fileConfig;
			}
		}
		*/

		//worldConfig.set("worlds", this.worldInheritanceMap);
	}

	@Override
	public String getDefaultGroup(String worldName) {
		return this.defaultGroups.get(worldName);
	}

	@Override
	public void setDefaultGroup(String groupName, String worldName) {
		this.defaultGroups.put(worldName, groupName);

		this.save();
	}

	@Override
	public Set<String> getGroups() {
		return this.groups.keySet();
	}

	@Override
	public Set<String> getRegisteredUsers() {
		return this.users.keySet();
	}

	public void save() {
		try {
			for (Map.Entry<File, HierarchicalConfiguration> entry : this.permissionFiles.entrySet()) {
                HierarchicalConfiguration config = entry.getValue();

                if (config instanceof AbstractHierarchicalFileConfiguration) { // should be just FileConfiguration
                    ((AbstractHierarchicalFileConfiguration)config).save(entry.getKey());
                }
			}
		} catch (Throwable e) {
			Logger.getLogger("Minecraft").severe("[PermissionsEx] Error during saving permissions: " + e.getMessage());
		}
	}

	@Override
	public void reload() {
		this.groups.clear();
		this.users.clear();
		this.defaultGroups.clear();
		this.worldInheritanceMap.clear();

		for (File permissionsFile : this.permissionFiles.keySet()) {
			this.loadPermissions(permissionsFile);
		}
	}

	@Override
	public GroupDataProvider getGroupDataProvider(String groupName) {
		FileGroupDataProvider groupData = this.groups.get(groupName.toLowerCase());
		Debug.print("Group request for %1", groupName);
		

		if (groupData == null) {
			Debug.print("CREATING NEW FGDP");
			groupData = new FileGroupDataProvider(this, null);
			this.groups.put(groupName.toLowerCase(), groupData);
		}

		return groupData;
	}

	@Override
	public UserDataProvider getUserDataProvider(String userName) {
		FileUserDataProvider userData = this.users.get(userName.toLowerCase());
		Debug.print("User request for %1", userName);

		if (userData == null) { // new user
			Debug.print("CREATING NEW FUDP");
			userData = new FileUserDataProvider(this, null);
			this.users.put(userName.toLowerCase(), userData);
		}

		return userData;
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
        /*
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
				if (entry.getValue().length == 0) {
					continue;
				}

				String nodePath = "permissions";
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					nodePath = "worlds.`" + entry.getKey() + "`." + nodePath;
				}

				userNode.setProperty(nodePath, Arrays.asList(entry.getValue()));
			}

			// Options
			for (Map.Entry<String, Map<String, String>> entry : user.getAllOptions().entrySet()) {
				if (entry.getValue().isEmpty()) {
					continue;
				}

				String nodePath = "options";
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					nodePath = "worlds.`" + entry.getKey() + "`." + nodePath;
				}

				userNode.setProperty(nodePath, entry.getValue());
			}

			// world-specific inheritance
			for (Map.Entry<String, PermissionGroup[]> entry : user.getAllGroups().entrySet()) {
				if (entry.getKey() == null) {
					continue;
				}

				List<String> groups = new ArrayList<String>();
				for (PermissionGroup group : entry.getValue()) {
					if (group == null) {
						continue;
					}

					groups.add(group.getName());
				}

				if (groups.isEmpty()) {
					continue;
				}

				userNode.setProperty("worlds.`" + entry.getKey() + "`.group", groups);
			}

			// world specific stuff
			for (String worldName : user.getWorlds()) {
				if (worldName == null) {
					continue;
				}

				String worldPath = "worlds.`" + worldName + "`.";
				// world-specific prefix
				String prefix = user.getOwnPrefix(worldName);
				if (prefix != null && !prefix.isEmpty()) {
					userNode.setProperty(worldPath + "prefix", prefix);
				}

				String suffix = user.getOwnSuffix(worldName);
				if (suffix != null && !suffix.isEmpty()) {
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
				if (entry.getValue().length == 0) {
					continue;
				}

				String nodePath = "permissions";
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					nodePath = "worlds.`" + entry.getKey() + "`." + nodePath;
				}

				groupNode.setProperty(nodePath, Arrays.asList(entry.getValue()));
			}

			// Options
			for (Map.Entry<String, Map<String, String>> entry : group.getAllOptions().entrySet()) {
				if (entry.getValue().isEmpty()) {
					continue;
				}

				String nodePath = "options";
				if (entry.getKey() != null && !entry.getKey().isEmpty()) {
					nodePath = "worlds.`" + entry.getKey() + "`." + nodePath;
				}

				groupNode.setProperty(nodePath, entry.getValue());
			}

			// world-specific inheritance
			for (Map.Entry<String, PermissionGroup[]> entry : group.getAllParentGroups().entrySet()) {
				if (entry.getKey() == null) {
					continue;
				}

				List<String> groups = new ArrayList<String>();
				for (PermissionGroup parentGroup : entry.getValue()) {
					if (parentGroup == null) {
						continue;
					}

					groups.add(parentGroup.getName());
				}

				if (groups.isEmpty()) {
					continue;
				}

				groupNode.setProperty("worlds.`" + entry.getKey() + "`.inheritance", groups);
			}

			// world specific stuff
			for (String worldName : group.getWorlds()) {
				if (worldName == null) {
					continue;
				}

				String worldPath = "worlds.`" + worldName + "`.";
				// world-specific prefix
				String prefix = group.getOwnPrefix(worldName);
				if (prefix != null && !prefix.isEmpty()) {
					groupNode.setProperty(worldPath + "prefix", prefix);
				}

				String suffix = group.getOwnSuffix(worldName);
				if (suffix != null && !suffix.isEmpty()) {
					groupNode.setProperty(worldPath + "suffix", suffix);
				}

				if (group.isDefault(worldName)) {
					groupNode.setProperty(worldPath + "default", true);
				}
			}

			if (group.isDefault(null)) {
				groupNode.setProperty("default", true);
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
		*/
	}
}
