package ru.tehkode.permissions.backends.file;

import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.backends.GroupDataProvider;


public class FileGroupDataProvider extends FileDataProvider implements GroupDataProvider{

	public FileGroupDataProvider(ConfigurationSection node) {
		super(node);
	}
	
	@Override
	public void save(PermissionGroup user) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
