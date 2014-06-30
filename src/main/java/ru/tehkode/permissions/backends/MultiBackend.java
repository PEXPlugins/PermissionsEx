package ru.tehkode.permissions.backends;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.data.Context;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;


/**
 * Backend containing multiple backends
 * Backend priority is first-come-first-serve -- whatever's listed first gets priority
 */
public class MultiBackend extends PermissionBackend {
	public static final Qualifier BACKEND = new Qualifier("backend") {
		@Override
		public boolean matches(Context context, String value) {
			return false;
		}
	};
	private final List<PermissionBackend> backends = new ArrayList<>();
	protected MultiBackend(PermissionManager manager, ConfigurationSection backendConfig) throws PermissionBackendException {
		super(manager, backendConfig, Executors.newSingleThreadExecutor());
		List<String> backendNames = backendConfig.getStringList("backends");
		if (backendNames.isEmpty()) {
			backendConfig.set("backends", new ArrayList<String>());
			throw new PermissionBackendException("No backends configured for multi backend! Please configure this!");
		}
		for (String name : backendConfig.getStringList("backends")) {
			PermissionBackend backend = manager.createBackend(name);
			backends.add(backend);
		}
	}

	@Override
	public int getSchemaVersion() {
		return -1;
	}

	@Override
	protected void setSchemaVersion(int version) {
		// no-op
	}

	@Override
	public void reload() throws PermissionBackendException {
		for (PermissionBackend backend : backends) {
			backend.reload();
		}
	}

	@Override
	public Collection<String> getUserNames() {
		Set<String> ret = new HashSet<>();
		for (PermissionBackend backend : backends) {
			ret.addAll(backend.getUserNames());
		}
		return Collections.unmodifiableSet(ret);
	}

	@Override
	public ListenableFuture<Iterator<MatcherGroup>> getAll() {
		List<ListenableFuture<Iterator<MatcherGroup>>> rawGroups = new ArrayList<>(backends.size());
		for (PermissionBackend backend : backends) {
			rawGroups.add(backend.getAll());
		}
		return Futures.transform(Futures.allAsList(rawGroups), new Function<List<Iterator<MatcherGroup>>, Iterator<MatcherGroup>>() {
			@Override
			public Iterator<MatcherGroup> apply(List<Iterator<MatcherGroup>> iterators) {
				return Iterators.concat(iterators.iterator());
			}
		});
	}
	private <T> List<T> concatList(List<List<T>> lists) {
		if (lists.isEmpty()) {
			return Collections.emptyList();
		}
		final List<T> ret = new ArrayList<>(lists.get(0).size() * lists.size());
		for (List<T> list : lists) {
			ret.addAll(list);
		}
		return ret;
	}

	@Override
	public ListenableFuture<List<MatcherGroup>> getMatchingGroups(String type) {
		final List<ListenableFuture<List<MatcherGroup>>> rawResults = new ArrayList<>(backends.size());
		for (PermissionBackend backend : backends) {
			rawResults.add(backend.getMatchingGroups(type));
		}
		return Futures.transform(Futures.allAsList(rawResults), new Function<List<List<MatcherGroup>>, List<MatcherGroup>>() {
			@Override
			public List<MatcherGroup> apply(List<List<MatcherGroup>> lists) {
				return concatList(lists);
			}
		});
	}

	@Override
	public ListenableFuture<List<MatcherGroup>> getMatchingGroups(String type, Qualifier qual, String qualValue) {
		final List<ListenableFuture<List<MatcherGroup>>> rawResults = new ArrayList<>(backends.size());
		for (PermissionBackend backend : backends) {
			rawResults.add(backend.getMatchingGroups(type, qual, qualValue));
		}
		return Futures.transform(Futures.allAsList(rawResults), new Function<List<List<MatcherGroup>>, List<MatcherGroup>>() {
			@Override
			public List<MatcherGroup> apply(List<List<MatcherGroup>> lists) {
				return concatList(lists);
			}
		});
	}


	@Override
	public ListenableFuture<MatcherGroup> createMatcherGroup(String type, Map<String, String> entries, Multimap<Qualifier, String> qualifiers) {
		return backends.get(0).createMatcherGroup(type, entries, qualifiers); // TODO: Add backend= qualifier
	}

	@Override
	public ListenableFuture<MatcherGroup> createMatcherGroup(String type, List<String> entries, Multimap<Qualifier, String> qualifiers) {
		return backends.get(0).createMatcherGroup(type, entries, qualifiers);
	}

	@Override
	public ListenableFuture<Collection<String>> getAllValues(Qualifier qualifier) {
		final List<ListenableFuture<Collection<String>>> rawResults = new ArrayList<>(backends.size());
		for (PermissionBackend backend : backends) {
			rawResults.add(backend.getAllValues(qualifier));
		}
		return Futures.transform(Futures.allAsList(rawResults), new Function<List<Collection<String>>, Collection<String>>() {
			@Override
			public Collection<String> apply(List<Collection<String>> lists) {
				Set<String> ret = new HashSet<>();
				for (Collection<String> raw : lists) {
					ret.addAll(raw);
				}
				return ret;
			}
		});
	}

	@Override
	public ListenableFuture<Boolean> hasAnyQualifier(Qualifier qualifier, String value) {
		List<ListenableFuture<Boolean>> results = new ArrayList<>(backends.size());
		for (PermissionBackend backend : backends) {
			results.add(backend.hasAnyQualifier(qualifier, value));
		}
		return Futures.transform(Futures.allAsList(results), new Function<List<Boolean>, Boolean>() {
			@Override
			public Boolean apply(List<Boolean> booleans) {
				for (Boolean bool : booleans) {
					if (bool) {
						return true;
					}
				}
				return false;
			}
		});
	}

	@Override
	public ListenableFuture<Void> replaceQualifier(Qualifier qualifier, String old, String newVal) {
		List<ListenableFuture<Void>> join = new ArrayList<>(backends.size());
		for (PermissionBackend backend : backends) {
			join.add(backend.replaceQualifier(qualifier, old, newVal));
		}
		return Futures.transform(Futures.allAsList(join), new Function<List<Void>, Void>() {
			@Override
			public Void apply(List<Void> voids) {
				return null;
			}
		});
	}

	@Override
	public ListenableFuture<List<MatcherGroup>> allWithQualifier(Qualifier qualifier) {
		List<ListenableFuture<List<MatcherGroup>>> ret = new ArrayList<>(backends.size());
		for (PermissionBackend backend : backends) {
			ret.add(backend.allWithQualifier(qualifier));
		}

		return Futures.transform(Futures.allAsList(ret), new Function<List<List<MatcherGroup>>, List<MatcherGroup>>() {
			@Override
			public List<MatcherGroup> apply(List<List<MatcherGroup>> lists) {
				return concatList(lists);
			}
		});
	}
}
