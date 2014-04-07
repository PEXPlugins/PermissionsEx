package ru.tehkode.permissions;

public interface PermissionsGroupData extends PermissionsData {


	public boolean isDefault(String world);

	public void setDefault(boolean def, String world);
}
