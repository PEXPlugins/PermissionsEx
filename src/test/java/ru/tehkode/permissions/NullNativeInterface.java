package ru.tehkode.permissions;

import ru.tehkode.permissions.events.PermissionEvent;

import java.util.UUID;

/**
 * Native interface implementation that does nothing, for testing.
 */
public class NullNativeInterface implements NativeInterface {
	@Override
	public String UUIDToName(UUID uid) {
		return null;
	}

	@Override
	public UUID nameToUUID(String name) {
		return null;
	}

	@Override
	public boolean isOnline(UUID uuid) {
		return false;
	}

	@Override
	public UUID getServerUUID() {
		return UUID.randomUUID();
	}

	@Override
	public void callEvent(PermissionEvent event) {

	}
}
