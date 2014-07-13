package ru.tehkode.permissions.backends.memory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.MultiValueAttribute;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Matcher group used when storing a matcher group structure in memory
 */
public abstract class MemoryMatcherGroup<T extends MemoryMatcherGroup<T>> extends MatcherGroup {
	private final String name;
	@SuppressWarnings("unchecked")
	private final T self = (T) this;
	private final Multimap<Qualifier, String> qualifiers;
	private final Map<String, String> entries;
	private final List<String> entriesList;
	protected final AbstractMemoryBackend<T> backend;
	private final AtomicBoolean valid = new AtomicBoolean(true);

	public static final Attribute<MemoryMatcherGroup<?>, String> NAME = new SimpleAttribute<MemoryMatcherGroup<?>, String>() {
		@Override
		public String getValue(MemoryMatcherGroup<?> memoryMatcherGroup) {
			return memoryMatcherGroup.getName();
		}
	};

	public static final Attribute<MemoryMatcherGroup<?>, Qualifier> QUALIFIERS = new MultiValueAttribute<MemoryMatcherGroup<?>, Qualifier>() {
		@Override
		public List<Qualifier> getValues(MemoryMatcherGroup<?> memoryMatcherGroup) {
			return ImmutableList.copyOf(memoryMatcherGroup.getQualifiers().keySet());
		}
	};

	@SuppressWarnings("unchecked")
	private static AtomicReference<Attribute<MemoryMatcherGroup<?>, String>[]> QUAL_ATTRS = new AtomicReference(new Attribute[Qualifier.getRegisteredCount()]);

	public static Attribute<MemoryMatcherGroup<?>, String> valuesForQualifier(final Qualifier qualifier) {
		Attribute<MemoryMatcherGroup<?>, String> ret;
		// TODO: clean this up
		while (true) {
			Attribute<MemoryMatcherGroup<?>, String>[] oldArr = QUAL_ATTRS.get(), arr;
			if (qualifier.getId() >= oldArr.length) {
				arr = new Attribute[Qualifier.getRegisteredCount()];
				System.arraycopy(oldArr, 0, arr, 0, oldArr.length);
			} else {
				arr = oldArr;
				ret = arr[qualifier.getId()];
				if (ret != null) { // Already set, no need to do the fancy stuff
					break;
				}
			}
			ret = arr[qualifier.getId()] = new MultiValueAttribute<MemoryMatcherGroup<?>, String>() {
				@Override
				public List<String> getValues(MemoryMatcherGroup<?> object) {
					return ImmutableList.copyOf(object.getQualifiers().get(qualifier));
				}
			};

			if (QUAL_ATTRS.compareAndSet(oldArr, arr)) {
				break;
			}
		}
		return ret;
	}


	protected MemoryMatcherGroup(String name, AbstractMemoryBackend<T> backend, Multimap<Qualifier, String> qualifiers, Map<String, String> entries) {
		super(backend);
		this.backend = backend;
		this.name = name;
		this.qualifiers = ImmutableMultimap.copyOf(qualifiers);
		this.entries = ImmutableMap.copyOf(entries);
		this.entriesList = null;
	}

	protected MemoryMatcherGroup(String name, AbstractMemoryBackend<T> backend, Multimap<Qualifier, String> qualifiers, List<String> entriesList) {
		super(backend);
		this.backend = backend;
		this.name = name;
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
	protected ListenableFuture<MatcherGroup> setQualifiersImpl(final Multimap<Qualifier, String> qualifiers) {
		return backend.transformGroup(self, new Callable<T>() {
			@Override
			public T call() throws Exception {
				T newGroup;
				if (isMap()) {
					newGroup = newSelf(getEntries(), qualifiers);
				} else if (isList()) {
					newGroup = newSelf(getEntriesList(), qualifiers);
				} else {
					throw new IllegalStateException("I'm not a list or a map? This shouldn't be possible");
				}
				return newGroup;
			}
		});
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
	protected ListenableFuture<MatcherGroup> setEntriesImpl(final Map<String, String> value) {
		return backend.transformGroup(self, new Callable<T>() {
			@Override
			public T call() throws Exception {
				return newSelf(value, getQualifiers());
			}
		});
	}

	@Override
	protected ListenableFuture<MatcherGroup> setEntriesImpl(final List<String> value) {
		return backend.transformGroup(self, new Callable<T>() {
			@Override
			public T call() throws Exception {
				return newSelf(value, getQualifiers());
			}
		});
	}

	@Override
	public final boolean isValid() {
		return valid.get();
	}

	@Override
	protected ListenableFuture<Boolean> removeImpl() {
		return Futures.immediateFuture(backend.removeGroup(self));
	}

	public boolean invalidate() {
		return valid.compareAndSet(true, false);
	}
}
