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
package ru.tehkode.permissions;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.config.Configuration;

/**
 *
 * @author code
 */
public abstract class PermissionBackend {

    protected final static String defaultBackend = "file";
    protected PermissionManager manager;
    protected Configuration config;

    protected PermissionBackend(PermissionManager manager, Configuration config) {
        this.manager = manager;
        this.config = config;
    }
    
    /**
     * Actual backend initialization should be done here
     */
    public abstract void initialize();

    /**
     * Returns new PermissionUser object for specified player name
     * 
     * @param name Player name
     * @return Return PermissionUser for specified player, or null if there is error.
     */
    public abstract PermissionUser getUser(String name);

    /**
     * Returns new PermissionGroup object for specified group name
     * 
     * @param name Group name
     * @return PermissionGroup object, or null if something is wrong
     */
    public abstract PermissionGroup getGroup(String name);

    /*
     * Creates new group with specified name, or returns PermissionGroup object,
     * if there is such group already exists.
     * 
     * @param name Group name
     * @returns PermissionGroup instance for specified group 
     */
    public PermissionGroup createGroup(String name) {
        return this.manager.getGroup(name);
    }

    /**
     * Removes group with specified group name
     * 
     * @param groupName Name of group which should be removed
     * @return true if group actually removed, false if group have child groups
     */
    public boolean removeGroup(String groupName) {
        if (this.getGroups(groupName).length > 0) {
            return false;
        }

        for (PermissionUser user : this.getUsers(groupName)) {
            user.removeGroup(groupName);
        }

        this.manager.getGroup(groupName).remove();

        return true;
    }

    /**
     * Returns default group, group which are assigned to users without specified group
     * 
     * @return Default group instance
     */
    public abstract PermissionGroup getDefaultGroup();
    
    /**
     * Set specified group as default
     * 
     * @param group 
     */
    public abstract void setDefaultGroup(PermissionGroup group);

    /**
     * Returns array with of world names which specified world inherit
     * 
     * @param world World name
     * @return Array of parent world, if there is no such than just empty array
     */
    public abstract String[] getWorldInheritance(String world);
    
    /**
     * Set world inheritance parents for specified world 
     * 
     * @param world world name which inheritance should be set
     * @param parentWorlds array of world names
     */
    public abstract void setWorldInheritance(String world, String[] parentWorlds);

    /**
     * Return all registered groups
     * @return
     */
    public abstract PermissionGroup[] getGroups();

    /**
     * Return all registered users
     *
     * @return
     */
    public abstract PermissionUser[] getUsers();
    
    
    /**
     * Return child groups of specified groups only.
     * If specified group have no child empty or not exists empty array would be returned
     * 
     * @param groupName
     * @return 
     */
    public PermissionGroup[] getGroups(String groupName){
        return this.getGroups(groupName, false);
    }
    
    /**
     * Return child groups of specified group.
     * If specified group have no child empty or not exists empty array would be
     * returned
     * 
     * @param groupName 
     * @param inheritance - If true than full list of descendants would be returned. 
     * 
     * @return
     */
    public PermissionGroup[] getGroups(String groupName, boolean inheritance) {
        List<PermissionGroup> groups = new LinkedList<PermissionGroup>();

        for (PermissionGroup group : this.getGroups()) {
            if (group.isChildOf(groupName, inheritance)) {
                groups.add(group);
            }
        }

        return groups.toArray(new PermissionGroup[]{});
    }
    
    /**
     * Return users of specified group only.
     * If there is no such group null will be returned
     *
     * @param groupName
     * @return
     */
    public PermissionUser[] getUsers(String groupName) {
        return getUsers(groupName, false);
    }

    /**
     * Return users of specified group (and child groups)
     * If there is no such group null will be returned
     *
     * @param groupName
     * @param inheritance - If true than returned users list of descendant groups. 
     * @return
     */
    public PermissionUser[] getUsers(String groupName, boolean inheritance) {
        List<PermissionUser> users = new LinkedList<PermissionUser>();

        for (PermissionUser user : this.getUsers()) {
            if (user.inGroup(groupName, inheritance)) {
                users.add(user);
            }
        }

        return users.toArray(new PermissionUser[]{});
    }
    
    /**
     * Reload backend (reread permissions file, reconnect to database, etc)
     */
    public abstract void reload();
    
