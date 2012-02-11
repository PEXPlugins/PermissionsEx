package ru.tehkode.permissions.backends.file.data;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionNode;
import ru.tehkode.permissions.backends.DataProvider;
import ru.tehkode.permissions.backends.file.FileBackend;
import ru.tehkode.utils.Debug;

public abstract class FileDataProvider implements DataProvider {

	protected ConfigurationSection node;
	protected FileBackend backend;
	
	public FileDataProvider(FileBackend backend, ConfigurationSection node) {
		this.node = node;
		this.backend = backend;
	}

	@Override
	public Map<String, List<PermissionGroup>> loadInheritance() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Map<String, Map<String, String>> loadOptions() {
		Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
		
		if (this.node != null && this.node.isConfigurationSection("worlds")) { // node have world-specific settings
			Map<String, Object> data = this.node.getConfigurationSection("worlds").getValues(true);
			Debug.print("Data %1", data);
		}
		
		Debug.print("OPTION %1", result);

        return result;
	}

	@Override
	public Map<String, List<String>> loadPermissions() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void save(PermissionNode user) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void remove(PermissionNode user) {
		throw new UnsupportedOperationException("Not supported yet.");
	}	
}
