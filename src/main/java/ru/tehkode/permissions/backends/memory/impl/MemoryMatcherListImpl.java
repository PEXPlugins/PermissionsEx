package ru.tehkode.permissions.backends.memory.impl;

import com.google.common.collect.Multimap;
import ru.tehkode.permissions.backends.memory.MemoryMatcherList;
import ru.tehkode.permissions.data.Qualifier;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple implementation of MemoryMatcherList
 */
public class MemoryMatcherListImpl extends MemoryMatcherList<MemoryMatcherGroupImpl, Void> {
	@Override
	protected void load(Void aVoid) throws IOException {
	}

	@Override
	public void save() throws IOException {
	}

	@Override
	protected MemoryMatcherGroupImpl newGroup(AtomicReference<MemoryMatcherGroupImpl> ptr, String type, Map<String, String> entries, Multimap<Qualifier, String> qualifiers) {
		return new MemoryMatcherGroupImpl(type, ptr, this, qualifiers, entries);
	}

	@Override
	protected MemoryMatcherGroupImpl newGroup(AtomicReference<MemoryMatcherGroupImpl> ptr, String type, List<String> entries, Multimap<Qualifier, String> qualifiers) {
		return new MemoryMatcherGroupImpl(type, ptr, this, qualifiers, entries);
	}
}
