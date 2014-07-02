package ru.tehkode.permissions.backends.memory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;

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
	private final List<String> entriesList;

	protected MemoryMatcherGroup(String name, AtomicReference<T> selfRef, MemoryMatcherList<T, ?> listRef, Multimap<Qualifier, String> qualifiers, Map<String, String> entries) {
		this.name = name;
		this.selfRef = selfRef;
		this.listRef = listRef;
		this.qualifiers = ImmutableMultimap.copyOf(qualifiers);
		this.entries = ImmutableMap.copyOf(entries);
		this.entriesList = null;
	}

	protected MemoryMatcherGroup(String name, AtomicReference<T> selfRef, MemoryMatcherList<T, ?> listRef, Multimap<Qualifier, String> qualifiers, List<String> entriesList) {
		this.name = name;
		this.selfRef = selfRef;
		this.listRef = listRef;
		this.qualifiers = ImmutableMultimap.copyOf(qualifiers);
		this.entriesList = ImmutableList.copyOf(entriesList);
		this.entries = null;
	}

	protected abstract T newSelf(Map<String, String> entries, Multimap<Qualifier, String> qualifiers);
	protected abstract T newSelf(List<String> entries, Multimap<Qualifier, String> qualifiers);

	@Override
	public final String getName() {
		return this.name;
	}

	@Override
	public final Multimap<Qualifier, String> getQualifiers() {
		return this.qualifiers;
	}

	@Override
	public MatcherGroup setQualifiers(Multimap<Qualifier, String> qualifiers) {
		if (selfRef.compareAndSet(self, null)) {
			listRef.deltaUpdate(selfRef, getName(), getQualifiers(), qualifiers);
			T newGroup;
			if (isMap()) {
				newGroup = newSelf(getEntries(), qualifiers);
			} else if (isList()) {
				newGroup = newSelf(getEntriesList(), qualifiers);
			} else {
				throw new IllegalStateException("I'm not a list or a map? This shouldn't be possible");
			}
			if (!selfRef.compareAndSet(null, newGroup)) {
				throw new IllegalStateException("Invalid state change occurred, somebody modified my reference while it was null");
			}
			return newGroup;
		} else {
			return null;
		}
	}

	@Override
	public final Map<String, String> getEntries() {
		return this.entries;
	}

	@Override
	public final List<String> getEntriesList() {
		return this.entriesList;
	}

	@Override
	public MatcherGroup setEntries(Map<String, String> value) {
		T newGroup = newSelf(value, getQualifiers());
		return selfRef.compareAndSet(self, newGroup) ? newGroup : null;
	}

	@Override
	public MatcherGroup setEntries(List<String> value) {
		T newGroup = newSelf(value, getQualifiers());
		return selfRef.compareAndSet(self, newGroup) ? newGroup : null;
	}

	@Override
	public final boolean isValid() {
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
