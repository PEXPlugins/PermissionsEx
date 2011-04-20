package ru.tehkode.permissions;

import java.util.LinkedList;
import java.util.List;
import ru.tehkode.permissions.config.Configuration;

/**
 *
 * @author code
 */
public abstract class PermissionBackend {

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
}
