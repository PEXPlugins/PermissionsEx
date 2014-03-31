/*
 * PermissionsEx - Permissions plugin for Bukkit
 * Copyright (C) 2011 t3hk0d3 http://www.tehkode.ru
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package ru.tehkode.permissions.backends.sql;

import org.apache.commons.dbcp2.BasicDataSource;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionsGroupData;
import ru.tehkode.permissions.PermissionsUserData;
import ru.tehkode.permissions.backends.PermissionBackend;
import ru.tehkode.permissions.exceptions.PermissionBackendException;
import ru.tehkode.utils.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author code
 */
public class SQLBackend extends PermissionBackend {
	protected Map<String, List<String>> worldInheritanceCache = new HashMap<>();
	private Map<String, Object> tableNames;
	private BasicDataSource ds;
	private String dbDriver;

	public SQLBackend(PermissionManager manager, ConfigurationSection config) throws PermissionBackendException {
		super(manager, config);
		final String dbUri = getConfig().getString("uri", "");
		final String dbUser = getConfig().getString("user", "");
		final String dbPassword = getConfig().getString("password", "");

		if (dbUri == null || dbUri.isEmpty()) {
			getConfig().set("uri", "mysql://localhost/exampledb");
			getConfig().set("user", "databaseuser");
			getConfig().set("password", "databasepassword");
			throw new PermissionBackendException("SQL connection is not configured, see config.yml");
		}
		dbDriver = dbUri.split(":", 2)[0];

		this.ds = new BasicDataSource();
		this.ds.setUrl("jdbc:" + dbUri);
		this.ds.setUsername(dbUser);
		this.ds.setPassword(dbPassword);
		this.ds.setMaxTotal(20);
		this.ds.setMaxWaitMillis(200); // 4 ticks

		try (SQLConnection conn = getSQL()) {
			conn.checkConnection();
		} catch (Exception e) {
			if (e.getCause() != null && e.getCause() instanceof Exception) {
				e = (Exception) e.getCause();
			}
			throw new PermissionBackendException("Unable to connect to SQL database", e);
		}

		getManager().getLogger().info("Successfully connected to SQL database");

		this.setupAliases();
		this.deployTables();
	}

	public SQLConnection getSQL() throws SQLException {
		if (ds == null) {
			throw new SQLException("SQL connection information was not correct, could not retrieve connection");
		}
		return new SQLConnection(ds.getConnection(), this);
	}

	public String getTableName(String identifier) {
		Map<String, Object> tableNames = this.tableNames;
		if (tableNames == null) {
			return identifier;
		}

		Object ret = tableNames.get(identifier);
		if (ret == null) {
			return identifier;
		}
		return ret.toString();
	}

	@Override
	public PermissionsUserData getUserData(String name) {
		return new SQLData(name, SQLData.Type.USER, this);
	}

	@Override
	public PermissionsGroupData getGroupData(String name) {
		return new SQLData(name, SQLData.Type.GROUP, this);
	}

	@Override
	public boolean hasUser(String userName) {
		try {
			ResultSet res = getSQL().prepAndBind("SELECT `id` FROM `{permissions_entity}` WHERE `type` = ? AND `name` = ?", SQLData.Type.USER.ordinal(), userName).executeQuery();
			return res.next();
		} catch (SQLException e) {
			return false;
		}
	}

	@Override
	public boolean hasGroup(String group) {
		try (SQLConnection conn = getSQL()) {
			ResultSet res = conn.prepAndBind("SELECT `id` FROM `{permissions_entity}` WHERE `type` = ? AND `name` = ?", SQLData.Type.GROUP.ordinal(), group).executeQuery();
			return res.next();
		} catch (SQLException e) {
			return false;
		}
	}

