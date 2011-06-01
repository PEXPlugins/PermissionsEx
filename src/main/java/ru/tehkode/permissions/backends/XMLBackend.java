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

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import ru.tehkode.permissions.PermissionBackend;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.config.Configuration;
import ru.tehkode.permissions.xml.XMLGroup;
import ru.tehkode.permissions.xml.XMLUser;
import ru.tehkode.permissions.xml.XPath;

public class XMLBackend extends PermissionBackend {

    protected Document document;
    protected File documentFile;

    public XMLBackend(PermissionManager manager, Configuration config) {
        super(manager, config);

        this.documentFile = new File(config.getString("permissions.basedir", "plugins/PermissionsEx/"), config.getString("permissions.backends.xml.file", "permissions.xml"));

        this.loadDocument(this.documentFile);

        if (!documentFile.exists()) {
            this.deployDefaults(documentFile);
        }
    }

    protected final void loadDocument(File xmlFile) {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();

        try {
            if (!xmlFile.exists()) {
                this.document = domFactory.newDocumentBuilder().newDocument();
            } else {
                this.document = domFactory.newDocumentBuilder().parse(xmlFile);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected final void deployDefaults(File xmlFile) {
        try {
            Logger.getLogger("Minecraft").info("[PermissionsEx-XML] Deploying \"permissions.xml\"");
            
            this.document.appendChild(document.createElement("permissions"));
            
            XMLGroup defaultGroup = new XMLGroup("default", manager, this);
            defaultGroup.getXMLElement().setAttribute("default", "true");
            this.save();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public Document getDocument() {
        return this.document;
    }

    public Element getGroupsNode() {        
        Element groupsNode = XPath.getElement(this.document.getDocumentElement(), "/permissions/groups");
        
        if(groupsNode == null){
            groupsNode = this.document.createElement("groups");
            this.document.getDocumentElement().appendChild(groupsNode);
        }
        
        return groupsNode;
    }
    
    public Element getUsersNode() {
        Element usersNode = XPath.getElement(this.document.getDocumentElement(), "/permissions/users");
        
        if(usersNode == null){
            usersNode = this.document.createElement("users");
            this.document.getDocumentElement().appendChild(usersNode);
        }
        
        return usersNode;
    }

    public void save() {
        if (this.document == null) {
            return;
        }

        try {
            Transformer optimusPride = TransformerFactory.newInstance().newTransformer();

            optimusPride.setOutputProperty(OutputKeys.INDENT, "yes");

            optimusPride.transform(new DOMSource(document), new StreamResult(this.documentFile));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void dumpData(OutputStreamWriter writer) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PermissionGroup getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PermissionGroup getGroup(String name) {
        return new XMLGroup(name, manager, this);
    }

    @Override
    public PermissionUser getUser(String name) {
        return new XMLUser(name, manager, this);
    }

    @Override
    public PermissionGroup[] getGroups() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PermissionUser[] getUsers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void reload() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
