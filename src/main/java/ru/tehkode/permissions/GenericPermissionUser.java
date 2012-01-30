package ru.tehkode.permissions;

import java.util.List;
import java.util.Map;
import ru.tehkode.permissions.backends.UserDataProvider;


public abstract class GenericPermissionUser extends PermissionUser {

	protected UserDataProvider provider;
	
	public GenericPermissionUser(String playerName, PermissionManager manager, UserDataProvider data) {
		super(playerName, manager);
		
		this.provider = data;
	}

	@Override
	public Map<String, List<PermissionGroup>> loadInheritance() {
		return this.provider.loadInheritance();
	}

	@Override
	protected Map<String, Map<String, String>> loadOptions() {
		return this.provider.loadOptions();
	}

	@Override
	protected Map<String, List<String>> loadPermissions() {
		return this.provider.loadPermissions();
	}

	@Override
	public void remove() {
		this.provider.remove(this);
	}

	@Override
	public void save() {
		this.provider.save(this);
	}

	
	
}
