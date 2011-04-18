package ru.tehkode.permissions.backends;

import ru.tehkode.permissions.PermissionBackend;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.config.Configuration;
import ru.tehkode.permissions.sql.SQLConnectionManager;
import ru.tehkode.permissions.sql.SQLPermissionGroup;
import ru.tehkode.permissions.sql.SQLPermissionUser;


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

    @Override
    public void reload() {
        // just do nothing...we are always "online", ie no persistence at all
    }


}