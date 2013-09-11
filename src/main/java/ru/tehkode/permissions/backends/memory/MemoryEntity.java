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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import ru.tehkode.permissions.PermissionEntity;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.backends.MemoryBackend;

public class MemoryEntity extends PermissionEntity {

    protected MemoryBackend backend;

    protected HashMap<String, String> worldPrefix;
    protected HashMap<String, String> worldSuffix;
    protected HashMap<String, String[]> worldsPermissions;
    protected Map<String, Map<String, String>> worldsOptions;
    protected String[] commonPermissions;
    protected Map<String, Set<String>> parents;

    public MemoryEntity(String name, PermissionManager manager, MemoryBackend backend) {
        super(name, manager);

        this.backend = backend;

        worldPrefix = new HashMap<String, String>();
        worldSuffix = new HashMap<String, String>();
        worldsPermissions = new HashMap<String, String[]>();
        worldsOptions = new HashMap<String, Map<String, String>>();
        parents = new HashMap<String, Set<String>>();
        commonPermissions = new String[0];
    }

    @Override
    public String getPrefix(String worldName) {
        return worldPrefix.containsKey(worldName) ? worldPrefix.get(worldName) : "";
    }

    @Override
    public void setPrefix(String prefix, String worldName) {
        worldPrefix.put(worldName, prefix);
        this.save();
    }

    @Override
    public String getSuffix(String worldName) {
        return worldSuffix.containsKey(worldName) ? worldSuffix.get(worldName) : "";
    }

    @Override
    public void setSuffix(String suffix, String worldName) {
        worldSuffix.put(worldName, suffix);
        this.save();
    }

    @Override
    public String[] getPermissions(String world) {
        String[] perms = worldsPermissions.containsKey(world) ? worldsPermissions.get(world)
                : new String[0];
        if (commonPermissions != null) {
            perms = (String[]) ArrayUtils.addAll(perms, commonPermissions);
        }
        return perms;
    }

    @Override
    public Map<String, String[]> getAllPermissions() {
        return worldsPermissions;
    }

    @Override
    public void setPermissions(String[] permissions, String world) {
        if (world == "") {
            commonPermissions = permissions;
        } else {
            worldsPermissions.put(world, permissions);
        }
        
        this.save();
    }

    @Override
    public String getOption(String option, String world, String defaultValue) {
        if (worldsOptions.containsKey(world)) {
            Map<String, String> worldOption = worldsOptions.get(world);
            if (worldOption.containsKey(option)) {
                return worldOption.get(option);
            }
        }
        return defaultValue;
    }

    @Override
    public void setOption(String option, String value, String world) {
        Map<String, String> newOption = new HashMap<String, String>();
        newOption.put(option, value);
        worldsOptions.put(world, newOption);

        this.save();
    }

    @Override
    public Map<String, String> getOptions(String world) {
        return worldsOptions.containsKey(world) ? worldsOptions.get(world)
                : new HashMap<String, String>();
    }

    @Override
    public Map<String, Map<String, String>> getAllOptions() {
        return worldsOptions;
    }

    @Override
    public void save() {

    }

    @Override
    public void remove() {
        // Do Nothing
    }

    @Override
    public String[] getWorlds() {
        Set<String> worlds = new HashSet<String>();

        worlds.addAll(worldsOptions.keySet());
        worlds.addAll(worldsPermissions.keySet());

        return worlds.toArray(new String[0]);
    }

    public void setParents(String[] parentGroups, String worldName) {
        parents.put(worldName, new HashSet<String>(Arrays.asList(parentGroups)));
    }

    public String[] getParentNames(String worldName) {
        if (this.parents == null) {
        }

        if (this.parents.containsKey(worldName)) {
            return this.parents.get(worldName).toArray(new String[0]);
        }

        return new String[0];
    }

}
