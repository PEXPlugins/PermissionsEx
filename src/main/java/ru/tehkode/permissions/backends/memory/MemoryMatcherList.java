package ru.tehkode.permissions.backends.memory;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Futures;
import ru.tehkode.permissions.backends.PermissionBackend;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * A thread-safe data structure for managing matcher groups stored in memory (with the capability to save)
 */
public abstract class MemoryMatcherList<T extends MemoryMatcherGroup<T, ? extends MemoryMatcherList<T, ?>>, SerializedType> {
	protected final ConcurrentLinkedQueue<AtomicReference<T>> groups = new ConcurrentLinkedQueue<>();
	// map(type->array[qualifier](map(value,list(groups))))
	private final ConcurrentMap<String, AtomicReferenceArray<ConcurrentMap<String, Collection<AtomicReference<T>>>>> lookup = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Collection<AtomicReference<T>>> anyTypeLookup = new ConcurrentHashMap<>();

	/**
	 * Returns the section list relevant for the specified parameters, navigating the nested maps
	 *
	 * @param type The type key of the section
	 * @param qualifier The qualifier to look up by
	 * @param value The value attached to this section
	 * @param create Whether to create the necessary structure if none is present
	 * @return The collection of references. Will only have one copy of each group, may be null if create is false
	 */
	@SuppressWarnings("unchecked")
	private Collection<AtomicReference<T>> getSectionSet(String type, Qualifier qualifier, String value, boolean create) {
		AtomicReferenceArray<ConcurrentMap<String, Collection<AtomicReference<T>>>> qualifiers = lookup.get(type);
		if (qualifiers == null) {
			if (!create) {
				return null;
			}
			qualifiers = new AtomicReferenceArray<>(Qualifier.getRegisteredCount());
			AtomicReferenceArray<ConcurrentMap<String, Collection<AtomicReference<T>>>> newQualifiers = lookup.putIfAbsent(type, qualifiers);
			if (newQualifiers != null) {
				qualifiers = newQualifiers;
			}
		}

		ConcurrentMap<String, Collection<AtomicReference<T>>> lists;
		while (true) {
			lists = qualifiers.get(qualifier.getId());
			if (lists != null || !create) {
				break;
			}
			lists = new ConcurrentHashMap<>();
			if (qualifiers.compareAndSet(qualifier.getId(), null, lists)) {
				break;
			}
		}
		if (lists == null) {
			return null;
		}

		Collection<AtomicReference<T>> ret = lists.get(value);
		if (ret == null && create) {
			ret = Collections.newSetFromMap(new ConcurrentHashMap<AtomicReference<T>, Boolean>());
			Collection<AtomicReference<T>> newRet = lists.putIfAbsent(value, ret);
			if (newRet != null) {
				ret = newRet;
			}
		}

		return ret;
	}

	/**
	 * Returns a collection of references to all matcher groups of the specified type
	 *
	 * @param type The type
	 * @param create Whether to create the necessary structure if none is present
	 * @return The collection of references
	 */
	private Collection<AtomicReference<T>> getSectionSet(String type, boolean create) {
		Collection<AtomicReference<T>> ret = anyTypeLookup.get(type);
		if (ret == null && create) {
			ret = Collections.newSetFromMap(new ConcurrentHashMap<AtomicReference<T>, Boolean>());
			Collection<AtomicReference<T>> newRet = anyTypeLookup.putIfAbsent(type, ret);
			if (newRet != null) {
				ret = newRet;
			}
		}
		return ret;
	}

	/**
	 * Insert the group and its reference into the necessary reference maps (for its qualifiers and type)
	 *
	 * @param ref The reference associated with this group
	 * @param group The group to insert
	 */
	protected void insertIntoLookup(AtomicReference<T> ref, T group) {
		for (Map.Entry<Qualifier, String> qual : group.getQualifiers().entries()) {
			getSectionSet(group.getName(), qual.getKey(), qual.getValue(), true).add(ref);
		}
		getSectionSet(group.getName(), true).add(ref);
	}

