package ru.tehkode.permissions.backends;

import ru.tehkode.permissions.PermissionGroup;

public interface GroupDataProvider extends DataProvider {
	
	public void save(PermissionGroup user);
}
