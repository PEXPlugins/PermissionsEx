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

package ru.tehkode.permissions.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.tehkode.utils.StringUtils;

/**
 *
 * @author code
 */
public class SQLConnectionManager {

    protected Connection db;

    public SQLConnectionManager(String uri, String user, String password, String dbDriver) {
        try {
            Class.forName(getDriverClass(dbDriver)).newInstance();
            db = DriverManager.getConnection("jdbc:" + uri, user, password);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        try {
            db.close();
        } catch (SQLException e) {
            Logger.getLogger("Minecraft").log(Level.WARNING, "Error while disconnecting from database: {0}", e.getMessage());
        } finally {
            db = null;
        }
    }

    public ResultSet selectQuery(String sql, Object... params) throws SQLException {
        PreparedStatement stmt = this.db.prepareStatement(sql);

        if (params != null) {
            this.bindParams(stmt, params);
        }

        return stmt.executeQuery();
    }

    public Object selectQueryOne(String sql, Object fallback, Object... params) {
        try {
            ResultSet result = this.selectQuery(sql, params);

            if (!result.next()) {
                return fallback;
            }

            return result.getObject(1);

        } catch (SQLException e) {
            Logger.getLogger("Minecraft").severe("SQL Error: " + e.getMessage());
        }

        return fallback;
    }

    public void updateQuery(String sql, Object... params) {
        try {
            PreparedStatement stmt = this.db.prepareStatement(sql);

            if (params != null) {
                this.bindParams(stmt, params);
            }

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void insert(String table, String[] fields, List<Object[]> rows) throws SQLException {
        String[] fieldValues = new String[fields.length];
        Arrays.fill(fieldValues, "?");
        String sql = "INSERT INTO " + table + " (" + StringUtils.implode(fields, ", ") + ") VALUES (" + StringUtils.implode(fieldValues, ", ") + ");";
        PreparedStatement stmt = this.db.prepareStatement(sql);

        for (Object[] params : rows) {
            this.bindParams(stmt, params);
            stmt.execute();
        }
    }

    public boolean isTableExist(String tableName) {
        try {
            return this.db.getMetaData().getTables(null, null, tableName, null).next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected static String getDriverClass(String alias) {

        if (alias.equals("mysql")) {
            alias = "com.mysql.jdbc.Driver";
        } else if (alias.equals("sqlite")) {
            alias = "org.sqlite.JDBC";
        } else if (alias.equals("postgre")) {
            alias = "org.postgresql.Driver";
        }

        return alias;
    }

    protected void bindParams(PreparedStatement stmt, Object[] params) throws SQLException {
        for (int i = 1; i <= params.length; i++) {
            Object param = params[i - 1];
            stmt.setObject(i, param);
        }
    }
}
