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

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One connection per thread, don't share your connections
 */
public class SQLConnection implements Closeable {
	private static final Pattern TABLE_PATTERN = Pattern.compile("\\{([^}]+)\\}");
	private Statement statement;
	private final Connection db;
	private final SQLBackend backend;

	public SQLConnection(Connection conn, SQLBackend backend) {
		this.backend = backend;
		this.db = conn;
	}

	public PreparedStatement prep(String query) throws SQLException {
		return this.db.prepareStatement(expandQuery(query));
	}

	/**
	 * Perform table name expansion on a query
	 * Example: <pre>SELECT * FROM `{permissions}`;</pre>
	 * @param query the query to get
	 * @return The expanded query
	 */
	public String expandQuery(String query) {
		String newQuery = backend.getQueryCache().getQuery(query);
		if (newQuery != null) {
			query = newQuery;
		}
		StringBuffer ret = new StringBuffer();
		Matcher m = TABLE_PATTERN.matcher(query);
		while (m.find()) {
			m.appendReplacement(ret, this.backend.getTableName(m.group(1)));
		}
		m.appendTail(ret);
		return ret.toString();
	}

	public Statement getStatement() throws SQLException {
		this.checkConnection();
		if (this.statement == null) {
			this.statement = this.db.createStatement();
		}
		return this.statement;
	}

	public boolean hasTable(String table) throws SQLException {
		this.checkConnection();
		table = expandQuery(table);
		return db.getMetaData().getTables(null, null, table, null).next();
	}

	public PreparedStatement prepAndBind(String query, Object... args) throws SQLException {
		PreparedStatement statement = prep(query);
		bind(statement, (Object[]) args);
		return statement;
	}

	public PreparedStatement bind(PreparedStatement statement, Object... args) throws SQLException {
		statement.clearParameters();
		final int argsExpected = statement.getParameterMetaData().getParameterCount();
		if (args.length != argsExpected) {
			throw new SQLException("Invalid argument number provided; expected " + argsExpected + " but got " + args.length);
		}
		for (int i = 0; i < args.length; ++i) {
			statement.setObject(i + 1, args[i]);
		}
		return statement;
	}

	protected void checkConnection() throws SQLException {
		if (this.backend.dbDriver.equals("sqlite")) {
			return;
		}

		if (!this.db.isValid(3)) {
			throw new SQLException("Lost connection with database!");
		}
	}

	@Override
	public void close() throws IOException {
		try {
			db.close();
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			close();
		} catch (IOException e) {
			backend.getLogger().log(Level.WARNING, "Error while disconnecting from database: {0}", e.getMessage());
		} finally {
			super.finalize();
		}
	}
}
