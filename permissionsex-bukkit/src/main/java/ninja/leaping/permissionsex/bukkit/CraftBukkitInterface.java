/**
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ninja.leaping.permissionsex.bukkit;

import org.bukkit.Bukkit;

/**
 * Interface to get versioned obfuscation of CraftBukkit classes
 */
public class CraftBukkitInterface {
    private static final String CRAFTBUKKIT_PREFIX = "org.bukkit.craftbukkit";
    private static final String VERSION;

    static {
        Class serverClass = Bukkit.getServer().getClass();
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
    public static Class getCBClass(String name) {
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
