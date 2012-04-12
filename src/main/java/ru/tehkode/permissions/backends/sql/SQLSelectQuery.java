package ru.tehkode.permissions.backends.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface SQLSelectQuery extends SQLQuery {
	
	public ResultSet getResults() throws SQLException;
	
	public boolean haveResults();
	
	@Override
	public SQLSelectQuery execute() throws SQLException;
		
}
