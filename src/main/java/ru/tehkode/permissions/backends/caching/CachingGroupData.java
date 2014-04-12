package ru.tehkode.permissions.backends.caching;

import ru.tehkode.permissions.PermissionsGroupData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Cached data for groups
 */
public class CachingGroupData extends CachingData implements PermissionsGroupData {
	private final PermissionsGroupData backingData;
	private final Map<String, Boolean> defaultsMap = new HashMap<>();
	public CachingGroupData(PermissionsGroupData backingData, Executor executor, Object lock) {
		super(executor, lock);
		this.backingData = backingData;
	}

	@Override
	protected PermissionsGroupData getBackingData() {
		return backingData;
	}

	@Override
	protected void clearCache() {
		super.clearCache();
		defaultsMap.clear();
	}

	@Override
	public boolean isDefault(String world) {
		Boolean bool = defaultsMap.get(world);
		if (bool == null) {
			synchronized (lock) {
				bool = getBackingData().isDefault(world);
			}
			defaultsMap.put(world, bool);
		}
		return bool;
	}

	@Override
	public void setDefault(final boolean def, final String world) {
		defaultsMap.put(world, def);
		execute(new Runnable() {
			@Override
			public void run() {
				getBackingData().setDefault(def, world);
			}
		});

	}
}
