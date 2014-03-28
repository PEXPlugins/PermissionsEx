package ru.tehkode.permissions;

import java.util.List;

public interface PermissionsGroupData extends PermissionsData {
	public List<String> getParents(String worldName);

	public void setParents(List<String> parents, String worldName);

	public boolean isDefault(String world);

	public void setDefault(boolean def, String world);
}
