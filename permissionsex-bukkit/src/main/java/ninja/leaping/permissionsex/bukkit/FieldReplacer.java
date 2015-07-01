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
