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
	private final Executor executor;
	protected final Object lock;
	private Map<String, List<String>> permissions;
	private Map<String, Map<String, String>> options;
	private Map<String, List<String>> parents;
	private Map<String, String> prefixMap = new ConcurrentHashMap<>(), suffixMap = new ConcurrentHashMap<>();
	private Set<String> worlds;

	public CachingData(Executor executor, Object lock) {
		this.executor = executor;
		this.lock = lock;
	}

	protected void execute(final Runnable run) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				synchronized (lock) {
					run.run();
				}
			}
		});
	}

	protected static String serializeNull(String obj) {
		return obj == null ? MAP_NULL : obj;
	}

	protected static String deserializeNull(String obj) {
		return obj == MAP_NULL ? null : obj;
	}

	protected abstract PermissionsData getBackingData();

	protected void loadPermissions() {
		synchronized (lock) {
			this.permissions = new HashMap<>(getBackingData().getPermissionsMap());
		}
	}

	protected void loadOptions() {
		synchronized (lock) {
			this.options = new HashMap<>();
			for (Map.Entry<String, Map<String, String>> e : getBackingData().getOptionsMap().entrySet()) {
				this.options.put(e.getKey(), new HashMap<>(e.getValue()));
			}
		}
	}

	protected void loadInheritance() {
		synchronized (lock) {
			this.parents = new HashMap<>(getBackingData().getParentsMap());
		}
	}

	protected void clearCache() {
		synchronized (lock) {
			permissions = null;
			options = null;
			parents = null;
			prefixMap.clear();
			suffixMap.clear();
			clearWorldsCache();
		}
	}

	@Override
	public void load() {
		synchronized (lock) {
			getBackingData().load();
			loadInheritance();
			loadOptions();
			loadPermissions();
			getPrefix(null);
			getSuffix(null);
			for (String world : getWorlds()) {
				getPrefix(world);
				getSuffix(world);
			}
		}
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
		if (this.permissions == null) {
			loadPermissions();
		}
		final List<String> safePermissions = new ArrayList<>(permissions);
		execute(new Runnable() {
			@Override
			public void run() {
				clearWorldsCache();
				getBackingData().setPermissions(safePermissions, worldName);
			}
		});
		this.permissions.put(worldName, safePermissions);
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
		synchronized (lock) {
			if (worlds == null) {
				worlds = getBackingData().getWorlds();
			}
			return worlds;
		}
	}

	protected void clearWorldsCache() {
		synchronized (lock) {
			worlds = null;
		}
	}

	@Override
	public String getPrefix(String worldName) {
		String prefix = prefixMap.get(serializeNull(worldName));
		if (prefix == null) {
			synchronized (lock) {
				prefix = getBackingData().getPrefix(worldName);
			}
			prefixMap.put(serializeNull(worldName), serializeNull(prefix));
		}
		return deserializeNull(prefix);

	}

	@Override
	public void setPrefix(final String prefix, final String worldName) {
		execute(new Runnable() {
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
			synchronized (lock) {
				suffix = getBackingData().getPrefix(worldName);
			}
			suffixMap.put(serializeNull(worldName), serializeNull(suffix));
		}
		return deserializeNull(suffix);
	}

	@Override
	public void setSuffix(final String suffix, final String worldName) {
		execute(new Runnable() {
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
		if (options == null) {
			loadOptions();
		}
		execute(new Runnable() {
			@Override
			public void run() {
				getBackingData().setOption(option, value, world);
			}
		});
		if (options != null) {
			Map<String, String> optionsMap = options.get(world);
			if (optionsMap == null) {
				// TODO Concurrentify
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
		if (this.parents == null) {
			loadInheritance();
		}
		final List<String> safeParents = new ArrayList<>(rawParents);
		execute(new Runnable() {
			@Override
			public void run() {
				getBackingData().setParents(safeParents, worldName);
			}
		});
		this.parents.put(worldName, Collections.unmodifiableList(safeParents));
	}

	@Override
	public boolean isVirtual() {
		return getBackingData().isVirtual();
	}

	@Override
	public void save() {
		execute(new Runnable() {
			@Override
			public void run() {
				getBackingData().save();
			}
		});
	}

	@Override
	public void remove() {
		synchronized (lock) {
			getBackingData().remove();
			clearCache();
		}
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
