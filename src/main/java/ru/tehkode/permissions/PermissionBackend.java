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

    public abstract PermissionUser getUser(String name);

    public abstract PermissionGroup getGroup(String name);

    public PermissionGroup createGroup(String name) {
        return this.getGroup(name);
    }

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

    public abstract PermissionGroup getDefaultGroup();

    /**
     * Return all registred groups
     * @return
     */
    public abstract PermissionGroup[] getGroups();

    /**
     * Return childs of specified group.
     * If specified group have no child empty or not exists array will be returned
     *
     * @return
     */
    public PermissionGroup[] getGroups(String groupName) {
        List<PermissionGroup> groups = new LinkedList<PermissionGroup>();

        for (PermissionGroup group : this.getGroups()) {
            if (group.isChildOf(groupName)) {
                groups.add(group);
            }
        }

        return groups.toArray(new PermissionGroup[]{});
    }

    /**
     * Return all registed users
     *
     * @return
     */
    public abstract PermissionUser[] getUsers();

    /**
     * Return users of specified group.
     * If there is no such group null will be returned
     *
     * @param groupName
     * @return
     */
    public PermissionUser[] getUsers(String groupName) {
        List<PermissionUser> users = new LinkedList<PermissionUser>();

        for (PermissionUser user : this.getUsers()) {
            if (user.inGroup(groupName)) {
                users.add(user);
            }
        }

        return users.toArray(new PermissionUser[]{});
    }

    public abstract void reload();
    protected static Map<String, Class> registedAliases = new HashMap<String, Class>();

    public static String getBackendClassName(String alias) {

        if (registedAliases.containsKey(alias)) {
            return registedAliases.get(alias).getName();
        }

        return alias;
    }

    public static Class getBackendClass(String alias) throws ClassNotFoundException {
        if (!registedAliases.containsKey(alias)) {
            return Class.forName(alias);
        }

        return registedAliases.get(alias);
    }

    public static void registerBackendAlias(String alias, Class<?> backendClass) {
        if (!PermissionBackend.class.isAssignableFrom(backendClass)) {
            throw new RuntimeException("Provided class should be subclass of PermissionBackend");
        }

        registedAliases.put(alias, backendClass);

        Logger.getLogger("Minecraft").info("[PermissionsEx] " + alias + " backend registred!");
    }

    public static String getBackendAlias(Class<?> backendClass) {
        if (registedAliases.containsValue(backendClass)) {
            for (String alias : registedAliases.keySet()) { // Is there better way to find key by value?
                if (registedAliases.get(alias).equals(backendClass)) {
                    return alias;
                }
            }
        }

        return backendClass.getName();
    }

    public static PermissionBackend getBackend(String backendName, PermissionManager manager, Configuration config) {
        return getBackend(backendName, manager, config, defaultBackend);
    }

    public static PermissionBackend getBackend(String backendName, PermissionManager manager, Configuration config, String fallBackBackend) {
        if (backendName == null || backendName.isEmpty()) {
            backendName = defaultBackend;
        }

        String className = getBackendClassName(backendName);

        try {
            Class backendClass = getBackendClass(backendName);

            Logger.getLogger("Minecraft").info("Switching to " + backendName + " backend");

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

    public abstract void dumpData(OutputStreamWriter writer) throws IOException;
}
