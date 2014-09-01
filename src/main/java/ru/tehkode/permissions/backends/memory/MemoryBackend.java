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

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionsGroupData;
import ru.tehkode.permissions.PermissionsUserData;
import ru.tehkode.permissions.backends.PermissionBackend;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

/*
 * Memory Backend
 * Zero Persistence. Does not attempt to save any and all permissions.
 *
 */
public class MemoryBackend extends PermissionBackend {

    public MemoryBackend(PermissionManager manager, ConfigurationSection config) throws PermissionBackendException {
        super(manager, config);
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
	}

	@Override
	public PermissionsUserData getUserData(String userName) {
		return new MemoryData(userName);
	}

	@Override
	public PermissionsGroupData getGroupData(String groupName) {
		return new MemoryData(groupName);
	}

	@Override
	public boolean hasUser(String userName) {
		return false;
	}

	@Override
	public boolean hasGroup(String group) {
		return false;
	}

	@Override
	public Collection<String> getUserIdentifiers() {
		return Collections.emptySet();
	}

	@Override
	public Collection<String> getUserNames() {
		return Collections.emptySet();
	}

	@Override
	public Collection<String> getGroupNames() {
		return Collections.emptySet();
	}

	@Override
	public List<String> getWorldInheritance(String world) {
		return Collections.emptyList();
	}

	@Override
	public Map<String, List<String>> getAllWorldInheritance() {
		return Collections.emptyMap();
	}

	@Override
	public void setWorldInheritance(String world, List<String> inheritance) {
	}

	@Override
	public void writeContents(Writer writer) throws IOException {

	}
}
