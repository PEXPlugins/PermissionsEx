package ru.tehkode.permissions;

import ru.tehkode.permissions.config.Configuration;


/**
 *
 * @author code
 */
public abstract class PermissionBackend {
    protected PermissionManager manager;

    protected Configuration config;

    protected PermissionBackend(PermissionManager manager, Configuration config){
        this.manager = manager;
        this.config  = config;
    }

    public abstract PermissionGroup getGroup(String name);
    public abstract PermissionUser  getUser(String name);
    public abstract PermissionGroup getDefaultGroup();

    public abstract void reload();
}