	/**
	 * Perform a delta update for the matcher
	 *
	 * @param ptr The reference to update around
	 * @param name The name of the group
	 * @param old The old qualifiers
	 * @param newVal New qualifiers
	 */
	void deltaUpdate(AtomicReference<T> ptr, String name, Multimap<Qualifier, String> old, Multimap<Qualifier, String> newVal) {
		for (Map.Entry<Qualifier, String> qual : old.entries()) {
			Collection<AtomicReference<T>> set = getSectionSet(name, qual.getKey(), qual.getValue(), false);
			if (set != null)  {
				set.remove(ptr);
			}
		}
		for (Map.Entry<Qualifier, String> qual : newVal.entries()) {
			getSectionSet(name, qual.getKey(), qual.getValue(), true).add(ptr);
		}
	}

	/**
	 * Removes this reference from the data structure
	 *
	 * @param ptr The reference to update around
	 * @param group The group to remove
	 */
	void remove(AtomicReference<T> ptr, T group) {
		for (Map.Entry<Qualifier, String> quals : group.getQualifiers().entries()) {
			Collection<AtomicReference<T>> removeFrom = getSectionSet(group.getName(), quals.getKey(), quals.getValue(), false);
			if (removeFrom != null) {
				removeFrom.remove(ptr);
			}
		}
		Collection<AtomicReference<T>> anyRemove = getSectionSet(group.getName(), false);
		if (anyRemove != null) {
			anyRemove.remove(ptr);
		}

		this.groups.remove(ptr);
	}

	// -- Load/save

	public MemoryMatcherList() {
		// Empty list
	}

	public MemoryMatcherList(SerializedType nodes) throws IOException {
		load(nodes);
	}

	/**
	 * Add the information from the {@link SerializedType} to this structure.
	 * This method is called from the constructor.
	 *
	 * @param data The data to load
	 * @throws IOException If any sort of error occurs, whether it is reading data or interpreting structure
	 */
	protected abstract void load(SerializedType data) throws IOException;

	/**
	 * Write data out to the store associated with {@link SerializedType}.
	 * The loader should probably be passed to child class constructors.
	 *
	 * @throws IOException when do you think this happens?
	 */
	public abstract void save() throws IOException;

	// Group access methods
	public List<MatcherGroup> get(String type) {
		Collection<AtomicReference<T>> groups = getSectionSet(type, false);
		if (groups == null || groups.isEmpty()) {
			return Collections.emptyList();
		}

		return unwrapList(groups);
	}

	public List<MatcherGroup> get(String type, Qualifier qual, String qualValue) {
		Collection<AtomicReference<T>> groups = getSectionSet(type, qual, qualValue, false);
		if (groups == null || groups.isEmpty()) {
			return Collections.emptyList();
		}

		return unwrapList(groups);
	}

	/**
	 * Attempts to get the group from its reference, repeatedly trying if it is null on the first attempt.
	 *
	 * @param ptr The reference to get
	 * @return A value if it is set. May be null (indicating a dead reference).
	 */
	protected T valFromPtr(AtomicReference<T> ptr) {
		T ret = ptr.get();
		if (ret == null) {
			for (short runCount = 0; (ret = ptr.get()) == null && runCount < 30000; runCount++);
		}
		return ret;
	}

	// Creation

	/**
	 * Create a new group instance. This does not insert in into the structure (see {@link #create(String, Map, Multimap)} for that).
	 *
	 * @param ptr The reference
	 * @param type The type arg
	 * @param entries The entries arg
	 * @param qualifiers The qualifiers arg
	 * @return The newly constructed {@link MatcherGroup}
	 */
	protected abstract T newGroup(AtomicReference<T> ptr, String type, Map<String, String> entries, Multimap<Qualifier, String> qualifiers);

	/**
	 * Create a new group, this time with a list value
	 * @see #newGroup(AtomicReference, String, Map, Multimap)
	 */
	protected abstract T newGroup(AtomicReference<T> ptr, String type, List<String> entries, Multimap<Qualifier, String> qualifiers);

	/**
	 * @see PermissionBackend#createMatcherGroup(String, Map, Multimap)
	 */
	public MatcherGroup create(String type, Map<String, String> entries, Multimap<Qualifier, String> qualifiers) {
		final AtomicReference<T> ptr = new AtomicReference<>();
		T group = newGroup(ptr, type, entries, qualifiers);
		groups.add(ptr);
		insertIntoLookup(ptr, group);
		ptr.set(group);
		return group;
	}

