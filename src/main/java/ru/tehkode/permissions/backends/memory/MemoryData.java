package ru.tehkode.permissions.backends.memory;

import com.google.common.collect.Sets;
import ru.tehkode.permissions.PermissionsGroupData;
import ru.tehkode.permissions.PermissionsUserData;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data for in-memory permissions
 */
public class MemoryData implements PermissionsGroupData, PermissionsUserData {
	private final String name;
	private final HashMap<String, String> worldPrefix = new HashMap<>();
	private final HashMap<String, String> worldSuffix = new HashMap<>();
	private final HashMap<String, List<String>> worldsPermissions = new HashMap<>();
	private final Map<String, Map<String, String>> worldsOptions = new HashMap<>();
	private final Map<String, List<String>> parents = new HashMap<>();
	private final Map<String, Boolean> defaultVals = new HashMap<>();

	public MemoryData(String name) {
		this.name = name;
	}

	@Override
	public List<String> getParents(String worldName) {
		return parents.containsKey(worldName) ? parents.get(worldName) : Collections.<String>emptyList();
	}

	@Override
	public void setParents(List<String> parents, String worldName) {
		this.parents.put(worldName, Collections.unmodifiableList(parents));
	}

	@Override
	public boolean isDefault(String world) {
		return defaultVals.containsKey(world) ? defaultVals.get(world) : false;
	}

	@Override
	public void setDefault(boolean def, String world) {
		defaultVals.put(world, def);
	}

	@Override
	public List<String> getPermissions(String worldName) {
		return worldsPermissions.containsKey(worldName) ? worldsPermissions.get(worldName)
				: Collections.<String>emptyList();
	}

	@Override
	public void setPermissions(List<String> permissions, String worldName) {
		worldsPermissions.put(worldName, Collections.unmodifiableList(permissions));
	}

	@Override
	public Map<String, List<String>> getPermissionsMap() {
		return Collections.unmodifiableMap(worldsPermissions);
	}

	@Override
	public Set<String> getWorlds() {
		return Sets.union(worldsOptions.keySet(), worldPrefix.keySet());
	}

	@Override
	public String getPrefix(String worldName) {
		return worldPrefix.containsKey(worldName) ? worldPrefix.get(worldName) : "";
	}

	@Override
	public void setPrefix(String prefix, String worldName) {
		worldPrefix.put(worldName, prefix);
	}

	@Override
	public String getSuffix(String worldName) {
		return worldSuffix.containsKey(worldName) ? worldSuffix.get(worldName) : "";
	}

	@Override
	public void setSuffix(String suffix, String worldName) {
		worldSuffix.put(worldName, suffix);
	}

	@Override
	public String getOption(String option, String worldName) {
		if (worldsOptions.containsKey(worldName)) {
			Map<String, String> worldOption = worldsOptions.get(worldName);
			if (worldOption.containsKey(option)) {
				return worldOption.get(option);
			}
		}
		return null;
	}

	@Override
	public void setOption(String option, String worldName, String value) {
		Map<String, String> worldOptions = worldsOptions.get(worldName);
		if (worldOptions == null) {
			worldOptions = new HashMap<String, String>();
			worldsOptions.put(worldName, worldOptions);
		}
		worldOptions.put(option, value);
	}

	@Override
	public Map<String, String> getOptions(String worldName) {
		return worldsOptions.containsKey(worldName) ? worldsOptions.get(worldName)
				: Collections.<String, String>emptyMap();
	}

	@Override
	public Map<String, Map<String, String>> getOptionsMap() {
		return Collections.unmodifiableMap(worldsOptions);
	}

	@Override
	public boolean isVirtual() {
		return true;
	}

	@Override
	public void save() {

	}

	@Override
	public void remove() {

	}
}
