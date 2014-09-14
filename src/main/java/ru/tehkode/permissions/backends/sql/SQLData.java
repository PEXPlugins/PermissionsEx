package ru.tehkode.permissions.backends.sql;

import ru.tehkode.permissions.PermissionsGroupData;
import ru.tehkode.permissions.PermissionsUserData;

import java.io.IOException;
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

	public SQLData(String identifier, Type type, SQLBackend backend) {
		this.identifier = identifier;
		this.type = type;
		this.backend = backend;
		fetchInfo();
	}

	protected void updateInfo() {
		if (!this.isVirtual()) { // Non-virtual, no-op
			return;
		}

		try (SQLConnection conn = backend.getSQL()) {
			conn.prepAndBind("entity.update", this.getIdentifier(), this.type.ordinal()).execute();
		} catch (SQLException | IOException e) {
			if (virtual.compareAndSet(true, false)) {
				this.updateInfo();
			}

			throw new RuntimeException(e);
		}

		backend.updateNameCache(this);
		this.virtual.set(false);
	}


	protected final void fetchInfo() {
		try (SQLConnection conn = backend.getSQL()) {
			ResultSet result = conn.prepAndBind("entity.fetch", this.getIdentifier(), this.type.ordinal()).executeQuery();

			if (result.next()) {
				// For teh case-insensetivity
				this.identifier = result.getString("name");

				this.virtual.set(false);
			} else {
				this.virtual.set(true);
			}
		} catch (SQLException | IOException e) {
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
			ResultSet set = conn.prepAndBind("entity.exists", identifier, this.type.ordinal()).executeQuery();
			if (set.next()) {
				return false;
			}

			if (this.isVirtual()) {
				this.identifier = identifier;
				return true;
			}

			conn.prepAndBind("entity.rename.entity", identifier, this.identifier, this.type.ordinal()).execute();
			conn.prepAndBind("entity.rename.permissions", identifier, this.identifier, this.type.ordinal()).execute();
			conn.prepAndBind("entity.rename.inheritance", identifier, this.identifier, this.type.ordinal()).execute();
			this.identifier = identifier;
			backend.updateNameCache(this);
			return true;
		} catch (SQLException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<String> getPermissions(String worldName) {
		try (SQLConnection conn = backend.getSQL()) {
			LinkedList<String> permissions = new LinkedList<>();
			ResultSet set = conn.prepAndBind("entity.permissions.get_world", getIdentifier(), this.type.ordinal(), worldName == null ? "" : worldName).executeQuery();

			while (set.next()) {
				permissions.add(set.getString("permission"));
			}

			return Collections.unmodifiableList(permissions);
		} catch (SQLException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setPermissions(List<String> permissions, String worldName) {
		if (worldName == null) {
			worldName = "";
		}

		try (SQLConnection conn = backend.getSQL()) {
			conn.prepAndBind("entity.permissions.clear", this.getIdentifier(), this.type.ordinal(), worldName).execute();

			if (permissions.size() > 0) {
				Set<String> includedPerms = new HashSet<>();
				PreparedStatement statement = conn.prepAndBind("entity.permissions.add", this.getIdentifier(), "toset", worldName, this.type.ordinal());
				for (int i = permissions.size() - 1; i >= 0; i--) { // insert in reverse order
					if (!includedPerms.contains(permissions.get(i))) {
						statement.setString(2, permissions.get(i));
						statement.addBatch();
						includedPerms.add(permissions.get(i));
					}
				}
				statement.executeBatch();
			}
		} catch (SQLException | IOException e) {
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
			ResultSet res = conn.prepAndBind("entity.permissions.get_all", getIdentifier(), type.ordinal()).executeQuery();
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
		} catch (SQLException | IOException e) {
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
			ResultSet res = conn.prepAndBind("entity.worlds.permissions", getIdentifier(), type.ordinal()).executeQuery();
			while (res.next()) {
				worlds.add(res.getString("world"));
			}
			res = conn.prepAndBind("entity.worlds.inheritance", getIdentifier(), type.ordinal()).executeQuery();
			while (res.next()) {
				worlds.add(res.getString("world"));
			}
			worlds.remove("");

			return Collections.unmodifiableSet(worlds);
		} catch (SQLException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getOption(String option, String worldName) {
		try (SQLConnection conn = backend.getSQL()) {
			ResultSet res = conn.prepAndBind("entity.options.get", getIdentifier(), this.type.ordinal(), option, worldName == null ? "" : worldName).executeQuery();
			if (res.next()) {
				return res.getString("value");
			}
		} catch (SQLException | IOException e) {
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

		try (SQLConnection conn = backend.getSQL()) {
			conn.prepAndBind("entity.options.delete", this.getIdentifier(), option, this.type.ordinal(), worldName).execute();
			if (value != null && !value.isEmpty()) {
				conn.prepAndBind("entity.options.add", getIdentifier(), this.type.ordinal(), option, worldName, value).execute();
			}
		} catch (SQLException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Map<String, String> getOptions(String worldName) {
		Map<String, String> options = new HashMap<>();

		try (SQLConnection conn = backend.getSQL()) {
			ResultSet set = conn.prepAndBind("entity.options.get_world", getIdentifier(), type.ordinal(), worldName == null ? "" : worldName).executeQuery();
			while (set.next()) {
				options.put(set.getString("permission"), set.getString("value"));
			}
		} catch (SQLException | IOException e) {
			throw new RuntimeException(e);
		}

		return Collections.unmodifiableMap(options);
	}

	@Override
	public Map<String, Map<String, String>> getOptionsMap() {
		Map<String, Map<String, String>> allOptions = new HashMap<>();

		try (SQLConnection conn = backend.getSQL()) {
			ResultSet res = conn.prepAndBind("entity.options.get_all", getIdentifier(), type.ordinal()).executeQuery();
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
		} catch (SQLException | IOException e) {
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
		if (this.isVirtual()) {
			this.updateInfo();
		}
	}

	@Override
	public void remove() {
		if (this.virtual.compareAndSet(false, true)) {
			try (SQLConnection conn = backend.getSQL()) {
				// clear inheritance info
				conn.prepAndBind("entity.delete.inheritance", this.getIdentifier(), this.type.ordinal()).execute();
				// clear permissions
				conn.prepAndBind("entity.delete.permissions", this.getIdentifier(), this.type.ordinal()).execute();
				// clear info
				conn.prepAndBind("entity.delete.entity", this.getIdentifier(), this.type.ordinal()).execute();
				backend.updateNameCache(this);
			} catch (SQLException | IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public Map<String, List<String>> getParentsMap() {
		Map<String, List<String>> ret = new HashMap<>();
		try (SQLConnection conn = backend.getSQL()) {
			ResultSet res = conn.prepAndBind("entity.parents.get_all", getIdentifier(), this.type.ordinal()).executeQuery();
			while (res.next()) {
				String world = res.getString("world");
				List<String> worldParents = ret.get(world);
				if (worldParents == null) {
					worldParents = new LinkedList<>();
					ret.put(world, worldParents);
				}
				worldParents.add(res.getString("parent"));
			}
		} catch (SQLException | IOException e) {
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
				res = conn.prepAndBind("entity.parents.get_world", getIdentifier(), type.ordinal(), worldName).executeQuery();
			}
			while (res.next()) {
				ret.add(res.getString("parent"));
			}
		} catch (SQLException | IOException e) {
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
				conn.prepAndBind("entity.parents.clear", this.getIdentifier(), this.type.ordinal(), worldName).execute();
			} else {
				conn.prepAndBind("DELETE FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = ? AND `world` IS NULL", this.getIdentifier(), this.type.ordinal()).execute();
			}

			PreparedStatement statement = conn.prepAndBind("entity.parents.add", this.getIdentifier(), "toset", this.type.ordinal(), worldName);
			for (int i = parents.size() - 1; i >= 0; --i) {
				final String group = parents.get(i);
				if (group == null || group.isEmpty()) {
					continue;
				}
				statement.setString(2, group);
				statement.addBatch();
			}
			statement.executeBatch();
		} catch (SQLException | IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void load() { // Nothing to load, we don't handle caching!
	}

	public Type getType() {
		return type;
	}

	public enum Type {
		GROUP, USER, WORLD
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
