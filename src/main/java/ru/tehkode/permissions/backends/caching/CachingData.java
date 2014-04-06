package ru.tehkode.permissions.backends.caching;

import ru.tehkode.permissions.PermissionsData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Data backend implementing a simple cache
 */
public abstract class CachingData implements PermissionsData {
	private static final String MAP_NULL = "\uDEAD\uBEEFNULLVAL";
	protected final Executor executor;
	private final Object worldsLock = new Object();
	private Map<String, List<String>> permissions;
	private Map<String, Map<String, String>> options;
	private Map<String, List<String>> parents;
	private Map<String, String> prefixMap = new ConcurrentHashMap<>(), suffixMap = new ConcurrentHashMap<>();
	private Set<String> worlds;

	public CachingData(Executor executor) {
		this.executor = executor;
	}

	protected static String serializeNull(String obj) {
		return obj == null ? MAP_NULL : obj;
	}

	protected static String deserializeNull(String obj) {
		return obj == MAP_NULL ? null : obj;
	}

	protected abstract PermissionsData getBackingData();

	protected void loadPermissions() {
		this.permissions = new HashMap<>(getBackingData().getPermissionsMap());
	}

	protected void loadOptions() {
		this.options = new HashMap<>();
		for (Map.Entry<String, Map<String, String>> e : getBackingData().getOptionsMap().entrySet()) {
			this.options.put(e.getKey(), new HashMap<>(e.getValue()));
		}
	}

	protected void loadInheritance() {
		this.parents = new HashMap<>(getBackingData().getParentsMap());
	}

	protected void clearCache() {
		permissions = null;
		options = null;
		parents = null;
		prefixMap.clear();
		suffixMap.clear();
		clearWorldsCache();
	}

	@Override
	public String getIdentifier() {
		return getBackingData().getIdentifier();
	}

	@Override
	public List<String> getPermissions(String worldName) {
		if (permissions == null) {
			loadPermissions();
		}
		List<String> ret = permissions.get(worldName);
		return ret == null ? Collections.<String>emptyList() : Collections.unmodifiableList(ret);
	}

	@Override
	public void setPermissions(List<String> permissions, final String worldName) {
		final List<String> safePermissions = new ArrayList<>(permissions);
		executor.execute(new Runnable() {
			@Override
			public void run() {
				clearWorldsCache();
				getBackingData().setPermissions(safePermissions, worldName);
			}
		});
		if (this.permissions != null) {
			this.permissions.put(worldName, safePermissions);
		}
	}

	@Override
	public Map<String, List<String>> getPermissionsMap() {
		if (permissions == null) {
			loadPermissions();
		}

		Map<String, List<String>> ret = new HashMap<>();
		for (Map.Entry<String, List<String>> e : permissions.entrySet()) {
			ret.put(e.getKey(), Collections.unmodifiableList(e.getValue()));
		}
		return Collections.unmodifiableMap(ret);
	}

	@Override
	public Set<String> getWorlds() {
		synchronized (worldsLock) {
			if (worlds == null) {
				worlds = getBackingData().getWorlds();
			}
			return worlds;
		}
	}

	protected void clearWorldsCache() {
		synchronized (worldsLock) {
			worlds = null;
		}
	}

	@Override
	public String getPrefix(String worldName) {
		String prefix = prefixMap.get(serializeNull(worldName));
		if (prefix == null) {
			prefix = getBackingData().getPrefix(worldName);
			prefixMap.put(serializeNull(worldName), serializeNull(prefix));
		}
		return deserializeNull(prefix);

	}

	@Override
	public void setPrefix(final String prefix, final String worldName) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				getBackingData().setPrefix(prefix, worldName);
			}
		});
		prefixMap.put(serializeNull(worldName), serializeNull(prefix));
	}

	@Override
	public String getSuffix(String worldName) {
		String suffix = suffixMap.get(serializeNull(worldName));
		if (suffix == null) {
			suffix = getBackingData().getPrefix(worldName);
			suffixMap.put(serializeNull(worldName), serializeNull(suffix));
		}
		return deserializeNull(suffix);
	}

	@Override
	public void setSuffix(final String suffix, final String worldName) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				getBackingData().setPrefix(suffix, worldName);
			}
		});
		suffixMap.put(serializeNull(worldName), serializeNull(suffix));
	}

	@Override
	public String getOption(String option, String worldName) {
		if (options == null) {
			loadOptions();
		}
		Map<String, String> worldOpts = options.get(worldName);
		if (worldOpts == null) {
			return null;
		}
		return worldOpts.get(option);
	}

	@Override
	public void setOption(final String option, final String value, final String world) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				getBackingData().setOption(option, value, world);
			}
		});
		if (options != null) {
			Map<String, String> optionsMap = options.get(world);
			if (optionsMap == null) {
				optionsMap = new HashMap<>();
				options.put(world, optionsMap);
				clearWorldsCache();
			}
			optionsMap.put(option, value);
		}
	}

	@Override
	public Map<String, String> getOptions(String worldName) {
		if (options == null) {
			loadOptions();
		}
		Map<String, String> opts = options.get(worldName);
		return opts == null ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(opts);
	}

	@Override
	public Map<String, Map<String, String>> getOptionsMap() {
		if (options == null) {
			loadPermissions();
		}
		Map<String, Map<String, String>> ret = new HashMap<>();
		for (Map.Entry<String, Map<String, String>> e : options.entrySet()) {
			ret.put(e.getKey(), Collections.unmodifiableMap(e.getValue()));
		}
		return Collections.unmodifiableMap(ret);
	}

	@Override
	public List<String> getParents(String worldName) {
		if (parents == null) {
			loadInheritance();
		}

		List<String> worldParents = parents.get(worldName);
		return worldParents == null ? Collections.<String>emptyList() : worldParents;
	}

	@Override
	public void setParents(final List<String> rawParents, final String worldName) {
		final List<String> safeParents = new ArrayList<>(rawParents);
		executor.execute(new Runnable() {
			@Override
			public void run() {
				getBackingData().setParents(safeParents, worldName);
			}
		});
		if (this.parents != null) {
			this.parents.put(worldName, Collections.unmodifiableList(safeParents));
		}
	}

	@Override
	public boolean isVirtual() {
		return getBackingData().isVirtual();
	}

	@Override
	public void save() {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				getBackingData().save();
			}
		});
	}

	@Override
	public void remove() {
		getBackingData().remove();
		clearCache();
	}

	@Override
	public Map<String, List<String>> getParentsMap() {
		if (parents == null) {
			loadInheritance();
		}
		Map<String, List<String>> ret = new HashMap<>();
		for (Map.Entry<String, List<String>> e : parents.entrySet()) {
			ret.put(e.getKey(), Collections.unmodifiableList(e.getValue()));
		}
		return Collections.unmodifiableMap(ret);
	}
}
