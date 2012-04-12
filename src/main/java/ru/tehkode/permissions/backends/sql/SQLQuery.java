package ru.tehkode.permissions.backends.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface SQLQuery {

	public PreparedStatement getStatement();

	public void bindParam(int param, Object value) throws SQLException;

	public void bindParams(Object... params) throws SQLException;

	public SQLQuery execute() throws SQLException;
}
