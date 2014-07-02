package ru.tehkode.permissions.backends.sql;

import ru.tehkode.permissions.data.Qualifier;

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
 *
 * This class remains to ease conversion of entity data to the new format.
 */
@Deprecated
public class SQLData {
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
			conn.prepAndBind("legacy.entity.update", this.getIdentifier(), this.type.ordinal()).execute();
		} catch (SQLException | IOException e) {
			if (virtual.compareAndSet(true, false)) {
				this.updateInfo();
			}

			throw new RuntimeException(e);
		}

		this.virtual.set(false);
	}


	protected final void fetchInfo() {
		try (SQLConnection conn = backend.getSQL()) {
			ResultSet result = conn.prepAndBind("legacy.entity.fetch", this.getIdentifier(), this.type.ordinal()).executeQuery();

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

	public Map<String, List<String>> getPermissionsMap() {
		Map<String, List<String>> allPermissions = new HashMap<>();

		try (SQLConnection conn = backend.getSQL()) {
			ResultSet res = conn.prepAndBind("legacy.entity.permissions.get_all", getIdentifier(), type.ordinal()).executeQuery();
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

	public Set<String> getWorlds() {
		Set<String> worlds = new HashSet<>();
		try (SQLConnection conn = backend.getSQL()) {
			ResultSet res = conn.prepAndBind("legacy.entity.worlds.permissions", getIdentifier(), type.ordinal()).executeQuery();
			while (res.next()) {
				worlds.add(res.getString("world"));
			}
			res = conn.prepAndBind("legacy.entity.worlds.inheritance", getIdentifier(), type.ordinal()).executeQuery();
			while (res.next()) {
				worlds.add(res.getString("world"));
			}
			worlds.remove("");

			return Collections.unmodifiableSet(worlds);
		} catch (SQLException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Map<String, Map<String, String>> getOptionsMap() {
		Map<String, Map<String, String>> allOptions = new HashMap<>();

		try (SQLConnection conn = backend.getSQL()) {
			ResultSet res = conn.prepAndBind("legacy.entity.options.get_all", getIdentifier(), type.ordinal()).executeQuery();
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

	public boolean isVirtual() {
		return virtual.get();
	}

	public void save() {
		if (this.isVirtual()) {
			this.updateInfo();
		}
	}

	public void remove() {
		if (this.virtual.compareAndSet(false, true)) {
			try (SQLConnection conn = backend.getSQL()) {
				// clear inheritance info
				conn.prepAndBind("legacy.entity.delete.inheritance", this.getIdentifier(), this.type.ordinal()).execute();
				// clear permissions
				conn.prepAndBind("legacy.entity.delete.permissions", this.getIdentifier(), this.type.ordinal()).execute();
				// clear info
				conn.prepAndBind("legacy.entity.delete.entity", this.getIdentifier(), this.type.ordinal()).execute();
			} catch (SQLException | IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public Map<String, List<String>> getParentsMap() {
		Map<String, List<String>> ret = new HashMap<>();
		try (SQLConnection conn = backend.getSQL()) {
			ResultSet res = conn.prepAndBind("legacy.entity.parents.get_all", getIdentifier(), this.type.ordinal()).executeQuery();
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

	public enum Type {
		GROUP, USER, WORLD;

		public static Qualifier toQualifier(Type type) {
			switch (type) {
				case GROUP: return Qualifier.GROUP;
				case USER: return Qualifier.USER;
				case WORLD: return Qualifier.WORLD;
				default: throw new IllegalArgumentException("Unknown Type " + type);
			}
		}
	}

	public Qualifier getQualifier() {
		return Type.toQualifier(type);
	}
}
