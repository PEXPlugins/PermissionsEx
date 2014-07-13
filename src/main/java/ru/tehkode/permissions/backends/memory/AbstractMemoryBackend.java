/*
 * PermissionsEx - Permissions plugin for Bukkit
 * Copyright (C) 2011 t3hk0d3 http://www.tehkode.ru
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package ru.tehkode.permissions.backends.memory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.googlecode.cqengine.CQEngine;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.index.radix.RadixTreeIndex;
import com.googlecode.cqengine.index.standingquery.StandingQueryIndex;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.backends.PermissionBackend;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

import static com.googlecode.cqengine.query.QueryFactory.*;

/*
 * Memory Backend
 *
 * This class functions as a base class for backends that read their data from the contents of a file loaded into memory
 *
 */
public abstract class AbstractMemoryBackend<T extends MemoryMatcherGroup<T>> extends PermissionBackend {
	private IndexedCollection<T> matcherList;
	private ConfigInstance<T> config;

	public AbstractMemoryBackend(PermissionManager manager, ConfigurationSection config) throws PermissionBackendException {
		super(manager, config, MoreExecutors.sameThreadExecutor());
	}

	protected AbstractMemoryBackend(PermissionManager manager, ConfigurationSection config, ExecutorService executor) throws PermissionBackendException {
		super(manager, config, executor);
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
		ConfigInstance<T> newValues;
		try {
			newValues = load();
		} catch (PermissionBackendException ex) {
			if (matcherList != null) {
				handleException(ex, "reloading existing backend");
				return;
			} else {
				throw ex;
			}
		}
		IndexedCollection<T> oldMatchers = matcherList,
		newMatchers = CQEngine.copyFromObjectLocking(newValues.getGroups(), 32);
		newMatchers.addIndex(HashIndex.onAttribute(cast(MemoryMatcherGroup.NAME)));
		for (Qualifier qual : Qualifier.getAll()) {
			newMatchers.addIndex(StandingQueryIndex.onQuery(equal(cast(MemoryMatcherGroup.QUALIFIERS), qual)));
			//newMatchers.addIndex(RadixTreeIndex.onAttribute(cast(MemoryMatcherGroup.valuesForQualifier(qual))));
		}

		matcherList = newMatchers;
		config = newValues;
		if (oldMatchers != null) {
			for (T group : oldMatchers) {
				group.invalidate();
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected ConfigInstance<T> load() throws PermissionBackendException {
		return new ConfigInstance.Memory();
	}

	protected void save(ConfigInstance<T> groups) {
	}

	public void save() {
		final IndexedCollection<T> matcherList = this.matcherList;
		final ConfigInstance<T> config = this.config;
		if (matcherList != null) {
			getExecutor().execute(new Runnable() {
				@Override
				public void run() {
					config.setGroups(matcherList);
					save(config);
				}
			});
		}
	}

	@Override
	public Collection<String> getUserNames() {
		return Collections.emptySet();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterable<MatcherGroup> getAll() {
		return (Iterable) matcherList;
	}

	@Override
	public ListenableFuture<List<MatcherGroup>> getMatchingGroups(final String type) {
		Preconditions.checkNotNull(type);
		return execute(new Callable<List<MatcherGroup>>() {
			@Override
			public List<MatcherGroup> call() throws Exception {
				return Lists.<MatcherGroup>newArrayList(matcherList.retrieve(equal(cast(MemoryMatcherGroup.NAME), type)));
			}
		});
	}

	@Override
	public ListenableFuture<List<MatcherGroup>> getMatchingGroups(final String type, final Qualifier qual, final String qualValue) {
		Preconditions.checkNotNull(type);
		Preconditions.checkNotNull(qual);
		return execute(new Callable<List<MatcherGroup>>() {
			@Override
			public List<MatcherGroup> call() throws Exception {
				if (qualValue == null) {
					return Lists.<MatcherGroup>newArrayList(matcherList.retrieve(and(
							equal(cast(MemoryMatcherGroup.NAME), type),
							not(equal(cast(MemoryMatcherGroup.QUALIFIERS), qual))
					)));
				} else {
					return Lists.<MatcherGroup>newArrayList(matcherList.retrieve(and(
							equal(cast(MemoryMatcherGroup.NAME), type),
							equal(cast(MemoryMatcherGroup.QUALIFIERS), qual),
							equal(cast(MemoryMatcherGroup.valuesForQualifier(qual)), qualValue)
					)));
				}
			}
		});
	}

	IndexedCollection<T> getRawCollection() {
		return matcherList;
	}

	/**
	 * Because generics are stupid
	 * @param orig The thing to be casted
	 * @param <O>  The object type
	 * @return The casted object
	 */
	@SuppressWarnings("unchecked")
	<O> Attribute<T, O> cast(Attribute<MemoryMatcherGroup<?>, O> orig) {
		return (Attribute) orig;
	}

	@Override
	protected ListenableFuture<MatcherGroup> createMatcherGroupImpl(final String type, final Map<String, String> entries, final Multimap<Qualifier, String> qualifiers) {
		Preconditions.checkNotNull(type);
		Preconditions.checkNotNull(entries);
		Preconditions.checkNotNull(qualifiers);
		return execute(new Callable<MatcherGroup>() {
			@Override
			public MatcherGroup call() throws Exception {
				T toAdd = newGroup(type, entries, qualifiers);
				matcherList.add(toAdd);
				return toAdd;
			}
		});
	}

	@Override
	protected ListenableFuture<MatcherGroup> createMatcherGroupImpl(final String type, final List<String> entries, final Multimap<Qualifier, String> qualifiers) {
		Preconditions.checkNotNull(type);
		Preconditions.checkNotNull(entries);
		Preconditions.checkNotNull(qualifiers);
		return execute(new Callable<MatcherGroup>() {
			@Override
			public MatcherGroup call() throws Exception {
				T toAdd = newGroup(type, entries, qualifiers);
				matcherList.add(toAdd);
				return toAdd;
			}
		});
	}

	protected abstract T newGroup(String type, Map<String, String> entries, Multimap<Qualifier, String> qualifiers);

	protected abstract T newGroup(String type, List<String> entries, Multimap<Qualifier, String> qualifiers);

	@Override
	public ListenableFuture<Collection<String>> getAllValues(final Qualifier qualifier) {
		Preconditions.checkNotNull(qualifier);
		return execute(new Callable<Collection<String>>() {
			@Override
			public Collection<String> call() throws Exception {
				Set<String> ret = new HashSet<>();
				for (T group : matcherList.retrieve(equal(cast(MemoryMatcherGroup.QUALIFIERS), qualifier))) {
					ret.addAll(group.getQualifiers().get(qualifier));
				}
				return ret;
			}
		});
	}

	@Override
	public ListenableFuture<Boolean> hasAnyQualifier(final Qualifier qualifier, final String value) {
		Preconditions.checkNotNull(qualifier);
		return execute(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				if (value == null) {
					return !matcherList.retrieve(not(equal(cast(MemoryMatcherGroup.QUALIFIERS), qualifier)))
							.isEmpty();
				} else {
					return !matcherList.retrieve(and(
							equal(cast(MemoryMatcherGroup.QUALIFIERS), qualifier),
							equal(cast(MemoryMatcherGroup.valuesForQualifier(qualifier)), value)
					)).isEmpty();
				}
			}
		});
	}

	@Override
	public ListenableFuture<Void> replaceQualifier(final Qualifier qualifier, final String old, final String newVal) {
		Preconditions.checkNotNull(qualifier);
		Preconditions.checkNotNull(old);
		if (old.equals(newVal)) {
			return Futures.immediateFuture(null);
		}

		return execute(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				for (T group : matcherList.retrieve(and(
						equal(cast(MemoryMatcherGroup.QUALIFIERS), qualifier),
						equal(cast(MemoryMatcherGroup.valuesForQualifier(qualifier)), old)
				))) {
					Multimap<Qualifier, String> newMap = HashMultimap.create(group.getQualifiers());
					newMap.remove(qualifier, old);
					newMap.put(qualifier, newVal);
					group.setQualifiers(newMap);
				}
				return null;
			}
		});
	}

