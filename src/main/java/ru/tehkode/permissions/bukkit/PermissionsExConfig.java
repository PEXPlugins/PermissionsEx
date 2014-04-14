package ru.tehkode.permissions.bukkit;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.backends.PermissionBackend;

/**
 * @author zml2008
 */
public class PermissionsExConfig {
	private final Configuration config;

	private final boolean useNetEvents;
	private final boolean debug;
	private final boolean allowOps;
	private final boolean userAddGroupsLast;
	private final boolean logPlayers;
	private final boolean createUserRecords;
	private final String defaultBackend;
	private final boolean updaterEnabled;
	private final boolean alwaysUpdate;

	public PermissionsExConfig(Configuration config) {
		this.config = config;
		this.useNetEvents = getBoolean("multiserver.use-netevents", true);
		this.debug = getBoolean("permissions.debug", false);
		this.allowOps = getBoolean("permissions.allowOps", false);
		this.userAddGroupsLast = getBoolean("permissions.user-add-groups-last", false);
		this.logPlayers = getBoolean("permissions.log-players", false);
		this.createUserRecords = getBoolean("permissions.createUserRecords", false);
		this.defaultBackend = getString("permissions.backend", PermissionBackend.DEFAULT_BACKEND);
		this.updaterEnabled = getBoolean("updater", true);
		this.alwaysUpdate = getBoolean("alwaysUpdate", false);
	}

	private boolean getBoolean(String key, boolean def) {
		if (!config.isSet(key)) {
			config.set(key, def);
		}
		return config.getBoolean(key, def);
	}

	private String getString(String key, String def) {
		String ret = config.getString(key);
		if (ret == null) {
			ret = def;
			config.set(key, ret);
		}
		return ret;
	}

	public boolean useNetEvents() {
		return useNetEvents;
	}

	public boolean isDebug() {
		return debug;
	}

	public boolean allowOps() {
		return allowOps;
	}

	public boolean userAddGroupsLast() {
		return userAddGroupsLast;
	}

	public String getDefaultBackend() {
		return defaultBackend;
	}

	public boolean shouldLogPlayers() {
		return logPlayers;
	}

	public boolean createUserRecords() {
		return createUserRecords;
	}

	public boolean updaterEnabled() {
		return updaterEnabled;
	}

	public boolean alwaysUpdate() { return alwaysUpdate; }

	public ConfigurationSection getBackendConfig(String backend) {
		ConfigurationSection section = config.getConfigurationSection("permissions.backends." + backend);
		if (section == null) {
			section = config.createSection("permissions.backends." + backend);
		}
		return section;
	}
}
