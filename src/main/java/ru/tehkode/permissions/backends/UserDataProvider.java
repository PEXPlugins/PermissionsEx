package ru.tehkode.permissions.backends;

import ru.tehkode.permissions.PermissionUser;

public interface UserDataProvider extends DataProvider {

	public void save(PermissionUser user);
}
