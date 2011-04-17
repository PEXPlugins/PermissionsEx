package ru.tehkode.permission.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import ru.tehkode.permission.PermissionGroup;
import ru.tehkode.permission.PermissionManager;
import ru.tehkode.permission.backends.SQLBackend;

/**
 *
 * @author code
 */
public class SQLPermissionGroup extends PermissionGroup {

    protected SQLBackend backend;
    protected Set<String> permissions = null;

    public SQLPermissionGroup(String name, PermissionManager manager, SQLBackend backend) {
        super(name, manager);

        this.backend = backend;
    }

    @Override
    public Set<PermissionGroup> getParentGroups() {
        Set<PermissionGroup> parentGroups = new HashSet<PermissionGroup>();

        String parents = (String) backend.sql.queryOne("SELECT parents FROM groups WHERE groups = ? LIMIT 1", "", this.name);
        if (!parents.isEmpty()) {
            for (String parent : parents.split(",")) {
                parentGroups.add(this.manager.getGroup(parent.trim()));
            }
        }

        return parentGroups;
    }

    @Override
    protected Set<String> getPermissions(String world) {
        if(permissions != null){
            return permissions;
        }

        permissions = new LinkedHashSet<String>();

        try {
            List<String> worldPermissions = new LinkedList<String>();
            List<String> commonPermissions = new LinkedList<String>();

            ResultSet results = this.backend.sql.query("SELECT permission, world FROM group_permissions WHERE group = ? AND (world = '' OR world = ?) AND value = ''", this.name, world);
            while (results.next()) {
                if (results.getString("world").isEmpty()) {
                    worldPermissions.add(results.getString("permission"));
                } else {
                    commonPermissions.add(results.getString("permission"));
                }
            }

            permissions.addAll(worldPermissions); // At first world specific permissions
            permissions.addAll(commonPermissions); // Then common
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return permissions;
    }

    @Override
    public String getPrefix() {
        return (String) backend.sql.queryOne("SELECT prefix FROM groups WHERE groups = ? LIMIT 1", "", this.name);
    }

    @Override
    public String getPostfix() {
        return (String) backend.sql.queryOne("SELECT postfix FROM groups WHERE groups = ? LIMIT 1", "", this.name);
    }

    @Override
    public String getPermissionValue(String permission, String world, boolean inheritance) {
        String value = (String) this.backend.sql.queryOne("SELECT value FROM group_permissions WHERE group = ? AND permission = ? AND world = ? LIMIT 1", "", this.getName(), permission, world);
        if (!value.isEmpty()) {
            return value;
        }

        if (inheritance) {
            for (PermissionGroup group : this.getParentGroups()) {
                value = group.getPermissionValue(permission, world, inheritance);
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }

        return "";
    }

    @Override
    public void addPermission(String permission, String value, String world) {
        this.setPermission(permission, value, world);
    }

    @Override
    public void setPermission(String permission, String value, String world) {
        try {
            // @TODO: make this moment more cross-database, remove mysql-only ON DUPLICATE for compatibility with postgre and sqlite
            backend.sql.query("INSERT INTO group_permissions (group, permission, value, world) VALUES (?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE value = ?", this.name, permission, value, world, value);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removePermission(String permission, String world) {
        try {
            backend.sql.query("DELETE FROM group_permissions WHERE group = ? AND permission = ? AND world = ?", this.getName(), permission, world);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
