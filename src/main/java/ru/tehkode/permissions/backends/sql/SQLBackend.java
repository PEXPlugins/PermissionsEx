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

import com.google.common.collect.ImmutableSet;
import org.apache.commons.dbcp.BasicDataSource;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionsData;
import ru.tehkode.permissions.PermissionsGroupData;
import ru.tehkode.permissions.PermissionsUserData;
import ru.tehkode.permissions.backends.PermissionBackend;
import ru.tehkode.permissions.backends.SchemaUpdate;
import ru.tehkode.permissions.backends.caching.CachingGroupData;
import ru.tehkode.permissions.backends.caching.CachingUserData;
import ru.tehkode.permissions.exceptions.PermissionBackendException;
import ru.tehkode.utils.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
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
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author code
 */
public class SQLBackend extends PermissionBackend {
	protected Map<String, List<String>> worldInheritanceCache = new HashMap<>();
	private final AtomicReference<ImmutableSet<String>> userNamesCache = new AtomicReference<>(), groupNamesCache = new AtomicReference<>();
	private Map<String, Object> tableNames;
	private SQLQueryCache queryCache;
	private static final SQLQueryCache DEFAULT_QUERY_CACHE;

	static {
		try {
			DEFAULT_QUERY_CACHE = new SQLQueryCache(SQLBackend.class.getResourceAsStream("/sql/default/queries.properties"), null);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private BasicDataSource ds;
	protected final String dbDriver;

	public SQLBackend(PermissionManager manager, final ConfigurationSection config) throws PermissionBackendException {
		super(manager, config);
		final String dbUri = getConfig().getString("uri", "");
		final String dbUser = getConfig().getString("user", "");
		final String dbPassword = getConfig().getString("password", "");

		if (dbUri == null || dbUri.isEmpty()) {
			getConfig().set("uri", "mysql://localhost/exampledb");
			getConfig().set("user", "databaseuser");
			getConfig().set("password", "databasepassword");
			manager.getConfiguration().save();
			throw new PermissionBackendException("SQL connection is not configured, see config.yml");
		}
		dbDriver = dbUri.split(":", 2)[0];

		this.ds = new BasicDataSource();
		String driverClass = getDriverClass(dbDriver);
		if (driverClass != null) {
			this.ds.setDriverClassName(driverClass);
		}
		this.ds.setUrl("jdbc:" + dbUri);
		this.ds.setUsername(dbUser);
		this.ds.setPassword(dbPassword);
		// https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
		this.ds.setMaxActive((Runtime.getRuntime().availableProcessors() * 2) + 1);
		this.ds.setMaxWait(200); // 4 ticks
		this.ds.setValidationQuery("SELECT 1 AS dbcp_validate");
		this.ds.setTestOnBorrow(true);

		InputStream queryLocation = getClass().getResourceAsStream("/sql/" + dbDriver + "/queries.properties");
		if (queryLocation != null) {
			try {
				this.queryCache = new SQLQueryCache(queryLocation, DEFAULT_QUERY_CACHE);
			} catch (IOException e) {
				throw new PermissionBackendException("Unable to access database-specific queries", e);
			}
		} else {
			this.queryCache = DEFAULT_QUERY_CACHE;
		}
		try (SQLConnection conn = getSQL()) {
			conn.checkConnection();
		} catch (Exception e) {
			if (e.getCause() != null && e.getCause() instanceof Exception) {
				e = (Exception) e.getCause();
			}
			throw new PermissionBackendException("Unable to connect to SQL database", e);
		}

		getManager().getLogger().info("Successfully connected to SQL database");

		addSchemaUpdate(new SchemaUpdate(2) {
			@Override
			public void performUpdate() throws PermissionBackendException {
				// Change encoding for all columns to utf8mb4
				// Change collation for all columns to utf8mb4_general_ci
				try (SQLConnection conn = getSQL()) {
					conn.prep("ALTER TABLE `{permissions}` DROP KEY `unique`, MODIFY COLUMN `permission` TEXT NOT NULL").execute();
				} catch (SQLException | IOException e) {
					throw new PermissionBackendException(e);
				}
			}
		});
		addSchemaUpdate(new SchemaUpdate(1) {
			@Override
			public void performUpdate() throws PermissionBackendException {
				try (SQLConnection conn = getSQL()) {
					PreparedStatement updateStmt = conn.prep("entity.options.add");
					ResultSet res = conn.prepAndBind("SELECT `name`, `type` FROM `{permissions_entity}` WHERE `default`='1'").executeQuery();
					while (res.next()) {
							conn.bind(updateStmt, res.getString("name"), res.getInt("type"), "default", "", "true");
							updateStmt.addBatch();
					}
					updateStmt.executeBatch();

					// Update tables
					conn.prep("ALTER TABLE `{permissions_entity}` DROP COLUMN `default`").execute();
				} catch (SQLException | IOException e) {
					throw new PermissionBackendException(e);
				}
			}
		});
		addSchemaUpdate(new SchemaUpdate(0) {
			@Override
			public void performUpdate() throws PermissionBackendException {
				try (SQLConnection conn = getSQL()) {
					// TODO: Table modifications not supported in SQLite
					// Prefix/sufix -> options
					PreparedStatement updateStmt = conn.prep("entity.options.add");
					ResultSet res = conn.prepAndBind("SELECT `name`, `type`, `prefix`, `suffix` FROM `{permissions_entity}` WHERE LENGTH(`prefix`)>0 OR LENGTH(`suffix`)>0").executeQuery();
					while (res.next()) {
						String prefix = res.getString("prefix");
						if (!prefix.isEmpty() && !prefix.equals("null")) {
							conn.bind(updateStmt, res.getString("name"), res.getInt("type"), "prefix", "", prefix);
							updateStmt.addBatch();
						}
						String suffix = res.getString("suffix");
						if (!suffix.isEmpty() && !suffix.equals("null")) {
							conn.bind(updateStmt, res.getString("name"), res.getInt("type"), "suffix", "", suffix);
							updateStmt.addBatch();
						}
					}
					updateStmt.executeBatch();

					// Data type corrections

					// Update tables
					conn.prep("ALTER TABLE `{permissions_entity}` DROP KEY `name`").execute();
					conn.prep("ALTER TABLE `{permissions_entity}` DROP COLUMN `prefix`, DROP COLUMN `suffix`").execute();
					conn.prep("ALTER TABLE `{permissions_entity}` ADD CONSTRAINT UNIQUE KEY `name` (`name`, `type`)").execute();

					conn.prep("ALTER TABLE `{permissions}` DROP KEY `unique`").execute();
					conn.prep("ALTER TABLE `{permissions}` ADD CONSTRAINT UNIQUE `unique` (`name`,`permission`,`world`,`type`)").execute();
				} catch (SQLException | IOException e) {
					throw new PermissionBackendException(e);
				}
			}
		});
		this.setupAliases();
		this.deployTables();
		performSchemaUpdate();

		try (SQLConnection conn = getSQL()) {
			conn.prep("ALTER TABLE `{permissions}` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci").execute();
			conn.prep("ALTER TABLE `{permissions_entity}` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci").execute();
			conn.prep("ALTER TABLE `{permissions_inheritance}` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci").execute();
		} catch (SQLException | IOException e) {
			// Ignore, this MySQL version just doesn't support it.
		}
	}

	@Override
	public int getSchemaVersion() {
		try (SQLConnection conn = getSQL()) {
			ResultSet res = conn.prepAndBind("entity.options.get", "system", SQLData.Type.WORLD.ordinal(), "schema_version", "").executeQuery();
			if (!res.next()) {
				return -1;
			}
			return res.getInt("value");
		} catch (SQLException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void setSchemaVersion(int version) {
		try (SQLConnection conn = getSQL()) {
			conn.prepAndBind("entity.options.delete", "system", "schema_version", SQLData.Type.WORLD.ordinal(), "").execute();
			conn.prepAndBind("entity.options.add", "system", SQLData.Type.WORLD.ordinal(), "schema_version", "", version).execute();
		} catch (SQLException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	SQLQueryCache getQueryCache() {
		return queryCache;
	}

	protected static String getDriverClass(String alias) {
		if (alias.equals("mysql")) {
			return "com.mysql.jdbc.Driver";
		} else if (alias.equals("sqlite")) {
			return "org.sqlite.JDBC";
		} else if (alias.matches("postgres?")) {
			return "org.postgresql.Driver";
		}
		return null;
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
		CachingUserData data = new CachingUserData(new SQLData(name, SQLData.Type.USER, this), getExecutor(), new Object());
		updateNameCache(userNamesCache, data);
		return data;
	}

	@Override
	public PermissionsGroupData getGroupData(String name) {
		CachingGroupData data = new CachingGroupData(new SQLData(name, SQLData.Type.GROUP, this), getExecutor(), new Object());
		updateNameCache(groupNamesCache, data);
		return data;
	}

	/**
	 * Update the cache of names for a newly created data object, if necessary.
	 *
	 * @param list The pointer to current cache state
	 * @param data The data to check for presence
	 */
	private void updateNameCache(AtomicReference<ImmutableSet<String>> list, PermissionsData data) {
		ImmutableSet<String> cache, newVal;
		do {
			newVal = cache = list.get();
			if (cache == null || (!cache.contains(data.getIdentifier()) && !data.isVirtual())) {
				newVal = null;
			}

		} while (!list.compareAndSet(cache, newVal));
	}

	/**
	 * Clear the names cache for the type of the provided data object
	 *
	 * @param data The data object that was updated making this necessary.
	 */
	void updateNameCache(SQLData data) {
		final AtomicReference<ImmutableSet<String>> ref;
		switch (data.getType()) {
			case USER:
				ref = userNamesCache;
				break;
			case GROUP:
				ref = groupNamesCache;
				break;
			default:
				return; // No cache for you
		}
		updateNameCache(ref, data);
	}

	/**
	 * Gets the names of known entities of the give type, returning cached values if possible
	 *
	 * @param cacheRef The cache reference to check
	 * @param type The type to get
	 * @return A set of known entity names
	 */
	private ImmutableSet<String> getEntityNames(AtomicReference<ImmutableSet<String>> cacheRef, SQLData.Type type) {
		while (true) {
			ImmutableSet<String> cache = cacheRef.get();
			if (cache != null) {
				return cache;
			} else {
				try (SQLConnection conn = getSQL()) {
					ImmutableSet<String> newCache = ImmutableSet.copyOf(SQLData.getEntitiesNames(conn, type, false));
					if (cacheRef.compareAndSet(null, newCache)) {
						return newCache;
					}
				} catch (SQLException | IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public boolean hasUser(String userName) {
		try (SQLConnection conn = getSQL()) {
			ResultSet res = conn.prepAndBind("entity.exists", userName, SQLData.Type.USER.ordinal()).executeQuery();
			return res.next();
		} catch (SQLException | IOException e) {
			return false;
		}
	}

	@Override
	public boolean hasGroup(String group) {
		try (SQLConnection conn = getSQL()) {
			ResultSet res = conn.prepAndBind("entity.exists", group, SQLData.Type.GROUP.ordinal()).executeQuery();
			return res.next();
		} catch (SQLException | IOException e) {
			return false;
		}
	}

	@Override
	public Collection<String> getGroupNames() {
		return getEntityNames(groupNamesCache, SQLData.Type.GROUP);
	}

	@Override
	public Collection<String> getUserIdentifiers() {
		return getEntityNames(userNamesCache, SQLData.Type.USER);
	}

	@Override
	public Collection<String> getUserNames() {
		// TODO: Look at implementing caching
		Set<String> ret = new HashSet<>();
		try (SQLConnection conn = getSQL()) {
			ResultSet set = conn.prepAndBind("SELECT `value` FROM `{permissions}` WHERE `type` = ? AND `permission` = 'name' AND `value` IS NOT NULL", SQLData.Type.USER.ordinal()).executeQuery();
			while (set.next()) {
				ret.add(set.getString("value"));
			}
		} catch (SQLException | IOException e) {
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
			if (conn.hasTable("{permissions}") && conn.hasTable("{permissions_entity}") && conn.hasTable("{permissions_inheritance}")) {
				return;
			}
			InputStream databaseDumpStream = getClass().getResourceAsStream("/sql/" + dbDriver + "/deploy.sql");

			if (databaseDumpStream == null) {
				throw new Exception("Can't find appropriate database dump for used database (" + dbDriver + "). Is it bundled?");
			}

			getLogger().info("Deploying default database scheme");
			executeStream(conn, databaseDumpStream);
			setSchemaVersion(getLatestSchemaVersion());
		} catch (Exception e) {
			throw new PermissionBackendException("Deploying of default data failed. Please initialize database manually using " + dbDriver + ".sql", e);
		}

		PermissionsGroupData defGroup = getGroupData("default");
		defGroup.setPermissions(Collections.singletonList("modifyworld.*"), null);
		defGroup.setOption("default", "true", null);
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
				ResultSet result = conn.prepAndBind("SELECT `parent` FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = ?;", world, SQLData.Type.WORLD.ordinal()).executeQuery();
				LinkedList<String> worldParents = new LinkedList<>();

				while (result.next()) {
					worldParents.add(result.getString("parent"));
				}

				this.worldInheritanceCache.put(world, Collections.unmodifiableList(worldParents));
			} catch (SQLException | IOException e) {
				throw new RuntimeException(e);
			}
		}

		return worldInheritanceCache.get(world);
	}

	@Override
	public Map<String, List<String>> getAllWorldInheritance() {
		try (SQLConnection conn = getSQL()) {
			ResultSet result = conn.prepAndBind("SELECT `child` FROM `{permissions_inheritance}` WHERE `type` = ?", SQLData.Type.WORLD.ordinal()).executeQuery();

			Map<String, List<String>> ret = new HashMap<>();
			while (result.next()) {
				final String world = result.getString("child");
				if (!ret.containsKey(world)) {
					ret.put(world, getWorldInheritance(world));
				}
			}
			return Collections.unmodifiableMap(ret);
		} catch (SQLException |IOException e) {
			return Collections.unmodifiableMap(worldInheritanceCache);
		}
	}

	@Override
	public void setWorldInheritance(String worldName, List<String> parentWorlds) {
		if (worldName == null || worldName.isEmpty()) {
			return;
		}

		try (SQLConnection conn = getSQL()) {
			conn.prepAndBind("DELETE FROM `{permissions_inheritance}` WHERE `child` = ? AND `type` = ?", worldName, SQLData.Type.WORLD.ordinal()).execute();

			PreparedStatement statement = conn.prepAndBind("INSERT INTO `{permissions_inheritance}` (`child`, `parent`, `type`) VALUES (?, ?, ?)", worldName, "toset", SQLData.Type.WORLD.ordinal());
			for (String parentWorld : parentWorlds) {
				statement.setString(2, parentWorld);
				statement.addBatch();
			}
			statement.executeBatch();

			this.worldInheritanceCache.put(worldName, parentWorlds);

		} catch (SQLException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void reload() {
		worldInheritanceCache.clear();
		userNamesCache.set(null);
		groupNamesCache.set(null);
	}

	@Override
	public void setPersistent(boolean persist) {}

	@Override
	public void writeContents(Writer writer) throws IOException {
		try (SQLConnection conn = getSQL()) {
			writeTable("permissions", conn, writer);
			writeTable("permissions_entity", conn, writer);
			writeTable("permissions_inheritance", conn, writer);
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}

	private void writeTable(String table, SQLConnection conn, Writer writer) throws IOException, SQLException {
		ResultSet res = conn.prep("SHOW CREATE TABLE `{" + table + "}`").executeQuery();
		if (!res.next()) {
			throw new IOException("No value for table create for table " + table);
		}
		writer.write(res.getString(2));
		writer.write(";\n");

		res = conn.prep("SELECT * FROM `{" + table + "}`").executeQuery();
		while (res.next()) {
			writer.write("INSERT INTO `{");
			writer.write(table);
			writer.write("}` VALUES (");

			for (int i = 1; i <= res.getMetaData().getColumnCount(); ++i) {
				String value = res.getString(i);
				Class<?> columnClazz;
				try {
					columnClazz = Class.forName(res.getMetaData().getColumnClassName(i));
				} catch (ClassNotFoundException e) {
					throw new IOException(e);
				}
				if (value == null) {
					value = "null";
				} else {
					if (String.class.equals(columnClazz)) {
						value = "'" + value + "'";
					}
				}
				writer.write(value);
				if (i == res.getMetaData().getColumnCount()) { // Last column
					writer.write(");\n");
				} else {
					writer.write(", ");
				}
			}
		}
		writer.write('\n');
	}

	@Override
	public void close() throws PermissionBackendException {
		super.close();
		if (ds != null) {
			try {
				ds.close();
			} catch (SQLException e) {
				throw new PermissionBackendException("Error while closing", e);
			}
		}
	}

}
