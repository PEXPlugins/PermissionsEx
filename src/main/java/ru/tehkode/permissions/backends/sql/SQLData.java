package ru.tehkode.permissions.backends.sql;

import ru.tehkode.permissions.PermissionsGroupData;
import ru.tehkode.permissions.PermissionsUserData;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Data for SQL entities
 */
public class SQLData implements PermissionsUserData, PermissionsGroupData {
	private String identifier;
	private final Type type;
	private final SQLBackend backend;

	// Cache
	private final AtomicBoolean virtual = new AtomicBoolean(true);
	private volatile boolean globalDef;
	private volatile String globalPrefix, globalSuffix;

	public SQLData(String identifier, Type type, SQLBackend backend) {
		this.identifier = identifier;
		this.type = type;
		this.backend = backend;
		fetchInfo();
	}

	// Cache updating
	private String emptyToNull(String enter) {
		if (enter == null || enter.equals("null")) {
			return null;
		}
		return enter;
	}

	private String nullToEmpty(String enter) {
		if (enter == null) {
			return "null";
		}
		return enter;
	}

	protected void updateInfo() {
		try (SQLConnection conn = backend.getSQL()) {
			conn.prepAndBind("INSERT INTO `{permissions_entity}` (`prefix`, `suffix`, `default`, `name`, `type`) VALUES (?, ?, ?, ?, ?)" +
					" ON DUPLICATE KEY UPDATE `prefix` = VALUES(`prefix`), `suffix` = VALUES(`suffix`), `default` = VALUES(`default`)",
					nullToEmpty(this.globalPrefix), nullToEmpty(this.globalSuffix), this.globalDef ? 1 : 0, this.getIdentifier(), this.type.ordinal()).execute();
		} catch (SQLException e) {
			if (virtual.compareAndSet(true, false)) {
				this.updateInfo();
			}

			throw new RuntimeException(e);
		}

		this.virtual.set(false);
	}


