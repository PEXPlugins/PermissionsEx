package ru.tehkode.permissions;

public interface PermissionsGroupData extends PermissionsData {
	/**
	 * Preload data from group
	 */
	public void load();

	public boolean isDefault(String world);

	public void setDefault(boolean def, String world);
}
