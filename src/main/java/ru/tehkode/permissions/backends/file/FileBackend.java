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

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.tehkode.permissions.PermissionsGroupData;
import ru.tehkode.permissions.PermissionsUserData;
import ru.tehkode.permissions.backends.PermissionBackend;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * @author code
 */
public class FileBackend extends PermissionBackend {

	public final static char PATH_SEPARATOR = '/';
	public FileConfig permissions;
	public File permissionsFile;

	public FileBackend(PermissionManager manager, ConfigurationSection config) {
		super(manager, config);
		String permissionFilename = getConfig().getString("file");

		// Default settings
		if (permissionFilename == null) {
			permissionFilename = "permissions.yml";
			getConfig().set("file", "permissions.yml");
		}

		String baseDir = "plugins/PermissionsEx"; // getString("permissions.basedir", plugin.getDataFolder().getPath());

		if (baseDir.contains("\\") && !"\\".equals(File.separator)) {
			baseDir = baseDir.replace("\\", File.separator);
		}

		File baseDirectory = new File(baseDir);
		if (!baseDirectory.exists()) {
			baseDirectory.mkdirs();
		}

		this.permissionsFile = new File(baseDir, permissionFilename);
	}

	@Override
	public List<String> getWorldInheritance(String world) {
		if (world != null && !world.isEmpty()) {
			List<String> parentWorlds = this.permissions.getStringList(buildPath("worlds", world, "/inheritance"));
			if (parentWorlds != null) {
				return Collections.unmodifiableList(parentWorlds);
			}
		}

		return Collections.emptyList();
	}

	@Override
	public Map<String, List<String>> getAllWorldInheritance() {
		ConfigurationSection worldsSection = this.permissions.getConfigurationSection("worlds");
		if (worldsSection == null) {
			return Collections.emptyMap();
		}

		Map<String, List<String>> ret = new HashMap<String, List<String>>();
		for (String world : worldsSection.getKeys(false)) {
			ret.put(world, getWorldInheritance(world));
		}
		return Collections.unmodifiableMap(ret);
	}

	@Override
	public void setWorldInheritance(String world, List<String> parentWorlds) {
		if (world == null || world.isEmpty()) {
			return;
		}

		this.permissions.set(buildPath("worlds", world, "inheritance"), parentWorlds);
		this.save();
	}

	@Override
	public PermissionsUserData getUserData(String userName) {
		return new FileData("users", userName, this.permissions, "group");
	}

	@Override
	public PermissionsGroupData getGroupData(String groupName) {
		return new FileData("groups", groupName, this.permissions, "inheritance");
	}

	@Override
	public boolean hasUser(String userName) {
		return this.permissions.isConfigurationSection(buildPath("users", userName));
	}

	@Override
	public boolean hasGroup(String group) {
		return this.permissions.isConfigurationSection(buildPath("groups", group));
	}

	@Override
	public Set<String> getDefaultGroupNames(String worldName) {
		ConfigurationSection groups = this.permissions.getConfigurationSection("groups");

		if (groups == null) {
			return Collections.emptySet();
		}

		Set<String> names = new HashSet<String>();

		String defaultGroupProperty = "default";
		if (worldName != null) {
			defaultGroupProperty = buildPath("worlds", worldName, defaultGroupProperty);
		}

		for (Map.Entry<String, Object> entry : groups.getValues(false).entrySet()) {
			if (entry.getValue() instanceof ConfigurationSection) {
				ConfigurationSection groupSection = (ConfigurationSection) entry.getValue();

				if (groupSection.getBoolean(defaultGroupProperty, false)) {
					names.add(entry.getKey());
				}
			}
		}

		return Collections.unmodifiableSet(names);
	}

	@Override
	public Collection<String> getUserNames() {
		ConfigurationSection users = this.permissions.getConfigurationSection("users");
		return users != null ? users.getKeys(false) : Collections.<String>emptyList();
	}

	@Override
	public Collection<String> getGroupNames() {
		ConfigurationSection groups = this.permissions.getConfigurationSection("groups");
		return groups != null ? groups.getKeys(false) : Collections.<String>emptySet();
	}

	public static String buildPath(String... path) {
		StringBuilder builder = new StringBuilder();

		boolean first = true;
		char separator = PATH_SEPARATOR; //permissions.options().pathSeparator();

		for (String node : path) {
			if (node.isEmpty()) {
				continue;
			}

			if (!first) {
				builder.append(separator);
			}

			builder.append(node);

			first = false;
		}

		return builder.toString();
	}

	@Override
	public void reload() throws PermissionBackendException {
		FileConfig newPermissions = new FileConfig(permissionsFile);
		newPermissions.options().pathSeparator(PATH_SEPARATOR);
		try {
			newPermissions.load();
			getLogger().info("Permissions file successfully reloaded");
			this.permissions = newPermissions;
		} catch (FileNotFoundException e) {
			if (this.permissions == null) {
				// First load, load even if the file doesn't exist
				this.permissions = newPermissions;
				initNewConfiguration();
			}
		} catch (Throwable e) {
			throw new PermissionBackendException("Error loading permissions file!", e);
		}
	}

	@Override
	public void validate() throws PermissionBackendException {

	}

	/**
	 * This method is called when the file the permissions config is supposed to save to
	 * does not exist yet,This adds default permissions & stuff
	 */
	private void initNewConfiguration() throws PermissionBackendException {
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
				throw new PermissionBackendException(e);
			}
		}
	}

	public void save() {
		try {
			this.permissions.save(permissionsFile);
		} catch (IOException e) {
			getManager().getLogger().severe("Error while saving permissions file: " + e.getMessage());
		}
	}
}
