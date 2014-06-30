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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.backends.PermissionBackend;
import ru.tehkode.permissions.backends.memory.impl.MemoryMatcherListImpl;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

/*
 * Memory Backend
 * Zero Persistence. Does not attempt to save any and all permissions.
 *
 */
public class MemoryBackend extends PermissionBackend {
	private MemoryMatcherListImpl matcherList;

    public MemoryBackend(PermissionManager manager, ConfigurationSection config) throws PermissionBackendException {
        super(manager, config, MoreExecutors.sameThreadExecutor());
		reload();
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
		MemoryMatcherListImpl oldMatchers = matcherList;
		matcherList = new MemoryMatcherListImpl();
		if (oldMatchers != null) {
			oldMatchers.close();
		}
	}

	@Override
	public Collection<String> getUserNames() {
		return Collections.emptySet();
	}

	@Override
	public ListenableFuture<Iterator<MatcherGroup>> getAll() {
		return execute(new Callable<Iterator<MatcherGroup>>() {
			@Override
			public Iterator<MatcherGroup> call() throws Exception {
				return matcherList.getAll();
			}
		});
	}

	@Override
	public ListenableFuture<List<MatcherGroup>> getMatchingGroups(final String type) {
		return execute(new Callable<List<MatcherGroup>>() {
			@Override
			public List<MatcherGroup> call() throws Exception {
				return matcherList.get(type);
			}
		});
	}

	@Override
	public ListenableFuture<List<MatcherGroup>> getMatchingGroups(final String type, final Qualifier qual, final String qualValue) {
		return execute(new Callable<List<MatcherGroup>>() {
			@Override
			public List<MatcherGroup> call() throws Exception {
				return matcherList.get(type, qual, qualValue);
			}
		});
	}

	@Override
	public ListenableFuture<MatcherGroup> createMatcherGroup(final String type, final Map<String, String> entries, final Multimap<Qualifier, String> qualifiers) {
		return execute(new Callable<MatcherGroup>() {
			@Override
			public MatcherGroup call() throws Exception {
				return matcherList.create(type, entries, qualifiers);
			}
		});
	}

	@Override
	public ListenableFuture<MatcherGroup> createMatcherGroup(final String type, final List<String> entries, final Multimap<Qualifier, String> qualifiers) {
		return execute(new Callable<MatcherGroup>() {
			@Override
			public MatcherGroup call() throws Exception {
				return matcherList.create(type, entries, qualifiers);
			}
		});
	}

	@Override
	public ListenableFuture<Collection<String>> getAllValues(final Qualifier qualifier) {
		return execute(new Callable<Collection<String>>() {
			@Override
			public Collection<String> call() throws Exception {
				return matcherList.getAllValues(qualifier);
			}
		});
	}

	@Override
	public ListenableFuture<Boolean> hasAnyQualifier(final Qualifier qualifier, final String value) {
		return execute(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return matcherList.hasAnyQualifier(qualifier, value);
			}
		});
	}

	@Override
	public ListenableFuture<Void> replaceQualifier(final Qualifier qualifier, final String old, final String newVal) {
		return execute(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				matcherList.replace(qualifier, old, newVal);
				return null;
			}
		});
	}

	@Override
	public ListenableFuture<List<MatcherGroup>> allWithQualifier(final Qualifier qualifier) {
		return execute(new Callable<List<MatcherGroup>>() {
			@Override
			public List<MatcherGroup> call() throws Exception {
				return matcherList.allWithQualifier(qualifier);
			}
		});
	}

}
