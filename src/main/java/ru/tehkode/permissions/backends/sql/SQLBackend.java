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

import com.google.common.collect.Multimap;
import org.apache.commons.dbcp.BasicDataSource;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.backends.PermissionBackend;
import ru.tehkode.permissions.backends.SchemaUpdate;
import ru.tehkode.permissions.callback.Callback;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author code
 */
public class SQLBackend extends PermissionBackend {
	private static final Pattern TABLE_PATTERN = Pattern.compile("\\{([^}]+)\\}");
	private static final SQLQueryCache DEFAULT_QUERY_CACHE;

	static {
		try {
			DEFAULT_QUERY_CACHE = new SQLQueryCache(SQLBackend.class.getResourceAsStream("/sql/default/queries.properties"), null);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private Map<String, Object> tableNames;
	private String tablePrefix;
	private SQLQueryCache queryCache;
	private final ConcurrentMap<Integer, SQLMatcherGroup> matcherCache = new ConcurrentHashMap<>();

	private BasicDataSource ds;
	protected final String dbDriver;

	public SQLBackend(PermissionManager manager, final ConfigurationSection config) throws PermissionBackendException {
		super(manager, config);
		final String dbUri = getConfig().getString("uri", "");
		final String dbUser = getConfig().getString("user", "");
		final String dbPassword = getConfig().getString("password", "");
		this.tablePrefix = getConfig().getString("prefix", "");

		if (dbUri == null || dbUri.isEmpty()) {
			getConfig().set("uri", "mysql://localhost/exampledb");
			getConfig().set("user", "databaseuser");
			getConfig().set("password", "databasepassword");
			getConfig().set("prefix", "pex");
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
		this.ds.setMaxActive(20);
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
				try (SQLConnection conn = getSQL()) {
					// The new tables have been deployed, just gotta move the old data over
					// Migrate everything over to matcher groups! So much fun :)
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
	}

	@Override
	public int getSchemaVersion() {
		try (SQLConnection conn = getSQL()) {
			ResultSet res = conn.prepAndBind("entity.options.get", "system", 2, "schema_version", "").executeQuery();
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
			conn.prepAndBind("entity.options.add", "system", 2, "schema_version", "", version).execute();
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

	/**
	 * Perform table name expansion on a query
	 * Example: <pre>SELECT * FROM `{permissions}`;</pre>
	 *
	 * @param query the query to get
	 * @return The expanded query
	 */
	public String expandQuery(String query) {
		String newQuery = getQueryCache().getQuery(query);
		if (newQuery != null) {
			query = newQuery;
		}
		StringBuffer ret = new StringBuffer();
		Matcher m = TABLE_PATTERN.matcher(query);
		while (m.find()) {
			m.appendReplacement(ret, getTableName(m.group(1)));
		}
		m.appendTail(ret);
		return ret.toString();
	}

	public String getTableName(String identifier) {
		if (identifier.startsWith("permissions")) { // Legacy tables
			Map<String, Object> tableNames = this.tableNames;
			if (tableNames == null) {
				return identifier;
			}

			Object ret = tableNames.get(identifier);
			if (ret == null) {
				return identifier;
			}
			return ret.toString();
		} else {
			return this.tablePrefix == null || this.tablePrefix.isEmpty() ? identifier : tablePrefix + "_" + identifier;
		}
	}

	@Override
	public Collection<String> getUserNames() {
		Set<String> ret = new HashSet<>();
		try (SQLConnection conn = getSQL()) {
			ResultSet set = conn.prepAndBind("SELECT `value` FROM `{permissions}` WHERE `type` = ? AND `name` = 'name' AND `value` IS NOT NULL", 1).executeQuery();
			while (set.next()) {
				ret.add(set.getString("value"));
			}
		} catch (SQLException | IOException e) {
			throw new RuntimeException(e);
		}
		return Collections.unmodifiableSet(ret);
	}

	SQLMatcherGroup getMatcherGroup(String name, int entityId) throws IOException, SQLException {
		while (true) {
			if (matcherCache.containsKey(entityId)) {
				SQLMatcherGroup ret = matcherCache.get(entityId);
				if (ret != null) {
					return ret;
				}
			}
			SQLMatcherGroup newGroup = new SQLMatcherGroup(this, name, entityId);
			SQLMatcherGroup oldGroup = matcherCache.put(entityId, newGroup);
			if (oldGroup != null) {
				oldGroup.invalidate();
			}
		}
	}

	void resetMatcherGroup(int entityId) {
		while (true) {
			SQLMatcherGroup ret = matcherCache.get(entityId);
			ret.invalidate();
			if (matcherCache.remove(entityId, ret)) {
				return;
			}
		}
	}

	@Override
	public Future<Iterator<MatcherGroup>> getAllMatcherGroups(Callback<Iterator<MatcherGroup>> callback) {
		return execute(new Callable<Iterator<MatcherGroup>>() {
			@Override
			public Iterator<MatcherGroup> call() throws Exception {
				try (SQLConnection conn = getSQL()) {
					List<MatcherGroup> ret = new LinkedList<>();
					ResultSet res = conn.prep("groups.get.all").executeQuery();
					while (res.next()) {
						ret.add(getMatcherGroup(res.getString("name"), res.getInt("id")));
					}
					return ret.iterator();
				}
			}
		}, callback);
	}

	@Override
	public Future<List<MatcherGroup>> getMatchingGroups(final String type, Callback<List<MatcherGroup>> callback) {
		return execute(new Callable<List<MatcherGroup>>() {
			@Override
			public List<MatcherGroup> call() throws Exception {
				try (SQLConnection conn = getSQL()) {
					List<MatcherGroup> ret = new LinkedList<>();
					ResultSet res = conn.prepAndBind("groups.get.name", type).executeQuery();
					while (res.next()) {
						ret.add(getMatcherGroup(type, res.getInt("id")));
					}
					return ret;
				}
			}
		}, callback);
	}

	@Override
	public Future<List<MatcherGroup>> getMatchingGroups(final String type, final Qualifier qual, final String qualValue, Callback<List<MatcherGroup>> callback) {
		return execute(new Callable<List<MatcherGroup>>() {
			@Override
			public List<MatcherGroup> call() throws Exception {
				try (SQLConnection conn = getSQL()) {
					List<MatcherGroup> ret = new LinkedList<>();
					ResultSet res = conn.prepAndBind("groups.get.name_qual", type, qual.getName(), qualValue).executeQuery();
					while (res.next()) {
						ret.add(getMatcherGroup(type, res.getInt(1)));
					}
					return ret;
				}
			}
		}, callback);
	}

	private int newEntity(SQLConnection conn, String type) throws SQLException {
		PreparedStatement stmt = conn.prepAndBind("groups.create", type);
		stmt.execute();
		ResultSet res = stmt.getGeneratedKeys();
		if (res.next()) {
			return res.getInt(1);
		} else {
			throw new SQLException("No generated ID returned when creating group!");
		}
	}

	@Override
	public Future<MatcherGroup> createMatcherGroup(final String type, final Map<String, String> entries, final Multimap<Qualifier, String> qualifiers, Callback<MatcherGroup> callback) {
		return execute(new Callable<MatcherGroup>() {
			@Override
			public MatcherGroup call() throws Exception {
				try (SQLConnection conn = getSQL()) {
					int entityId = newEntity(conn, type);
					PreparedStatement entriesAdd = conn.prepAndBind("entries.add", entityId, "", "");
					for (Map.Entry<String, String> entry : entries.entrySet()) {
						entriesAdd.setString(2, entry.getKey());
						entriesAdd.setString(3, entry.getValue());
						entriesAdd.addBatch();
					}
					entriesAdd.executeBatch();

					PreparedStatement ret = conn.prepAndBind("qualifiers.add", entityId, "", "");
					for (Map.Entry<Qualifier, String> entry : qualifiers.entries()) {
						ret.setString(2, entry.getKey().getName());
						ret.setString(3, entry.getValue());
						ret.addBatch();
					}
					ret.executeBatch();

					resetMatcherGroup(entityId); // Just in case
					return getMatcherGroup(type, entityId);
				}
			}
		}, callback);
	}

	@Override
	public Future<MatcherGroup> createMatcherGroup(final String type, final List<String> entries, final Multimap<Qualifier, String> qualifiers, Callback<MatcherGroup> callback) {
		return execute(new Callable<MatcherGroup>() {
			@Override
			public MatcherGroup call() throws Exception {
				try (SQLConnection conn = getSQL()) {
					int entityId = newEntity(conn, type);
					PreparedStatement entriesAdd = conn.prepAndBind("entries.add", entityId, "", null);
					for (String entry : entries) {
						entriesAdd.setString(2, entry);
						entriesAdd.addBatch();
					}
					entriesAdd.executeBatch();

					PreparedStatement ret = conn.prepAndBind("qualifiers.add", entityId, "", "");
					for (Map.Entry<Qualifier, String> entry : qualifiers.entries()) {
						ret.setString(2, entry.getKey().getName());
						ret.setString(3, entry.getValue());
						ret.addBatch();
					}
					ret.executeBatch();

					resetMatcherGroup(entityId);
					return getMatcherGroup(type, entityId);
				}
			}
		}, callback);
	}

	@Override
	public Future<Collection<String>> getAllValues(final Qualifier qualifier, Callback<Collection<String>> callback) {
		return execute(new Callable<Collection<String>>() {
			@Override
			public Collection<String> call() throws Exception {
				try (SQLConnection conn = getSQL()) {
					Set<String> ret = new HashSet<>();
					ResultSet res = conn.prepAndBind("qualifiers.all_values", qualifier.getName()).executeQuery();
					while (res.next()) {
						ret.add(res.getString(1));
					}
					return ret;
				}
			}
		}, callback);
	}

	@Override
	public Future<Boolean> hasAnyQualifier(final Qualifier qualifier, final String value, Callback<Boolean> callback) {
		return execute(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				try (SQLConnection conn = getSQL()) {
					ResultSet res = conn.prepAndBind("qualifiers.any_with_value", qualifier.getName(), value).executeQuery();
					return res.next();
				}
			}
		}, callback);
	}

	@Override
	public Future<Void> replaceQualifier(final Qualifier qualifier, final String old, final String newVal) {
		return execute(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				try (SQLConnection conn = getSQL()) {
					ResultSet res = conn.prepAndBind("qualifiers.replace", newVal, qualifier.getName(), old).executeQuery();
					while (res.next()) {
						resetMatcherGroup(res.getInt(1));
					}
				}
				return null;
			}
		}, null);

	}

	@Override
	public Future<List<MatcherGroup>> allWithQualifier(final Qualifier qualifier, Callback<List<MatcherGroup>> callback) {
		return execute(new Callable<List<MatcherGroup>>() {
			@Override
			public List<MatcherGroup> call() throws Exception {
				try (SQLConnection conn = getSQL()) {
					List<MatcherGroup> ret = new LinkedList<>();
					ResultSet res = conn.prepAndBind("qualifiers.any_with_key", qualifier.getName()).executeQuery();
					while (res.next()) {
						ret.add(getMatcherGroup(res.getString(1), res.getInt(2)));
					}
					return ret;
				}
			}
		}, callback);
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
			sqlQuery = expandQuery(sqlQuery + ";");
			s.addBatch(sqlQuery);
		}
		s.executeBatch();
	}

	protected final void deployTables() throws PermissionBackendException {
		try (SQLConnection conn = getSQL()) {
			if (conn.hasTable("{groups}") && conn.hasTable("{qualifiers}") && conn.hasTable("{entries}")) {
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

		/*PermissionsGroupData defGroup = getGroupData("default");
		defGroup.setPermissions(Collections.singletonList("modifyworld.*"), null);
		defGroup.setOption("default", "true", null);
		defGroup.save();*/

		getLogger().info("Database scheme deploying complete.");
	}

	public void reload() {
		while (!matcherCache.isEmpty()) {
			for (Iterator<SQLMatcherGroup> it = matcherCache.values().iterator(); it.hasNext(); ) {
				it.next().invalidate();
				it.remove();
			}
		}
		matcherCache.clear();
	}

	@Override
	public void setPersistent(boolean persist) {
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
		super.close();
	}

	/**
	 * Removes the given entity id from the cache.
	 *
	 * @param entityId The id to remove
	 */
	void uncache(int entityId) {
		matcherCache.remove(entityId);
	}
}
