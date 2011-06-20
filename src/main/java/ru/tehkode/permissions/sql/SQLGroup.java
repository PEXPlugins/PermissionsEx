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

public class SQLGroup extends PermissionGroup {

    protected SQLEntity backend;

    public SQLGroup(String name, PermissionManager manager, SQLConnectionManager sql) {
        super(name, manager);

        this.backend = new SQLEntity(SQLEntity.Type.GROUP, name, sql);

        this.setName(this.backend.name);

        this.prefix = backend.getPrefix();
        this.suffix = backend.getSuffix();
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

        String userValue = this.backend.getOption(option, world);
        if (!userValue.isEmpty()) {
            return userValue;
        }

        for (PermissionGroup group : this.getParentGroups()) {
            String value = group.getOption(option, world);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }

        return userValue;
    }

    @Override
    public Map<String, String> getOptions(String world) {
        return this.backend.getOptions(world);
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
    protected String[] getParentGroupsNamesImpl() {
        return this.backend.getParentNames();
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
        if (permissions == null) {
            return;
        }

        backend.setPermissions(permissions, world);
    }

    @Override
    public void setOption(String permission, String value, String world) {
        if (permission == null) {
            return;
        }

        backend.setPermission(permission, value, world);
    }

    @Override
    public void setParentGroups(String[] parentGroups) {
        if (parentGroups == null) {
            return;
        }

        this.backend.setParents(parentGroups);
    }

    @Override
    public void removePermission(String permission, String world) {
        if (permission == null) {
            return;
        }

        backend.removePermission(permission, world);
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
    public void addPermission(String permission, String world) {
        backend.addPermission(permission, world);
    }

    @Override
    public void save() {
        this.backend.save();
    }

    @Override
    public void removeGroup() {
        this.backend.remove();
    }
}
