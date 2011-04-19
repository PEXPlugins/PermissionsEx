package ru.tehkode.permissions.backends;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
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
            ResultSet result = this.sql.query("SELECT name FROM permissions_entity WHERE type = ? AND default = 1", SQLEntity.Type.GROUP.ordinal());

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
        InputStreamReader reader = new InputStreamReader(this.getClass().getResourceAsStream("/sql/default.sql"));

        StringBuilder dstBuffer = new StringBuilder();

        try {
            CharBuffer tmpBuffer = CharBuffer.allocate(1024);
            while (reader.read(tmpBuffer) > 0) {
                reader.read(tmpBuffer);
                dstBuffer.append(tmpBuffer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Logger.getLogger("Minecraft").info(dstBuffer.toString());

    }

    @Override
    protected void removeGroupActually(String name) {
        // Not yet impelented ... this is argueble moment
    }

    @Override
    public void reload() {
        // just do nothing...we are always "online", ie no persistence at all
    }
}