    /**
     * Dump data in native for backend format
     * 
     * @param writer Writer where dumped data should be written
     * @throws IOException 
     */
    public abstract void dumpData(OutputStreamWriter writer) throws IOException;
    
    /**
     * Array of backend aliases
     */
    protected static Map<String, Class<? extends PermissionBackend>> registedAliases = new HashMap<String, Class<? extends PermissionBackend>>();

    /**
     * Return class name specified alias, if there is no class found than alias
     * would be returned
     * 
     * @param alias
     * @return
     */
    public static String getBackendClassName(String alias) {

        if (registedAliases.containsKey(alias)) {
            return registedAliases.get(alias).getName();
        }

        return alias;
    }

    /**
     * Returns Class object for specified alias, if there is no alias registered
     * than trying to find it using Class.forName(alias)
     * 
     * @param alias
     * @return 
     * @throws ClassNotFoundException 
     */
    public static Class getBackendClass(String alias) throws ClassNotFoundException {
        if (!registedAliases.containsKey(alias)) {
            return Class.forName(alias);
        }

        return registedAliases.get(alias);
    }

    /**
     * Register new alias for specified Backend class
     * 
     * @param alias
     * @param backendClass 
     */
    public static void registerBackendAlias(String alias, Class<? extends PermissionBackend> backendClass) {
        if (!PermissionBackend.class.isAssignableFrom(backendClass)) {
            throw new RuntimeException("Provided class should be subclass of PermissionBackend");
        }

        registedAliases.put(alias, backendClass);

        Logger.getLogger("Minecraft").info("[PermissionsEx] " + alias + " backend registered!");
    }

    /**
     * Return alias for specified Backend class
     * If there is no such class registered, than fullname of such class would
     * be returned using backendClass.getName();
     * 
     * @param backendClass
     * @return 
     */
    public static String getBackendAlias(Class<? extends PermissionBackend> backendClass) {
        if (registedAliases.containsValue(backendClass)) {
            for (String alias : registedAliases.keySet()) { // Is there better way to find key by value?
                if (registedAliases.get(alias).equals(backendClass)) {
                    return alias;
                }
            }
        }

        return backendClass.getName();
    }
    
    /**
     * Returns new Backend class instance for specified backendName
     * 
     * @param backendName Class name or alias of backend
     * @param config Configuration object to access backend settings
     * @return new instance of PermissionBackend object
     */
    public static PermissionBackend getBackend(String backendName, Configuration config){
        return getBackend(backendName, PermissionsEx.getPermissionManager(), config, defaultBackend);
    }

    /**
     * Returns new Backend class instance for specified backendName
     * 
     * @param backendName Class name or alias of backend
     * @param manager PermissionManager object
     * @param config Configuration object to access backend settings
     * @return new instance of PermissionBackend object
     */
    public static PermissionBackend getBackend(String backendName, PermissionManager manager, Configuration config) {
        return getBackend(backendName, manager, config, defaultBackend);
    }

    /**
     * Returns new Backend class instance for specified backendName
     * 
     * @param backendName Class name or alias of backend
     * @param manager PermissionManager object
     * @param config Configuration object to access backend settings
     * @param fallBackBackend name of backend which should be used if specified backend not found or failed to initialize
     * @return 
     */
    public static PermissionBackend getBackend(String backendName, PermissionManager manager, Configuration config, String fallBackBackend) {
        if (backendName == null || backendName.isEmpty()) {
            backendName = defaultBackend;
        }

        String className = getBackendClassName(backendName);

        try {
            Class backendClass = getBackendClass(backendName);

            Logger.getLogger("Minecraft").info("[PermissionsEx] Initializing " + backendName + " backend");

            Constructor<PermissionBackend> constructor = backendClass.getConstructor(PermissionManager.class, Configuration.class);
            return (PermissionBackend) constructor.newInstance(manager, config);
        } catch (ClassNotFoundException e) {

            Logger.getLogger("Minecraft").warning("[PermissionsEx] Specified backend \"" + backendName + "\" are not found.");

            if (fallBackBackend == null) {
                throw new RuntimeException(e);
            }

            if (!className.equals(getBackendClassName(fallBackBackend))) {
                return getBackend(fallBackBackend, manager, config, null);
            } else {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
