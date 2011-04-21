package ru.tehkode.permissions;

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

    public PermissionGroup createGroup(String name){
        return this.getGroup(name);
    }

    public boolean removeGroup(String groupName){
        if(this.getGroups(groupName).length > 0){
            return false;
        }

        for(PermissionUser user : this.getUsers(groupName)){
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
     * If specified group have no childs empty or not exists array will be returned
     *
     * @param parentGroupName
     * @return
     */
    public PermissionGroup[] getGroups(String groupName) {
        List<PermissionGroup> groups = new LinkedList<PermissionGroup>();

        for(PermissionGroup group : this.getGroups()){
            if(group.isChildOf(groupName)){
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

        for (PermissionUser user : this.getUsers()){
            if(user.inGroup(groupName)){
                users.add(user);
            }
        }

        return users.toArray(new PermissionUser[]{});
    }

    public abstract void reload();


    protected static Map<String, String> registedAliases = new HashMap<String, String>();

    /**
     * @todo Make this thing reconfigurable and flexible. Think about it
     *
     * @param alias
     * @return String - classname for given alias
     */
    public static String getBackendClassName(String alias) {

        if(registedAliases.containsKey(alias)){
            return registedAliases.get(alias);
        }

        return alias;
    }

    public static void registerBackendAlias(String alias, Class<?> backendClass) {
        if(!PermissionBackend.class.isAssignableFrom(backendClass)){
            throw new RuntimeException("Provided class should be subclass of PermissionBackend");
        }

        registedAliases.put(alias, backendClass.getName());
    }

    public static PermissionBackend getBackend(String backendName, PermissionManager manager, Configuration config) {
        if (backendName == null || backendName.isEmpty()) {
            backendName = defaultBackend;
        }

        String className = getBackendClassName(backendName);

        try {
            return (PermissionBackend)Class.forName(className).getConstructor(PermissionManager.class, Configuration.class).newInstance(manager, config);
        } catch (ClassNotFoundException e) {
            Logger.getLogger("Minecraft").severe("Selected backend \""+backendName+"\" are not found. Falling back to file backend");

            if(!className.equals(getBackendClassName(defaultBackend))){
                return getBackend(defaultBackend, manager, config);
            } else {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
