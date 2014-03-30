package ru.tehkode.permissions.backends.file;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class FileConfig extends YamlConfiguration {
	private final File file;

	public FileConfig(File file) {
		super();
		this.options().pathSeparator(FileBackend.PATH_SEPARATOR);
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	public void load() throws IOException, InvalidConfigurationException {
		this.load(file);
	}

	public void save() throws IOException {
		this.save(file);
	}
}
