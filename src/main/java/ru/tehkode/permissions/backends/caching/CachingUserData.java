package ru.tehkode.permissions.backends.caching;

import ru.tehkode.permissions.PermissionsUserData;

import java.util.concurrent.Executor;

/**
 * User data using a cache.
 */
public class CachingUserData extends CachingData implements PermissionsUserData {
	private final PermissionsUserData userData;
	public CachingUserData(PermissionsUserData userData, Executor executor) {
		super(executor);
		this.userData = userData;
	}

	@Override
	protected PermissionsUserData getBackingData() {
		return this.userData;
	}

	@Override
	public boolean setIdentifier(final String identifier) {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				getBackingData().setIdentifier(identifier);
			}
		});
		return true; // TODO make this more accurate
	}
}
