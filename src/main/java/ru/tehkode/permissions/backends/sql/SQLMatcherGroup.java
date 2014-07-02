package ru.tehkode.permissions.backends.sql;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ListenableFuture;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Matcher group that sources data from SQL
 */
public class SQLMatcherGroup extends MatcherGroup {
	private static enum State {
		VALID, UPDATING, INVALID
	}
	private final SQLBackend backend;
	private final String name;
	private final int entityId;
	private final AtomicReference<State> valid = new AtomicReference<>(State.VALID);
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
	public ListenableFuture<MatcherGroup> setQualifiers(final Multimap<Qualifier, String> qualifiers) {
		return backend.execute(new Callable<MatcherGroup>() {
			@Override
			public MatcherGroup call() throws Exception {
				if (valid.compareAndSet(State.VALID, State.UPDATING)) {
					try (SQLConnection conn = backend.getSQL()) {
						conn.beginTransaction();
						try {
							conn.prepAndBind("qualifiers.clear").execute();
							PreparedStatement stmt = conn.prepAndBind("qualifiers.add", entityId, "", "");
							for (Map.Entry<Qualifier, String> entry : qualifiers.entries()) {
								stmt.setString(2, entry.getKey().getName());
								stmt.setString(3, entry.getValue());
								stmt.addBatch();
							}
							stmt.executeBatch();
						} finally {
							conn.endTransaction();
						}
						backend.resetMatcherGroup(getEntityId());
						return backend.getMatcherGroup(getName(), getEntityId());
					}
				} else {
					throw new InvalidGroupException();
				}
			}
		});
	}

	@Override
	public Map<String, String> getEntries() {
		return entries;
	}

	@Override
	public List<String> getEntriesList() {
		// TODO Implement more correctly
		return ImmutableList.copyOf(entries.keySet());
	}

	@Override
	public ListenableFuture<MatcherGroup> setEntries(final Map<String, String> value) {
		return backend.execute(new Callable<MatcherGroup>() {
			@Override
			public MatcherGroup call() throws Exception {
				if (valid.compareAndSet(State.VALID, State.UPDATING)) {
					try (SQLConnection conn = backend.getSQL()) {
						conn.beginTransaction();
						try {
							conn.prepAndBind("entries.clear").execute();
							PreparedStatement stmt = conn.prepAndBind("entries.add", entityId, "", "");
							for (Map.Entry<String, String> entry : value.entrySet()) {
								stmt.setString(2, entry.getKey());
								stmt.setString(3, entry.getValue());
								stmt.addBatch();
							}
							stmt.executeBatch();
						} finally {
							conn.endTransaction();
						}
						backend.resetMatcherGroup(getEntityId());
						return backend.getMatcherGroup(getName(), getEntityId());
					}
				} else {
					throw new InvalidGroupException();
				}
			}
		});
	}

	@Override
	public ListenableFuture<MatcherGroup> setEntries(final List<String> value) {
		return backend.execute(new Callable<MatcherGroup>() {
			@Override
			public MatcherGroup call() throws Exception {
				if (valid.compareAndSet(State.VALID, State.UPDATING)) {
					try (SQLConnection conn = backend.getSQL()) {
						conn.beginTransaction();
						try {
							conn.prepAndBind("entries.clear").execute();
							PreparedStatement stmt = conn.prepAndBind("entries.add", entityId, "", null);
							for (String entry : value) {
								stmt.setString(2, entry);
								stmt.addBatch();
							}
							stmt.executeBatch();
						} finally {
							conn.endTransaction();
						}
						backend.resetMatcherGroup(getEntityId());
						return backend.getMatcherGroup(getName(), getEntityId());
					}
				} else {
					throw new InvalidGroupException();
				}
			}
		});
	}

	void invalidate() {
		valid.set(State.INVALID);
	}

	@Override
	public boolean isValid() {
		return valid.get() != State.INVALID;
	}

	@Override
	public ListenableFuture<Boolean> remove() {
		return backend.execute(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				if (valid.compareAndSet(State.VALID, State.UPDATING)) {
					try (SQLConnection conn = backend.getSQL()) {
						conn.prepAndBind("groups.delete", getEntityId()).execute();
					} catch (SQLException | IOException e) {
						valid.set(State.VALID);
						throw e;
					}
					backend.resetMatcherGroup(entityId);
					return true;
				}
				return false;
			}
		});
	}
}
