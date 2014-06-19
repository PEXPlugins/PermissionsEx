package ru.tehkode.permissions.backends.memory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Matcher group used when storing a matcher group structure in memory
 */
public abstract class MemoryMatcherGroup<T extends MemoryMatcherGroup<T>> extends MatcherGroup {
	private final String name;
	@SuppressWarnings("unchecked")
	private final T self = (T) this;
	protected final AtomicReference<T> selfRef;
	protected final MemoryMatcherList<T, ?> listRef;
	private final Multimap<Qualifier, String> qualifiers;
	private final Map<String, String> entries;

	protected MemoryMatcherGroup(String name, AtomicReference<T> selfRef, MemoryMatcherList<T, ?> listRef, Multimap<Qualifier, String> qualifiers, Map<String, String> entries) {
		this.name = name;
		this.selfRef = selfRef;
		this.listRef = listRef;
		this.qualifiers = ImmutableMultimap.copyOf(qualifiers);
		this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
	}

	protected MemoryMatcherGroup(String name, AtomicReference<T> selfRef, MemoryMatcherList<T, ?> listRef, Multimap<Qualifier, String> qualifiers, List<String> entriesList) {
		this.name = name;
		this.selfRef = selfRef;
		this.listRef = listRef;
		this.qualifiers = ImmutableMultimap.copyOf(qualifiers);
		Map<String, String> entries = new LinkedHashMap<>();
		for (String entry : entriesList) {
			entries.put(entry, null);
		}
		this.entries = Collections.unmodifiableMap(entries);
	}

	protected abstract T newSelf(Map<String, String> entries, Multimap<Qualifier, String> qualifiers);

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Multimap<Qualifier, String> getQualifiers() {
		return this.qualifiers;
	}

	@Override
	public MatcherGroup setQualifiers(Multimap<Qualifier, String> qualifiers) {
		if (selfRef.compareAndSet(self, null)) {
			listRef.deltaUpdate(selfRef, getName(), getQualifiers(), qualifiers);
			T newGroup = newSelf(getEntries(), qualifiers);
			if (!selfRef.compareAndSet(null, newGroup)) {
				throw new IllegalStateException("Invalid state change occurred, somebody modified my reference while it was null");
			}
			return newGroup;
		} else {
			return null;
		}
	}

	@Override
	public Map<String, String> getEntries() {
		return this.entries;
	}

	@Override
	public MatcherGroup setEntries(Map<String, String> value) {
		T newGroup = newSelf(value, getQualifiers());
		return selfRef.compareAndSet(self, newGroup) ? newGroup : null;
	}

	@Override
	public boolean isValid() {
		return this.equals(selfRef.get());
	}

	@Override
	public boolean remove() {
		if (selfRef.compareAndSet(self, null)) {
			listRef.remove(selfRef, self);
			return true;
		}
		return false;
	}
}
