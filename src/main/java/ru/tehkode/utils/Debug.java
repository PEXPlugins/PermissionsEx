package ru.tehkode.utils;

public class Debug {

	public static void print(String message, Object... args) {
		int i = 1;
		for (Object arg : args) {
			message = message.replace("%" + i++, arg != null ? arg.toString() : "null");
		}

		System.out.println("[PermissionsEx][DEBUG] " + message);
	}
}
