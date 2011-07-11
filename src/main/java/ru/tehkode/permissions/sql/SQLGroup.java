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

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.ProxyPermissionGroup;
import ru.tehkode.permissions.events.PermissionEntityEvent;

public class SQLGroup extends ProxyPermissionGroup {

    protected SQLEntity backend;

    public SQLGroup(String name, PermissionManager manager, SQLConnectionManager sql) {
        super(new SQLEntity(name, manager, SQLEntity.Type.GROUP, sql));

        this.backend = (SQLEntity) this.backendEntity;
    }

    @Override
    protected String[] getParentGroupsNamesImpl() {
        return this.backend.getParentNames();
    }

    @Override
    public void setParentGroups(String[] parentGroups) {
        if (parentGroups == null) {
            return;
        }

        this.backend.setParents(parentGroups);
        
        this.callEvent(new PermissionEntityEvent(this, PermissionEntityEvent.Action.INHERITANCE_CHANGED));
    }
}
