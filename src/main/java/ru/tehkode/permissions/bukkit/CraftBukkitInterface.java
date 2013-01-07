package ru.tehkode.permissions.bukkit;

import org.bukkit.Bukkit;

/**
 * Interface to get versioned obfuscation of CraftBukkit classes
 */
public class CraftBukkitInterface {
    private static final String CRAFTBUKKIT_PREFIX; 
    private static final String VERSION;
    private static final boolean IS_BUKKIT_FORGE;
    static {
        Class serverClass = Bukkit.getServer().getClass();			
        if (!serverClass.getSimpleName().equals("CraftServer") && !serverClass.getSimpleName().equals("BukkitServer")) {
            VERSION = null;
        } else if (serverClass.getName().equals("org.bukkit.craftbukkit.CraftServer") || serverClass.getName().equals("keepcalm.mods.bukkit.bukkitAPI.BukkitServer")) {
            VERSION = "";
        } else {
            String name = serverClass.getName();
            name = name.substring("org.bukkit.craftbukkit".length());
            name = name.substring(0, name.length() - "CraftServer".length());
            VERSION = name;
        }
        
        if (serverClass.getPackage().getName().startsWith("keepcalm.mods.bukkit")) {
        	IS_BUKKIT_FORGE = true;
        	CRAFTBUKKIT_PREFIX = "keepcalm.mods.bukkit.bukkitAPI";
        }
        else {
        	CRAFTBUKKIT_PREFIX = "org.bukkit.craftbukkit";
        	IS_BUKKIT_FORGE = false;
        }
    }

    private CraftBukkitInterface() {}

    /**
     * Get the versioned class name from a class name without the o.b.c prefix.
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
