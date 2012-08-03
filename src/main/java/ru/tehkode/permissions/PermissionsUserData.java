package ru.tehkode.permissions;

import java.util.List;

public interface PermissionsUserData {

    /**
     * Returns user groups in specified world
     * @param worldName
     */
    public List<String> getGroups(String worldName);

    /**
     * Set groups in specified world
     *
     * @param groups
     * @param worldName
     */
    public void setGroups(List<PermissionGroup> groups, String worldName);
}
