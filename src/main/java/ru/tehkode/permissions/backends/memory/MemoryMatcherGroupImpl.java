package ru.tehkode.permissions.backends.memory;

import com.google.common.collect.Multimap;
import ru.tehkode.permissions.data.Qualifier;

import java.util.List;
import java.util.Map;

/**
 * Simple implementation of a MemoryMatcherGroup
 */
public class MemoryMatcherGroupImpl extends MemoryMatcherGroup<MemoryMatcherGroupImpl> {
	MemoryMatcherGroupImpl(String name, AbstractMemoryBackend backend, Multimap<Qualifier, String> qualifiers, Map<String, String> entries) {
		super(name, backend, qualifiers, entries);
	}

	MemoryMatcherGroupImpl(String name, AbstractMemoryBackend backend, Multimap<Qualifier, String> qualifiers, List<String> entriesList) {
		super(name, backend, qualifiers, entriesList);
	}

	@Override
	protected MemoryMatcherGroupImpl newSelf(Map<String, String> entries, Multimap<Qualifier, String> qualifiers) {
		return new MemoryMatcherGroupImpl(getName(), backend, qualifiers, entries);
	}

	@Override
	protected MemoryMatcherGroupImpl newSelf(List<String> entries, Multimap<Qualifier, String> qualifiers) {
		return new MemoryMatcherGroupImpl(getName(), backend, qualifiers, entries);
	}

	@Override
	public String toString() {
		return "MemoryMatcherGroupImpl{"
				+ "name=" + getName()
				+ ",qualifiers=" + getQualifiers()
				+ ",entries=" + getEntries()
				+ ",entriesList=" + getEntriesList()
				+ "}";
	}
}
