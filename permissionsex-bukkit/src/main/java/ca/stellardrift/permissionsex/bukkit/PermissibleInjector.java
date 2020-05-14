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
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;

import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class handles injection of {@link Permissible}s into {@link Player}s for various server implementations.
 */
abstract class PermissibleInjector {
    protected final String clazzName, fieldName;
    protected final boolean copyValues;

    public PermissibleInjector(String clazzName, String fieldName, boolean copyValues) {
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
     * @throws NoSuchFieldException   when the permissions field could not be found in the Permissible
     * @throws IllegalAccessException when things go very wrong
     */
    public Permissible inject(Player player, Permissible permissible) throws NoSuchFieldException, IllegalAccessException {
        Field permField = getPermissibleField(player);
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

    public Permissible getPermissible(Player player) throws NoSuchFieldException, IllegalAccessException {
        return (Permissible) getPermissibleField(player).get(player);
    }

    private Field getPermissibleField(Player player) throws NoSuchFieldException {
        Class<?> humanEntity;
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

        Field permField = humanEntity.getDeclaredField(fieldName);
        // Make it public for reflection
        permField.setAccessible(true);
        return permField;
    }

    @SuppressWarnings("unchecked")
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

    static class ServerNamePermissibleInjector extends PermissibleInjector {
        protected final String serverName;

        public ServerNamePermissibleInjector(String clazz, String field, boolean copyValues, String serverName) {
            super(clazz, field, copyValues);
            this.serverName = serverName;
        }

        @Override
        public boolean isApplicable(Player player) {
            return player.getServer().getName().equalsIgnoreCase(serverName);
        }
    }

    static class ClassPresencePermissibleInjector extends PermissibleInjector {

        public ClassPresencePermissibleInjector(String clazzName, String fieldName, boolean copyValues) {
            super(clazzName, fieldName, copyValues);
        }

        @Override
        public boolean isApplicable(Player player) {
            try {
                return Class.forName(clazzName).isInstance(player);
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }

    static class ClassNameRegexPermissibleInjector extends PermissibleInjector {
        private final String regex;

        public ClassNameRegexPermissibleInjector(String clazz, String field, boolean copyValues, String regex) {
            super(clazz, field, copyValues);
            this.regex = regex;
        }

        @Override
        public boolean isApplicable(Player player) {
            return player.getClass().getName().matches(regex);
        }
    }
}
