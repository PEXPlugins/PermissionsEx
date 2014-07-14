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
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.backends.memory.ConfigInstance;
import ru.tehkode.permissions.backends.memory.MemoryBackend;
import ru.tehkode.permissions.backends.yaml.YamlBackend;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;
import ru.tehkode.permissions.exceptions.PermissionBackendException;
import ru.tehkode.utils.PrefixedThreadFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author code
 */
public class FileBackend extends MemoryBackend {
	private final FileConfig loader;
	private final Object loadSaveLock = new Object();

	public FileBackend(PermissionManager manager, ConfigurationSection config) throws PermissionBackendException {
		super(manager, config, Executors.newSingleThreadExecutor(new PrefixedThreadFactory("PEX-file")));
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

		this.loader = new FileConfig(this, new File(baseDir, permissionFilename));
		reload();
		if (!loader.getFile().exists()) {
			try {
				loader.getFile().createNewFile();
				if (oldFilename == null) {
					initializeDefaultConfiguration();
				}
			} catch (IOException e) {
				throw new PermissionBackendException(e);
			}
		}

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
	protected Executor getExecutor() {
		return super.getExecutor();
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
	protected ConfigInstance load() throws PermissionBackendException {
		try {
			return loader.load();
		} catch (FileNotFoundException e) {
			return new ConfigInstance.Memory();
		} catch (IOException e) {
			throw new PermissionBackendException(e);
		}
	}

	@Override
	protected void save(ConfigInstance data) {
		try {
			synchronized (loadSaveLock) {
				loader.save(data);
			}
		} catch (IOException e) {
			handleException(e, "saving");
		}

	}

	@Override
	protected FileMatcherGroup newGroup(String type, Map<String, String> entries, Multimap<Qualifier, String> qualifiers) {
		return new FileMatcherGroup(type, this, qualifiers, entries, null, null);
	}

	@Override
	protected FileMatcherGroup newGroup(String type, List<String> entries, Multimap<Qualifier, String> qualifiers) {
		return new FileMatcherGroup(type, this, qualifiers, entries, null, null);
	}

	@Override
	public void setPersistent(boolean persistent) {
		super.setPersistent(persistent);
		this.loader.setSaveSuppressed(!persistent);
		if (persistent) {
			this.save();
		}
	}
}
