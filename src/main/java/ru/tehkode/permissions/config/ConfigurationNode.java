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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Iterator;

/**
 *
 * @author code
 */
public class ConfigurationNode extends org.bukkit.util.config.ConfigurationNode {

	// Regexp credits to _303_ 
	protected static Pattern escapedSplit = Pattern.compile("(?:(?<=\\.)|(?<=^))(?:(?:\\`?)([^\\`]+)(?:\\`?))(?:(?=\\.)|(?=$))");

	public ConfigurationNode() {
		this(new HashMap<String, Object>());
	}

	public ConfigurationNode(Map<String, Object> root) {
		super(root);
	}

	public Map<String, Object> getRoot() {
		return root;
	}
	
	protected String[] splitPath(String path){
		String[] parts;
		if (path.contains("`")) {
			List<String> foundParts = new ArrayList<String>();
			Matcher matcher = escapedSplit.matcher(path);
			while (matcher.find()) {
				foundParts.add(matcher.group(1));
			}
			parts = foundParts.toArray(new String[0]);
		} else {
			parts = path.split("\\.");
		}
		
		return parts;
	}

	@Override
	public void setProperty(String path, Object value) {
		if (!path.contains(".")) {
			root.put(path, value);
			return;
		}
		
		if(value instanceof ConfigurationNode){
			value = ((ConfigurationNode)value).root;
		}

		String[] parts = this.splitPath(path);
		
		Map<String, Object> node = root;

		for (int i = 0; i < parts.length; i++) {
			Object o = node.get(parts[i]);

			// Found our target!
			if (i == parts.length - 1) {
				node.put(parts[i], value);
				return;
			}

			if (o == null || !(o instanceof Map)) {
				// This will override existing configuration data!
				o = new HashMap<String, Object>();
				node.put(parts[i], o);
			}

			node = (Map<String, Object>) o;
		}

	}
	
	@Override
	public void removeProperty(String path) {
        if (!path.contains(".")) {
            root.remove(path);
            return;
        }

        String[] parts = this.splitPath(path);
        Map<String, Object> node = root;

        for (int i = 0; i < parts.length; i++) {
            Object o = node.get(parts[i]);

            // Found our target!
            if (i == parts.length - 1) {
                node.remove(parts[i]);
                return;
            }

            node = (Map<String, Object>) o;
        }
    }

	@Override
	public Object getProperty(String path) {
		if (!path.contains(".")) {
			Object val = root.get(path);

			if (val == null) {
				return null;
			}

                        // Check, if we didn't get some Integers, Doubles
                        // for our mapping stuff.

                        else if (val instanceof Map) {
                           Map<String, Object> toAdd = new HashMap<String, Object>();
                           Iterator it = ((Map<Object, Object>) val).keySet().iterator();
                           while ( it.hasNext())
                           {
                               Object Key = it.next();
                               Map<Object, Object> itemMap = (Map<Object,Object>) val;
                               String item;
                               try {
                                   item = (String) Key;
                               }
                               catch (ClassCastException e)
                               {
                                   Object Value;
                                   if (e.getMessage().contains("Integer"))
                                   {
                                       Value = itemMap.get(Key);
                                       item = Integer.toString((Integer) Key);
                                       toAdd.put(item, Value);
                                       it.remove();
                                   }
                                   else if (e.getMessage().contains("Double"))
                                   {
                                       it.remove();
                                   }
                               }

                           }
                           for (String Key : toAdd.keySet())
                                ((Map<String, Object>) val).put(Key, toAdd.get(Key));

                        }
			return val;
		}

		String[] parts = this.splitPath(path);
		
		Map<String, Object> node = root;

		for (int i = 0; i < parts.length; i++) {
			Object o = node.get(parts[i]);

			if (o == null) {
				return null;
			}

			if (i == parts.length - 1) {
				return o;
			}

			try {
				node = (Map<String, Object>) o;
			} catch (ClassCastException e) {
				return null;
			}
		}

		return null;
	}

	/**
	 * Get a list of nodes. Non-valid entries will not be in the list.
	 * There will be no null slots. If the list is not defined, the
	 * default will be returned. 'null' can be passed for the default
	 * and an empty list will be returned instead. The node must be
	 * an actual node and cannot be just a boolean.
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
			if (o instanceof ConfigurationNode) {
				list.add((ConfigurationNode) o);
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
	 * @param path path to node (dot notation)
	 * @return ConfigurationNode or null
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

				if (entry.getValue() instanceof ConfigurationNode) {
					nodes.put(entry.getKey() , (ConfigurationNode) entry.getValue());
				} else if (entry.getValue() instanceof Map) {
					nodes.put(entry.getKey() , new ConfigurationNode((Map<String, Object>) entry.getValue()));
				}
			}

			return nodes;
		} else {
			return null;
		}
	}
}
