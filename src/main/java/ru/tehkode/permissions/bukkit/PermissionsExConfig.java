package ru.tehkode.permissions.bukkit;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.backends.PermissionBackend;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author zml2008
 */
public class PermissionsExConfig {
	private final Configuration config;
	private final PermissionsEx plugin;

	private final boolean useNetEvents;
	private final boolean debug;
	private final boolean allowOps;
	private final boolean userAddGroupsLast;
	private final boolean logPlayers;
	private final boolean createUserRecords;
	private final String defaultBackend;
	private final boolean updaterEnabled;
	private final boolean alwaysUpdate;
	private final boolean informPlayers;
	private final List<String> serverTags;
	private final String basedir;

	public PermissionsExConfig(Configuration config, PermissionsEx plugin) {
		this.config = config;
		this.plugin = plugin;
		this.useNetEvents = getBoolean("multiserver.use-netevents", true);
		this.serverTags = getStringList("multiserver.server-tags");
		this.debug = getBoolean("permissions.debug", false);
		this.allowOps = getBoolean("permissions.allowOps", false);
		this.userAddGroupsLast = getBoolean("permissions.user-add-groups-last", false);
		this.logPlayers = getBoolean("permissions.log-players", false);
		this.createUserRecords = getBoolean("permissions.createUserRecords", false);
		this.defaultBackend = getString("permissions.backend", PermissionBackend.DEFAULT_BACKEND);
		this.updaterEnabled = getBoolean("updater", true);
		this.alwaysUpdate = getBoolean("alwaysUpdate", false);
		this.informPlayers = getBoolean("permissions.informplayers.changes", false);
		this.basedir = getString("permissions.basedir", "plugins/PermissionsEx");
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

	private List<String> getStringList(String key, String... def) {
		List<String> ret = config.getStringList(key);
		if (ret == null) {
			ret = Arrays.asList(def);
			config.set(key, ret);
		}
		return Collections.unmodifiableList(ret);
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

	public boolean alwaysUpdate() {
		return alwaysUpdate;
	}

	public boolean informPlayers() {
		return informPlayers;
	}

	public List<String> getServerTags() {
		return serverTags;
	}

	public String getBasedir() {
		return basedir;
	}

	public ConfigurationSection getBackendConfig(String backend) {
		ConfigurationSection section = config.getConfigurationSection("permissions.backends." + backend);
		if (section == null) {
			section = config.createSection("permissions.backends." + backend);
		}
		return section;
	}

	public void save() {
		plugin.saveConfig();
	}
}
