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

package ru.tehkode.permissions.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author code
 */
public class ConfigurationNode extends org.bukkit.util.config.ConfigurationNode {

    public ConfigurationNode() {
        this(new HashMap<String, Object>());
    }

    public ConfigurationNode(Map<String, Object> root) {
        super(root);
    }

    public Map<String, Object> getRoot() {
        return root;
    }

    @Override
    public void setProperty(String path, Object value) {
         if(value instanceof ConfigurationNode){
             value = ((ConfigurationNode)value).getRoot();
         }

        super.setProperty(path, value);
     }

    /**
     * Gets a list of nodes. Non-valid entries will not be in the list.
     * There will be no null slots. If the list is not defined, the
     * default will be returned. 'null' can be passed for the default
     * and an empty list will be returned instead. The node must be
     * an actual node and cannot be just a boolean,
     *
     * @param path path to node (dot notation)
     * @param def default value or null for an empty list as default
     * @return list of integers
     */
    public List<ConfigurationNode> getNodesList(String path, List<ConfigurationNode> def) {
        List<Object> raw = getList(path);
        if (raw == null) {
            return def != null ? def : new ArrayList<ConfigurationNode>();
        }

        List<ConfigurationNode> list = new ArrayList<ConfigurationNode>();
        for (Object o : raw) {
            if (o instanceof ConfigurationNode){
                list.add((ConfigurationNode)o);
            }
            if (o instanceof Map) {
                list.add(new ConfigurationNode((Map<String, Object>) o));
            }
        }

        return list;
    }

    /**
     * Get a configuration node at a path. If the node doesn't exist or the
     * path does not lead to a node, null will be returned. A node has
     * key/value mappings.
     *
     * @param path
     * @return node or null
     */
    @Override
    public ConfigurationNode getNode(String path) {
        Object raw = getProperty(path);
        if (raw instanceof ConfigurationNode) {
            return (ConfigurationNode) raw;
        }

        if (raw instanceof Map) {
            return new ConfigurationNode((Map<String, Object>) raw);
        }

        return null;
    }

    /**
     * Get a list of nodes at a location. If the map at the particular location
     * does not exist or it is not a map, null will be returned.
     *
     * @param path path to node (dot notation)
     * @return map of nodes
     */
    public Map<String, ConfigurationNode> getNodesMap(String path) {
        Object o = getProperty(path);
        if (o == null) {
            return null;
        }
        
        if (o instanceof Map) {
            Map<String, ConfigurationNode> nodes =
                    new HashMap<String, ConfigurationNode>();

            for (Map.Entry<String, Object> entry : ((Map<String, Object>) o).entrySet()) {
                if (entry.getValue() instanceof ConfigurationNode){
                    nodes.put(entry.getKey(), (ConfigurationNode)entry.getValue());
                } else if (entry.getValue() instanceof Map) {
                    nodes.put(entry.getKey(), new ConfigurationNode((Map<String, Object>) entry.getValue()));
                }
            }

            return nodes;
        } else {
            return null;
        }
    }
}
