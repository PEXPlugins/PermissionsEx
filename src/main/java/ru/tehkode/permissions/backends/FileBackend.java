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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.tehkode.permissions.PermissionBackend;
import ru.tehkode.permissions.PermissionEntity;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.backends.file.FileEntity;
import ru.tehkode.permissions.backends.file.FileGroup;
import ru.tehkode.permissions.backends.file.FileUser;

/**
 * @author code
 */
public class FileBackend extends PermissionBackend {

	public final static char PATH_SEPARATOR = '/';
	public FileConfiguration permissions;
	public File permissionsFile;

	public FileBackend(PermissionManager manager, Configuration config) {
		super(manager, config);
	}

	@Override
	public void initialize() {
		String permissionFilename = config.getString("permissions.backends.file.file");

		// Default settings
		if (permissionFilename == null) {
			permissionFilename = "permissions.yml";
			config.set("permissions.backends.file.file", "permissions.yml");
		}

		String baseDir = config.getString("permissions.basedir", "plugins/PermissionsEx");

		if (baseDir.contains("\\") && !"\\".equals(File.separator)) {
			baseDir = baseDir.replace("\\", File.separator);
		}

		File baseDirectory = new File(baseDir);
		if (!baseDirectory.exists()) {
			baseDirectory.mkdirs();
		}

		this.permissionsFile = new File(baseDir, permissionFilename);

		this.reload();
	}

	@Override
	public String[] getWorldInheritance(String world) {
		if (world != null && !world.isEmpty()) {
			List<String> parentWorlds = this.permissions.getStringList(buildPath("worlds", world, "/inheritance"));
			if (parentWorlds != null) {
				return parentWorlds.toArray(new String[parentWorlds.size()]);
			}
		}

		return new String[0];
	}

	@Override
	public void setWorldInheritance(String world, String[] parentWorlds) {
		if (world == null || world.isEmpty()) {
			return;
		}

		this.permissions.set(buildPath("worlds", world, "inheritance"), Arrays.asList(parentWorlds));
		this.save();
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
		ConfigurationSection groups = this.permissions.getConfigurationSection("groups");

		if (groups == null) {
			throw new RuntimeException("No groups defined. Check your permissions file.");
		}

		String defaultGroupProperty = "default";
		if (worldName != null) {
			defaultGroupProperty = buildPath("worlds", worldName, defaultGroupProperty);
		}

		for (Map.Entry<String, Object> entry : groups.getValues(false).entrySet()) {
			if (entry.getValue() instanceof ConfigurationSection) {
				ConfigurationSection groupSection = (ConfigurationSection) entry.getValue();

				if (groupSection.getBoolean(defaultGroupProperty, false)) {
					return this.manager.getGroup(entry.getKey());
				}
			}
		}

		if (worldName == null) {
			throw new RuntimeException("Default user group is not defined. Please select one using the \"default: true\" property");
		}

		return null;
	}

	@Override
	public void setDefaultGroup(PermissionGroup group, String worldName) {
		ConfigurationSection groups = this.permissions.getConfigurationSection("groups");

		String defaultGroupProperty = "default";
		if (worldName != null) {
			defaultGroupProperty = buildPath("worlds", worldName, defaultGroupProperty);
		}

		for (Map.Entry<String, Object> entry : groups.getValues(false).entrySet()) {
			if (entry.getValue() instanceof ConfigurationSection) {
				ConfigurationSection groupSection = (ConfigurationSection) entry.getValue();

				groupSection.set(defaultGroupProperty, false);

				if (!groupSection.getName().equals(group.getName())) {
					groupSection.set(defaultGroupProperty, null);
				} else {
					groupSection.set(defaultGroupProperty, true);
				}
			}
		}

		this.save();
	}

