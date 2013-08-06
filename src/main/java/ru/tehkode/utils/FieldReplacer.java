package ru.tehkode.utils;

import java.lang.reflect.Field;

/**
 * @author zml2008
 */
public class FieldReplacer<Instance, Type> {
	private final Class<Type> requiredType;
	private final Field field;

	public FieldReplacer(Class<? extends Instance> clazz, String fieldName, Class<Type> requiredType) {
		try {
			this.requiredType = requiredType;
			field = clazz.getDeclaredField(fieldName);

			field.setAccessible(true);
			if (!requiredType.isAssignableFrom(field.getType())) {
				throw new ExceptionInInitializerError("Field of wrong type");
			}
		} catch (NoSuchFieldException e) {
			throw new ExceptionInInitializerError(e);
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
}
