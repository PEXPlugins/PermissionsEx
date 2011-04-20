package ru.tehkode.permissions.sql;

import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;

/**
 *
 * @author code
 */
public class SQLPermissionUser extends PermissionUser {

    protected SQLEntity backend;

    public SQLPermissionUser(String name, PermissionManager manager, SQLConnectionManager sql) {
        super(name, manager);

        this.backend = new SQLEntity(SQLEntity.Type.USER, name, sql);
    }

    @Override
    public void setSuffix(String suffix) {
        backend.setSuffix(suffix);
    }

    @Override
    public void setPrefix(String prefix) {
        backend.setPrefix(prefix);
    }

    @Override
    public void setPermissions(String[] permissions, String world) {
        backend.setPermissions(permissions, world);
    }

    @Override
    public void setPermission(String permission, String value, String world) {
        backend.setPermission(permission, value, world);
    }

    @Override
    public void setGroups(PermissionGroup[] parentGroups) {
        backend.setParents(parentGroups);
    }

    @Override
    public void removePermission(String permission, String world) {
        backend.removePermission(permission, world);
    }

    @Override
    public boolean isVirtual() {
        return backend.isVirtual();
    }

    @Override
    public String getSuffix() {
        return backend.getSuffix();
    }

    @Override
    public String getPrefix() {
        return backend.getPrefix();
    }

    @Override
    public String[] getPermissions(String world) {
        return backend.getPermissions(world);
    }

    @Override
    public String[] getGroupNames() {
        return backend.getParentNames();
    }

    @Override
    public void addPermission(String permission, String value, String world) {
        backend.addPermission(permission, value, world);
    }

    @Override
    public String getPermissionValue(String permission, String world, boolean inheritance) {
        if (permission == null) {
            return "";
        }

        String userValue = backend.getPermissionValue(permission, world, inheritance);
        if (!userValue.isEmpty()) {
            return userValue;
        }

        if (inheritance) {
            for (PermissionGroup group : this.getGroups()) {
                String value = group.getPermissionValue(permission, world, inheritance);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }

        return userValue;
    }

    @Override
    public void save() {
        this.backend.save();
    }

    @Override
    public void remove() {
        this.backend.remove();
    }
}
