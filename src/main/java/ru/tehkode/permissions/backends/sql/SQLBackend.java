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

import org.bukkit.configuration.Configuration;
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
import java.util.*;

/**
 * @author code
 */
public class SQLBackend extends PermissionBackend {

	protected Map<String, List<String>> worldInheritanceCache = new HashMap<String, List<String>>();
	private Map<String, Object> tableNames;
	private ThreadLocal<SQLConnection> conn;

	public SQLBackend(PermissionManager manager, Configuration config) {
		super(manager, config);
		final String dbUri = getConfig().getString("uri", "");
		final String dbUser = getConfig().getString("user", "");
		final String dbPassword = getConfig().getString("password", "");

		if (dbUri == null || dbUri.isEmpty()) {
			getConfig().set("uri", "mysql://localhost/exampledb");
			getConfig().set("user", "databaseuser");
			getConfig().set("password", "databasepassword");

		} else {
			conn = new ThreadLocal<SQLConnection>() {
				@Override
				public SQLConnection initialValue() {
					return new SQLConnection(dbUri, dbUser, dbPassword, SQLBackend.this);
				}
			};
		}
		this.setupAliases();
	}

	@Override
	public void validate() throws PermissionBackendException {
		if (conn == null) {
			throw new PermissionBackendException("SQL connection is not configured, see config.yml");
		}

		try {
			SQLConnection conn = getSQL(); // Test connection
			conn.checkConnection();
		} catch (Exception e) {
			if (e.getCause() != null && e.getCause() instanceof Exception) {
				e = (Exception) e.getCause();
			}
			throw new PermissionBackendException(e);
		}


		getManager().getLogger().info("Successfully connected to SQL database");

		this.deployTables();
	}

	public SQLConnection getSQL() {
		return conn.get();
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
			ResultSet res = getSQL().prepAndBind("SELECT `id` FROM `{permissions_backend}` where `type` = ? AND `name` = ?", SQLData.Type.USER.ordinal(), userName).executeQuery();
			return res.next();
		} catch (SQLException e) {
			return false;
		}
	}

	@Override
	public boolean hasGroup(String group) {
		try {
			ResultSet res = getSQL().prepAndBind("SELECT `id` FROM `{permissions_backend}` where `type` = ? AND `name` = ?", SQLData.Type.GROUP.ordinal(), group).executeQuery();
			return res.next();
		} catch (SQLException e) {
			return false;
		}
}

	@Override
	public Set<String> getDefaultGroupNames(String worldName) {
		try {
			ResultSet result;

			if (worldName == null) {
				result = getSQL().prepAndBind("SELECT `name` FROM `{permissions_entity}` WHERE `type` = ? AND `default` = 1", SQLData.Type.GROUP.ordinal()).executeQuery();
			} else {
				result = this.getSQL().prepAndBind("SELECT `name` FROM `{permissions}` WHERE `permission` = 'default' AND `value` = 'true' AND `type` = ? AND `world` = ?",
						SQLData.Type.GROUP.ordinal(), worldName).executeQuery();
			}
			Set<String> ret = new HashSet<String>();
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
		return SQLData.getEntitiesNames(getSQL(), SQLData.Type.GROUP, false);
	}

	@Override
	public Collection<String> getUserNames() {
		return SQLData.getEntitiesNames(getSQL(), SQLData.Type.USER, false);
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
		try {
			if (this.getSQL().hasTable("{permissions}")) {
				return;
			}
			InputStream databaseDumpStream = getClass().getResourceAsStream("/sql/" + getSQL().getDriver() + ".sql");

			if (databaseDumpStream == null) {
				throw new Exception("Can't find appropriate database dump for used database (" + getSQL().getDriver() + "). Is it bundled?");
			}

			getLogger().info("Deploying default database scheme");

			executeStream(getSQL(), databaseDumpStream);

			PermissionsGroupData defGroup = getGroupData("default");
			defGroup.setPermissions(Collections.singletonList("modifyworld.*"), null);
			defGroup.setDefault(true, null);
			defGroup.save();

			getLogger().info("Database scheme deploying complete.");

		} catch (Exception e) {
			throw new PermissionBackendException("Deploying of default data failed. Please initialize database manually using " + getSQL().getDriver() + ".sql", e);
		}
	}

	@Override
	public List<String> getWorldInheritance(String world) {
		if (world == null || world.isEmpty()) {
			return Collections.emptyList();
		}

		if (!worldInheritanceCache.containsKey(world)) {
			try {
				ResultSet result = this.getSQL().prepAndBind("SELECT `parent` FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = 2;", world).executeQuery();
				LinkedList<String> worldParents = new LinkedList<String>();

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
		try {
			ResultSet result = getSQL().prepAndBind("SELECT `child` FROM `{permissions_inheritance}` WHERE `type` = 2 ").executeQuery();

			Map<String, List<String>> ret = new HashMap<String, List<String>>();
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

		try {
			this.getSQL().prepAndBind("DELETE FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = 2", worldName).execute();

			PreparedStatement statement = this.getSQL().prepAndBind("INSERT INTO `{permissions_inheritance}` (`child`, `parent`, `type`) VALUES (?, ?, 2)", worldName, "toset");
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

	@Override
	public void reload() {
		worldInheritanceCache.clear();
	}
}
