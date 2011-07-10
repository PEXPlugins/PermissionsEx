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
package ru.tehkode.permissions.file;

import java.util.*;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.ProxyPermissionGroup;
import ru.tehkode.permissions.backends.FileBackend;
import ru.tehkode.permissions.config.ConfigurationNode;
import ru.tehkode.permissions.events.PermissionEntityEvent;

/**
 *
 * @author code
 */
public class FileGroup extends ProxyPermissionGroup {

    protected ConfigurationNode node;

    public FileGroup(String name, PermissionManager manager, FileBackend backend) {
        super(new FileEntity(name, manager, backend, "groups"));
        
        this.node = ((FileEntity)this.backendEntity).getConfigNode();
    }
    
    @Override
    public String[] getParentGroupsNamesImpl() {
        return this.node.getStringList("inheritance", new LinkedList<String>()).toArray(new String[0]);
    }

    @Override
    public void setParentGroups(String[] parentGroups) {
        if (parentGroups == null) {
            return;
        }

        this.node.setProperty("inheritance", Arrays.asList(parentGroups));

        this.save();
        
        this.callEvent(new PermissionEntityEvent(this, PermissionEntityEvent.Action.INHERITANCE_CHANGED));
    }   
    
}
