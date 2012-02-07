package ru.tehkode.permissions;

import java.util.List;
import java.util.Map;
import ru.tehkode.permissions.backends.UserDataProvider;
import ru.tehkode.utils.Debug;


public class GenericPermissionUser extends PermissionUser {

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
		Debug.print("LOL OPTION: " + this.provider.getClass().getName());
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