	@Override
	public PermissionGroup[] getGroups() {
		List<PermissionGroup> groups = new LinkedList<PermissionGroup>();
		ConfigurationSection groupsSection = this.permissions.getConfigurationSection("groups");

		if (groupsSection == null) {
			return new PermissionGroup[0];
		}

		for (String groupName : groupsSection.getKeys(false)) {
			groups.add(this.manager.getGroup(groupName));
		}

		Collections.sort(groups);

		return groups.toArray(new PermissionGroup[0]);
	}

	@Override
	public PermissionUser[] getRegisteredUsers() {
		List<PermissionUser> users = new LinkedList<PermissionUser>();
		ConfigurationSection usersSection = this.permissions.getConfigurationSection("users");

		if (usersSection != null) {
			for (String userName : usersSection.getKeys(false)) {
				users.add(this.manager.getUser(userName));
			}
		}

		return users.toArray(new PermissionUser[users.size()]);
	}

	public static String buildPath(String... path) {
		StringBuilder builder = new StringBuilder();

		boolean first = true;
		char separator = PATH_SEPARATOR; //permissions.options().pathSeparator();

		for (String node : path) {
			if (!first) {
				builder.append(separator);
			}

			builder.append(node);

			first = false;
		}

		return builder.toString();
	}

	@Override
	public void reload() {
		FileConfiguration newPermissions = new YamlConfiguration();
		newPermissions.options().pathSeparator(PATH_SEPARATOR);
		try {

			newPermissions.load(permissionsFile);

			Logger.getLogger("Minecraft").info("Permissions file successfully reloaded");

			this.permissions = newPermissions;
		} catch (FileNotFoundException e) {
			if (this.permissions == null) {
				// First load, load even if the file doesn't exist
				this.permissions = newPermissions;
				initNewConfiguration();
			}
		} catch (Throwable e) {
			Logger.getLogger("Minecraft").warning("Error loading permissions file:\n " + e.getMessage());

			if (this.permissions == null) {
				// this is required to drop PEX plugin on first faulty initialization
				// so PEX won't load with incorrect config
				throw new IllegalStateException("Error loading permissions file", e);
			}
		}
	}

