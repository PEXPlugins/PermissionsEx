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

import java.util.LinkedList;
import java.util.List;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XPath {

    protected static javax.xml.xpath.XPath xpath = XPathFactory.newInstance().newXPath();

    public static Element getElement(Element element, String path) {
        try {
            return (Element) xpath.evaluate(path, element, XPathConstants.NODE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Element> getElementList(Element element, String path) {
        try {
            NodeList set = (NodeList) xpath.evaluate(path, element, XPathConstants.NODESET);
            List<Element> result = new LinkedList<Element>();

            for (int i = 0; i < set.getLength(); i++) {
                Node node = set.item(i);
                if (node instanceof Element) {
                    result.add((Element) node);
                }
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
