package ru.tehkode.permissions.backends;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionsGroupData;
import ru.tehkode.permissions.PermissionsUserData;
import ru.tehkode.permissions.bukkit.ErrorReport;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Backend for permission
 *
 * Default group:
 * Groups have a default flag. All users are in groups with default marked true.
 * No default group is required to exist.
 */
public abstract class PermissionBackend {
	private final PermissionManager manager;
	private final ConfigurationSection backendConfig;
	/**
	 * Executor currently being used to execute backend tasks
	 */
	private volatile Executor activeExecutor;
	/**
	 * Executor that consistently maintains a reference to the executor actively being used
	 */
	private final Executor activeExecutorPtr,
			onThreadExecutor;
	private final ExecutorService asyncExecutor;
	private final List<SchemaUpdate> schemaUpdates = new LinkedList<>();

	protected PermissionBackend(PermissionManager manager, ConfigurationSection backendConfig) throws PermissionBackendException {
		this.manager = manager;
		this.backendConfig = backendConfig;
		this.asyncExecutor = Executors.newSingleThreadExecutor();
		this.onThreadExecutor = new Executor() {
			@Override
			public void execute(Runnable runnable) {
				runnable.run();
			}
		};
		this.activeExecutor = asyncExecutor; // Default

		this.activeExecutorPtr = new Executor() {
			@Override
			public void execute(Runnable runnable) {
				PermissionBackend.this.activeExecutor.execute(runnable);
			}
		};
	}

	protected void addSchemaUpdate(SchemaUpdate update) {
		schemaUpdates.add(update);
		Collections.sort(schemaUpdates);
	}

	protected void performSchemaUpdate() {
		int version = getSchemaVersion();
		int newVersion = version;
		try {
			for (SchemaUpdate update : schemaUpdates) {
				try {
					if (update.getUpdateVersion() <= version) {
						continue;
					}
					if (newVersion == version) { // No updates have been performed yet
						backupDatabase();
					}
					update.performUpdate();
					newVersion = Math.max(update.getUpdateVersion(), newVersion);
				} catch (Throwable t) {
					ErrorReport.handleError("While updating to " + update.getUpdateVersion() + " from " + newVersion, t);
					break;
				}
			}
		} finally {
			if (newVersion != version) {
				setSchemaVersion(newVersion);
			}
		}
	}

	protected void backupDatabase() throws IOException {
		try (Writer w = new FileWriter(new File(manager.getConfiguration().getBasedir(), getConfig().getName() + "-backup." + getSchemaVersion() + ".bak"))) {
			writeContents(w);
		}
	}

	/**
	 * Return the current schema version.
	 * -1 indicates that the schema version is unknown.
	 *
	 * @return The current schema version
	 */
	public abstract int getSchemaVersion();

	/**
	 * Update the schema version. May be a no-op for unversioned schemas.
	 *
	 * @param version The new version
	 */
	protected abstract void setSchemaVersion(int version);

	public int getLatestSchemaVersion() {
		if (schemaUpdates.isEmpty()) {
			return -1;
		}
		return schemaUpdates.get(schemaUpdates.size() - 1).getUpdateVersion();
	}

	protected Executor getExecutor() {
		return activeExecutorPtr;
	}

	protected final PermissionManager getManager() {
		return manager;
	}

	protected final ConfigurationSection getConfig() {
		return backendConfig;
	}

	public abstract void reload() throws PermissionBackendException;

	public abstract PermissionsUserData getUserData(String userName);

	public abstract PermissionsGroupData getGroupData(String groupName);

	public abstract boolean hasUser(String userName);

	public abstract boolean hasGroup(String group);

	/**
	 * Return list of identifiers associated with users. These may not be user-readable
	 * @return Identifiers associated with users
	 */
	public abstract Collection<String> getUserIdentifiers();

	/**
	 * Return friendly names of known users. These cannot be passed to {@link #getUserData(String)} to return a valid user object
	 * @return Names associated with users
	 */
	public abstract Collection<String> getUserNames();

