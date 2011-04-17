/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.tehkode.permission.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author code
 */
public class SQLConnectionManager {
    protected String dbUri = "";
    protected String dbUser = "";
    protected String dbPassword = "";
    protected Connection db;

    public SQLConnectionManager(String dbName, String user, String password, String dbDriver) {
        try {
            Class.forName(dbDriver).newInstance();
            db = DriverManager.getConnection("jdbc:" + dbUri, dbUser, dbPassword);
        } catch (Exception e){
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

    public ResultSet query(String sql) throws SQLException {
        return this.query(sql, null);
    }

    public ResultSet query(String sql, Object... params) throws SQLException {
        PreparedStatement stmt = this.db.prepareStatement(sql);

        if(params != null){
            this.bindParams(stmt, params);
        }

        return stmt.executeQuery();
    }

    public Object queryOne(String sql, Object fallback, Object... params) {
        try {
        ResultSet result = this.query(sql, params);

        if(!result.next()){
            return fallback;
        }

        return result.getObject(1);

        } catch (SQLException e){
            Logger.getLogger("Minecraft").severe("SQL Error: " + e.getMessage());
        }

        return fallback;
    }

    protected void bindParams(PreparedStatement stmt, Object[] params) throws SQLException {
        for (int i = 1 ; i <= params.length ; i++){
            Object param = params[i - 1];
            stmt.setObject(i, param);
        }
    }
}
