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

/**
 * Interface to get versioned obfuscation of CraftBukkit classes
 */
@file:JvmName("CraftBukkitInterface")
package ca.stellardrift.permissionsex.bukkit

import org.bukkit.Bukkit

private const val CRAFTBUKKIT_PREFIX = "org.bukkit.craftbukkit"
private var VERSION: String? = Bukkit.getServer().javaClass.let { serverClass ->
    when {
        serverClass.simpleName != "CraftServer" -> null
        serverClass.name == "org.bukkit.craftbukkit.CraftServer" -> "."
        else -> {
            var name: String = serverClass.name
            name = name.substring("org.bukkit.craftbukkit".length)
            name = name.substring(0, name.length - "CraftServer".length)
            name
        }
    }
}

/**
 * Get the versioned class name from a class name without the o.b.c prefix.
 *
 * @param simpleName The name of the class without the "org.bukkit.craftbukkit" prefix
 * @return The versioned class name, or `null` if not CraftBukkit.
 */
fun getCraftClassName(simpleName: String): String? {
    return if (VERSION == null) {
        null
    } else CRAFTBUKKIT_PREFIX + VERSION + simpleName
}

/**
 * Get the class from the name returned by passing `name` into [.getCBClassName]
 *
 * @param name The name of the class without the "org.bukkit.craftbukkit" prefix
 * @return The versioned class, or `null` if not CraftBukkit
 */
fun getCraftClass(name: String): Class<*>? {
    return if (VERSION == null) {
        null
    } else try {
        Class.forName(getCraftClassName(name))
    } catch (e: ClassNotFoundException) {
        null
    }
}
