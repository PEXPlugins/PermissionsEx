package ru.tehkode.permissions.backends.caching;

import ru.tehkode.permissions.PermissionsData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Data backend implementing a simple cache
 */
public abstract class CachingData implements PermissionsData {
	private final Executor executor;
	protected final ReadWriteLock lock;
	private Map<String, List<String>> permissions;
	private Map<String, Map<String, String>> options;
	private Map<String, List<String>> parents;
	private volatile Set<String> worlds;

	public CachingData(Executor executor, ReadWriteLock lock) {
		this.executor = executor;
		this.lock = lock;
	}

	protected void executeWrite(final Runnable run) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				lock.writeLock().lock();
				try {
					run.run();
				} finally {
					lock.readLock().lock();
					try {
						lock.writeLock().unlock();
						getBackingData().save();
					} finally {
						lock.readLock().unlock();
					}
				}
			}
		});
	}
	protected void executeRead(final Runnable run) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				lock.readLock().lock();
				try {
					run.run();
				} finally {
					lock.readLock().unlock();
				}
			}
		});
		}

	protected abstract PermissionsData getBackingData();

	protected void loadPermissions() {
		lock.readLock().lock();
		try {
			this.permissions = new HashMap<>(getBackingData().getPermissionsMap());
		} finally {
			lock.readLock().unlock();
		}
	}

	protected void loadOptions() {
		lock.readLock().lock();
		try {
			this.options = new HashMap<>();
			for (Map.Entry<String, Map<String, String>> e : getBackingData().getOptionsMap().entrySet()) {
				this.options.put(e.getKey(), new HashMap<>(e.getValue()));
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	protected void loadInheritance() {
		lock.readLock().lock();
		try {
			this.parents = new HashMap<>(getBackingData().getParentsMap());
		} finally {
			lock.readLock().unlock();
		}
	}

	protected void clearCache() {
		lock.writeLock().lock();
		try {
			permissions = null;
			options = null;
			parents = null;
			clearWorldsCache();
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void load() {
		lock.writeLock().lock();
		try {
			getBackingData().load();
			loadInheritance();
			loadOptions();
			loadPermissions();
		} finally {
			lock.writeLock().unlock();
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
		executeWrite(new Runnable() {
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
		Set<String> worlds = this.worlds;
		if (worlds == null) {
			lock.readLock().lock();
			try {
				this.worlds = worlds = getBackingData().getWorlds();
			} finally {
				lock.readLock().unlock();
			}
		}
		return worlds;
	}

	protected void clearWorldsCache() {
		worlds = null;
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
		executeWrite(new Runnable() {
			@Override
			public void run() {
				if (options != null) {
					Map<String, String> optionsMap = options.get(world);
					if (optionsMap == null) {
						// TODO Concurrentify
						optionsMap = new HashMap<>();
						options.put(world, optionsMap);
						clearWorldsCache();
					}
					if (value == null) {
						optionsMap.remove(option);
					} else {
						optionsMap.put(option, value);
					}
				}
				getBackingData().setOption(option, value, world);
			}
		});
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
			loadOptions();
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
		executeWrite(new Runnable() {
			@Override
			public void run() {
				parents.put(worldName, Collections.unmodifiableList(safeParents));
				getBackingData().setParents(safeParents, worldName);
			}
		});
	}

	@Override
	public boolean isVirtual() {
		return getBackingData().isVirtual();
	}

	@Override
	public void save() {
		executeRead(new Runnable() {
			@Override
			public void run() {
				getBackingData().save();
			}
		});
	}

	@Override
	public void remove() {
		lock.writeLock().lock();
		try {
			getBackingData().remove();
			clearCache();
		} finally {
			lock.writeLock().unlock();
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
