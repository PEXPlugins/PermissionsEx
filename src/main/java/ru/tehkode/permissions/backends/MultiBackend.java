package ru.tehkode.permissions.backends;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
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
		super(manager, backendConfig);
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
	public Iterator<MatcherGroup> getAllMatcherGroups() {
		return Iterators.concat(Lists.transform(backends, new Function<PermissionBackend, Iterator<MatcherGroup>>() {
			@Override
			public Iterator<MatcherGroup> apply(PermissionBackend permissionBackend) {
				return permissionBackend.getAllMatcherGroups();
			}
		}).iterator());
	}

	@Override
	public List<MatcherGroup> getMatchingGroups(String type) {
		return null;
	}

	@Override
	public List<MatcherGroup> getMatchingGroups(String type, Qualifier qual, String qualValue) {
		ImmutableList.Builder<MatcherGroup> build = ImmutableList.builder();
		for (PermissionBackend backend : backends) {
			build.addAll(backend.getMatchingGroups(type, qual, qualValue));
		}
		return build.build();
	}


	@Override
	public MatcherGroup createMatcherGroup(String type, Map<String, String> entries, Multimap<Qualifier, String> qualifiers) {
		return backends.get(0).createMatcherGroup(type, entries, qualifiers); // TODO: Add backend= qualifier
	}

	@Override
	public MatcherGroup createMatcherGroup(String type, List<String> entries, Multimap<Qualifier, String> qualifiers) {
		return backends.get(0).createMatcherGroup(type, entries, qualifiers);
	}

	@Override
	public Collection<String> getAllValues(Qualifier qualifier) {
		Set<String> ret = new HashSet<>();
		for (PermissionBackend backend : backends) {
			ret.addAll(backend.getAllValues(qualifier));
		}
		return ret;
	}

	@Override
	public boolean hasAnyQualifier(Qualifier qualifier, String value) {
		for (PermissionBackend backend : backends) {
			if (backend.hasAnyQualifier(qualifier, value)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public java.util.concurrent.Future<Void> replaceQualifier(Qualifier qualifier, String old, String newVal) {
		for (PermissionBackend backend : backends) {
			backend.replaceQualifier(qualifier, old, newVal);
		}
		return null;
	}

	@Override
	public List<MatcherGroup> allWithQualifier(Qualifier qualifier) {
		List<MatcherGroup> ret = new ArrayList<>();
		for (PermissionBackend backend : backends) {
			ret.addAll(backend.allWithQualifier(qualifier));
		}
		return ret;
	}
}
