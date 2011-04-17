/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.tehkode.permission.backends;

import org.bukkit.util.config.Configuration;
import ru.tehkode.permission.PermissionBackend;
import ru.tehkode.permission.PermissionGroup;
import ru.tehkode.permission.PermissionManager;
import ru.tehkode.permission.PermissionUser;
import ru.tehkode.permission.sql.SQLConnectionManager;
import ru.tehkode.permission.sql.SQLPermissionGroup;
import ru.tehkode.permission.sql.SQLPermissionUser;


/**
 *
 * @author code
 */
public class SQLBackend extends PermissionBackend {

    public SQLConnectionManager sql;

    public SQLBackend(PermissionManager manager, Configuration config){
        super(manager, config);

        
        String dbDriver   = config.getString("permission.backend.sql.driver", "com.mysql.jdbc.driver");
        String dbUri      = config.getString("permission.backend.sql.uri");
        String dbUser     = config.getString("permission.backend.sql.user");
        String dbPassword = config.getString("permission.backend.sql.password");

        if(dbUri.isEmpty()){
            config.setProperty("permission.backend.sql.uri", "mysql://localhost/exampledb");
            config.setProperty("permission.backend.sql.user", "databaseuser");
            config.setProperty("permission.backend.sql.password", "databasepassword");

            throw new RuntimeException("SQL Connection not configured, check config.yml");
        }

        sql = new SQLConnectionManager(dbUri, dbUser, dbPassword, dbDriver);
    }

    @Override
    public PermissionUser getUser(String name){
        return new SQLPermissionUser(name, manager, this);
    }

    @Override
    public PermissionGroup getGroup(String name){
        return new SQLPermissionGroup(name, manager, this);
    }

    @Override
    public PermissionGroup getDefaultGroup() {
        throw new UnsupportedOperationException("Not supported yet.");
    }


}