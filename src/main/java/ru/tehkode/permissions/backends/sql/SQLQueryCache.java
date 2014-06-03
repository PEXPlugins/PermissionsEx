package ru.tehkode.permissions.backends.sql;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Multilevel string database for handling database type-specific queries
 */
public class SQLQueryCache {
	private SQLQueryCache parent;
	private final Properties queries;

	public SQLQueryCache(Properties props, SQLQueryCache parent) {
		this.queries = props;
		this.parent = parent;
	}

	public SQLQueryCache(InputStream is, SQLQueryCache parent) throws IOException {
		this.queries = new Properties();
		this.queries.load(is);
		this.parent = parent;
	}

	public String getQuery(String lookupKey) {
		String query = this.queries.getProperty(lookupKey);
		if (query == null) {
			return parent == null ? null : parent.getQuery(lookupKey);
		}
		return query;
	}
}
