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

	public PermissionsExConfig(Configuration config) {
		this.config = config;
		this.useNetEvents = config.getBoolean("multiserver.use-netevents", true);
		this.debug = config.getBoolean("permissions.debug", false);
		this.allowOps = config.getBoolean("permissions.allowOps", false);
		this.userAddGroupsLast = config.getBoolean("permissions.user-add-groups-last", false);
		this.logPlayers = config.getBoolean("permissions.log-players", false);
		this.createUserRecords = config.getBoolean("permissions.createUserRecords", false);
		this.defaultBackend = config.getString("permissions.backend", PermissionBackend.DEFAULT_BACKEND);
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

	public ConfigurationSection getBackendConfig(String backend) {
		return config.getConfigurationSection("permissions.backends." + backend);
	}
}
