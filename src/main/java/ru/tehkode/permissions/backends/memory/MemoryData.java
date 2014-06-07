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
	private String name;
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
	public void load() {
	}

	@Override
	public String getIdentifier() {
		return name;
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
			worldOptions = new HashMap<>();
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

	@Override
	public Map<String, List<String>> getParentsMap() {
		return Collections.unmodifiableMap(parents);
	}

	@Override
	public boolean setIdentifier(String identifier) {
		this.name = identifier;
		return true;
	}
}