	/**
	 * This method is called when the file the permissions config is supposed to save to
	 * does not exist yet,This adds default permissions & stuff
	 */
	private void initNewConfiguration() {
		if (!permissionsFile.exists()) {
			try {
				permissionsFile.createNewFile();

				// Load default permissions
				permissions.set("groups/default/default", true);


				List<String> defaultPermissions = new LinkedList<String>();
				// Specify here default permissions
				defaultPermissions.add("modifyworld.*");

				permissions.set("groups/default/permissions", defaultPermissions);

				this.save();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public void save() {
		try {
			this.permissions.save(permissionsFile);
		} catch (IOException e) {
			Logger.getLogger("Minecraft").severe("[PermissionsEx] Error during saving permissions file: " + e.getMessage());
		}
	}

	@Override
	public void dumpData(OutputStreamWriter writer) throws IOException {
		YamlConfiguration config = new YamlConfiguration();
		config.options().pathSeparator(PATH_SEPARATOR).indent(4);

		// Groups
		for (PermissionGroup group : this.manager.getGroups()) {
			ConfigurationSection groupSection = config.createSection(buildPath("groups", group.getName()));

			// Inheritance
			if (group.getParentGroupsNames().length > 0) {
				groupSection.set("inheritance", Arrays.asList(group.getParentGroupsNames()));
			}

			dumpEntityData(group, groupSection);

			// world-specific inheritance
			for (Map.Entry<String, PermissionGroup[]> entry : group.getAllParentGroups().entrySet()) {
				if (entry.getKey() == null) continue;

				List<String> groups = new ArrayList<String>();
				for (PermissionGroup parentGroup : entry.getValue()) {
					if (parentGroup == null) {
						continue;
					}

					groups.add(parentGroup.getName());
				}

				if (groups.isEmpty()) continue;

				groupSection.set(buildPath("worlds", entry.getKey(), "inheritance"), groups);
			}

			// world specific stuff
			for (String worldName : group.getWorlds()) {
				if (worldName == null) continue;

				String worldPath = buildPath("worlds", worldName);
				// world-specific prefix
				String prefix = group.getOwnPrefix(worldName);
				if (prefix != null && !prefix.isEmpty()) {
					groupSection.set(buildPath(worldPath, "prefix"), prefix);
				}

				String suffix = group.getOwnSuffix(worldName);
				if (suffix != null && !suffix.isEmpty()) {
					groupSection.set(buildPath(worldPath, "suffix"), suffix);
				}

				if (group.isDefault(worldName)) {
					groupSection.set(buildPath(worldPath, "suffix"), true);
				}
			}

			if (group.isDefault(null)) {
				groupSection.set("default", true);
			}
		}

		// World inheritance
		for (World world : Bukkit.getServer().getWorlds()) {
			String[] parentWorlds = manager.getWorldInheritance(world.getName());
			if (parentWorlds.length == 0) {
				continue;
			}

			config.set(buildPath("worlds", world.getName(), "inheritance"), Arrays.asList(parentWorlds));
		}

		// Users setup
		for (PermissionUser user : this.manager.getUsers()) {
			ConfigurationSection userSection = config.createSection(buildPath("users", user.getName()));

			// Inheritance
			if (user.getGroupsNames().length > 0) {
				userSection.set("group", Arrays.asList(user.getGroupsNames()));
			}

			// Prefix
			if (user.getOwnPrefix() != null && !user.getOwnPrefix().isEmpty()) {
				userSection.set("prefix", user.getOwnPrefix());
			}

			//Suffix
			if (user.getOwnSuffix() != null && !user.getOwnSuffix().isEmpty()) {
				userSection.set("suffix", user.getOwnSuffix());
			}

			dumpEntityData(user, userSection);

			// world-specific inheritance
			for (Map.Entry<String, PermissionGroup[]> entry : user.getAllGroups().entrySet()) {
				if (entry.getKey() == null) continue;

				List<String> groups = new ArrayList<String>();
				for (PermissionGroup group : entry.getValue()) {
					if (group == null) {
						continue;
					}

					groups.add(group.getName());
				}

				if (groups.isEmpty()) continue;

				userSection.set(buildPath("worlds", entry.getKey(), "group"), groups);
			}

			// world specific prefix & suffix
			for (String worldName : user.getWorlds()) {
				if (worldName == null) continue;

				String worldPath = buildPath("worlds", worldName);
				// world-specific prefix
				String prefix = user.getOwnPrefix(worldName);
				if (prefix != null && !prefix.isEmpty()) {
					userSection.set(buildPath(worldPath, "prefix"), prefix);
				}

				String suffix = user.getOwnSuffix(worldName);
				if (suffix != null && !suffix.isEmpty()) {
					userSection.set(buildPath(worldPath, "suffix"), suffix);
				}
			}
		}

		// Write data
		writer.write(config.saveToString());
		writer.flush();
	}

	// Some of the methods are common in PermissionEntity. Sadly not very many of them.
	private void dumpEntityData(PermissionEntity entity, ConfigurationSection section) {

		// Permissions
		for (Map.Entry<String, String[]> entry : entity.getAllPermissions().entrySet()) {
			if (entry.getValue().length == 0) continue;

			String nodePath = "permissions";
			if (entry.getKey() != null && !entry.getKey().isEmpty()) {
				nodePath = buildPath("worlds", entry.getKey(), nodePath);
			}

			section.set(nodePath, Arrays.asList(entry.getValue()));
		}

		// Options
		for (Map.Entry<String, Map<String, String>> entry : entity.getAllOptions().entrySet()) {
			if (entry.getValue().isEmpty()) continue;

			String nodePath = "options";
			if (entry.getKey() != null && !entry.getKey().isEmpty()) {
				nodePath = buildPath("worlds", entry.getKey(), nodePath);
			}

			section.set(nodePath, entry.getValue());
		}
	}
}
