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
package ru.tehkode.permissions.backends;

import java.io.IOException;
import java.io.OutputStreamWriter;

import org.bukkit.configuration.Configuration;

import ru.tehkode.permissions.PermissionBackend;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.backends.memory.MemoryGroup;
import ru.tehkode.permissions.backends.memory.MemoryUser;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

/*
 * Memory Backend
 * Zero Persistence. Does not attempt to save any and all permissions.
 * 
 */
public class MemoryBackend extends PermissionBackend {

    public MemoryBackend(PermissionManager manager, Configuration config) {
        super(manager, config);
    }

    @Override
    public void initialize() throws PermissionBackendException {
        
    }

    @Override
    public PermissionUser getUser(String name) {
        return new MemoryUser(name, manager, this);
    }

    @Override
    public PermissionGroup getGroup(String name) {
        return new MemoryGroup(name, manager, this);
    }

    @Override
    public PermissionGroup getDefaultGroup(String worldName) {
        return this.manager.getGroup("Default");
    }

    @Override
    public void setDefaultGroup(PermissionGroup group, String worldName) {

    }

    @Override
    public String[] getWorldInheritance(String world) {
        return new String[0];
    }

    @Override
    public void setWorldInheritance(String world, String[] parentWorlds) {
        // Do Nothing
    }

    @Override
    public PermissionGroup[] getGroups() {
        return new PermissionGroup[0];
    }

    @Override
    public PermissionUser[] getRegisteredUsers() {
        return new PermissionUser[0];
    }

    @Override
    public void reload() throws PermissionBackendException {
        // Do Nothing

    }

    @Override
    public void dumpData(OutputStreamWriter writer) throws IOException {
        // Do Nothing
    }

}
