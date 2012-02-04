package ru.tehkode.permissions.backends.file.data;

import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.backends.UserDataProvider;
import ru.tehkode.permissions.backends.file.FileBackend;

public class FileUserDataProvider extends FileDataProvider implements UserDataProvider {

	public FileUserDataProvider(FileBackend backend, ConfigurationSection node) {
		super(backend, node);
	}


}
