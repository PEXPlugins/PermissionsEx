package ru.tehkode.permissions.backends.memory;

import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.MoreExecutors;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.data.Qualifier;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

import java.util.List;
import java.util.Map;

/**
 * Memory backend instance
 */
public class MemoryBackend extends AbstractMemoryBackend<MemoryMatcherGroupImpl> {
	public MemoryBackend(PermissionManager manager, ConfigurationSection config) throws PermissionBackendException {
		super(manager, config, MoreExecutors.sameThreadExecutor());
		reload();
	}

	@Override
	protected MemoryMatcherGroupImpl newGroup(String type, Map<String, String> entries, Multimap<Qualifier, String> qualifiers) {
		return new MemoryMatcherGroupImpl(type, this, qualifiers, entries);
	}

	@Override
	protected MemoryMatcherGroupImpl newGroup(String type, List<String> entries, Multimap<Qualifier, String> qualifiers) {
		return new MemoryMatcherGroupImpl(type, this, qualifiers, entries);
	}
}
