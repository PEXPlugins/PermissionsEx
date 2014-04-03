package ru.tehkode.permissions.backends.sql;

import com.google.common.collect.ImmutableList;
import ru.tehkode.permissions.PermissionsGroupData;
import ru.tehkode.permissions.PermissionsUserData;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data for SQL entities
 */
public class SQLData implements PermissionsUserData, PermissionsGroupData {
	private String identifier;
	private final Type type;
	private final SQLBackend backend;

	// Cache
	private Map<String, List<String>> worldsPermissions = null;
	private Map<String, Map<String, String>> worldsOptions = null;
	private List<String> commonPermissions = null;
	private Map<String, String> commonOptions = null;
	private Map<String, Set<String>> parents = null;
	private boolean virtual = true;
	private boolean def;
	private String prefix, suffix;

	public SQLData(String identifier, Type type, SQLBackend backend) {
		this.identifier = identifier;
		this.type = type;
		this.backend = backend;
		fetchInfo();
	}

	// Cache updating

	protected void updateInfo() {
		String sql;
		if (this.isVirtual()) { // This section are suspicious, here was problem which are resolved mysticaly. Keep eye on it.
			sql = "INSERT INTO `{permissions_entity}` (`prefix`, `suffix`, `default`, `name`, `type`) VALUES (?, ?, ?, ?, ?)";
		} else {
			sql = "UPDATE `{permissions_entity}` SET `prefix` = ?, `suffix` = ?, `default` = ? WHERE `name` = ? AND `type` = ?";
		}

		try (SQLConnection conn = backend.getSQL()) {
			conn.prepAndBind(sql, nullToEmpty(this.prefix), nullToEmpty(this.suffix), this.def ? 1 : 0, this.getIdentifier(), this.type.ordinal()).execute();
		} catch (SQLException e) {
			if (this.isVirtual()) {
				this.virtual = false;
				this.updateInfo(); // try again
			}

			throw new RuntimeException(e);
		}

		this.virtual = false;
	}