	/**
	 * @see PermissionBackend#createMatcherGroup(String, Map, Multimap)
	 */
	public MatcherGroup create(String type, List<String> entries, Multimap<Qualifier, String> qualifiers) {
		final AtomicReference<T> ptr = new AtomicReference<>();
		T group = newGroup(ptr, type, entries, qualifiers);
		groups.add(ptr);
		insertIntoLookup(ptr, group);
		ptr.set(group);
		return group;
	}

	private List<MatcherGroup> unwrapList(Collection<AtomicReference<T>> wrapped) {
		List<MatcherGroup> ret = new LinkedList<>();
		for (AtomicReference<T> ptr : wrapped) {
			T entry = valFromPtr(ptr);
			if (entry != null) {
				ret.add(entry);
			}
		}
		return ret;
	}

	/**
	 * @see PermissionBackend#getAllValues(Qualifier)
	 */
	public Collection<String> getAllValues(Qualifier qualifier) {
		final Set<String> ret = new HashSet<>();
		for (Map.Entry<String, AtomicReferenceArray<ConcurrentMap<String, Collection<AtomicReference<T>>>>> ent : lookup.entrySet()) {
			ConcurrentMap<String, Collection<AtomicReference<T>>> test = ent.getValue().get(qualifier.getId());
			if (test != null) {
				ret.addAll(test.keySet());
			}
		}
		return Collections.unmodifiableSet(ret);
	}

	/**
	 * @see PermissionBackend#hasAnyQualifier(Qualifier, String)
	 */
	public boolean hasAnyQualifier(Qualifier qualifier, String value) {
		for (Map.Entry<String, AtomicReferenceArray<ConcurrentMap<String, Collection<AtomicReference<T>>>>> ent : lookup.entrySet()) {
			ConcurrentMap<String, Collection<AtomicReference<T>>> test = ent.getValue().get(qualifier.getId());
			if (test != null && test.containsKey(value)) {
				return true;
			}
		}
		return false;
	}


	/**
	 *
	 * @see PermissionBackend#getAll()
	 */
	public Iterable<MatcherGroup> getAll() {
		return Iterables.transform(groups, new Function<AtomicReference<T>, MatcherGroup>() {
			@Override
			public MatcherGroup apply(AtomicReference<T> ptr) {
				return valFromPtr(ptr);
			}
		});
	}

	public void replace(Qualifier qualifier, String old, String newVal) {
		for (Map.Entry<String, AtomicReferenceArray<ConcurrentMap<String, Collection<AtomicReference<T>>>>> ent : lookup.entrySet()) {
			ConcurrentMap<String, Collection<AtomicReference<T>>> test = ent.getValue().get(qualifier.getId());
			if (test != null) {
				Collection<AtomicReference<T>> toMove = test.get(old);
				if (toMove != null && !toMove.isEmpty()) {
					for (AtomicReference<T> ptr : toMove) {
						T matcher;
						Multimap<Qualifier, String> newQuals;
						while (true) {
							matcher = valFromPtr(ptr);
							newQuals = HashMultimap.create(matcher.getQualifiers());
							if (newQuals.remove(qualifier, old)) {
								newQuals.put(qualifier, newVal);
							} else {
								break;
							}
							try {
								matcher.setQualifiers(newQuals).get(2, TimeUnit.MILLISECONDS);
								break;
							} catch (InterruptedException | ExecutionException e) {
								if (e.getCause() instanceof MatcherGroup.InvalidGroupException) {
									continue;
								}
								throw new RuntimeException(e);
							} catch (TimeoutException e) {
								continue;
							}
						}
					}
				}
			}
		}
	}

	public List<MatcherGroup> allWithQualifier(Qualifier qual) {
		List<MatcherGroup> ret = new LinkedList<>();
		for (Map.Entry<String, AtomicReferenceArray<ConcurrentMap<String, Collection<AtomicReference<T>>>>> ent : lookup.entrySet()) {
			ConcurrentMap<String, Collection<AtomicReference<T>>> map = ent.getValue().get(qual.getId());
			if (map != null) {
				for (Collection<AtomicReference<T>> list : map.values()) {
					for (AtomicReference<T> ptr : list) {
						ret.add(valFromPtr(ptr));
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Invalidate all references in this store. Should be used <b>after</b> a thing
	 */
	public void close() {
		for (AtomicReference<T> matcher : groups) {
			matcher.set(null);
		}
	}
}
