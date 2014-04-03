package ru.tehkode.permissions.backends.file;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class FileConfig extends YamlConfiguration {
	private final File file;
	private boolean saveSuppressed;

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
		if (!saveSuppressed) {
			this.save(file);
		}
	}

	public boolean isSaveSuppressed() {
		return saveSuppressed;
	}

	void setSaveSuppressed(boolean saveSuppressed) {
		this.saveSuppressed = saveSuppressed;
	}

	@Override
	public void loadFromString(String contents) throws InvalidConfigurationException {
		synchronized (this) {
			super.loadFromString(contents);
		}
	}

	@Override
	public String saveToString() {
		synchronized (this) {
			return super.saveToString();
		}
	}
}
