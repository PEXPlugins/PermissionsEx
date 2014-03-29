package ru.tehkode.permissions;

import java.util.List;

public interface PermissionsGroupData extends PermissionsData {


	public boolean isDefault(String world);

	public void setDefault(boolean def, String world);
}
