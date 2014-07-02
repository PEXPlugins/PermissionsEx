package ru.tehkode.permissions.backends.yaml;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ListenableFuture;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.backends.PermissionBackend;
import ru.tehkode.permissions.backends.SchemaUpdate;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * Backend using the yaml format
 */
public class YamlBackend extends PermissionBackend {
	public final static char PATH_SEPARATOR = '/';
	public YamlConfig permissions;
	public File permissionsFile;
	private final Object lock = new Object();

	public YamlBackend(PermissionManager manager, ConfigurationSection config) throws PermissionBackendException {
		super(manager, config, Executors.newSingleThreadExecutor());
		String permissionFilename = getConfig().getString("file");

		// Default settings
		if (permissionFilename == null) {
			permissionFilename = "permissions.yml";
			getConfig().set("file", "permissions.yml");
		}

		String baseDir = manager.getConfiguration().getBasedir();

		if (baseDir.contains("\\") && !"\\".equals(File.separator)) {
			baseDir = baseDir.replace("\\", File.separator);
		}

		File baseDirectory = new File(baseDir);
		if (!baseDirectory.exists()) {
			baseDirectory.mkdirs();
		}

		this.permissionsFile = new File(baseDir, permissionFilename);
		addSchemaUpdate(new SchemaUpdate(1) {
			@Override
			public void performUpdate() {
				ConfigurationSection userSection = permissions.getConfigurationSection("users");
				if (userSection != null) {
					for (Map.Entry<String, Object> e : userSection.getValues(false).entrySet()) {
						if (e.getValue() instanceof ConfigurationSection) {
							allWorlds((ConfigurationSection) e.getValue());
						}
					}
				}
				ConfigurationSection groupSection = permissions.getConfigurationSection("groups");
				if (groupSection != null) {
					for (Map.Entry<String, Object> e : groupSection.getValues(false).entrySet()) {
						if (e.getValue() instanceof ConfigurationSection) {
							allWorlds((ConfigurationSection) e.getValue());
						}
					}
				}
			}

			private void allWorlds(ConfigurationSection section) {
				singleWorld(section);
				ConfigurationSection worldSection = section.getConfigurationSection("worlds");
				if (worldSection != null) {
					for (Map.Entry<String, Object> e : worldSection.getValues(false).entrySet()) {
						if (e.getValue() instanceof ConfigurationSection) {
							singleWorld((ConfigurationSection) e.getValue());
						}
					}
				}
			}

			private void singleWorld(ConfigurationSection section) {
				if (section.isSet("prefix")) {
					section.set(buildPath("options", "prefix"), section.get("prefix"));
					section.set("prefix", null);
				}

				if (section.isSet("suffix")) {
					section.set(buildPath("options", "suffix"), section.get("suffix"));
					section.set("suffix", null);
				}

				if (section.isSet("default")) {
					section.set(buildPath("options", "default"), section.get("default"));
					section.set("default", null);
				}
			}
		});
		reload();
		performSchemaUpdate();
	}

	@Override
	public int getSchemaVersion() {
		synchronized (lock) {
			return this.permissions.getInt("schema-version", -1);
		}
	}

	@Override
	protected void setSchemaVersion(int version) {
		synchronized (lock) {
			this.permissions.set("schema-version", version);
			save();
		}
	}

	@Override
	public Collection<String> getUserNames() {
		synchronized (lock) {
			ConfigurationSection users = this.permissions.getConfigurationSection("users");

			if (users == null) {
				return Collections.emptySet();
			}

			Set<String> userNames = new HashSet<>();

			for (Map.Entry<String, Object> entry : users.getValues(false).entrySet()) {
				if (entry.getValue() instanceof ConfigurationSection) {
					ConfigurationSection userSection = (ConfigurationSection) entry.getValue();

					String name = userSection.getString(buildPath("options", "name"));
					if (name != null) {
						userNames.add(name);
					}
				}
			}
			return Collections.unmodifiableSet(userNames);
		}
	}

	final Map<String, String> uuidMappings = new HashMap<>();

