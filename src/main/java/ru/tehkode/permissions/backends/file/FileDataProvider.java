package ru.tehkode.permissions.backends.file;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionNode;
import ru.tehkode.permissions.backends.DataProvider;

public abstract class FileDataProvider implements DataProvider {

	protected ConfigurationSection node;
	
	public FileDataProvider(ConfigurationSection node) {
		this.node = node;
	}

	@Override
	public Map<String, List<PermissionGroup>> loadInheritance() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Map<String, Map<String, String>> loadOptions() {
		Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
		
		if (this.node.isConfigurationSection("worlds")) { // node have world-specific settings
			Map<String, Object> data = this.node.getConfigurationSection("worlds").getValues(true);
			System.out.println("Data: " + data);
		}

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
