package ru.tehkode.permissions;

public interface PermissionMatcher {
	public boolean matches(String expression, String permission);
}
