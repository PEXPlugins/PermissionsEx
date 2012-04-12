package ru.tehkode.permissions.backends.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BasicSQLSelectQuery extends BasicSQLQuery implements SQLSelectQuery {

	protected ResultSet results = null;

	public BasicSQLSelectQuery(PreparedStatement stmt) {
		super(stmt);
	}

	public BasicSQLSelectQuery(PreparedStatement stmt, ResultSet results) {
		super(stmt);
		this.results = results;
	}

	@Override
	public ResultSet getResults() {
		return results;
	}

	@Override
	public boolean haveResults() {
		return results != null;
	}

	@Override
	public SQLSelectQuery execute() throws SQLException {
		this.results = stmt.executeQuery();
		
		return this;
	}
}
