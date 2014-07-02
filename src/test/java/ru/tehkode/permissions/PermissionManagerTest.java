package ru.tehkode.permissions;


import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.Before;
import ru.tehkode.permissions.bukkit.PermissionsExConfig;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

import java.util.logging.Logger;

/**
 * Base class for tests that need a PermissionManager instance.
 */
public abstract class PermissionManagerTest {
	private static final Logger LOGGER = Logger.getLogger(PermissionManagerTest.class.getCanonicalName());
	private PermissionManager manager;

	@Before
	public void setUp() throws PermissionBackendException {
		manager = new PermissionManager(new PermissionsExConfig(new MemoryConfiguration()), LOGGER, new NullNativeInterface());
	}

	protected void applyConfiguration(Configuration permissionsConfig) {
		permissionsConfig.set("permissions.backend", "memory");

	}

	protected PermissionManager getManager() {
		return manager;
	}
}
