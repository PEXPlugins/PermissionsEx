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

import org.w3c.dom.Element;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.ProxyPermissionGroup;
import ru.tehkode.permissions.backends.XMLBackend;

public class XMLGroup extends ProxyPermissionGroup {

    protected XMLEntity entity;
    protected XMLBackend backend;

    public XMLGroup(String groupName, PermissionManager manager, XMLBackend backend) {
        super(new XMLEntity(groupName, manager, getGroupElement(groupName, backend), backend));

        this.backend = backend;
        this.entity = (XMLEntity)this.backendEntity;
    }

    protected static Element getGroupElement(String groupName, XMLBackend backend) {
        Element element = XPath.getElement(backend.getGroupsNode(), "/groups/group[@name=\"" + groupName + "\"]");
        System.out.println("before: " + element);
        if (element == null) {
            element = backend.getDocument().createElement("group");
            element.setAttribute("name", groupName);
            backend.getGroupsNode().appendChild(element);
        }
        System.out.println("after: " + element);

        return element;
    }

    @Override
    public String[] getOwnPermissions(String world) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected String[] getParentGroupsNamesImpl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void removeGroup() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setParentGroups(PermissionGroup[] parentGroups) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Element getXMLElement(){
        return this.entity.getXMLElement();
    }
}
