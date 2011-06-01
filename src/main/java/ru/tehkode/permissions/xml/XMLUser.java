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
package ru.tehkode.permissions.xml;

import org.bukkit.Bukkit;
import org.w3c.dom.Element;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.ProxyPermissionUser;
import ru.tehkode.permissions.backends.XMLBackend;

public class XMLUser extends ProxyPermissionUser {

    protected XMLBackend backend;
    protected XMLEntity entity;

    public XMLUser(String playerName, PermissionManager manager, XMLBackend backend) {
        super(new XMLEntity(playerName, manager, prepareUserElement(playerName, backend), backend));

        this.backend = backend;
        this.entity = (XMLEntity)backendEntity;
    }

    protected static Element prepareUserElement(String userName, XMLBackend backend) {
        Element element = XPath.getElement(backend.getGroupsNode(), "/users/user[@name=\"" + userName + "\"]");
        
        if (element == null) {
            element = backend.getDocument().createElement("user");
            element.setAttribute("name", userName);
            backend.getGroupsNode().appendChild(element);
        }

        return element;
    }

    @Override
    protected String[] getGroupsNamesImpl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String[] getOwnPermissions(String world) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setGroups(PermissionGroup[] groups) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public Element getXMLElement(){
        return this.entity.getXMLElement();
    }
}