	@Override
	public Iterable<MatcherGroup> getAll() {
		List<MatcherGroup> provider = new LinkedList<>();
		synchronized (lock) {
			uuidMappings.clear();
			entityList(provider, permissions.getConfigurationSection("groups"), Qualifier.GROUP, "inheritance");
			entityList(provider, permissions.getConfigurationSection("users"), Qualifier.USER, "group");
			ConfigurationSection worldInheritanceSection = permissions.getConfigurationSection("worlds");
			if (worldInheritanceSection != null) {
				for (Map.Entry<String, Object> entry : worldInheritanceSection.getValues(false).entrySet()) {
					if (entry.getValue() instanceof ConfigurationSection) {
						worldSection(provider, entry.getKey(), (ConfigurationSection) entry.getValue());
					}
				}
			}
			if (!uuidMappings.isEmpty()) {
				provider.add(new YamlMatcherGroup(MatcherGroup.UUID_ALIASES_KEY, ImmutableMultimap.<Qualifier, String>of(), new HashMap<>(uuidMappings)));
			}
		}

		return provider;
	}


	private void worldSection(List<MatcherGroup> provider, String world, ConfigurationSection section) {
		List<String> worldInheritance = section.getStringList("inheritance");
		if (worldInheritance != null && !worldInheritance.isEmpty()) {
			provider.add(new YamlMatcherGroup(MatcherGroup.WORLD_INHERITANCE_KEY, ImmutableMultimap.of(Qualifier.WORLD, world), worldInheritance));
		}
	}

