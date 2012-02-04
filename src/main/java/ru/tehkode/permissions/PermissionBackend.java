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
import java.util.*;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.backends.GroupDataProvider;
import ru.tehkode.permissions.backends.UserDataProvider;
import ru.tehkode.permissions.bukkit.PermissionsEx;

/**
 *
 * @author t3hk0d3
 */
public abstract class PermissionBackend {
    
    protected final static String defaultBackend = "file";
    protected PermissionManager manager;
    protected ConfigurationSection config;
	protected boolean createUserRecords = false;
    
    protected PermissionBackend(PermissionManager manager, ConfigurationSection config) {
        this.manager = manager;
        this.config = config;
		
		this.createUserRecords = config.getBoolean("permissions.createUserRecords", this.createUserRecords);
    }

    /**
     * Backend initialization should be done here
     */
    public abstract void initialize();

    /**
     * Removes the specified group
     * 
     * @param groupName Name of the group which should be removed
     * @return true if group was removed, false if group has child groups
     */
    public boolean removeGroup(String groupName) {		
		PermissionGroup group = this.manager.getGroup(groupName);
        
        for (PermissionUser user : group.getUsersList()) {
            user.removeGroup(group);
        }
        
        group.remove();
        
        return true;
    }

    /**
     * Returns default group, a group that is assigned to a user without a group set
     * 
     * @return Default group instance
     */
    public abstract String getDefaultGroup(String worldName);

    /**
     * Set group as default group
     * 
     * @param group 
     */
    public abstract void setDefaultGroup(String groupName, String worldName);

    /**
     * Returns an array of world names of specified world name
     * 
     * @param world world name
     * @return Array of parent worlds. If there is no parent world return empty array
     */
    public abstract List<String> getWorldInheritance(String world);

    /**
     * Set world inheritance parents for specified world 
     * 
     * @param world world name which inheritance should be set
     * @param parentWorlds array of parent world names
     */
    public abstract void setWorldInheritance(String world, List<String> parentWorlds);

    /**
     * Return all registered groups
     * 
     * @return
     */
    public abstract Set<String> getGroups();


    /**
     * Return all registered users
     *
     * @return
     */
    public abstract Set<String> getRegisteredUsers();

	public abstract UserDataProvider getUserDataProvider(String userName);
	
	public abstract GroupDataProvider getGroupDataProvider(String groupName);

    /**
     * Reload backend (reread permissions file, reconnect to database, etc)
     */
    public abstract void reload();

    /**
     * Dump data to native backend format
     * 
     * @param writer Writer where dumped data should be written to
     * @throws IOException 
     */
    public abstract void dumpData(OutputStreamWriter writer) throws IOException;
    /**
     * Array of backend aliases
     */
    protected static Map<String, Class<? extends PermissionBackend>> registedAliases = new HashMap<String, Class<? extends PermissionBackend>>();

    /**
     * Return class name for alias
     * 
     * @param alias
     * @return Class name if found or alias if there is no such class name present
     */
    public static String getBackendClassName(String alias) {
        
        if (registedAliases.containsKey(alias)) {
            return registedAliases.get(alias).getName();
        }
        
        return alias;
    }

    /**
     * Returns Class object for specified alias, if there is no alias registered
     * then try to find it using Class.forName(alias)
     * 
     * @param alias
     * @return
     * @throws ClassNotFoundException 
     */
    public static Class<? extends PermissionBackend> getBackendClass(String alias) throws ClassNotFoundException {
        if (!registedAliases.containsKey(alias)) {
            return (Class<? extends PermissionBackend>) Class.forName(alias);
        }
        
        return registedAliases.get(alias);
    }

    /**
     * Register new alias for specified backend class
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
     * Return alias for specified backend class
     * If there is no such class registered the fullname of this class would
     * be returned using backendClass.getName();
     * 
     * @param backendClass
     * @return alias or class fullname when not found using backendClass.getName()
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
     * Returns new backend class instance for specified backendName
     * 
     * @param backendName Class name or alias of backend
     * @param config Configuration object to access backend settings
     * @return new instance of PermissionBackend object
     */
    public static PermissionBackend getBackend(String backendName, Configuration config) {
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
     * @param fallBackBackend name of backend that should be used if specified backend was not found or failed to initialize
     * @return new instance of PermissionBackend object
     */
    public static PermissionBackend getBackend(String backendName, PermissionManager manager, Configuration config, String fallBackBackend) {
        if (backendName == null || backendName.isEmpty()) {
            backendName = defaultBackend;
        }
        
        String className = getBackendClassName(backendName);
        
        try {
            Class<? extends PermissionBackend> backendClass = getBackendClass(backendName);
            
            Logger.getLogger("Minecraft").info("[PermissionsEx] Initializing " + backendName + " backend");
            
            Constructor<? extends PermissionBackend> constructor = backendClass.getConstructor(PermissionManager.class, Configuration.class);
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

	public boolean isCreateUserRecords() {
		return this.createUserRecords;
	}
}
