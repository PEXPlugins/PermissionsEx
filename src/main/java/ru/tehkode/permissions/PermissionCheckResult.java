package ru.tehkode.permissions;

public enum PermissionCheckResult {

	UNDEFINED(false),
	TRUE(true),
	FALSE(false);

	protected boolean result;

	private PermissionCheckResult(boolean result) {
		this.result = result;
	}

	public boolean toBoolean() {
		return this.result;
	}

	@Override
	public String toString() {
		return this == UNDEFINED ? "undefined" : Boolean.toString(result);
	}

	public static PermissionCheckResult fromBoolean(final boolean result) {
		return result ? TRUE : FALSE;
	}
}
