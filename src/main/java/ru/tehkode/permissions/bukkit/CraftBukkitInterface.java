package ru.tehkode.permissions.bukkit;

import org.bukkit.Bukkit;

/**
 * Interface to get versioned obfuscation of CraftBukkit classes
 */
public class CraftBukkitInterface {
    private static final String CRAFTBUKKIT_PREFIX = "org.bukkit.craftbukkit";
    private static final String VERSION;
    
    static {
    	String version = "";
		if (!checkVersion(version)) {
			StringBuilder builder = new StringBuilder();
			for (int a = 0; a < 10; a++) {
				for (int b = 0; b < 10; b++) {
					for (int c = 0; c < 10; c++) {
						// Format:
						// [package].v1_4_R1.[trail]
						builder.setLength(0);
						builder.append('v').append(a).append('_').append(b).append('_').append('R').append(c);
						version = builder.toString();
						if (checkVersion(version)) {
							a = b = c = 10;
						}
					}
				}
			}
		}
		VERSION = version.isEmpty() ? "" : ("."+version);
    }
    
	private static boolean checkVersion(String version) {
		try {
			if (version.isEmpty()) {
				Class.forName("net.minecraft.server.World");
			} else {
				Class.forName("net.minecraft.server." + version + ".World");
			}
			return true;
		} catch (ClassNotFoundException ex) {
			return false;
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
