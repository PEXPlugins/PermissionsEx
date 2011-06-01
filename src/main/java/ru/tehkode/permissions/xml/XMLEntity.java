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

import java.util.Map;
import org.w3c.dom.Element;
import ru.tehkode.permissions.PermissionEntity;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.backends.XMLBackend;

public class XMLEntity extends PermissionEntity {

    protected Element element;
    protected XMLBackend backend;

    public XMLEntity(String name, PermissionManager manager, Element element, XMLBackend backend) {
        super(name, manager);

        this.element = element;
        this.backend = backend;
    }
    
    public Element getXMLElement(){
        System.out.println("entity: " + element);
        return this.element;
    }

    @Override
    public void addPermission(String permission, String world) {
        Element permissions = XPath.getElement(this.element, "//permissions" + (world == null || world.isEmpty() ? "[not(@world) or @world='']" : "[@world=\"\"]"));

        if (permissions == null) {
            permissions = backend.getDocument().createElement("permissions");
            if (world != null && !world.isEmpty()) {
                permissions.setAttribute("world", world);
            }

            this.element.appendChild(permissions);
        }

        Element permissionNode = backend.getDocument().createElement("permission");
        permissionNode.setTextContent(permission);

        permissions.insertBefore(element, permissions.getFirstChild());
    }

    @Override
    public Map<String, Map<String, String>> getAllOptions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, String[]> getAllPermissions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getOption(String permission, String world, boolean inheritance) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map<String, String> getOptions(String world) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String[] getPermissions(String world) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removePermission(String permission, String world) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void save() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setOption(String permission, String value, String world) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setPermissions(String[] permissions, String world) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