	@Override
	public ListenableFuture<List<MatcherGroup>> allWithQualifier(final Qualifier qualifier) {
		Preconditions.checkNotNull(qualifier);
		return execute(new Callable<List<MatcherGroup>>() {
			@Override
			public List<MatcherGroup> call() throws Exception {
				return Lists.<MatcherGroup>newArrayList(matcherList.retrieve(equal(cast(MemoryMatcherGroup.QUALIFIERS), qualifier)));
			}
		});
	}

	public ListenableFuture<MatcherGroup> transformGroup(T matcherGroup, Callable<T> callable) {
		Preconditions.checkNotNull(matcherGroup);
		Preconditions.checkNotNull(callable);
		if (matcherGroup.invalidate()) {
			try {
				T newGroup = callable.call();
				if (matcherList.remove(matcherGroup)) { // TODO: think about ordering
					matcherList.add(newGroup);
					save();
					return Futures.<MatcherGroup>immediateFuture(newGroup);
				} else { // Group is no longer valid
					return Futures.immediateFailedFuture(new MatcherGroup.InvalidGroupException());
				}
			} catch (Exception e) {
				return Futures.immediateFailedFuture(e);
			}
		} else {
			return Futures.immediateFailedCheckedFuture(new MatcherGroup.InvalidGroupException());
		}
	}

	public boolean removeGroup(T group) {
		Preconditions.checkNotNull(group);
		return matcherList.remove(group);
	}
}
