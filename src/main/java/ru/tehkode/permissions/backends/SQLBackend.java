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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import ru.tehkode.permissions.PermissionBackend;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.config.Configuration;
import ru.tehkode.permissions.sql.SQLConnectionManager;
import ru.tehkode.permissions.sql.SQLEntity;
import ru.tehkode.permissions.sql.SQLPermissionGroup;
import ru.tehkode.permissions.sql.SQLPermissionUser;
import ru.tehkode.utils.StringUtils;

/**
 *
 * @author code
 */
public class SQLBackend extends PermissionBackend {

    public SQLConnectionManager sql;

    public SQLBackend(PermissionManager manager, Configuration config) {
        super(manager, config);


        String dbDriver = config.getString("permissions.backends.sql.driver", "mysql");
        String dbUri = config.getString("permissions.backends.sql.uri", "");
        String dbUser = config.getString("permissions.backends.sql.user", "");
        String dbPassword = config.getString("permissions.backends.sql.password", "");

        if (dbUri == null || dbUri.isEmpty()) {
            config.setProperty("permissions.backends.sql.uri", "mysql://localhost/exampledb");
            config.setProperty("permissions.backends.sql.user", "databaseuser");
            config.setProperty("permissions.backends.sql.password", "databasepassword");

            config.save();

            Logger.getLogger("Minecraft").severe("SQL Connection are not configured, check config.yml");

            throw new RuntimeException("SQL Connection are not configured, check config.yml");
        }

        Logger.getLogger("Minecraft").info("Connecting sql database server on \"" + dbUri + "\"");

        sql = new SQLConnectionManager(dbUri, dbUser, dbPassword, dbDriver);

        Logger.getLogger("Minecraft").info("Successfuly connected to database");

        this.deployTables(dbDriver);
    }

    @Override
    public PermissionUser getUser(String name) {
        return new SQLPermissionUser(name, manager, this.sql);
    }

    @Override
    public PermissionGroup getGroup(String name) {
        return new SQLPermissionGroup(name, manager, this.sql);
    }

    @Override
    public PermissionGroup getDefaultGroup() {
        try {
            ResultSet result = this.sql.selectQuery("SELECT `name` FROM `permissions_entity` WHERE `type` = ? AND `default` = 1 LIMIT 1", SQLEntity.Type.GROUP.ordinal());

            if (!result.next()) {
                throw new RuntimeException("There is no default group set, this is serious issue");
            }

            return this.manager.getGroup(result.getString("name"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PermissionGroup[] getGroups() {
        String[] groupNames = SQLEntity.getEntitiesNams(sql, SQLEntity.Type.GROUP, false);
        List<PermissionGroup> groups = new LinkedList<PermissionGroup>();

        for (String groupName : groupNames) {
            groups.add(this.manager.getGroup(groupName));
        }

        return groups.toArray(new PermissionGroup[0]);
    }

    @Override
    public PermissionUser[] getUsers() {
        String[] userNames = SQLEntity.getEntitiesNams(sql, SQLEntity.Type.USER, false);
        PermissionUser[] users = new PermissionUser[userNames.length];

        int index = 0;
        for (String groupName : userNames) {
            users[index++] = this.manager.getUser(groupName);
        }

        return users;
    }

    protected final void deployTables(String driver) {
        if (this.sql.isTableExist("permissions")) {
            return;
        }

        //Logger.getLogger("Minecraft").severe("Please deploy bundled database dump.");

        //throw new RuntimeException("No database scheme found. Please upload bundled (default.sql) one.");

        try {
            InputStream databaseDumpStream = getClass().getResourceAsStream("/sql/" + driver + ".sql");

            if (databaseDumpStream == null) {
                throw new Exception("Can't find apporiate database dump for used database (" + driver + "). Is it bundled?");
            }

            String deploySQL = StringUtils.readStream(databaseDumpStream);

            Logger.getLogger("Minecraft").info("Deploying default database scheme");

            for (String sqlQuery : deploySQL.trim().split(";")) {
                sqlQuery = sqlQuery.trim();
                if (sqlQuery.isEmpty()) {
                    continue;
                }

                sqlQuery = sqlQuery + ";";

                this.sql.updateQuery(sqlQuery);
            }

            Logger.getLogger("Minecraft").info("Database scheme deploying complete.");

        } catch (Exception e) {
            Logger.getLogger("Minecraft").severe("Deploying of default scheme failed. Please init database manually using defaults.sql");
        }
    }

    @Override
    public void dumpData(OutputStreamWriter writer) throws IOException {

        // Users
        for (PermissionUser user : this.manager.getUsers()) {
            // Basic info (Prefix/Suffix)
            writer.append("INSERT INTO permissions_entity ( name, type, prefix, suffix ) VALUES ( '" + user.getName() + "', 1, '" + user.getOwnPrefix() + "','" + user.getOwnSuffix() + "' );\n");

            // Inheritance
            for (String group : user.getGroupsNames()) {
                writer.append("INSERT INTO permissions_inheritance ( child, parent, type ) VALUES ( '" + user.getName() + "', '" + group + "',  1);\n");
            }

            // Permissions
            for (Map.Entry<String, String[]> entry : user.getAllPermissions().entrySet()) {
                for (String permission : entry.getValue()) {
                    writer.append("INSERT INTO permissions ( name, type, permission, world ) VALUES ('" + user.getName() + "', 1, '" + permission + "', '" + entry.getKey() + "'); \n");
                }
            }

            // Options
            for (Map.Entry<String, Map<String, String>> entry : user.getAllOptions().entrySet()) {
                for (Map.Entry<String, String> option : entry.getValue().entrySet()) {
                    String value = option.getValue().replace("'", "\\'");
                    writer.append("INSERT INTO permissions ( name, type, permission, world, value ) VALUES ('" + user.getName() + "', 1, '" + option.getKey() + "', '" + entry.getKey() + "', '" + value + "' );\n");
                }
            }
        }

        PermissionGroup defaultGroup = manager.getDefaultGroup();

        // Groups
        for (PermissionGroup group : this.manager.getGroups()) {
            // Basic info (Prefix/Suffix)
            writer.append("INSERT INTO permissions_entity ( name, type, prefix, suffix, `default` ) VALUES ( '" + group.getName() + "', 0, '" + group.getOwnPrefix() + "','" + group.getOwnSuffix() + "', " + (group.equals(defaultGroup) ? "1" : "0") + " );\n");

            // Inheritance
            for (String parent : group.getParentGroupsNames()) {
                writer.append("INSERT INTO permissions_inheritance ( child, parent, type ) VALUES ( '" + group.getName() + "', '" + parent + "',  0);\n");
            }

            // Permissions
            for (Map.Entry<String, String[]> entry : group.getAllPermissions().entrySet()) {
                for (String permission : entry.getValue()) {
                    writer.append("INSERT INTO permissions ( name, type, permission, world ) VALUES ('" + group.getName() + "', 0, '" + permission + "', '" + entry.getKey() + "');\n");
                }
            }

            // Options
            for (Map.Entry<String, Map<String, String>> entry : group.getAllOptions().entrySet()) {
                for (Map.Entry<String, String> option : entry.getValue().entrySet()) {
                    String value = option.getValue().replace("'", "\\'");
                    writer.append("INSERT INTO permissions ( name, type, permission, world, value ) VALUES ('" + group.getName() + "', 0, '" + option.getKey() + "', '" + entry.getKey() + "', '" + value + "' );\n");
                }
            }
        }

        
        writer.flush();
    }

    @Override
    public void reload() {
        // just do nothing...we are always "online", ie no persistence at all
    }
}
