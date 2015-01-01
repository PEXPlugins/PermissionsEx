package ru.tehkode.permissions.backends;

import org.bukkit.configuration.ConfigurationSection;
import org.json.simple.JSONObject;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionsGroupData;
import ru.tehkode.permissions.PermissionsUserData;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Backend containing multiple backends
 * Backend priority is first-come-first-serve -- whatever's listed first gets priority
 */
public class MultiBackend extends PermissionBackend {
	private final List<PermissionBackend> backends = new ArrayList<>();
	private final Map<String, PermissionBackend> fallbackBackends = new HashMap<>();
	public MultiBackend(PermissionManager manager, ConfigurationSection backendConfig) throws PermissionBackendException {
		super(manager, backendConfig);
		Map<String, PermissionBackend> backendMap = new HashMap<>();
		List<String> backendNames = backendConfig.getStringList("backends");
		if (backendNames.isEmpty()) {
			backendConfig.set("backends", new ArrayList<String>());
			throw new PermissionBackendException("No backends configured for multi backend! Please configure this!");
		}
		for (String name : backendConfig.getStringList("backends")) {
			PermissionBackend backend = manager.createBackend(name);
			backends.add(backend);
			backendMap.put(name, backend);
		}

		// Fallbacks
		ConfigurationSection fallbackSection = backendConfig.getConfigurationSection("fallback");
		if (fallbackSection != null) {
			for (Map.Entry<String, Object> ent : fallbackSection.getValues(false).entrySet()) {
				@SuppressWarnings("SuspiciousMethodCalls")
				PermissionBackend backend = backendMap.get(ent.getValue());
				if (backend == null) {
					throw new PermissionBackendException("Fallback backend type " + ent.getValue() + " is not listed in the backends section of MultiBackend (and must be for this contraption to work)");
				}
				fallbackBackends.put(ent.getKey(), backend);
			}
		}
	}

	@Override
	public int getSchemaVersion() {
		return -1;
	}

	@Override
	protected void setSchemaVersion(int version) {
		// no-op
	}

	public PermissionBackend getFallbackBackend(String type) {
		if (fallbackBackends.containsKey(type)) {
			return fallbackBackends.get(type);
		}
		return backends.get(0);
	}

	@Override
	public void reload() throws PermissionBackendException {
		for (PermissionBackend backend : backends) {
			backend.reload();
		}
	}

	@Override
	public PermissionsUserData getUserData(String userName) {
		for (PermissionBackend backend : backends) {
			if (backend.hasUser(userName)) {
				return backend.getUserData(userName);
			}
		}
		return getFallbackBackend("user").getUserData(userName);
	}

	@Override
	public PermissionsGroupData getGroupData(String groupName) {
		for (PermissionBackend backend : backends) {
			if (backend.hasGroup(groupName)) {
				return backend.getGroupData(groupName);
			}
		}
		return getFallbackBackend("group").getGroupData(groupName);
	}

	@Override
	public boolean hasUser(String userName) {
		for (PermissionBackend backend : backends) {
			if (backend.hasUser(userName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasGroup(String group) {
		for (PermissionBackend backend : backends) {
			if (backend.hasGroup(group)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Collection<String> getUserIdentifiers() {
		Set<String> ret = new HashSet<>();
		for (PermissionBackend backend : backends) {
			ret.addAll(backend.getUserIdentifiers());
		}
		return Collections.unmodifiableSet(ret);
	}

	@Override
	public Collection<String> getUserNames() {
		Set<String> ret = new HashSet<>();
		for (PermissionBackend backend : backends) {
			ret.addAll(backend.getUserNames());
		}
		return Collections.unmodifiableSet(ret);
	}

	@Override
	public Collection<String> getGroupNames() {
		Set<String> ret = new HashSet<>();
		for (PermissionBackend backend : backends) {
			ret.addAll(backend.getGroupNames());
		}
		return Collections.unmodifiableSet(ret);
	}

	@Override
	public List<String> getWorldInheritance(String world) {
		for (PermissionBackend backend : backends) {
			List<String> potentialRet = backend.getWorldInheritance(world);
			if (potentialRet != null && potentialRet.size() > 0) {
				return potentialRet;
			}
		}
		return Collections.emptyList();
	}

	@Override
	public Map<String, List<String>> getAllWorldInheritance() {
		Map<String, List<String>> ret = new HashMap<>();
		for (int i = backends.size(); i >= 0; --i) {
			ret.putAll(backends.get(i).getAllWorldInheritance());
		}
		return Collections.unmodifiableMap(ret);
	}

	@Override
	public void setWorldInheritance(String world, List<String> inheritance) {
		getFallbackBackend("world").setWorldInheritance(world, inheritance);
	}

	@Override
	public void writeContents(Writer writer) throws IOException {
		JSONObject obj = new JSONObject();
		for (PermissionBackend backend : backends) {
			final StringWriter stringW = new StringWriter();
			backend.writeContents(stringW);
			obj.put(backend.toString(), stringW.toString());
		}
		obj.writeJSONString(writer);
	}
}
