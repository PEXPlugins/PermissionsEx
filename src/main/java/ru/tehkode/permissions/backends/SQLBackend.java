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

import java.sql.ResultSet;
import java.sql.SQLException;
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

        this.deployTables();
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
        PermissionGroup[] groups = new PermissionGroup[groupNames.length];

        int index = 0;
        for (String groupName : groupNames) {
            groups[index++] = this.manager.getGroup(groupName);
        }

        return groups;
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

    protected final void deployTables() {
        if (this.sql.isTableExist("permissions")) {
            return;
        }

        //Logger.getLogger("Minecraft").severe("Please deploy bundled database dump.");

        //throw new RuntimeException("No database scheme found. Please upload bundled (default.sql) one.");

        try {
            String deploySQL = StringUtils.readStream(getClass().getResourceAsStream("/sql/default.sql"));

            Logger.getLogger("Minecraft").info("Deploying default database scheme");

            for (String sqlQuery : deploySQL.trim().split(";")) {
                sqlQuery = sqlQuery.trim();
                if(sqlQuery.isEmpty()){
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
    public void reload() {
        // just do nothing...we are always "online", ie no persistence at all
    }
}
