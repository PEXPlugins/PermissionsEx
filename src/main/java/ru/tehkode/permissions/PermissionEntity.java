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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;

/**
 *
 * @author code
 */
public abstract class PermissionEntity {

    protected PermissionManager manager;
    private String name;
    protected boolean virtual = true;
    protected String prefix = "";
    protected String suffix = "";
    protected Map<String, List<String>> timedPermissions = new ConcurrentHashMap<String, List<String>>();
    protected Map<String, Long> timedPermissionsTime = new ConcurrentHashMap<String, Long>();

    public PermissionEntity(String name, PermissionManager manager) {
        this.manager = manager;
        this.name = name;
    }

    /**
     * Returns native name of permission entity (User or Group)
     * In case of User should be equal to Player's name on server
     * 
     * @return Name of Entity 
     */
    public String getName() {
        return this.name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    /**
     * Returns prefix of entity
     * 
     * @return prefix 
     */
    public String getPrefix() {
        return this.prefix;
    }

    /**
     * Set prefix to specified value
     * 
     * @param prefix 
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns suffix of this entity
     * 
     * @return suffix
     */
    public String getSuffix() {
        return this.suffix;
    }

    /**
     * Set suffix to specified value
     * 
     * @param suffix 
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    /**
     * Checks if entity have specified permission in default server world
     * 
     * @param permission Permission to check
     * @return true if this entity have such permission, false otherwise
     */
    public boolean has(String permission) {
        return this.has(permission, Bukkit.getServer().getWorlds().get(0).getName());
    }

    /**
     * Check if entity have specified permission in specified world
     * 
     * @param permission Permission to check
     * @param world World to check permission in
     * @return true if this entity have such permission, false otherwise
     */
    public boolean has(String permission, String world) {
        if (permission != null && permission.isEmpty()) { // empty permission for public access :)
            return true;
        }

        String expression = getMatchingExpression(permission, world);

        if (this.manager.isDebug()) {
            Logger.getLogger("Minecraft").info("User " + this.getName() + " checked for \"" + permission + "\", \"" + expression + "\" found");
        }

        return this.explainExpression(expression);
    }

    /**
     * Returns all entity permissions in specified world
     * 
     * @param world
     * @return Array of permission expressions
     */
    public abstract String[] getPermissions(String world);

    /**
     * Returns permissions for all worlds
     * Common permissions stored as "" (empty string) world.
     * 
     * @return Map with world name as key and permissions array as value
     */
    public abstract Map<String, String[]> getAllPermissions();

    /**
     * Add permissions for specified world
     * 
     * @param permission
     * @param world 
     */
    public abstract void addPermission(String permission, String world);

    /**
     * Add permission in common space (all worlds)
     * 
     * @param permission 
     */
    public void addPermission(String permission) {
        this.addPermission(permission, "");
    }

    /**
     * Remove specified permission from world
     * 
     * @param permission
     * @param world 
     */
    public abstract void removePermission(String permission, String world);

    /**
     * Remove specified permission from all worlds
     * 
     * @param permission 
     */
    public void removePermission(String permission) {
        for (String world : this.getAllPermissions().keySet()) {
            this.removePermission(permission, world);
        }
    }

    /**
     * Set specified permissions for specified world
     * 
     * @param permissions Array of permissions to set
     * @param world World to set permissions for
     */
    public abstract void setPermissions(String[] permissions, String world);

    /**
     * Set specified permissions in common space (all world)
     * 
     * @param permission 
     */
    public void setPermissions(String[] permission) {
        this.setPermissions(permission, "");
    }

    /**
     * Get option with specified name for specified world
     * 
     * @param option Name of option
     * @param world World to look for
     * @param defaultValue Default value to fallback if such option is not found
     * @return Value of option as String
     */
    public abstract String getOption(String option, String world, String defaultValue);

    /**
     * Return specified option.
     * Option would be looked in common options
     * 
     * @param option Option name
     * @return option value, or empty string if option not found
     */
    public String getOption(String option) {
        // @todo Replace empty string with null
        return this.getOption(option, "", "");
    }

    /**
     * Return specified option for specified world
     * 
     * @param option Option name
     * @param world World to look in
     * @return option value, or empty string if option not found
     */
    public String getOption(String option, String world) {
        // @todo Replace empty string with null
        return this.getOption(option, world, "");
    }
    
        /**
     * Return integer value of specified permission
     * 
     * @param optionName
     * @param world
     * @param defaultValue
     * @return option value, or specified defaultValue if option not found or not integer number
     */
    public int getOptionInteger(String optionName, String world, int defaultValue) {
        String option = this.getOption(optionName, world, Integer.toString(defaultValue));

        try {
            return Integer.parseInt(option);
        } catch (NumberFormatException e) {
        }

        return defaultValue;
    }

    /**
     * Returns double value of specified option
     * 
     * @param optionName
     * @param world
     * @param defaultValue
     * @return option value, or specified defaultValue if option is not found or not double number
     */
    public double getOptionDouble(String optionName, String world, double defaultValue) {
        String option = this.getOption(optionName, world, Double.toString(defaultValue));

        try {
            return Double.parseDouble(option);
        } catch (NumberFormatException e) {
        }

        return defaultValue;
    }

    /**
     * Returns boolean value of specified option
     * 
     * @param optionName
     * @param world
     * @param defaultValue
     * @return option value, or specified defaultValue if option not found or value is not boolean
     */
    public boolean getOptionBoolean(String optionName, String world, boolean defaultValue) {
        String option = this.getOption(optionName, world, Boolean.toString(defaultValue));

        if ("false".equalsIgnoreCase(option)) {
            return false;
        } else if ("true".equalsIgnoreCase(option)) {
            return true;
        }

        return defaultValue;
    }
    
    /**
     * Set specified option for specified world
     * 
     * @param option Option name
     * @param value Value to set, null to remove
     * @param world World name
     */
    public abstract void setOption(String option, String value, String world);

    /**
     * Set specified option for all worlds, can be overriden by world specific option
     * 
     * @param option Option name
     * @param value Value to set, null to remove
     */
    public void setOption(String permission, String value) {
        this.setOption(permission, value, "");
    }

    /**
     * Get options for specified world
     * 
     * @param world
     * @return Option value in string
     */
    public abstract Map<String, String> getOptions(String world);

    /**
     * Returns options for all worlds
     * Common options stored as "" (empty string) world.
     * 
     * @return Map with world name as key and map of options as value
     */
    public abstract Map<String, Map<String, String>> getAllOptions();

    /**
     * Save in-memory data to storage backend
     */
    public abstract void save();

    /**
     * Remove entity data from backend
     */
    public abstract void remove();

    /**
     * Return state of entity
     * 
     * @return true if entity is only in-memory
     */
    public boolean isVirtual() {
        return this.virtual;
    }

    /**
     * Return entity timed (temporary) permission for specified world
     * 
     * @param world
     * @return Array of permissions
     */
    public String[] getTimedPermissions(String world) {
        if (world == null) {
            world = "";
        }

        if (!this.timedPermissions.containsKey(world)) {
            return new String[0];
        }

        return this.timedPermissions.get(world).toArray(new String[0]);
    }

    /**
     * Returns remaining lifetime of specified permission in specified world
     * 
     * @param permission Name of permission
     * @param world
     * @return seconds of remaining lifetime of timed permission, 0 if permission is transient
     */
    public int getTimedPermissionLifetime(String permission, String world) {
        if (world == null) {
            world = "";
        }

        if (!this.timedPermissionsTime.containsKey(world + ":" + permission)) {
            return 0;
        }

        return (int) (this.timedPermissionsTime.get(world + ":" + permission).longValue() - (System.currentTimeMillis() / 1000L));
    }

    /**
     * Adds timed permission to specified world for specified amount of seconds
     * 
     * @param permission
     * @param world
     * @param lifeTime Lifetime of permission in seconds, 0 for transient permission (world disappear only after server reload)
     */
    public void addTimedPermission(final String permission, String world, int lifeTime) {
        if (world == null) {
            world = "";
        }

        if (!this.timedPermissions.containsKey(world)) {
            this.timedPermissions.put(world, new LinkedList<String>());
        }

        this.timedPermissions.get(world).add(permission);

        final String finalWorld = world;

        if (lifeTime > 0) {
            TimerTask task = new TimerTask() {

                @Override
                public void run() {
                    removeTimedPermission(permission, finalWorld);
                }
            };

            this.manager.registerTask(task, lifeTime);

            this.timedPermissionsTime.put(world + ":" + permission, (System.currentTimeMillis() / 1000L) + lifeTime);
        }
    }

    /**
     * Removes specified timed permission from specified world
     * 
     * @param permission
     * @param world 
     */
    public void removeTimedPermission(String permission, String world) {
        if (world == null) {
            world = "";
        }

        if (!this.timedPermissions.containsKey(world)) {
            return;
        }

        this.timedPermissions.get(world).remove(permission);
        this.timedPermissions.remove(world + ":" + permission);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!getClass().equals(obj.getClass())) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        final PermissionEntity other = (PermissionEntity) obj;
        return this.name.equals(other.name);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + this.getName() + ")";
    }