	protected final void fetchInfo() {
		try (SQLConnection conn = backend.getSQL()) {
			ResultSet result = conn.prepAndBind("SELECT `name`, `prefix`, `suffix`, `default` FROM `{permissions_entity}` WHERE `name` = ? AND `type` = ? LIMIT 1", this.getIdentifier(), this.type.ordinal()).executeQuery();

			if (result.next()) {
				this.globalPrefix = emptyToNull(result.getString("prefix"));
				this.globalSuffix = emptyToNull(result.getString("suffix"));
				this.globalDef = result.getBoolean("default");

				// For teh case-insensetivity
				this.identifier = result.getString("name");

				this.virtual.set(false);
			} else {
				this.globalPrefix = "";
				this.globalSuffix = "";
				this.virtual.set(true);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	// Interface methods

	public String getIdentifier() {
		return identifier;
	}

	@Override
	public boolean setIdentifier(String identifier) {
		try (SQLConnection conn = backend.getSQL()) {
			ResultSet set = conn.prepAndBind("SELECT `id` from `{permissions_entity}` WHERE `name` = ? AND `type` = ? LIMIT 1", identifier, this.type.ordinal()).executeQuery();
			if (set.next()) {
				return false;
			}

			if (this.isVirtual()) {
				this.identifier = identifier;
				return true;
			}

			conn.prepAndBind("UPDATE `{permissions_entity}` SET `name` = ? WHERE `name` = ? AND `type` = ?", identifier, this.identifier, this.type.ordinal()).execute();
			conn.prepAndBind("UPDATE `{permissions}` SET `name` = ? WHERE `name` = ? AND `type` = ?", identifier, this.identifier, this.type.ordinal()).execute();
			conn.prepAndBind("UPDATE `{permissions_inheritance}` SET `child` = ? WHERE `child` = ? AND `type` = ?", identifier, this.identifier, this.type.ordinal()).execute();
			this.identifier = identifier;
			return true;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<String> getPermissions(String worldName) {
		try (SQLConnection conn = backend.getSQL()) {
			LinkedList<String> permissions = new LinkedList<>();
			ResultSet set = conn.prepAndBind("SELECT `permission` FROM `{permissions}` WHERE `name` = ? AND `type` = ? AND `world` = ? AND CHAR_LENGTH(`value`) = 0 ORDER BY `id` DESC", getIdentifier(), this.type.ordinal(), worldName == null ? "" : worldName).executeQuery();

			while (set.next()) {
				permissions.add(set.getString("permission"));
			}

			return Collections.unmodifiableList(permissions);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setPermissions(List<String> permissions, String worldName) {
		if (worldName == null) {
			worldName = "";
		}

		try (SQLConnection conn = backend.getSQL()) {
			conn.prepAndBind("DELETE FROM `{permissions}` WHERE `name` = ? AND `type` = ? AND `world` = ? AND `value` = ''", this.getIdentifier(), this.type.ordinal(), worldName).execute();

			if (permissions.size() > 0) {
				Set<String> includedPerms = new HashSet<>();
				PreparedStatement statement = conn.prepAndBind("INSERT INTO `{permissions}` (`name`, `permission`, `value`, `world`, `type`) VALUES (?, ?, '', ?, ?)", this.getIdentifier(), "toset", worldName, this.type.ordinal());
				for (int i = permissions.size() - 1; i >= 0; i--) { // insert in reverse order
					if (!includedPerms.contains(permissions.get(i))) {
						statement.setString(2, permissions.get(i));
						statement.addBatch();
						includedPerms.add(permissions.get(i));
					}
				}
				statement.executeBatch();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		if (permissions.size() > 0 && this.isVirtual()) {
			this.save();
		}
	}

	@Override
	public Map<String, List<String>> getPermissionsMap() {
		Map<String, List<String>> allPermissions = new HashMap<>();

		try (SQLConnection conn = backend.getSQL()) {
			ResultSet res = conn.prepAndBind("SELECT `permission`, `world` FROM `{permissions}` WHERE `name` = ? AND `type` = ? AND CHAR_LENGTH(`value`) = 0 ORDER BY `id` DESC", getIdentifier(), type.ordinal()).executeQuery();
			while (res.next()) {
				String world = res.getString("world");
				if (world.isEmpty()) {
					world = null;
				}
				List<String> perms = allPermissions.get(world);
				if (perms == null) {
					perms = new LinkedList<>();
					allPermissions.put(world, perms);
				}
				perms.add(res.getString("permission"));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		Map<String, List<String>> ret = new HashMap<>();
		for (Map.Entry<String, List<String>> e : allPermissions.entrySet()) {
			ret.put(e.getKey(), Collections.unmodifiableList(e.getValue()));
		}
		return Collections.unmodifiableMap(ret);
	}

	@Override
	public Set<String> getWorlds() {
		Set<String> worlds = new HashSet<>();
		try (SQLConnection conn = backend.getSQL()) {
			ResultSet res = conn.prepAndBind("SELECT `world` FROM `{permissions}` WHERE `name` = ? AND `type` = ?", getIdentifier(), type.ordinal()).executeQuery();
			while (res.next()) {
				worlds.add(res.getString("world"));
			}
			res = conn.prepAndBind("SELECT `world` FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = ?", getIdentifier(), type.ordinal()).executeQuery();
			while (res.next()) {
				worlds.add(res.getString("world"));
			}
			worlds.remove("");

			return Collections.unmodifiableSet(worlds);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getPrefix(String worldName) {
		return (worldName == null || worldName.isEmpty()) ? this.globalPrefix : this.getOption("prefix", worldName);
	}

	@Override
	public void setPrefix(String prefix, String worldName) {
		if (worldName == null || worldName.isEmpty()) {
			this.globalPrefix = prefix;
			this.updateInfo();
		} else {
			this.setOption("prefix", prefix, worldName);
		}
	}

	@Override
	public String getSuffix(String worldName) {
		return (worldName == null || worldName.isEmpty()) ? this.globalSuffix : this.getOption("suffix", worldName);
	}

	@Override
	public void setSuffix(String suffix, String worldName) {
		if (worldName == null || worldName.isEmpty()) {
			this.globalSuffix = suffix;
			this.updateInfo();
		} else {
			this.setOption("suffix", suffix, worldName);
		}
	}

	@Override
	public String getOption(String option, String worldName) {
		try (SQLConnection conn = backend.getSQL()) {
			ResultSet res = conn.prepAndBind("SELECT `value` FROM `{permissions}` WHERE `name` = ? AND `type` = ? AND `permission` = ? AND `world` = ? AND CHAR_LENGTH(`value`) > 0 LIMIT 1", getIdentifier(), this.type.ordinal(), option, worldName == null ? "" : worldName).executeQuery();
			if (res.next()) {
				return res.getString("value");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void setOption(String option, String value, String worldName) {
		if (option == null || option.isEmpty()) {
			return;
		}

		if (worldName == null) {
			worldName = "";
		}

		if (value == null || value.isEmpty()) {
			try (SQLConnection conn = backend.getSQL()) {
				conn.prepAndBind("DELETE FROM `{permissions}` WHERE `name` = ? AND `permission` = ? AND `type` = ? AND `world` = ? AND CHAR_LENGTH(`value`) > 0", this.getIdentifier(), option, this.type.ordinal(), worldName).execute();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		} else {
			try (SQLConnection conn = backend.getSQL()) {
				conn.prepAndBind("INSERT INTO `{permissions}` (`name`, `type`, `permission`, `world`, `value`) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `value` = VALUES(`value`)", getIdentifier(), this.type.ordinal(), option, worldName, value).execute();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public Map<String, String> getOptions(String worldName) {
		Map<String, String> options = new HashMap<>();
		try (SQLConnection conn = backend.getSQL()) {
			ResultSet set = conn.prepAndBind("SELECT `permission`, `value` FROM `{permissions}` WHERE `name` = ? AND `type` = ? AND `world` = ? AND CHAR_LENGTH(`value`) > 0", getIdentifier(), type.ordinal(), worldName == null ? "" : worldName).executeQuery();
			while (set.next()) {
				options.put(set.getString("permission"), set.getString("value"));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		return Collections.unmodifiableMap(options);
	}

	@Override
	public Map<String, Map<String, String>> getOptionsMap() {
		Map<String, Map<String, String>> allOptions = new HashMap<>();

		try (SQLConnection conn = backend.getSQL()) {
			ResultSet res = conn.prepAndBind("SELECT `permission`, `value`, `world` FROM `{permissions}` WHERE `name` = ? AND `type` = ? AND CHAR_LENGTH(`value`) > 0", getIdentifier(), type.ordinal()).executeQuery();
			while (res.next()) {
				String world = res.getString("world");
				if (world.isEmpty()) {
					world = null;
				}
				Map<String, String> worldOpts = allOptions.get(world);
				if (worldOpts == null) {
					worldOpts = new HashMap<>();
					allOptions.put(world, worldOpts);
				}
				worldOpts.put(res.getString("permission"), res.getString("value"));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		return Collections.unmodifiableMap(allOptions);
	}

	@Override
	public boolean isVirtual() {
		return virtual.get();
	}

	@Override
	public void save() {
		this.updateInfo();
	}

	@Override
	public void remove() {
		if (this.virtual.compareAndSet(false, true)) {
			try (SQLConnection conn = backend.getSQL()) {
				// clear inheritance info
				conn.prepAndBind("DELETE FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = ?", this.getIdentifier(), this.type.ordinal()).execute();
				// clear permissions
				conn.prepAndBind("DELETE FROM `{permissions}` WHERE `name` = ? AND `type` = ?", this.getIdentifier(), this.type.ordinal()).execute();
				// clear info
				conn.prepAndBind("DELETE FROM `{permissions_entity}` WHERE `name` = ? AND `type` = ?", this.getIdentifier(), this.type.ordinal()).execute();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public Map<String, List<String>> getParentsMap() {
		Map<String, List<String>> ret = new HashMap<>();
		try (SQLConnection conn = backend.getSQL()) {
			ResultSet res = conn.prepAndBind("SELECT `parent`, `world` FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = ? ORDER BY `id` DESC", getIdentifier(), this.type.ordinal()).executeQuery();
			while (res.next()) {
				String world = res.getString("world");
				List<String> worldParents = ret.get(world);
				if (worldParents == null) {
					worldParents = new LinkedList<>();
					ret.put(world, worldParents);
				}
				worldParents.add(res.getString("parent"));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return Collections.unmodifiableMap(ret);
	}

	@Override
	public List<String> getParents(String worldName) {
		List<String> ret = new LinkedList<>();
		try (SQLConnection conn = backend.getSQL()) {
			ResultSet res;
			if (worldName == null) {
				res = conn.prepAndBind("SELECT `parent` FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = ? AND `world` IS NULL ORDER BY `id` DESC", getIdentifier(), type.ordinal()).executeQuery();
			} else {
				res = conn.prepAndBind("SELECT `parent` FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = ? AND `world` = ? ORDER BY `id` DESC", getIdentifier(), type.ordinal(), worldName).executeQuery();
			}
			while (res.next()) {
				ret.add(res.getString("parent"));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		return Collections.unmodifiableList(ret);
	}

	@Override
	public void setParents(List<String> parents, String worldName) {
		if (this.isVirtual()) {
			this.save();
		}
		try (SQLConnection conn = backend.getSQL()) {
			// Clean out existing records
			if (worldName != null) { // damn NULL
				conn.prepAndBind("DELETE FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = ? AND `world` = ?", this.getIdentifier(), this.type.ordinal(), worldName).execute();
			} else {
				conn.prepAndBind("DELETE FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = ? AND `world` IS NULL", this.getIdentifier(), this.type.ordinal()).execute();
			}

			PreparedStatement statement = conn.prepAndBind("INSERT INTO `{permissions_inheritance}` (`child`, `parent`, `type`, `world`) VALUES (?, ?, ?, ?)", this.getIdentifier(), "toset", this.type.ordinal(), worldName);
			for (int i = parents.size() - 1; i >= 0; --i) {
				final String group = parents.get(i);
				if (group == null || group.isEmpty()) {
					continue;
				}
				statement.setString(2, group);
				statement.addBatch();
			}
			statement.executeBatch();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void load() { // Nothing to load, we don't handle caching!
	}

	@Override
	public boolean isDefault(String world) {
		if (world == null) {
			return this.globalDef;
		} else {
			return Boolean.parseBoolean(getOption("default", world));
		}
	}

	@Override
	public void setDefault(boolean def, String world) {
		if (world == null) {
			this.globalDef = def;
			updateInfo();
		} else {
			this.setOption("default", world, String.valueOf(def));
		}
	}

	public enum Type {
		GROUP, USER
	}

	public static Set<String> getEntitiesNames(SQLConnection sql, Type type, boolean defaultOnly) throws SQLException {
		Set<String> entities = new HashSet<>();

		ResultSet result = sql.prepAndBind("SELECT `name` FROM `{permissions_entity}` WHERE `type` = ? " + (defaultOnly ? " AND `default` = 1" : ""), type.ordinal()).executeQuery();

		while (result.next()) {
			entities.add(result.getString("name"));
		}

		result.close();

		return Collections.unmodifiableSet(entities);
	}
}
