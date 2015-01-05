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
	private ConfigurationSection node;
	protected boolean virtual = true;
	private final String parentPath;

	public FileData(ConfigurationSection node, String parentPath) {
		this.config = (FileConfig) node.getRoot();
		this.node = node;
		this.virtual = node.getParent().getConfigurationSection(node.getName()) == null;
		this.parentPath = parentPath;
	}

	@Override
	public String getIdentifier() {
		return node.getName();
	}

	@Override
	public boolean setIdentifier(String identifier) {
		String caseCorrectedIdentifier = config.isLowerCased(node.getParent().getCurrentPath()) ? identifier.toLowerCase() : identifier;
		ConfigurationSection existing = node.getParent().getConfigurationSection(identifier);
		if (existing == null && config.isLowerCased(node.getParent().getCurrentPath())) {
			ConfigurationSection users = node.getParent();

			if (users != null) {
				for (Map.Entry<String, Object> entry : users.getValues(false).entrySet()) {
					if (entry.getKey().equalsIgnoreCase(caseCorrectedIdentifier)
							&& entry.getValue() instanceof ConfigurationSection) {
						existing = (ConfigurationSection) entry.getValue();
					}
				}
			}
		}

		if (existing != null) {
			return false;
		}
		ConfigurationSection oldNode = node;
		this.node = oldNode.getParent().createSection(caseCorrectedIdentifier, node.getValues(false));
		oldNode.getParent().set(oldNode.getName(), null);
		if (this.isVirtual()) {
			node.getParent().set(node.getName(), null);
		}
		return true;
	}

	/**
	 * Permissions
	 */
	@Override
	public List<String> getPermissions(String worldName) {
		List<String> result = this.node.getStringList(formatPath(worldName, "permissions"));

		return result == null ? Collections.<String>emptyList() : Collections.unmodifiableList(result);
	}

	@Override
	public void setPermissions(List<String> permissions, String worldName) {
		this.node.set(formatPath(worldName, "permissions"), permissions == null ? null : new ArrayList<>(permissions));
	}

	@Override
	public Map<String, List<String>> getPermissionsMap() {
		Map<String, List<String>> allPermissions = new HashMap<>();

		// Common permissions
		List<String> commonPermissions = this.node.getStringList("permissions");
		if (commonPermissions != null) {
			allPermissions.put(null, Collections.unmodifiableList(commonPermissions));
		}

		//World-specific permissions
		ConfigurationSection worldsSection = this.node.getConfigurationSection("worlds");
		if (worldsSection != null) {
			for (String world : worldsSection.getKeys(false)) {
				List<String> worldPermissions = this.node.getStringList(FileBackend.buildPath("worlds", world, "permissions"));
				if (commonPermissions != null) {
					allPermissions.put(world, Collections.unmodifiableList(worldPermissions));
				}
			}
		}

		return Collections.unmodifiableMap(allPermissions);
	}

	@Override
	public Set<String> getWorlds() {
		ConfigurationSection worldsSection = this.node.getConfigurationSection("worlds");

		if (worldsSection == null) {
			return Collections.emptySet();
		}

		return Collections.unmodifiableSet(worldsSection.getKeys(false));
	}

	@Override
	public String getOption(String option, String worldName) {
		return this.node.getString(formatPath(worldName, "options", option));
	}

	@Override
	public void setOption(String option, String value, String worldName) {
		this.node.set(formatPath(worldName, "options", option), value);
	}

	@Override
	public Map<String, String> getOptions(String worldName) {
		ConfigurationSection optionsSection = this.node.getConfigurationSection(formatPath(worldName, "options"));

		if (optionsSection == null) {
			return Collections.emptyMap();
		}

		return Collections.unmodifiableMap(collectOptions(optionsSection));

	}

	@Override
	public Map<String, Map<String, String>> getOptionsMap() {
		Map<String, Map<String, String>> allOptions = new HashMap<>();

		allOptions.put(null, this.getOptions(null));

		for (String worldName : this.getWorlds()) {
			allOptions.put(worldName, this.getOptions(worldName));
		}

		return Collections.unmodifiableMap(allOptions);
	}

	@Override
	public boolean isVirtual() {
		return virtual;
	}

	@Override
	public void save() {
		if (isVirtual()) {
			this.node.getParent().set(node.getName(), node);
			virtual = false;
		}

		try {
			this.config.save();
		} catch (IOException e) {
			PermissionsEx.getPermissionManager().getLogger().log(Level.SEVERE, "Error saving data for  " + node.getCurrentPath(), e);
		}
	}

	@Override
	public void remove() {
		node.getParent().set(node.getName(), null);
		this.virtual = true;
	}

	@Override
	public Map<String, List<String>> getParentsMap() {
		Map<String, List<String>> ret = new HashMap<>();
		ret.put(null, getParents(null));

		for (String world : getWorlds()) {
			ret.put(world, getParents(world));
		}
		return Collections.unmodifiableMap(ret);
	}


	@Override
	public List<String> getParents(String worldName) {
		List<String> parents = this.node.getStringList(formatPath(worldName, parentPath));
		for (Iterator<String> it = parents.iterator(); it.hasNext(); ) {
			final String test = it.next();
			if (test == null || test.isEmpty()) {
				it.remove();
			}
		}

		if (parents == null || parents.isEmpty()) {
			return Collections.emptyList();
		}

		return Collections.unmodifiableList(parents);
	}

	@Override
	public void setParents(List<String> parents, String worldName) {
		this.node.set(formatPath(worldName, parentPath), parents == null ? null : new ArrayList<>(parents));
	}

	@Override
	public void load() {
		// Already loaded bc file
	}

	private Map<String, String> collectOptions(ConfigurationSection section) {
		Map<String, String> options = new LinkedHashMap<>();
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
