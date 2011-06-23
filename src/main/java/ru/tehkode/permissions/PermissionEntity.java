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

    public String getName() {
        return this.name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public boolean has(String permission) {
        return this.has(permission, Bukkit.getServer().getWorlds().get(0).getName());
    }

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

    public void setPermission(String permission, String value) {
        this.setOption(permission, value, "");
    }

    public void removePermission(String permission) {
        this.removePermission(permission, "");
    }

    public String getOption(String option) {
        return this.getOption(option, "", "");
    }

    public String getOption(String option, String world) {
        return this.getOption(option, world, "");
    }

    public boolean getOptionBoolean(String optionName, String world, boolean defaultValue) {
        String option = this.getOption(optionName, world, Boolean.toString(defaultValue));

        if ("false".equalsIgnoreCase(option)) {
            return false;
        } else if ("true".equalsIgnoreCase(option)) {
            return true;
        }

        return defaultValue;
    }

    public int getOptionInteger(String optionName, String world, int defaultValue) {
        String option = this.getOption(optionName, world, Integer.toString(defaultValue));

        try {
            return Integer.parseInt(option);
        } catch (NumberFormatException e) {
        }

        return defaultValue;
    }

    public double getOptionDouble(String optionName, String world, double defaultValue) {
        String option = this.getOption(optionName, world, Double.toString(defaultValue));

        try {
            return Double.parseDouble(option);
        } catch (NumberFormatException e) {
        }

        return defaultValue;
    }

    protected boolean explainExpression(String expression) {
        if (expression == null || expression.isEmpty()) {
            return false;
        }

        return !expression.startsWith("-"); // If expression have - (minus) before then that mean expression are negative
    }

    public void setPermissions(String[] permission) {
        this.setPermissions(permission, null);
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setSuffix(String postfix) {
        this.suffix = postfix;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(" + this.getName() + ")";
    }

    public boolean isVirtual() {
        return this.virtual;
    }

    public String[] getTimedPermissions(String world) {
        if (world == null) {
            world = "";
        }

        if (!this.timedPermissions.containsKey(world)) {
            return new String[0];
        }

        return this.timedPermissions.get(world).toArray(new String[0]);
    }

    public int getTimedPermissionLifetime(String permission, String world) {
        if (!this.timedPermissionsTime.containsKey(world + ":" + permission)) {
            return 0;
        }

        return (int) (this.timedPermissionsTime.get(world + ":" + permission).longValue() - (System.currentTimeMillis() / 1000L));
    }

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

    public void removeTimedPermission(String permission, String world) {
        if (!this.timedPermissions.containsKey(world)) {
            return;
        }

        this.timedPermissions.get(world).remove(permission);
        this.timedPermissions.remove(world + ":" + permission);
    }

    public abstract String[] getPermissions(String world);

    public abstract Map<String, String[]> getAllPermissions();

    public abstract Map<String, Map<String, String>> getAllOptions();

    public abstract Map<String, String> getOptions(String world);

    public abstract String getOption(String option, String world, String defaultValue);

    public void setOption(String permission, String value) {
        this.setOption(permission, value, null);
    }

    public abstract void setOption(String option, String value, String world);

    public abstract void addPermission(String permission, String world);

    public abstract void removePermission(String permission, String world);

    public abstract void setPermissions(String[] permissions, String world);

    public abstract void save();

    public abstract void remove();

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
    /**
     * Pattern cache
     */
    protected static HashMap<String, Pattern> patternCache = new HashMap<String, Pattern>();

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

    protected static String prepareRegexp(String expression) {
        return expression.replace(".", "\\.").replace("*", "(.*)");
    }
}
