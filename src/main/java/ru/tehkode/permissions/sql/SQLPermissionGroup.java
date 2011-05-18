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

public class SQLPermissionGroup extends PermissionGroup {

    protected SQLEntity backend;

    public SQLPermissionGroup(String name, PermissionManager manager, SQLConnectionManager sql) {
        super(name, manager);

        this.backend = new SQLEntity(SQLEntity.Type.GROUP, name, sql);
        this.prefix = backend.getPrefix();
        this.suffix = backend.getSuffix();
    }

    @Override
    public String getOption(String permission, String world, boolean inheritance) {
        if (permission == null) {
            return "";
        }

        String userValue = this.backend.getPermissionValue(permission, world, inheritance);
        if (!userValue.isEmpty()) {
            return userValue;
        }

        if (inheritance) {
            for (PermissionGroup group : this.getParentGroups()) {
                String value = group.getOption(permission, world, inheritance);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }

        return userValue;
    }

    @Override
    public Map<String, String> getOptions(String world) {
        return this.backend.getOptions(world);
    }

    @Override
    protected String[] getParentGroupsNamesImpl() {
        return this.backend.getParentNames();
    }

    @Override
    public void setPrefix(String prefix) {
        backend.setPrefix(prefix);
        this.prefix = prefix;
    }

    @Override
    public void setSuffix(String suffix) {
        backend.setSuffix(suffix);
        this.suffix = suffix;
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
    public void setParentGroups(PermissionGroup[] parentGroups) {
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
