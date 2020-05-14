/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2020 zml [at] stellardrift [dot] ca and PermissionsEx contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ca.stellardrift.permissionsex.bukkit;

import org.bukkit.Bukkit;

/**
 * Interface to get versioned obfuscation of CraftBukkit classes
 */
public class CraftBukkitInterface {
    private static final String CRAFTBUKKIT_PREFIX = "org.bukkit.craftbukkit";
    private static final String VERSION;

    static {
        Class<?> serverClass = Bukkit.getServer().getClass();
        if (!serverClass.getSimpleName().equals("CraftServer")) {
            VERSION = null;
        } else if (serverClass.getName().equals("org.bukkit.craftbukkit.CraftServer")) {
            VERSION = ".";
        } else {
            String name = serverClass.getName();
            name = name.substring("org.bukkit.craftbukkit".length());
            name = name.substring(0, name.length() - "CraftServer".length());
            VERSION = name;
        }
    }

    private CraftBukkitInterface() {
    }

    /**
     * Get the versioned class name from a class name without the o.b.c prefix.
     *
     * @param simpleName The name of the class without the "org.bukkit.craftbukkit" prefix
     * @return The versioned class name, or {@code null} if not CraftBukkit.
     */
    public static String getCBClassName(String simpleName) {
        if (VERSION == null) {
            return null;
        }

        return CRAFTBUKKIT_PREFIX + VERSION + simpleName;
    }

    /**
     * Get the class from the name returned by passing {@code name} into {@link #getCBClassName(String)}
     *
     * @param name The name of the class without the "org.bukkit.craftbukkit" prefix
     * @return The versioned class, or {@code null} if not CraftBukkit
     */
    public static Class<?> getCBClass(String name) {
        if (VERSION == null) {
            return null;
        }

        try {
            return Class.forName(getCBClassName(name));
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