	/*public List<PermissionsUserData> getUsers() {
		List<PermissionsUserData> userData = new ArrayList<PermissionsUserData>();
		for (String name : getUserNames()) {
			userData.add(getUserData(name));
		}
		return Collections.unmodifiableList(userData);
	}*/

	public abstract Collection<String> getGroupNames();

	/*public List<PermissionsGroupData> getGroups() {
		List<PermissionsGroupData> groupData = new ArrayList<PermissionsGroupData>();
		for (String name : getGroupNames()) {
			groupData.add(getGroupData(name));
		}
		Collections.sort(groupData);
		return Collections.unmodifiableList(groupData);
	}*/

	// -- World inheritance

	public abstract List<String> getWorldInheritance(String world);

	public abstract Map<String, List<String>> getAllWorldInheritance();

	public abstract void setWorldInheritance(String world, List<String> inheritance);

	public void close() throws PermissionBackendException {
		asyncExecutor.shutdown();
		try {
			if (!asyncExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
				getLogger().warning("All backend tasks not completed after 30 seconds, waiting 2 minutes.");
				if (!asyncExecutor.awaitTermination(2, TimeUnit.MINUTES)) {
					getLogger().warning("All backend tasks not completed after another 2 minutes, giving up on the wait.");
				}
			}
		} catch (InterruptedException e) {
			throw new PermissionBackendException(e);
		}
	}

	public final Logger getLogger() {
		return manager.getLogger();
	}

	/**
	 * Load data from alternate backend.
	 * Assume that this overwrites all data in the receiving backend (except for users not included in transferring backend)
	 *
	 * @param backend The backend to load data from
	 */
	public void loadFrom(PermissionBackend backend) {
		setPersistent(false);
		try {
			for (String group : backend.getGroupNames()) {
				BackendDataTransfer.transferGroup(backend.getGroupData(group), getGroupData(group));
			}

			for (String user : backend.getUserIdentifiers()) {
				BackendDataTransfer.transferUser(backend.getUserData(user), getUserData(user));
			}

			for (Map.Entry<String, List<String>> ent : backend.getAllWorldInheritance().entrySet()) {
				setWorldInheritance(ent.getKey(), ent.getValue()); // Could merge data but too complicated & too lazy
			}
		} finally {
			setPersistent(true);
		}
	}


	public void revertUUID() {
		this.setPersistent(false);
		try {
			for (String ident : getUserIdentifiers()) {
				PermissionsUserData data = getUserData(ident);
				String name = data.getOption("name", null);
				if (name != null) {
					data.setIdentifier(name);
					data.setOption("name", null, null);
				}
			}
		} finally {
			this.setPersistent(true);
		}
	}

	public void setPersistent(boolean persistent) {
		if (persistent) {
			this.activeExecutor = asyncExecutor;
		} else {
			this.activeExecutor = onThreadExecutor;
		}
	}

	/**
	 * Allow this backend to write its contents to a file.
	 * @param writer The writer to dump contents to.
	 */
	public abstract void writeContents(Writer writer) throws IOException;

	// -- Backend lookup/creation

	public final static String DEFAULT_BACKEND = "file";

	/**
	 * Array of backend aliases
	 */
	private static final Map<String, Class<? extends PermissionBackend>> REGISTERED_ALIASES = new HashMap<>();

	/**
	 * Return class name for alias
	 *
	 * @param alias Alias for backend
	 * @return Class name if found or alias if there is no such class name present
	 */
	public static String getBackendClassName(String alias) {

		if (REGISTERED_ALIASES.containsKey(alias)) {
			return REGISTERED_ALIASES.get(alias).getName();
		}

		return alias;
	}

	/**
	 * Returns Class object for specified alias, if there is no alias registered
	 * then try to find it using Class.forName(alias)
	 *
	 * @param alias
	 * @return
	 * @throws ClassNotFoundException
	 */
	public static Class<? extends PermissionBackend> getBackendClass(String alias) throws ClassNotFoundException {
		if (!REGISTERED_ALIASES.containsKey(alias)) {
			Class<?> clazz = Class.forName(alias);
			if (!PermissionBackend.class.isAssignableFrom(clazz)) {
				throw new IllegalArgumentException("Provided class " + alias + " is not a subclass of PermissionBackend!");
			}
			return clazz.asSubclass(PermissionBackend.class);
		}

		return REGISTERED_ALIASES.get(alias);
	}