	@Override
	public Set<String> getDefaultGroupNames(String worldName) {
		try (SQLConnection conn = getSQL()) {
			ResultSet result;

			if (worldName == null) {
				result = conn.prepAndBind("SELECT `name` FROM `{permissions_entity}` WHERE `type` = ? AND `default` = 1", SQLData.Type.GROUP.ordinal()).executeQuery();
			} else {
				result = conn.prepAndBind("SELECT `name` FROM `{permissions}` WHERE `permission` = 'default' AND `value` = 'true' AND `type` = ? AND `world` = ?",
						SQLData.Type.GROUP.ordinal(), worldName).executeQuery();
			}
			Set<String> ret = new HashSet<>();
			while (result.next()) {
				ret.add(result.getString("name"));
			}

			return ret;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public Collection<String> getGroupNames() {
		try (SQLConnection conn = getSQL()) {
			return SQLData.getEntitiesNames(conn, SQLData.Type.GROUP, false);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Collection<String> getUserIdentifiers() {
		try (SQLConnection conn = getSQL()) {
			return SQLData.getEntitiesNames(conn, SQLData.Type.USER, false);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Collection<String> getUserNames() {
		Set<String> ret = new HashSet<>();
		try (SQLConnection conn = getSQL()) {
			ResultSet set = conn.prepAndBind("SELECT `value` FROM `{permissions}` WHERE `type` = ? AND `name` = 'name' AND `value` IS NOT NULL", SQLData.Type.USER.ordinal()).executeQuery();
			while (set.next()) {
				ret.add(set.getString("value"));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return Collections.unmodifiableSet(ret);
	}

	protected final void setupAliases() {
		ConfigurationSection aliases = getConfig().getConfigurationSection("aliases");

		if (aliases == null) {
			return;
		}

		tableNames = aliases.getValues(false);
	}

	private void executeStream(SQLConnection conn, InputStream str) throws SQLException, IOException {
		String deploySQL = StringUtils.readStream(str);


		Statement s = conn.getStatement();

		for (String sqlQuery : deploySQL.trim().split(";")) {
			sqlQuery = sqlQuery.trim();
			if (sqlQuery.isEmpty()) {
				continue;
			}

			sqlQuery = conn.expandQuery(sqlQuery + ";");

			s.addBatch(sqlQuery);
		}
		s.executeBatch();
	}

	protected final void deployTables() throws PermissionBackendException {
		try (SQLConnection conn = getSQL()) {
			if (conn.hasTable("{permissions}")) {
				return;
			}
			InputStream databaseDumpStream = getClass().getResourceAsStream("/sql/" + dbDriver + ".sql");

			if (databaseDumpStream == null) {
				throw new Exception("Can't find appropriate database dump for used database (" + dbDriver + "). Is it bundled?");
			}

			getLogger().info("Deploying default database scheme");
			executeStream(conn, databaseDumpStream);
		} catch (Exception e) {
			throw new PermissionBackendException("Deploying of default data failed. Please initialize database manually using " + dbDriver + ".sql", e);
		}

		PermissionsGroupData defGroup = getGroupData("default");
		defGroup.setPermissions(Collections.singletonList("modifyworld.*"), null);
		defGroup.setDefault(true, null);
		defGroup.save();

		getLogger().info("Database scheme deploying complete.");
	}

	@Override
	public List<String> getWorldInheritance(String world) {
		if (world == null || world.isEmpty()) {
			return Collections.emptyList();
		}

		if (!worldInheritanceCache.containsKey(world)) {
			try (SQLConnection conn = getSQL()) {
				ResultSet result = conn.prepAndBind("SELECT `parent` FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = 2;", world).executeQuery();
				LinkedList<String> worldParents = new LinkedList<>();

				while (result.next()) {
					worldParents.add(result.getString("parent"));
				}

				this.worldInheritanceCache.put(world, Collections.unmodifiableList(worldParents));
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		return worldInheritanceCache.get(world);
	}

	@Override
	public Map<String, List<String>> getAllWorldInheritance() {
		try (SQLConnection conn = getSQL()) {
			ResultSet result = conn.prepAndBind("SELECT `child` FROM `{permissions_inheritance}` WHERE `type` = 2 ").executeQuery();

			Map<String, List<String>> ret = new HashMap<>();
			while (result.next()) {
				final String world = result.getString("child");
				if (!ret.containsKey(world)) {
					ret.put(world, getWorldInheritance(world));
				}
			}
			return Collections.unmodifiableMap(ret);
		} catch (SQLException e) {
			return Collections.unmodifiableMap(worldInheritanceCache);
		}
	}

	@Override
	public void setWorldInheritance(String worldName, List<String> parentWorlds) {
		if (worldName == null || worldName.isEmpty()) {
			return;
		}

		try (SQLConnection conn = getSQL()) {
			conn.prepAndBind("DELETE FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = 2", worldName).execute();

			PreparedStatement statement = conn.prepAndBind("INSERT INTO `{permissions_inheritance}` (`child`, `parent`, `type`) VALUES (?, ?, 2)", worldName, "toset");
			for (String parentWorld : parentWorlds) {
				statement.setString(2, parentWorld);
				statement.addBatch();
			}
			statement.executeBatch();

			this.worldInheritanceCache.put(worldName, parentWorlds);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void reload() {
		worldInheritanceCache.clear();
	}

	@Override
	public void close() throws PermissionBackendException {
		if (ds != null) {
			try {
				ds.close();
			} catch (SQLException e) {
				throw new PermissionBackendException("Error while closing", e);
			}
		}
	}
}
