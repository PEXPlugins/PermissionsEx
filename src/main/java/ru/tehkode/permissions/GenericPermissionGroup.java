package ru.tehkode.permissions;

import java.util.List;
import java.util.Map;
import ru.tehkode.permissions.backends.GroupDataProvider;

public abstract class GenericPermissionGroup extends PermissionGroup {

	protected GroupDataProvider provider;

	public GenericPermissionGroup(String groupName, PermissionManager manager, GroupDataProvider data) {
		super(groupName, manager);

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
