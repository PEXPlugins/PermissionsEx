package ru.tehkode.permissions.backends.sql;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Matcher group that sources data from SQL
 */
public class SQLMatcherGroup extends MatcherGroup {
	private final SQLBackend backend;
	private final String name;
	private final int entityId;
	private final AtomicBoolean valid = new AtomicBoolean(true);
	private final Map<String, String> entries;
	private final Multimap<Qualifier, String> qualifiers;

	public SQLMatcherGroup(SQLBackend backend, String name, int entityId) throws SQLException, IOException {
		this.backend = backend;
		this.name = name;
		this.entityId = entityId;
		try (SQLConnection conn = backend.getSQL()) {
			// Entries
			ImmutableMap.Builder<String, String> entries = ImmutableMap.builder();
			ResultSet res = conn.prepAndBind("qualifiers.get", entityId).executeQuery();
			while (res.next()) {
				entries.put(res.getString(1), res.getString(2));
			}
			this.entries = entries.build();

			// Queries
			ImmutableMultimap.Builder<Qualifier, String> qualifiers = ImmutableMultimap.builder();
			res = conn.prepAndBind("qualifiers.get", entityId).executeQuery();
			while (res.next()) {
				qualifiers.put(Qualifier.fromString(res.getString(1)), res.getString(2));
			}
			this.qualifiers = qualifiers.build();
		}
	}
	@Override
	public String getName() {
		return name;
	}

	public int getEntityId() {
		return entityId;
	}

	@Override
	public Multimap<Qualifier, String> getQualifiers() {
		return qualifiers;
	}

	@Override
	public MatcherGroup setQualifiers(Multimap<Qualifier, String> qualifiers) {
		try (SQLConnection conn = backend.getSQL()) {
			conn.prepAndBind("qualifiers.clear").execute();
			PreparedStatement stmt = conn.prepAndBind("qualifiers.add", entityId, "", "");
			for (Map.Entry<Qualifier, String> entry : qualifiers.entries()) {
				stmt.setString(2, entry.getKey().getName());
				stmt.setString(3, entry.getValue());
				stmt.addBatch();
			}
			stmt.executeBatch();
			backend.resetMatcherGroup(getEntityId());
			return backend.getMatcherGroup(getName(), getEntityId());
		} catch (SQLException | IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Map<String, String> getEntries() {
		return entries;
	}

	@Override
	public MatcherGroup setEntries(Map<String, String> value) {
		try (SQLConnection conn = backend.getSQL()) {
			conn.prepAndBind("entries.clear").execute();
			PreparedStatement stmt = conn.prepAndBind("entries.add", entityId, "", "");
			for (Map.Entry<String, String> entry : value.entrySet()) {
				stmt.setString(2, entry.getKey());
				stmt.setString(3, entry.getValue());
				stmt.addBatch();
			}
			stmt.executeBatch();
			backend.resetMatcherGroup(getEntityId());
			return backend.getMatcherGroup(getName(), getEntityId());
		} catch (SQLException | IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	void invalidate() {
		valid.set(false);
	}

	@Override
	public boolean isValid() {
		return valid.get();
	}

	@Override
	public boolean remove() {
		if (valid.compareAndSet(true, false)) {
			try (SQLConnection conn = backend.getSQL()) {
				conn.prepAndBind("groups.delete", getEntityId()).execute();
			} catch (SQLException | IOException e) {
				e.printStackTrace();
			}
			backend.resetMatcherGroup(this.entityId);
			return true;
		}
		return false;
	}
}
