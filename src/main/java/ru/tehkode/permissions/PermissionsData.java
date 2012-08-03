package ru.tehkode.permissions;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PermissionsData {

    /**
     * Returns all permissions for specified world
     *
     * @param worldName
     * @return
     */
    public List<String> getPermissions(String worldName);

    /**
     * Set permissions for specified world
     *
     * @param permissions
     * @param worldName
     */
    public void setPermissions(List<String> permissions, String worldName);

    /**
     * Returns ALL permissions for each world
     *
     * @return
     */
    public Map<String, List<String>> getPermissionsMap();

    /**
     * Returns worlds where entity has permissions/options
     *
     * @return
     */
    public Set<String> getWorlds();

    /**
     * Returns prefix in specified world
     *
     * @param worldName
     * @return
     */
    @Deprecated
    public String getPrefix(String worldName);

    /**
     * Sets prefix in specified world
     *
     * @param prefix
     * @param worldName
     */
    @Deprecated
    public void setPrefix(String prefix, String worldName);

    /**
     * Returns suffix in specified world
     *
     * @param worldName
     * @return
     */
    @Deprecated
    public String getSuffix(String worldName);

    /**
     * Set suffix in specified world
     *
     * @param prefix
     * @param worldName
     */
    @Deprecated
    public void setSuffix(String suffix, String worldName);

    /**
     * Returns option value in specified worlds.
     * null if option is not defined in that world
     *
     * @param option
     * @param worldName
     * @return
     */
    public String getOption(String option, String worldName);

    /**
     * Sets option value in specified world
     *
     * @param option
     * @param worldName
     * @param value
     */
    public void setOption(String option, String worldName, String value);

    /**
     * Returns all options in specified world
     *
     * @param worldName
     * @return
     */
    public Map<String, String> getOptions(String worldName);

    /**
     * Returns ALL options in each world
     *
     * @return
     */
    public Map<String, Map<String, String>> getOptionsMap();


    /**
     * Returns true if this User/Group exists only in server memory
     *
     * @return
     */
    public boolean isVirtual();

    /**
     * Commit data to backend
     */
    public void save();

    /**
     * Completely remove data from backend
     */
    public void remove();
}
