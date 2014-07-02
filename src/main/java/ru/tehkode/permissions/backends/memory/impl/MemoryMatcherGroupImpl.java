package ru.tehkode.permissions.backends.memory.impl;

import com.google.common.collect.Multimap;
import ru.tehkode.permissions.backends.memory.MemoryMatcherGroup;
import ru.tehkode.permissions.data.Qualifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of the abstract memory matcher group used for MemoryBackend
 */
public class MemoryMatcherGroupImpl extends MemoryMatcherGroup<MemoryMatcherGroupImpl, MemoryMatcherListImpl> {
	public MemoryMatcherGroupImpl(String name, AtomicReference<MemoryMatcherGroupImpl> selfRef, MemoryMatcherListImpl listRef, Multimap<Qualifier, String> qualifiers, Map<String, String> entries) {
		super(name, selfRef, listRef, qualifiers, entries);
	}

	public MemoryMatcherGroupImpl(String name, AtomicReference<MemoryMatcherGroupImpl> selfRef, MemoryMatcherListImpl listRef, Multimap<Qualifier, String> qualifiers, List<String> entriesList) {
		super(name, selfRef, listRef, qualifiers, entriesList);
	}

	@Override
	protected MemoryMatcherGroupImpl newSelf(Map<String, String> entries, Multimap<Qualifier, String> qualifiers) {
		return new MemoryMatcherGroupImpl(getName(), selfRef, listRef, qualifiers, entries);
	}

	@Override
	protected MemoryMatcherGroupImpl newSelf(List<String> entries, Multimap<Qualifier, String> qualifiers) {
		return new MemoryMatcherGroupImpl(getName(), selfRef, listRef, qualifiers, entries);
	}
}
