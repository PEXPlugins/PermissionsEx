package ru.tehkode.permissions;


import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.After;
import org.junit.Before;
import ru.tehkode.permissions.sponge.PermissionsExConfig;
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
		final Configuration config = new MemoryConfiguration();
		applyConfiguration(config);
		manager = new PermissionManager(new PermissionsExConfig(config), LOGGER, new NullNativeInterface());
	}

	@After
	public void tearDown() {
		manager.end();
	}

	protected void applyConfiguration(Configuration permissionsConfig) {
		permissionsConfig.set("permissions.backend", "ru.tehkode.permissions.backends.memory.MemoryBackend");

	}

	protected PermissionManager getManager() {
		return manager;
	}
}