    protected boolean explainExpression(String expression) {
        if (expression == null || expression.isEmpty()) {
            return false;
        }

        return !expression.startsWith("-"); // If expression have - (minus) before then that mean expression are negative
    }

    protected String getMatchingExpression(String permission, String world) {
        return this.getMatchingExpression(this.getPermissions(world), permission);
    }

    protected String getMatchingExpression(String[] permissions, String permission) {
        for (String expression : permissions) {
            if (isMatches(expression, permission, true)) {
                return expression;
            }
        }

        return null;
    }

    protected static String prepareRegexp(String expression) {
        return expression.replace(".", "\\.").replace("*", "(.*)");
    }
    /**
     * Pattern cache
     */
    protected static HashMap<String, Pattern> patternCache = new HashMap<String, Pattern>();

    /**
     * Checks if specified permission matches specified permission expression
     * 
     * @param expression permission expression - what user have in his permissions list (permission.nodes.*)
     * @param permission permission which are checking for (permission.node.some.subnode)
     * @param additionalChecks check for parent node matching
     * @return 
     */
    public static boolean isMatches(String expression, String permission, boolean additionalChecks) {
        String localExpression = expression;

        if (localExpression.startsWith("-")) {
            localExpression = localExpression.substring(1);
        }

        if (!patternCache.containsKey(localExpression)) {
            patternCache.put(localExpression, Pattern.compile(prepareRegexp(localExpression)));
        }

        if (patternCache.get(localExpression).matcher(permission).matches()) {
            return true;
        }

        if (additionalChecks && localExpression.endsWith(".*") && isMatches(localExpression.substring(0, localExpression.length() - 2), permission, false)) {
            return true;
        }
        /*
        if (additionalChecks && !expression.endsWith(".*") && isMatches(expression + ".*", permission, false)) {
        return true;
        }
         */
        return false;
    }
}
