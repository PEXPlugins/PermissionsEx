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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.tehkode.permissions.PermissionBackend;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.backends.file.FileGroup;
import ru.tehkode.permissions.backends.file.FileUser;

/**
 *
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

		this.permissions.set(buildPath("worlds", world,  "inheritance"), Arrays.asList(parentWorlds));
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
		permissions = new YamlConfiguration();
		permissions.options().pathSeparator(PATH_SEPARATOR);
				
		try {
			permissions.load(permissionsFile);
		} catch (FileNotFoundException e) {
			// do nothing
		} catch (Throwable e) {
			throw new IllegalStateException("Error loading permissions file", e);
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
		throw new UnsupportedOperationException("Sorry, data dumping is broken!");
	}
}
