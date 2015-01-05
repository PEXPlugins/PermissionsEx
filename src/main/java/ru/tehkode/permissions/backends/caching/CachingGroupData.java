package ru.tehkode.permissions.backends.caching;

import ru.tehkode.permissions.PermissionsGroupData;

import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Cached data for groups
 */
public class CachingGroupData extends CachingData implements PermissionsGroupData {
	private final PermissionsGroupData backingData;
	public CachingGroupData(PermissionsGroupData backingData, Executor executor, ReadWriteLock lock) {
		super(executor, lock);
		this.backingData = backingData;
	}

	@Override
	protected PermissionsGroupData getBackingData() {
		return backingData;
	}
}
