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
package ru.tehkode.permissions.backends.file;

import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.backends.PermissionBackend;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.backends.yaml.YamlBackend;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * @author code
 */
public class FileBackend extends PermissionBackend {
	private final FileConfig loader;
	private volatile FileMatcherList matcherGroups;
	private final Object loadSaveLock = new Object();

	public FileBackend(PermissionManager manager, ConfigurationSection config) throws PermissionBackendException {
		super(manager, config, Executors.newSingleThreadExecutor());
		String permissionFilename = getConfig().getString("file");
		String oldFilename = null;

		// Default settings
		if (permissionFilename == null) {
			permissionFilename = "permissions.pex";
			getConfig().set("file", permissionFilename);
		} else if (permissionFilename.endsWith(".yml")) {
			oldFilename = permissionFilename;
			permissionFilename = permissionFilename.substring(0, permissionFilename.length() - 4) + ".pex";
		}

		String baseDir = manager.getConfiguration().getBasedir();

		if (baseDir.contains("\\") && !"\\".equals(File.separator)) {
			baseDir = baseDir.replace("\\", File.separator);
		}

		File baseDirectory = new File(baseDir);
		if (!baseDirectory.exists()) {
			baseDirectory.mkdirs();
		}

		this.loader = new FileConfig(new File(baseDir, permissionFilename));
		if (oldFilename != null && !loader.getFile().exists()) {
			try {
				loader.getFile().createNewFile();
			} catch (IOException e) {
				throw new PermissionBackendException(e);
			}
		}
		reload();
		performSchemaUpdate();

		// Perform conversion from YAML
		if (oldFilename != null) {
			YamlBackend oldBackend = new YamlBackend(manager, config);
			loadFrom(oldBackend);
			oldBackend.close();
			getConfig().set("file", permissionFilename);
		}
	}

	@Override
	protected <V> ListenableFuture<V> execute(Callable<V> func) {
		ListenableFutureTask<V> ret = ListenableFutureTask.create(func);
		ret.run();
		return ret;
	}

	@Override
	public int getSchemaVersion() {
		MatcherGroup ret;
		try {
			ret = this.getOne(MatcherGroup.GENERAL_KEY).get();
		} catch (InterruptedException | ExecutionException e) {
			return -1;
		}
		return ret == null || !ret.getEntries().containsKey("schema-version") ? -1 : Integer.parseInt(ret.getEntries().get("schema-version"));
	}

	@Override
	protected void setSchemaVersion(int version) {
		try {
			this.getFirstOrAdd(MatcherGroup.GENERAL_KEY).get().putEntry("schema-version", String.valueOf(version));
		} catch (InterruptedException | ExecutionException e) {
			handleException(e, "setting schema version");
		}
	}

	@Override
	public Collection<String> getUserNames() {
		Set<String> userNames = new HashSet<>();
		for (MatcherGroup group : this.matcherGroups.get(MatcherGroup.UUID_ALIASES_KEY)) {
			userNames.addAll(group.getEntries().values());
		}
		return Collections.unmodifiableSet(userNames);
	}

	@Override
	public ListenableFuture<Iterator<MatcherGroup>> getAll() {
		return execute(new Callable<Iterator<MatcherGroup>>() {
			@Override
			public Iterator<MatcherGroup> call() throws Exception {
				return matcherGroups.getAll();
			}
		});
	}

	@Override
	public ListenableFuture<List<MatcherGroup>> getMatchingGroups(final String type) {
		return execute(new Callable<List<MatcherGroup>>() {
			@Override
			public List<MatcherGroup> call() throws Exception {
				return matcherGroups.get(type);
			}
		});
	}

	@Override
	public ListenableFuture<List<MatcherGroup>> getMatchingGroups(final String type, final Qualifier key, final String value) {
		return execute(new Callable<List<MatcherGroup>>() {
			@Override
			public List<MatcherGroup> call() throws Exception {
				return matcherGroups.get(type, key, value);
			}
		});
	}

	@Override
	public ListenableFuture<MatcherGroup> createMatcherGroup(final String type, final Map<String, String> entries, final Multimap<Qualifier, String> qualifiers) {
		return execute(new Callable<MatcherGroup>() {
			@Override
			public MatcherGroup call() throws Exception {
				return matcherGroups.create(type, entries, qualifiers);
			}
		});
	}

	@Override
	public ListenableFuture<MatcherGroup> createMatcherGroup(final String type, final List<String> entries, final Multimap<Qualifier, String> qualifiers) {
		return execute(new Callable<MatcherGroup>() {
			@Override
			public MatcherGroup call() throws Exception {
				return matcherGroups.create(type, entries, qualifiers);
			}
		});
	}

	@Override
	public ListenableFuture<Collection<String>> getAllValues(final Qualifier qualifier) {
		return execute(new Callable<Collection<String>>() {
			@Override
			public Collection<String> call() throws Exception {
				return matcherGroups.getAllValues(qualifier);
			}
		});
	}

	@Override
	public ListenableFuture<Boolean> hasAnyQualifier(final Qualifier qualifier, final String value) {
		return execute(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return matcherGroups.hasAnyQualifier(qualifier, value);
			}
		});
	}

	@Override
	public ListenableFuture<Void> replaceQualifier(final Qualifier qualifier, final String old, final String newVal) {
		return execute(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				matcherGroups.replace(qualifier, old, newVal);
				return null;
			}
		});
	}

	@Override
	public ListenableFuture<List<MatcherGroup>> allWithQualifier(final Qualifier qualifier) {
		return execute(new Callable<List<MatcherGroup>>() {
			@Override
			public List<MatcherGroup> call() throws Exception {
				return matcherGroups.allWithQualifier(qualifier);
			}
		});
	}

	@Override
	public void reload() throws PermissionBackendException {
		try {
			synchronized (loadSaveLock) {
				this.matcherGroups = loader.load();
			}
			getLogger().info("Permissions file successfully reloaded");
		} catch (FileNotFoundException e) {
			if (this.matcherGroups == null) {
				// First load, load even if the file doesn't exist
				this.matcherGroups = new FileMatcherList(this.loader);
				initializeDefaultConfiguration();
			}
		} catch (Throwable e) {
			throw new PermissionBackendException("Error loading permissions file!", e);
		}
	}

	@Override
	public void setPersistent(boolean persistent) {
		super.setPersistent(persistent);
		this.loader.setSaveSuppressed(!persistent);
		if (persistent) {
			this.save();
		}
	}

	public void save() {
		if (matcherGroups != null) {
			getExecutor().execute(new Runnable() {
				@Override
				public void run() {
					try {
						synchronized (loadSaveLock) {
							final FileMatcherList matcherGroups = FileBackend.this.matcherGroups;
							if (matcherGroups != null) {
								matcherGroups.save();
							}
						}
					} catch (IOException e) {
						getLogger().log(Level.SEVERE, "Error saving file backend data!", e);
					}
				}
			});
		}
	}
}
