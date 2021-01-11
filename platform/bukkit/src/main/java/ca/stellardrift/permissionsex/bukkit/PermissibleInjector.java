/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2021 zml [at] stellardrift [dot] ca and PermissionsEx contributors
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
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static ca.stellardrift.permissionsex.bukkit.CraftBukkitInterface.craftClassName;

/**
 * This class handles injection of {@link Permissible}s into {@link Player}s for various server implementations.
 */
abstract class PermissibleInjector {
    static final PermissibleInjector[] INJECTORS = {
        new ClassPresencePermissibleInjector("net.glowstone.entity.GlowHumanEntity", "permissions", true),
        new ClassPresencePermissibleInjector("org.getspout.server.entity.SpoutHumanEntity", "permissions", true),
        new ClassNameRegexPermissibleInjector(
            "org.getspout.spout.player.SpoutCraftPlayer",
            "perm",
            false,
            Pattern.compile("org\\.getspout\\.spout\\.player\\.SpoutCraftPlayer")
        ),
        new ClassPresencePermissibleInjector(
            craftClassName("entity.CraftHumanEntity"),
            "perm",
            true
        )
    };

    protected final @Nullable String clazzName;
    protected final String fieldName;
    protected final boolean copyValues;

    PermissibleInjector(final @Nullable String clazzName, String fieldName, boolean copyValues) {
        this.clazzName = clazzName;
        this.fieldName = fieldName;
        this.copyValues = copyValues;
    }

    /**
     * Attempts to inject {@code permissible} into {@code player},
     *
     * @param player      The player to have {@code permissible} injected into
     * @param permissible The permissible to inject into {@code player}
     * @return the old permissible if the injection was successful, otherwise null
     * @throws NoSuchFieldException when the permissions field could not be found in the Permissible
     * @throws IllegalAccessException when things go very wrong
     */
    public @Nullable Permissible inject(final Player player, final @Nullable Permissible permissible) throws NoSuchFieldException, IllegalAccessException {
        final @Nullable Field permField = getPermissibleField(player);
        if (permField == null) {
            return null;
        }
        Permissible oldPerm = (Permissible) permField.get(player);
        if (copyValues && permissible instanceof PermissibleBase) {
            PermissibleBase newBase = (PermissibleBase) permissible;
            PermissibleBase oldBase = (PermissibleBase) oldPerm;
            copyValues(oldBase, newBase);
        }

        // Inject permissible
        permField.set(player, permissible);
        return oldPerm;
    }

    public Permissible getPermissible(final Player player) throws NoSuchFieldException, IllegalAccessException {
        return (Permissible) getPermissibleField(player).get(player);
    }

    private @Nullable Field getPermissibleField(final Player player) throws NoSuchFieldException {
        final Class<?> humanEntity;
        try {
            humanEntity = Class.forName(clazzName);
        } catch (ClassNotFoundException e) {
            Logger.getLogger("PermissionsEx").warning("[PermissionsEx] Unknown server implementation being used!");
            return null;
        }

        if (!humanEntity.isAssignableFrom(player.getClass())) {
            Logger.getLogger("PermissionsEx").warning("[PermissionsEx] Strange error while injecting permissible!");
            return null;
        }

        final Field permField = humanEntity.getDeclaredField(this.fieldName);
        // Make it public for reflection
        permField.setAccessible(true);
        return permField;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void copyValues(PermissibleBase old, PermissibleBase newPerm) throws NoSuchFieldException, IllegalAccessException {
        // Attachments
        Field attachmentField = PermissibleBase.class.getDeclaredField("attachments");
        attachmentField.setAccessible(true);
        List<Object> attachmentPerms = (List<Object>) attachmentField.get(newPerm);
        attachmentPerms.clear();
        attachmentPerms.addAll((List) attachmentField.get(old));
        newPerm.recalculatePermissions();
    }

    public abstract boolean isApplicable(Player player);

    static final class ServerNamePermissibleInjector extends PermissibleInjector {
        private final String serverName;

        ServerNamePermissibleInjector(final String clazz, final String field, final boolean copyValues, final String serverName) {
            super(clazz, field, copyValues);
            this.serverName = serverName;
        }

        @Override
        public boolean isApplicable(final Player player) {
            return player.getServer().getName().equalsIgnoreCase(this.serverName);
        }
    }

    static final class ClassPresencePermissibleInjector extends PermissibleInjector {

        ClassPresencePermissibleInjector(final @Nullable String clazzName, final String fieldName, final boolean copyValues) {
            super(clazzName, fieldName, copyValues);
        }

        @Override
        public boolean isApplicable(final Player player) {
            if (this.clazzName == null) {
                return false;
            }

            try {
                return Class.forName(this.clazzName).isInstance(player);
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }

    static final class ClassNameRegexPermissibleInjector extends PermissibleInjector {
        private final Pattern regex;

        ClassNameRegexPermissibleInjector(final String clazz, final String field, final boolean copyValues, final Pattern regex) {
            super(clazz, field, copyValues);
            this.regex = regex;
        }

        @Override
        public boolean isApplicable(Player player) {
            return this.regex.matcher(player.getClass().getName()).matches();
        }
    }
}
