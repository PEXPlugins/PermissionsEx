package ru.tehkode.permissions.backends.file;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class FileConfig extends YamlConfiguration {
	private final File file, tempFile, oldFile;
	private final Object lock;
	private boolean saveSuppressed;

	public FileConfig(File file) {
		this(file, new Object());
	}

	public FileConfig(File file, Object lock) {
		super();
		this.lock = lock;
		this.options().pathSeparator(FileBackend.PATH_SEPARATOR);
		this.file = file;
		this.tempFile = new File(file.getPath() + ".tmp");
		this.oldFile = new File(file.getPath() + ".old");
	}

	public File getFile() {
		return file;
	}

	public void load() throws IOException, InvalidConfigurationException {
		this.load(file);
	}

	public void save() throws IOException {
		if (!saveSuppressed) {
			this.save(tempFile);
			oldFile.delete();
			if (!file.exists() || file.renameTo(oldFile)) {
				if (!tempFile.renameTo(file)) {
					throw new IOException("Unable to overwrite config with temporary file! New config is at " + tempFile + ", old config at" + oldFile);
				} else {
					if (!oldFile.delete()) {
						throw new IOException("Unable to delete old file " + oldFile);
					}
				}
			}
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
		synchronized (lock) {
			super.loadFromString(contents);
		}
	}

	@Override
	public String saveToString() {
		synchronized (lock) {
			return super.saveToString();
		}
	}
}
