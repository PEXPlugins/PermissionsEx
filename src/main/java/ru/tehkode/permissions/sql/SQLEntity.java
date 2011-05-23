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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.tehkode.permissions.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import ru.tehkode.permissions.PermissionGroup;

/**
 *
 * @author code
 */
public class SQLEntity {

    public enum Type {

        GROUP, USER
    }
    protected SQLConnectionManager db;
    protected String name;
    protected boolean virtual;
    protected Map<String, List<String>> worldsPermissions = null;
    protected Map<String, Map<String, String>> worldsOptions = null;
    protected List<String> commonPermissions = null;
    protected Map<String, String> commonOptions = null;
    protected List<String> parents = null;
    protected String prefix = null;
    protected String suffix = null;
    protected Type type;

    public SQLEntity(SQLEntity.Type type, String name, SQLConnectionManager db) {
        this.db = db;
        this.name = name;
        this.type = type;

        this.fetchInfo();
    }

    public static String[] getEntitiesNams(SQLConnectionManager sql, Type type, boolean defaultOnly) {
        try {
            List<String> entities = new LinkedList<String>();

            ResultSet result = sql.selectQuery("SELECT name FROM permissions_entity WHERE type = ? " + (defaultOnly ? " AND default = 1" : ""), type.ordinal());
            while (result.next()) {
                entities.add(result.getString("name"));
            }

            return entities.toArray(new String[0]);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;

        this.updateInfo();
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;

        this.updateInfo();
    }

    public boolean isVirtual() {
        return virtual;
    }

    public String[] getParentNames() {
        if (this.parents == null) {
            this.fetchInheritance();
        }

        return this.parents.toArray(new String[0]);
    }

    public String[] getPermissions(String world) {
        List<String> permissions = new LinkedList<String>();

        if (commonPermissions == null) {
            this.fetchPermissions();
        }

        if (world != null && !world.isEmpty() && this.worldsPermissions != null) {
            List<String> worldPermissions = this.worldsPermissions.get(world);
            if (worldPermissions != null) {
                permissions.addAll(worldPermissions);
            }
        }

        permissions.addAll(commonPermissions);

        return permissions.toArray(new String[0]);
    }

    public String getPermissionValue(String permission, String world, boolean inheritance) {
        if ((world == null || world.isEmpty()) && this.commonOptions.containsKey(permission)) {
            return this.commonOptions.get(permission);
        }

        return "";
    }

    public void addPermission(String permission, String world) {
        this.setPermission(permission, null, world);
    }

    public void setPermission(String permission, String value, String world) {
        if(permission == null || permission.isEmpty()){
            return;
        }

        Boolean newOption = true;
        if (this.worldsPermissions == null) {
            this.fetchPermissions();
        }

        if (world != null && !world.isEmpty() && worldsOptions.get(world) != null && worldsOptions.get(world).containsKey(permission)) {
            newOption = false;
        }

        if (newOption && this.commonOptions.containsKey(permission)) {
            newOption = false;
        }

        if (value == null) {
            value = "";
        }

        if (world == null) {
            world = "";
        }

        if (newOption) {
            this.db.updateQuery("INSERT INTO permissions (name, permission, value, world, type) VALUES (?, ?, ?, ?, ?)", this.name, permission, value, world, this.type.ordinal());
        } else {
            this.db.updateQuery("UPDATE permissions SET value = ? WHERE name = ? AND type = ? AND permission = ?", value, this.name, this.type.ordinal(), permission);
        }

        if (this.isVirtual()) {
            this.save();
        }

        // Refresh permissions
        this.fetchPermissions();
    }

    public void removePermission(String permission, String world) {
        if (world == null) {
            world = "";
        }

        this.db.updateQuery("DELETE FROM permissions WHERE name = ? AND permission = ? AND world = ? AND type = ?", this.name, permission, world, this.type.ordinal());

        this.fetchPermissions();
    }

    public void setParents(PermissionGroup[] parentGroups) {
        try {
            // Clean out existing records
            this.db.updateQuery("DELETE FROM permissions_inheritance WHERE child = ? AND type = ?", this.name, this.type.ordinal());

            List<Object[]> rows = new LinkedList<Object[]>();
            for (PermissionGroup group : parentGroups) {
                if (group == null || group.getName().isEmpty()) {
                    continue;
                }

                rows.add(new Object[]{this.name, group.getName(), this.type.ordinal()});
            }

            this.db.insert("permissions_inheritance", new String[]{"child", "parent", "type"}, rows);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (this.isVirtual()) {
            this.save();
        }

        //reload inherirance
        this.parents = null;
        this.fetchInheritance();
    }

    public Map<String, String> getOptions(String world) {
        Map<String, String> options = new HashMap<String, String>();

        // put common options
        options.putAll(this.commonOptions);
        // override them with world-specific
        if (this.worldsOptions.containsKey(world)) {
            options.putAll(this.worldsOptions.get(world));
        }

        return options;
    }

    public void setPermissions(String[] permissions, String world) {
        this.db.updateQuery("DELETE FROM permissions WHERE name = ? AND type = ? AND world = ? AND value = ''", this.name, this.type.ordinal(), world);
        for (String permission : permissions) {
            this.setPermission(permission, "", world);
        }

        if (this.isVirtual()) {
            this.save();
        }

        this.fetchPermissions();
    }

    public void save() {
        this.updateInfo();
    }

    public void remove() {
        // clear inheritance info
        this.db.updateQuery("DELETE FROM permissions_inheritance WHERE child = ? AND type = ?", this.name, this.type.ordinal());
        // clear permissions
        this.db.updateQuery("DELETE FROM permissions WHERE name = ? AND type = ?", this.name, this.type.ordinal());
        // clear info
        this.db.updateQuery("DELETE FROM permissions_entity WHERE name = ? AND type = ?", this.name, this.type.ordinal());

        this.virtual = true;
        this.commonOptions.clear();
        this.commonPermissions.clear();
        this.worldsOptions.clear();
        this.worldsPermissions.clear();
        this.parents.clear();
    }

    protected void updateInfo() {
        String sql;
        if (this.isVirtual()) { // This section are suspicious, here was problem which are resolved mysticaly. Keep eye on it.
            sql = "INSERT INTO permissions_entity (prefix, suffix, name, type) VALUES (?, ?, ?, ?)";
        } else {
            sql = "UPDATE permissions_entity SET prefix = ?, suffix = ? WHERE name = ? AND type = ?";
        }

        this.db.updateQuery(sql, this.prefix, this.suffix, this.name, this.type.ordinal());

        this.virtual = false;
    }

    protected final void fetchPermissions() {
        this.worldsOptions = new HashMap<String, Map<String, String>>();
        this.worldsPermissions = new HashMap<String, List<String>>();
        this.commonOptions = new HashMap<String, String>();
        this.commonPermissions = new LinkedList<String>();

        try {
            ResultSet results = this.db.selectQuery("SELECT permission, world, value FROM permissions WHERE name = ? AND type = ? ORDER BY id DESC", this.name, this.type.ordinal());
            while (results.next()) {
                String permission = results.getString("permission").trim();
                String world = results.getString("world").trim();
                String value = results.getString("value").trim();

                // @TODO: to this in more optimal way
                if (value.isEmpty()) {
                    if (!world.isEmpty()) {
                        List<String> worldPermissions = this.worldsPermissions.get(world);
                        if (worldPermissions == null) {
                            worldPermissions = new LinkedList<String>();
                            this.worldsPermissions.put(world, worldPermissions);
                        }

                        worldPermissions.add(permission);
                    } else {
                        this.commonPermissions.add(permission);
                    }
                } else {
                    if (!world.isEmpty()) {
                        Map<String, String> worldOptions = this.worldsOptions.get(world);
                        if (worldOptions == null) {
                            worldOptions = new HashMap<String, String>();
                            worldsOptions.put(world, worldOptions);
                        }

                        worldOptions.put(permission, value);
                    } else {
                        commonOptions.put(permission, value);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected final void fetchInheritance() {
        try {
            this.parents = new LinkedList<String>();
            ResultSet results = this.db.selectQuery("SELECT parent FROM permissions_inheritance WHERE child = ? AND type = ?", this.name, this.type.ordinal());

            while (results.next()) {
                this.parents.add(results.getString("parent"));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected final void fetchInfo() {
        try {
            ResultSet result = this.db.selectQuery("SELECT prefix, suffix FROM permissions_entity WHERE name = ? AND type = ? LIMIT 1", this.name, this.type.ordinal());
            if (result.next()) {
                this.prefix = result.getString("prefix");
                this.suffix = result.getString("suffix");
                this.virtual = false;
            } else {
                this.prefix = "";
                this.suffix = "";
                this.virtual = true;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