	/**
	 * Register new alias for specified backend class
	 *
	 * @param alias
	 * @param backendClass
	 */
	public static void registerBackendAlias(String alias, Class<? extends PermissionBackend> backendClass) {
		if (!PermissionBackend.class.isAssignableFrom(backendClass)) {
			throw new IllegalArgumentException("Provided class should be subclass of PermissionBackend"); // This should be enforced at compile time
		}

		REGISTERED_ALIASES.put(alias, backendClass);

		//PermissionsEx.getLogger().info(alias + " backend registered!");
	}

	/**
	 * Return alias for specified backend class
	 * If there is no such class registered the fullname of this class would
	 * be returned using backendClass.getName();
	 *
	 * @param backendClass
	 * @return alias or class fullname when not found using backendClass.getName()
	 */
	public static String getBackendAlias(Class<? extends PermissionBackend> backendClass) {
		if (REGISTERED_ALIASES.containsValue(backendClass)) {
			for (String alias : REGISTERED_ALIASES.keySet()) { // Is there better way to find key by value?
				if (REGISTERED_ALIASES.get(alias).equals(backendClass)) {
					return alias;
				}
			}
		}

		return backendClass.getName();
	}

	/**
	 * Returns new backend class instance for specified backendName
	 *
	 * @param backendName Class name or alias of backend
	 * @param config      Configuration object to access backend settings
	 * @return new instance of PermissionBackend object
	 */
	public static PermissionBackend getBackend(String backendName, Configuration config) throws PermissionBackendException {
		return getBackend(backendName, PermissionsEx.getPermissionManager(), config, DEFAULT_BACKEND);
	}

	/**
	 * Returns new Backend class instance for specified backendName
	 *
	 * @param backendName Class name or alias of backend
	 * @param manager     PermissionManager object
	 * @param config      Configuration object to access backend settings
	 * @return new instance of PermissionBackend object
	 */
	public static PermissionBackend getBackend(String backendName, PermissionManager manager, ConfigurationSection config) throws PermissionBackendException {
		return getBackend(backendName, manager, config, DEFAULT_BACKEND);
	}

	/**
	 * Returns new Backend class instance for specified backendName
	 *
	 * @param backendName     Class name or alias of backend
	 * @param manager         PermissionManager object
	 * @param config          Configuration object to access backend settings
	 * @param fallBackBackend name of backend that should be used if specified backend was not found or failed to initialize
	 * @return new instance of PermissionBackend object
	 */
	public static PermissionBackend getBackend(String backendName, PermissionManager manager, ConfigurationSection config, String fallBackBackend) throws PermissionBackendException{
		if (backendName == null || backendName.isEmpty()) {
			backendName = DEFAULT_BACKEND;
		}

		String className = getBackendClassName(backendName);

		try {
			Class<? extends PermissionBackend> backendClass = getBackendClass(backendName);

			manager.getLogger().info("Initializing " + backendName + " backend");

			Constructor<? extends PermissionBackend> constructor = backendClass.getConstructor(PermissionManager.class, ConfigurationSection.class);
			return constructor.newInstance(manager, config);
		} catch (ClassNotFoundException e) {

			manager.getLogger().warning("Specified backend \"" + backendName + "\" is unknown.");

			if (fallBackBackend == null) {
				throw new RuntimeException(e);
			}

			if (!className.equals(getBackendClassName(fallBackBackend))) {
				return getBackend(fallBackBackend, manager, config, null);
			} else {
				throw new RuntimeException(e);
			}
		} catch (Throwable e) {
			if (e instanceof InvocationTargetException) {
				e = e.getCause();
				if (e instanceof PermissionBackendException) {
					throw ((PermissionBackendException) e);
				}
			}
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{config=" + getConfig().getName() + "}";
	}

}
