/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.tehkode.permission;

import org.bukkit.util.config.Configuration;

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
}
