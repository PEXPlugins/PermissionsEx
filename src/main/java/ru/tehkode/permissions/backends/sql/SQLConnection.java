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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.tehkode.utils.StringUtils;

public class SQLConnection {

	protected static Pattern placeholderPattern = Pattern.compile("\\`([^\\`]+)\\`");
	protected Connection db;
	protected String uri;
	protected String user;
	protected String password;
	protected String dbDriver;
	protected Map<String, String> aliases = new HashMap<String, String>();

	public SQLConnection(String uri, String user, String password, String dbDriver) {
		try {

			Class.forName(getDriverClass(dbDriver)).newInstance();

			this.uri = uri;
			this.user = user;
			this.password = password;

			this.connect();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setAlias(String tableName, String alias) {
		this.aliases.put(tableName, alias);
	}

	public String getAlias(String tableName) {
		if (this.aliases.containsKey(tableName)) {
			return this.aliases.get(tableName);
		}

		return tableName;
	}

	public ResultSet select(String sql, Object... params) throws SQLException {
		this.checkConnection();

		SQLSelectQuery query = new SQLSelectQuery(sql, params);

		query.execute();

		return query.getResults();
	}

	public <T> T selectSingle(String sql, T fallback, Object... params) {
		try {
			this.checkConnection();

			ResultSet result = this.select(sql, params);

			if (!result.next()) {
				return fallback;
			}

			return (T) result.getObject(1);

		} catch (SQLException e) {
			Logger.getLogger("Minecraft").severe("SQL Error: " + e.getMessage());
		}

		return fallback;
	}

	public void executeUpdate(String sql, Object... params) throws SQLException {
		this.checkConnection();

		SQLQuery query = new SQLQuery(sql, params);

		query.execute();
	}

	public void insert(String table, String[] fields, List<Object[]> rows) throws SQLException {
		this.checkConnection();

		String[] fieldPlaceholders = new String[fields.length];
		Arrays.fill(fieldPlaceholders, "?");
		String sql = "INSERT INTO `" + table + "` (`" + StringUtils.implode(fields, "`, `") + "`) VALUES (" + StringUtils.implode(fieldPlaceholders, ", ") + ");";

		SQLQuery query = new SQLQuery(sql);

		for (Object[] params : rows) {
			query.bindParams(params);
			query.execute();
		}
	}

	public boolean isTableExist(String tableName) {
		try {
			this.checkConnection();

			return this.db.getMetaData().getTables(null, null, this.getAlias(tableName), null).next();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isFieldExists(String tableName, String fieldName) {
		try {
			this.checkConnection();


			return this.db.getMetaData().getColumns(null, null, this.getAlias(tableName), fieldName).next();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	protected void checkConnection() throws SQLException {
		if (this.db.getClass().getName().startsWith("org.sqlite")) {
			return;
		}

		if (!this.db.isValid(3)) {
			Logger.getLogger("Minecraft").warning("Lost connection with sql server. Reconnecting.");
			this.connect();
		}
	}

	protected final void connect() throws SQLException {
		Logger.getLogger("Minecraft").info("[PermissionsEx-SQL] Connecting to database \"" + this.uri + "\"");
		db = DriverManager.getConnection("jdbc:" + uri, user, password);
	}

	protected static String getDriverClass(String alias) {

		if (alias.equals("mysql")) {
			alias = "com.mysql.jdbc.Driver";
		} else if (alias.equals("sqlite")) {
			alias = "org.sqlite.JDBC";
		} else if (alias.equals("postgre")) {
			alias = "org.postgresql.Driver";
		}

		return alias;
	}

	protected final String prepareQuery(String sql) {
		Matcher match = placeholderPattern.matcher(sql);

		while (match.find()) {
			sql = sql.replace(match.group(0), "`" + getAlias(match.group(1)) + "`");
		}

		return sql;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			db.close();
		} catch (SQLException e) {
			Logger.getLogger("Minecraft").log(Level.WARNING, "Error while disconnecting from database: {0}", e.getMessage());
		} finally {
			super.finalize();
		}
	}

	public class SQLQuery extends BasicSQLQuery {

		public SQLQuery(String sql, Object... params) throws SQLException {
			super(db.prepareStatement(prepareQuery(sql)));

			if (params != null) {
				bindParams(params);
			}
		}
	}

	public class SQLSelectQuery extends BasicSQLSelectQuery {

		public SQLSelectQuery(String sql, Object[] params) throws SQLException {
			super(db.prepareStatement(prepareQuery(sql)));

			if (params != null) {
				bindParams(params);
			}
		}
	}
}
