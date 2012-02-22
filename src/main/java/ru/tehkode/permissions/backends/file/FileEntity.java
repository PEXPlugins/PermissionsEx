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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionEntity;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.backends.FileBackend;

public class FileEntity extends PermissionEntity {

	protected ConfigurationSection node;
	protected FileBackend backend;
	protected String nodePath;

	public FileEntity(String entityName, PermissionManager manager, FileBackend backend, String baseNode) {
		super(entityName, manager);

		this.backend = backend;
		this.node = this.getNode(baseNode, this.getName());
	}

	protected final ConfigurationSection getNode(String baseNode, String entityName) {
		this.nodePath = baseNode + "/" + entityName;
		ConfigurationSection entityNode = backend.permissions.getConfigurationSection(this.nodePath);

		if (entityNode != null) {
			this.virtual = false;
			return entityNode;
		}

		ConfigurationSection users = backend.permissions.getConfigurationSection(baseNode);

		if (users != null) {
			for (Map.Entry<String, Object> entry : users.getValues(false).entrySet()) {
				if (entry.getKey().equalsIgnoreCase(entityName)
						&& entry.getValue() instanceof ConfigurationSection) {
					this.setName(entry.getKey());
					this.nodePath = baseNode + "/" + this.getName();
					this.virtual = false;

					return (ConfigurationSection) entry.getValue();
				}
			}
		}

		this.virtual = true;

		return backend.permissions.createSection(nodePath);
	}

	public ConfigurationSection getConfigNode() {
		return this.node;
	}

	@Override
	public String[] getPermissions(String world) {
		String permissionsNode = "permissions";
		if (world != null && !world.isEmpty()) {
			permissionsNode = "worlds/" + world + "/" + permissionsNode;
		}

		List<String> permissions = this.node.getStringList(permissionsNode);

		if (permissions == null) {
			return new String[0];
		}

		return permissions.toArray(new String[permissions.size()]);
	}

	@Override
	public void setPermissions(String[] permissions, String world) {
		String permissionsNode = "permissions";
		if (world != null && !world.isEmpty()) {
			permissionsNode = "worlds/" + world + "/" + permissionsNode;
		}

		if (permissions.length > 0) {
			this.node.set(permissionsNode, Arrays.asList(permissions));
		} else {
			this.node.set(permissionsNode, null);
		}

		this.save();
	}

	@Override
	public String[] getWorlds() {
		ConfigurationSection worldsSection = this.node.getConfigurationSection("worlds");

		if (worldsSection == null) {
			return new String[0];
		}

		return worldsSection.getKeys(false).toArray(new String[0]);
	}

	@Override
	public Map<String, String> getOptions(String world) {

		String optionNode = (world == null) ? "options" : "worlds/" + world + "/options";
		ConfigurationSection optionsSection = this.node.getConfigurationSection(optionNode);

		if (optionsSection != null) {
			return this.collectOptions(optionsSection);
		}

		return new HashMap<String, String>(0);
	}

	@Override
	public String getOption(String option, String world, String defaultValue) {
		return this.node.getString((world == null ? "options" : "worlds/" + world + "/options") + "/" + option, defaultValue);
	}

	@Override
	public void setOption(String option, String value, String world) {
		String path = "options";
		if (world != null && !world.isEmpty()) {
			path = "worlds/" + world + "/" + path;
		}
		path += "/" + option;

		if (value != null && !value.isEmpty()) {
			this.node.set(path, value);
		} else {
			this.node.set(path, null);
		}

		this.save();
	}

	@Override
	public String getPrefix(String worldName) {
		String prefixNode = "prefix";

		if (worldName != null && !worldName.isEmpty()) {
			prefixNode = "worlds/" + worldName + "/" + prefixNode;
		}

		return this.node.getString(prefixNode);
	}

	@Override
	public String getSuffix(String worldName) {
		String suffixNode = "suffix";

		if (worldName != null && !worldName.isEmpty()) {
			suffixNode = "worlds/" + worldName + "/" + suffixNode;
		}

		return this.node.getString(suffixNode);
	}

	@Override
	public void setPrefix(String prefix, String worldName) {
		String prefixNode = "prefix";

		if (worldName != null && !worldName.isEmpty()) {
			prefixNode = "worlds/" + worldName + "/" + prefixNode;
		}

		if (prefix != null && !prefix.isEmpty()) {
			this.node.set(prefixNode, prefix);
		} else {
			this.node.set(prefixNode, null);
		}

		this.save();
	}

	@Override
	public void setSuffix(String suffix, String worldName) {
		String suffixNode = "suffix";

		if (worldName != null && !worldName.isEmpty()) {
			suffixNode = "worlds/" + worldName + "/" + suffixNode;
		}

		if (suffix != null && !suffix.isEmpty()) {
			this.node.set(suffixNode, suffix);
		} else {
			this.node.set(suffixNode, null);
		}

		this.save();
	}

	@Override
	public Map<String, String[]> getAllPermissions() {
		Map<String, String[]> allPermissions = new HashMap<String, String[]>();

		// Common permissions
		List<String> commonPermissions = this.node.getStringList("permissions");
		if (commonPermissions != null) {
			allPermissions.put(null, commonPermissions.toArray(new String[0]));
		}

		//World-specific permissions
		ConfigurationSection worldsSection = this.node.getConfigurationSection("worlds");
		if (worldsSection != null) {
			for (String world : worldsSection.getKeys(false)) {
				List<String> worldPermissions = this.node.getStringList("worlds/" + world + "/permissions");
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

		allOptions.put(null, this.getOptions(null));
		
		for (String worldName : this.getWorlds()) {
			allOptions.put(worldName, this.getOptions(worldName));
		}

		return allOptions;
	}

	private Map<String, String> collectOptions(ConfigurationSection section) {
		Map<String, String> options = new LinkedHashMap<String, String>();

		for (String key : section.getKeys(true)) {
			if (section.isConfigurationSection(key)) {
				continue;
			}

			options.put(key.replace(section.getRoot().options().pathSeparator(), '.'), section.getString(key));
		}

		return options;
	}

	@Override
	public void save() {
		this.backend.permissions.set(nodePath, node);

		this.backend.save();
	}

	@Override
	public void remove() {
		this.backend.permissions.set(nodePath, null);

		this.backend.save();
	}
}
