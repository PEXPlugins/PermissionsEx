package ru.tehkode.permissions.backends.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class BasicSQLQuery implements SQLQuery {

	protected PreparedStatement stmt;

	public BasicSQLQuery(PreparedStatement stmt) {
		this.stmt = stmt;
	}

	@Override
	public PreparedStatement getStatement() {
		return stmt;
	}

	@Override
	public SQLQuery execute() throws SQLException {
		stmt.execute();

		return this;
	}

	@Override
	public final void bindParam(int param, Object value) throws SQLException {
		stmt.setObject(param, value);
	}

	@Override
	public final void bindParams(Object[] params) throws SQLException {
		for (int i = 1; i <= params.length; i++) {
			stmt.setObject(i, params[i - 1]);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			if (stmt != null) {
				stmt.close(); // This should close resultsets too
			}
		} finally {
			super.finalize();
		}
	}
}
