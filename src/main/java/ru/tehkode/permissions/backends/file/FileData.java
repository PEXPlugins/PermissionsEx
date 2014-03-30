package ru.tehkode.permissions.backends.file;

import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionsGroupData;
import ru.tehkode.permissions.PermissionsUserData;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class FileData implements PermissionsUserData, PermissionsGroupData {
	protected transient final FileConfig config;
	private  String nodePath;
	private ConfigurationSection node;
	protected boolean virtual = true;
	private final String parentPath;

	public FileData(String basePath, String name, FileConfig config, String parentPath) {
		this.config = config;
		this.node = findNode(name, basePath);
		this.parentPath = parentPath;
	}

	private ConfigurationSection findNode(String entityName, String basePath) {
		this.nodePath = FileBackend.buildPath(basePath, entityName);

		ConfigurationSection entityNode = this.config.getConfigurationSection(this.nodePath);

		if (entityNode != null) {
			this.virtual = false;
			return entityNode;
		}

		ConfigurationSection users = this.config.getConfigurationSection(basePath);

		if (users != null) {
			for (Map.Entry<String, Object> entry : users.getValues(false).entrySet()) {
				if (entry.getKey().equalsIgnoreCase(entityName)
						&& entry.getValue() instanceof ConfigurationSection) {
					this.nodePath = FileBackend.buildPath(basePath, entry.getKey());
					return (ConfigurationSection) entry.getValue();
				}
			}
		}

		// Silly workaround for empty nodes
		ConfigurationSection section = this.config.createSection(nodePath);
		this.config.set(nodePath, null);

		return section;

	}

	/**
	 * Permissions
	 */
	@Override
	public List<String> getPermissions(String worldName) {
		List<String> result = this.node.getStringList(formatPath(worldName, "permissions"));

		return result == null ? new LinkedList<String>() : result;
	}

	@Override
	public void setPermissions(List<String> permissions, String worldName) {
		this.node.set(formatPath(worldName, "permissions"), permissions.isEmpty() ? null : permissions);
		save();
	}

	@Override
	public Map<String, List<String>> getPermissionsMap() {
		Map<String, List<String>> allPermissions = new HashMap<String, List<String>>();

		// Common permissions
		List<String> commonPermissions = this.node.getStringList("permissions");
		if (commonPermissions != null) {
			allPermissions.put(null, commonPermissions);
		}

		//World-specific permissions
		ConfigurationSection worldsSection = this.node.getConfigurationSection("worlds");
		if (worldsSection != null) {
			for (String world : worldsSection.getKeys(false)) {
				List<String> worldPermissions = this.node.getStringList(FileBackend.buildPath("worlds", world, "permissions"));
				if (commonPermissions != null) {
					allPermissions.put(world, worldPermissions);
				}
			}
		}

		return allPermissions;
	}

	@Override
	public Set<String> getWorlds() {
		ConfigurationSection worldsSection = this.node.getConfigurationSection("worlds");

		if (worldsSection == null) {
			return new HashSet<String>();
		}

		return worldsSection.getKeys(false);
	}

	@Override
	public String getPrefix(String worldName) {
		return this.node.getString(formatPath(worldName, "prefix"));
	}

	@Override
	public void setPrefix(String prefix, String worldName) {
		this.node.set(formatPath(worldName, "prefix"), prefix);
		save();
	}

	@Override
	public String getSuffix(String worldName) {
		return this.node.getString(formatPath(worldName, "suffix"));
	}

	@Override
	public void setSuffix(String suffix, String worldName) {
		this.node.set(formatPath(worldName, "suffix"), suffix);
		save();
	}

	@Override
	public String getOption(String option, String worldName) {
		return this.node.getString(formatPath(worldName, "options", option));
	}

	@Override
	public void setOption(String option, String worldName, String value) {
		this.node.set(formatPath(worldName, "options", option), value);
		save();
	}

	@Override
	public Map<String, String> getOptions(String worldName) {
		ConfigurationSection optionsSection = this.node.getConfigurationSection(formatPath(worldName, "options"));

		if (optionsSection == null) {
			return new HashMap<String, String>(0);
		}

		return collectOptions(optionsSection);
	}

	@Override
	public Map<String, Map<String, String>> getOptionsMap() {
		Map<String, Map<String, String>> allOptions = new HashMap<String, Map<String, String>>();

		allOptions.put(null, this.getOptions(null));

		for (String worldName : this.getWorlds()) {
			allOptions.put(worldName, this.getOptions(worldName));
		}

		return allOptions;
	}

	@Override
	public boolean isVirtual() {
		return virtual;
	}

	@Override
	public void save() {
		this.config.set(nodePath, node);
		try {
			this.config.save();
		} catch (IOException e) {
			PermissionsEx.getPermissionManager().getLogger().log(Level.SEVERE, "Error saving data for  " + nodePath, e);
		}
	}

	@Override
	public void remove() {
		this.config.set(nodePath, null);
		this.save();
	}


	@Override
	public List<String> getParents(String worldName) {
		List<String> parents = this.node.getStringList(formatPath(worldName, parentPath));

		if (parents == null || parents.isEmpty()) {
			return Collections.emptyList();
		}

		return Collections.unmodifiableList(parents);
	}

	@Override
	public void setParents(List<String> parents, String worldName) {
		this.node.set(formatPath(worldName, parentPath), parents);
		save();
	}

	@Override
	public boolean isDefault(String world) {
		return world == null ? this.node.getBoolean("default") : this.node.getBoolean(formatPath(world, "default"));
	}

	@Override
	public void setDefault(boolean def, String world) {
		if (world == null) {
			this.node.set("default", def);
		} else {
			this.node.set(formatPath(world, "default"), def);
		}
		save();
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

	protected static String formatPath(String worldName, String node, String value) {
		String path = FileBackend.buildPath(node, value);

		if (worldName != null && !worldName.isEmpty()) {
			path = FileBackend.buildPath("worlds", worldName, path);
		}

		return path;
	}

	protected static String formatPath(String worldName, String node) {
		String path = node;

		if (worldName != null && !worldName.isEmpty()) {
			path = FileBackend.buildPath("worlds", worldName, path);
		}

		return path;
	}
}
