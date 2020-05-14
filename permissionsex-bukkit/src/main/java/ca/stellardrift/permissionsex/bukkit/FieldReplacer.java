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

import java.lang.reflect.Field;

/**
 * @author zml2008
 */
public class FieldReplacer<Instance, Type> {
    private final Class<Type> requiredType;
    private final Field field;

    public FieldReplacer(Class<? extends Instance> clazz, String fieldName, Class<Type> requiredType) {
        this.requiredType = requiredType;
        field = getField(clazz, fieldName);
        if (field == null) {
            throw new ExceptionInInitializerError("No such field " + fieldName + " in class " + clazz);
        }

        field.setAccessible(true);
        if (!requiredType.isAssignableFrom(field.getType())) {
            throw new ExceptionInInitializerError("Field of wrong type");
        }
    }

    public Type get(Instance instance) {
        try {
            return this.requiredType.cast(field.get(instance));
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }

    public void set(Instance instance, Type newValue) {
        try {
            field.set(instance, newValue);
        } catch (IllegalAccessException e) {
            throw new Error(e); // This shouldn't happen because we call setAccessible in the constructor
        }
    }

    private static Field getField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
