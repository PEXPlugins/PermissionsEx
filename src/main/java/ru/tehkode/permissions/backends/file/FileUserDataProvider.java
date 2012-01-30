package ru.tehkode.permissions.backends.file;

import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.backends.UserDataProvider;

public class FileUserDataProvider extends FileDataProvider implements UserDataProvider {

	public FileUserDataProvider(ConfigurationSection node) {
		super(node);
	}
	
	@Override
	public void save(PermissionUser user) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
}
