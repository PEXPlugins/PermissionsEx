/*
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