	protected final void fetchPermissions() {
		this.worldsOptions = new HashMap<>();
		this.worldsPermissions = new HashMap<>();
		this.commonOptions = new HashMap<>();
		this.commonPermissions = new LinkedList<>();

		try (SQLConnection conn = backend.getSQL()) {
			ResultSet results = conn.prepAndBind("SELECT `permission`, `world`, `value` FROM `{permissions}` WHERE `name` = ? AND `type` = ? ORDER BY `id` DESC", this.getIdentifier(), this.type.ordinal()).executeQuery();
			while (results.next()) {
				String permission = results.getString("permission").trim();
				String world = results.getString("world").trim();
				String value = results.getString("value");

				// @TODO: to this in more optimal way
				if (value.isEmpty()) {
					if (!world.isEmpty()) {
						List<String> worldPermissions = this.worldsPermissions.get(world);
						if (worldPermissions == null) {
							worldPermissions = new LinkedList<>();
							this.worldsPermissions.put(world, worldPermissions);
						}

						worldPermissions.add(permission);
					} else {
						this.commonPermissions.add(permission);
					}
				} else {
					if (!world.isEmpty()) {
						Map<String, String> worldOptions = this.worldsOptions.get(world);
						if (worldOptions == null) {
							worldOptions = new HashMap<>();
							worldsOptions.put(world, worldOptions);
						}

						worldOptions.put(permission, value);
					} else {
						commonOptions.put(permission, value);
					}
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	protected final void fetchInheritance() {
		try (SQLConnection conn = backend.getSQL()) {
			this.parents = new HashMap<>();

			ResultSet results = conn.prepAndBind("SELECT `parent`, `world` FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = ? ORDER BY `id` DESC", this.getIdentifier(), this.type.ordinal()).executeQuery();

			while (results.next()) {
				String parentName = results.getString(1);
				String worldName = results.getString(2);

				if (!this.parents.containsKey(worldName)) {
					this.parents.put(worldName, new HashSet<String>());
				}

				this.parents.get(worldName).add(parentName);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

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

	protected final void fetchInfo() {
		try (SQLConnection conn = backend.getSQL()) {
			ResultSet result = conn.prepAndBind("SELECT `name`, `prefix`, `suffix`, `default` FROM `{permissions_entity}` WHERE `name` = ? AND `type` = ? LIMIT 1", this.getIdentifier(), this.type.ordinal()).executeQuery();

			if (result.next()) {
				this.prefix = emptyToNull(result.getString("prefix"));
				this.suffix = emptyToNull(result.getString("suffix"));
				this.def = result.getBoolean("default");

				// For teh case-insensetivity
				this.identifier = result.getString("name");

				this.virtual = false;
			} else {
				this.prefix = "";
				this.suffix = "";
				this.virtual = true;
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
		List<String> permissions = new LinkedList<>();

		if (commonPermissions == null) {
			this.fetchPermissions();
		}

		if (worldName != null && !worldName.isEmpty()) {
			List<String> worldPermissions = this.worldsPermissions.get(worldName);
			if (worldPermissions != null) {
				permissions.addAll(worldPermissions);
			}
		} else {
			permissions = commonPermissions;
		}

		return Collections.unmodifiableList(permissions);
	}

	@Override
	public void setPermissions(List<String> permissions, String worldName) {
		if (worldName == null) {
			worldName = "";
		}

		try (SQLConnection conn = backend.getSQL()) {
			conn.prepAndBind("DELETE FROM `{permissions}` WHERE `name` = ? AND `type` = ? AND `world` = ? AND `value` = ''", this.getIdentifier(), this.type.ordinal(), worldName).execute();

			PreparedStatement statement = conn.prepAndBind("INSERT INTO `{permissions}` (`name`, `permission`, `value`, `world`, `type`) VALUES (?, ?, '', ?, ?)", this.getIdentifier(), "toset", worldName, this.type.ordinal());
			for (int i = permissions.size() - 1; i >= 0; i--) { // insert in reverse order
				statement.setString(2, permissions.get(i));
				statement.addBatch();
			}
			statement.executeBatch();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		if (this.isVirtual()) {
			this.save();
		}

		if (this.commonPermissions == null) {
			this.fetchPermissions();
		} else {
			if (worldName.isEmpty()) {
				this.commonPermissions = new ArrayList<>(permissions);
			} else {
				this.worldsPermissions.put(worldName, new ArrayList<>(permissions));
			}
		}
	}

	@Override
	public Map<String, List<String>> getPermissionsMap() {
		Map<String, List<String>> allPermissions = new HashMap<>();

		if (this.commonPermissions == null) {
			this.fetchPermissions();
		}

		allPermissions.put(null, Collections.unmodifiableList(this.commonPermissions));

		for (Map.Entry<String, List<String>> entry : this.worldsPermissions.entrySet()) {
			allPermissions.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
		}

		return Collections.unmodifiableMap(allPermissions);
	}

	@Override
	public Set<String> getWorlds() {
		Set<String> worlds = new HashSet<>();

		worlds.addAll(worldsOptions.keySet());
		worlds.addAll(worldsPermissions.keySet());
		worlds.addAll(parents.keySet());

		return Collections.unmodifiableSet(worlds);
	}

	@Override
	public String getPrefix(String worldName) {
		return (worldName == null || worldName.isEmpty()) ? this.prefix : this.getOption("prefix", worldName);
	}

	@Override
	public void setPrefix(String prefix, String worldName) {
		if (worldName == null || worldName.isEmpty()) {
			this.prefix = prefix;
			this.updateInfo();
		} else {
			this.setOption("prefix", prefix, worldName);
		}
	}

	@Override
	public String getSuffix(String worldName) {
		return (worldName == null || worldName.isEmpty()) ? this.suffix : this.getOption("suffix", worldName);
	}

	@Override
	public void setSuffix(String suffix, String worldName) {
		if (worldName == null || worldName.isEmpty()) {
			this.suffix = suffix;
			this.updateInfo();
		} else {
			this.setOption("suffix", suffix, worldName);
		}
	}

	@Override
	public String getOption(String option, String worldName) {
		if (this.commonOptions == null) {
			fetchPermissions();
		}
		if (worldName != null && !worldName.isEmpty() && this.worldsOptions.containsKey(worldName)) {
			if (this.worldsOptions.get(worldName).containsKey(option)) {
				return this.worldsOptions.get(worldName).get(option);
			}
		}

		if ((worldName == null || worldName.isEmpty()) && this.commonOptions.containsKey(option)) {
			return this.commonOptions.get(option);
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
				conn.prepAndBind("DELETE FROM `{permissions}` WHERE `name` = ? AND `permission` = ? AND `type` = ? AND `world` = ?", this.getIdentifier(), option, this.type.ordinal(), worldName).execute();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}

			if (commonOptions == null) {
				fetchPermissions();
			} else {
				if (!worldName.isEmpty() && this.worldsOptions.containsKey(worldName)) {
					this.worldsOptions.get(worldName).remove(option);
				} else {
					this.commonOptions.remove(option);
				}
			}

			return;
		}

		Boolean newOption = true;
		if (this.commonOptions == null) {
			this.fetchPermissions();
		}

		if (!worldName.isEmpty() && worldsOptions.containsKey(worldName) && worldsOptions.get(worldName).containsKey(option)) {
			newOption = false;
		} else if (worldName.isEmpty() && commonOptions.containsKey(option)) {
			newOption = false;
		}


		try (SQLConnection conn = backend.getSQL()) {
			if (newOption) {
				conn.prepAndBind("INSERT INTO `{permissions}` (`name`, `permission`, `value`, `world`, `type`) VALUES (?, ?, ?, ?, ?)", this.getIdentifier(), option, value, worldName, this.type.ordinal()).execute();
			} else {
				conn.prepAndBind("UPDATE `{permissions}` SET `value` = ? WHERE `name` = ? AND `type` = ? AND `permission` = ?", value, this.getIdentifier(), this.type.ordinal(), option).execute();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		if (this.isVirtual()) {
			this.save();
		}

		if (worldName.isEmpty()) {
			commonOptions.put(option, value);
		} else {
			Map<String, Map<String, String>> worldsOptions = this.worldsOptions;
			Map<String, String> options = worldsOptions.get(worldName);
			if (options == null) {
				options = new HashMap<>();
				worldsOptions.put(worldName, options);
			}
			options.put(option, value);
		}
	}

	@Override
	public Map<String, String> getOptions(String worldName) {
		Map<String, String> options = worldName == null ? this.commonOptions : this.worldsOptions.get(worldName);

		return options != null ? Collections.unmodifiableMap(options) : Collections.<String, String>emptyMap();
	}

	@Override
	public Map<String, Map<String, String>> getOptionsMap() {
		Map<String, Map<String, String>> allOptions = new HashMap<>();

		if (this.commonOptions == null) {
			this.fetchPermissions();
		}

		allOptions.put(null, Collections.unmodifiableMap(this.commonOptions));

		for (Map.Entry<String, Map<String, String>> entry : this.worldsOptions.entrySet()) {
			allOptions.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
		}

		return Collections.unmodifiableMap(allOptions);
	}

	@Override
	public boolean isVirtual() {
		return virtual;
	}

	@Override
	public void save() {
		this.updateInfo();
	}

	@Override
	public void remove() {
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

		this.virtual = true;
		this.commonOptions.clear();
		this.commonPermissions.clear();
		this.worldsOptions.clear();
		this.worldsPermissions.clear();
		this.parents.clear();
	}

	@Override
	public List<String> getParents(String worldName) {
		if (this.parents == null) {
			this.fetchInheritance();
		}

		if (this.parents.containsKey(worldName)) {
			return ImmutableList.copyOf(this.parents.get(worldName));
		}

		return Collections.emptyList();
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
				conn.prepAndBind("DELETE FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = ? AND IFNULL(`world`, 1)", this.getIdentifier(), this.type.ordinal()).execute();
			}


			PreparedStatement statement = conn.prepAndBind("INSERT INTO `{permissions_inheritance}` (`child`, `parent`, `type`, `world`) VALUES (?, ?, ?, ?)", this.getIdentifier(), "toset", this.type.ordinal(), worldName);
			for (String group : parents) {
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

		//reload inheritance
		this.parents = null;
		this.fetchInheritance();
	}

	@Override
	public boolean isDefault(String world) {
		if (world == null) {
			return this.def;
		} else {
			return Boolean.parseBoolean(getOption("default", world));
		}
	}

	@Override
	public void setDefault(boolean def, String world) {
		if (world == null) {
			this.def = def;
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

		ResultSet result = sql.prepAndBind("SELECT name FROM `{permissions_entity}` WHERE `type` = ? " + (defaultOnly ? " AND `default` = 1" : ""), type.ordinal()).executeQuery();

		while (result.next()) {
			entities.add(result.getString("name"));
		}

		result.close();

		return Collections.unmodifiableSet(entities);
	}
}
