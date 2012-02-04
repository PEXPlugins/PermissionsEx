package ru.tehkode.permissions.backends.file.data;

import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.backends.GroupDataProvider;
import ru.tehkode.permissions.backends.file.FileBackend;


public class FileGroupDataProvider extends FileDataProvider implements GroupDataProvider{

	public FileGroupDataProvider(FileBackend backend, ConfigurationSection node) {
		super(backend, node);
	}

}
