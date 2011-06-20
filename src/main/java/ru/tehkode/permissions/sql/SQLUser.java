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
package ru.tehkode.permissions.sql;

import java.util.Map;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;

/**
 *
 * @author code
 */
public class SQLUser extends PermissionUser {

    protected SQLEntity backend;

    public SQLUser(String name, PermissionManager manager, SQLConnectionManager sql) {
        super(name, manager);

        this.backend = new SQLEntity(SQLEntity.Type.USER, name, sql);

        this.setName(this.backend.name);

        this.prefix = backend.getPrefix();
        this.suffix = backend.getSuffix();
    }

    @Override
    public void setPrefix(String prefix) {
        backend.setPrefix(prefix);
        super.setPrefix(prefix);
    }

    @Override
    public void setSuffix(String suffix) {
        backend.setSuffix(suffix);
        super.setSuffix(suffix);
    }

    @Override
    public void setPermissions(String[] permissions, String world) {
        backend.setPermissions(permissions, world);

        this.clearCache();
    }

    @Override
    public void setOption(String permission, String value, String world) {
        backend.setPermission(permission, value, world);

        this.clearCache();
    }

    @Override
    public Map<String, Map<String, String>> getAllOptions() {
        return this.backend.getAllOptions();
    }

    @Override
    public Map<String, String[]> getAllPermissions() {
        return this.backend.getAllPermissions();
    }

    @Override
    public void setGroups(String[] parentGroups) {
        backend.setParents(parentGroups);

        this.clearCache();
    }

    @Override
    public void removePermission(String permission, String world) {
        backend.removePermission(permission, world);

        this.clearCache();
    }

    @Override
    public boolean isVirtual() {
        return backend.isVirtual();
    }

    @Override
    public String[] getOwnPermissions(String world) {
        return backend.getPermissions(world);
    }

    @Override
    public Map<String, String> getOptions(String world) {
        return backend.getOptions(world);
    }

    @Override
    protected String[] getGroupsNamesImpl() {
        return backend.getParentNames();
    }

    @Override
    public void addPermission(String permission, String world) {
        backend.addPermission(permission, world);

        this.clearCache();
    }

    @Override
    public String getOwnOption(String option, String world) {
        return this.backend.getOption(option, world);
    }

    @Override
    public String getOption(String option, String world) {
        if (option == null) {
            return "";
        }

        String userValue = backend.getOption(option, world);

        if (userValue == null || userValue.isEmpty()) {
            for (PermissionGroup group : this.getGroups()) {
                String value = group.getOption(option, world);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }

        return userValue;
    }

    @Override
    public void save() {
        this.backend.save();
    }

    @Override
    public void remove() {
        this.backend.remove();
    }
}