	private void entityList(List<MatcherGroup> provider, ConfigurationSection section, Qualifier type, String inheritanceKey) {
		if (section != null) {
			for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
				if (entry.getValue() instanceof ConfigurationSection) {
					entitySectionToGroups(provider, entry.getKey(), (ConfigurationSection) entry.getValue(), type, inheritanceKey);
				}
			}
		}
	}

	private void entitySectionToGroups(List<MatcherGroup> provider, String name, ConfigurationSection section, Qualifier sectionType, String inheritanceKey) {
		worldEntitySectionToGroups(provider, name, section, sectionType, null, inheritanceKey);
		ConfigurationSection worldSection = section.getConfigurationSection("worlds");
		if (worldSection != null) {
			for (Map.Entry<String, Object> entry : worldSection.getValues(false).entrySet()) {
				if (entry.getValue() instanceof ConfigurationSection) {
					worldEntitySectionToGroups(provider, name, (ConfigurationSection) entry.getValue(), sectionType, entry.getKey(), inheritanceKey);
				}
			}
		}
	}

	private void worldEntitySectionToGroups(List<MatcherGroup> provider, String name, ConfigurationSection section, Qualifier sectionType, String world, String inheritanceKey) {
		Multimap<Qualifier, String> qualifiers = qualifiersFor(name, sectionType, world);
		// Permissions
		List<String> permissions = section.getStringList("permissions");
		if (permissions != null && !permissions.isEmpty()) {
			provider.add(new YamlMatcherGroup(MatcherGroup.PERMISSIONS_KEY, qualifiers, permissions));
		}

		// Options
		ConfigurationSection optionsSection = section.getConfigurationSection("options");
		if (optionsSection != null) {
			// Correctly convert username storage
			String realName = optionsSection.getString("name", null);
			if (realName != null) {
				uuidMappings.put(name, realName);
				optionsSection.set("name", null);
			}

			Map<String, String> options = new LinkedHashMap<>();
			for (String key : optionsSection.getKeys(true)) {
				if (optionsSection.isConfigurationSection(key)) {
					continue;
				}

				options.put(key.replace(optionsSection.getRoot().options().pathSeparator(), '.'), optionsSection.getString(key));
			}
			if (!options.isEmpty()) {
				provider.add(new YamlMatcherGroup(MatcherGroup.OPTIONS_KEY, qualifiers, options));
			}
		}

		// Inheritance
		List<String> inheritance = section.getStringList(inheritanceKey);
		if (inheritance != null && !inheritance.isEmpty()) {
			provider.add(new YamlMatcherGroup(MatcherGroup.INHERITANCE_KEY, qualifiers, inheritance));
		}
	}

	private ImmutableMultimap<Qualifier, String> qualifiersFor(String name, Qualifier type, String world) {
		return world == null ? ImmutableMultimap.of(type, name) : ImmutableMultimap.of(type, name, Qualifier.WORLD, world);
	}

	@Override
	public ListenableFuture<List<MatcherGroup>> getMatchingGroups(String type) {
		throw new UnsupportedOperationException("YAML backend is import-only.");
	}

	@Override
	public ListenableFuture<List<MatcherGroup>> getMatchingGroups(String type, Qualifier qual, String qualValue) {
		throw new UnsupportedOperationException("YAML backend is import-only.");
	}

	@Override
	public ListenableFuture<MatcherGroup> createMatcherGroup(String type, Map<String, String> entries, Multimap<Qualifier, String> qualifiers) {
		throw new UnsupportedOperationException("YAML backend is import-only.");
	}

	@Override
	public ListenableFuture<MatcherGroup> createMatcherGroup(String type, List<String> entries, Multimap<Qualifier, String> qualifiers) {
		throw new UnsupportedOperationException("YAML backend is import-only.");
	}

	@Override
	public ListenableFuture<Collection<String>> getAllValues(Qualifier qualifier) {
		throw new UnsupportedOperationException("YAML backend is import-only.");
	}

	@Override
	public ListenableFuture<Boolean> hasAnyQualifier(Qualifier qualifier, String value) {
		throw new UnsupportedOperationException("YAML backend is import-only.");
	}

	@Override
	public ListenableFuture<Void> replaceQualifier(Qualifier qualifier, String old, String newVal) {
		throw new UnsupportedOperationException("YAML backend is import-only.");
	}

	@Override
	public ListenableFuture<List<MatcherGroup>> allWithQualifier(Qualifier qualifier) {
		throw new UnsupportedOperationException("YAML backend is import-only.");
	}

	public static String buildPath(String... path) {
		StringBuilder builder = new StringBuilder();

		boolean first = true;
		char separator = PATH_SEPARATOR; //permissions.options().pathSeparator();

		for (String node : path) {
			if (node.isEmpty()) {
				continue;
			}

			if (!first) {
				builder.append(separator);
			}

			builder.append(node);

			first = false;
		}

		return builder.toString();
	}

	@Override
	public void reload() throws PermissionBackendException {
		YamlConfig newPermissions = new YamlConfig(permissionsFile);
		newPermissions.options().pathSeparator(PATH_SEPARATOR);
		try {
			newPermissions.load();
			getLogger().info("Permissions file successfully reloaded");
			this.permissions = newPermissions;
		} catch (FileNotFoundException e) {
			if (this.permissions == null) {
				// First load, load even if the file doesn't exist
				this.permissions = newPermissions;
				initializeDefaultConfiguration();
			}
		} catch (Throwable e) {
			throw new PermissionBackendException("Error loading permissions file!", e);
		}
	}

	/**
	 * This method is called when the file the permissions config is supposed to save to
	 * does not exist yet,This adds default permissions & stuff
	 */
	protected void initializeDefaultConfiguration() throws PermissionBackendException {
		if (!permissionsFile.exists()) {
			try {
				permissionsFile.createNewFile();

				// Load default permissions
				permissions.set("groups/default/default", true);


				List<String> defaultPermissions = new LinkedList<>();
				// Specify here default permissions
				defaultPermissions.add("modifyworld.*");

				permissions.set("groups/default/permissions", defaultPermissions);
				permissions.set("schema-version", getLatestSchemaVersion());

				this.save();
			} catch (IOException e) {
				throw new PermissionBackendException(e);
			}
		}
	}

	@Override
	public void loadFrom(PermissionBackend backend) {
		this.setPersistent(false);
		try {
			super.loadFrom(backend);
		} finally {
			this.setPersistent(true);
		}
		save();
	}

	@Override
	public void setPersistent(boolean persistent) {
		super.setPersistent(persistent);
		this.permissions.setSaveSuppressed(!persistent);
		if (persistent) {
			this.save();
		}
	}

	public void save() {
		try {
			this.permissions.save();
		} catch (IOException e) {
			getManager().getLogger().severe("Error while saving permissions file: " + e.getMessage());
		}
	}
}
