package ru.tehkode.permissions.sql;

import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;

public class SQLPermissionGroup extends PermissionGroup {

    protected SQLEntity backend;

    public SQLPermissionGroup(String name, PermissionManager manager, SQLConnectionManager sql) {
        super(name, manager);

        this.backend = new SQLEntity(SQLEntity.Type.GROUP, name, sql);
    }

    @Override
    public String getPermissionValue(String permission, String world, boolean inheritance) {
        if (permission == null) {
            return "";
        }

        String userValue = this.backend.getPermissionValue(permission, world, inheritance);
        if (!userValue.isEmpty()) {
            return userValue;
        }

        if (inheritance) {
            for (PermissionGroup group : this.getParentGroups()) {
                String value = group.getPermissionValue(permission, world, inheritance);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }

        return userValue;
    }

    @Override
    protected String[] getParentGroupsNamesImpl() {
        return this.backend.getParentNames();
    }

    @Override
    public String getPrefix() {
        return this.backend.getPrefix();
    }

    @Override
    public String getSuffix() {
        return this.backend.getSuffix();
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
        if (permissions == null) {
            return;
        }

        backend.setPermissions(permissions, world);
    }

    @Override
    public void setPermission(String permission, String value, String world) {
        if (permission == null) {
            return;
        }

        backend.setPermission(permission, value, world);
    }

    @Override
    public void setParentGroups(PermissionGroup[] parentGroups) {
        if (parentGroups == null) {
            return;
        }

        this.backend.setParents(parentGroups);
    }

    @Override
    public void removePermission(String permission, String world) {
        if (permission == null) {
            return;
        }

        backend.removePermission(permission, world);
    }

    @Override
    public boolean isVirtual() {
        return backend.isVirtual();
    }

    @Override
    public String[] getPermissions(String world) {
        return backend.getPermissions(world);
    }

    @Override
    public void addPermission(String permission, String value, String world) {
        backend.addPermission(permission, value, world);
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
