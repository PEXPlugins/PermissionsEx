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

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Locale;

public class Compatibility {
    private static final @Nullable MethodHandle locale;
    private static final boolean localeOnSpigot;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        // Locale
        MethodType localeType = MethodType.methodType(String.class);

        MethodHandle _locale = null;
        boolean _localeOnSpigot = false;
        try {
            try {
                _locale = lookup.findVirtual(Player.class, "getLocale", localeType);
            } catch (NoSuchMethodException ex) {
                try {
                    _locale = lookup.findVirtual(Player.Spigot.class, "getLocale", localeType);
                    _localeOnSpigot = true;
                } catch (NoSuchMethodException ex2) {
                    System.err.println("PermissionsEx: Unable to find method to get player locale!");
                    ex.printStackTrace();
                }
            }
        } catch (IllegalAccessException ex) {
            System.err.println("PermissionsEx: Unable to access method to get player locale!");
            ex.printStackTrace();
        }
        locale = _locale;
        localeOnSpigot = _localeOnSpigot;
    }

    private Compatibility() {
    }

    public static Locale getLocale(Player player) {
        String result = null;
        try {
            if (localeOnSpigot) {
                result = (String) locale.invoke(player.spigot());
            } else {
                if (locale != null) {
                    result = (String) locale.invoke(player);
                }
            }
        } catch (Throwable t) {
            player.getServer().getLogger().severe("PermissionsEx: Unable to determine locale for player " + player.getName());
        }
        return result == null ? Locale.getDefault() : CommanderKt.toLocale(result);
    }
}
