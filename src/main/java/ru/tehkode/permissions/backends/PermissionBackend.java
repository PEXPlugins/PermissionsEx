package ru.tehkode.permissions.backends;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Callables;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.ErrorReport;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.callback.Callback;
import ru.tehkode.permissions.callback.CallbackTask;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Backend for permission
 * <p/>
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
					update.performUpdate();
					newVersion = Math.max(update.getUpdateVersion(), newVersion);
				} catch (Throwable t) {
					ErrorReport.handleError("While updating to " + update.getUpdateVersion() + " from " + newVersion, t);
				}
			}
		} finally {
			if (newVersion != version) {
				setSchemaVersion(newVersion);
			}
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

	protected <V> FutureTask<V> execute(Callable<V> func, Callback<V> callback) {
		CallbackTask<V> ret = new CallbackTask<>(func, callback);
		getExecutor().execute(ret);
		return ret;
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

	/**
	 * Return friendly names of known users. These may not
	 *
	 * @return Names associated with users
	 */
	public abstract Collection<String> getUserNames();


	// -- World inheritance

	/**
	 * Returns an iterator that will produce all matcher groups known to this backend.
	 *
	 * @return
	 */
	public abstract Future<Iterator<MatcherGroup>> getAllMatcherGroups(Callback<Iterator<MatcherGroup>> callback);

	public Future<MatcherGroup> getOne(final String type, final Qualifier qual, final String qualValue, final Callback<MatcherGroup> callback) {
		return execute(new Callable<MatcherGroup>() {
			@Override
			public MatcherGroup call() throws Exception {
				List<MatcherGroup> groups = getMatchingGroups(type, qual, qualValue, null).get();
				return groups.size() == 1 ? groups.get(0) : null;
				}
		}, callback);
	}

	public Future<MatcherGroup> getFirst(final String type, final Qualifier qual, final String qualValue, final Callback<MatcherGroup> callback) {
		return execute(new Callable<MatcherGroup>() {
			@Override
			public MatcherGroup call() throws Exception {
				List<MatcherGroup> groups = getMatchingGroups(type, qual, qualValue, null).get();
				if (!groups.isEmpty()) {
					return groups.get(0);
				} else {
					return null;
				}
			}
		}, callback);
	}

	public Future<MatcherGroup> getOne(final String type, Callback<MatcherGroup> callback) {
		return execute(new Callable<MatcherGroup>() {
			@Override
			public MatcherGroup call() throws Exception {
				List<MatcherGroup> groups = getMatchingGroups(type, null).get();
				return groups != null && groups.size() == 1 ? groups.get(0) : null;
			}
		}, callback);
	}

	public Future<MatcherGroup> getFirst(final String type, Callback<MatcherGroup> callback) {
		return execute(new Callable<MatcherGroup>() {
			@Override
			public MatcherGroup call() throws Exception {
				List<MatcherGroup> groups = getMatchingGroups(type, null).get();
				return groups == null || groups.isEmpty() ? null : groups.get(0);
			}
		}, callback);
	}

	public Future<MatcherGroup> getFirstOrAdd(final String type, final Callback<MatcherGroup> callback) {
		Future<MatcherGroup> ret = getFirst(type, new Callback<MatcherGroup>() {
			@Override
			public void onSuccess(MatcherGroup result) {
				if (result == null) {
					createMatcherGroup(type, Collections.<String, String>emptyMap(), ImmutableMultimap.<Qualifier, String>of(), callback);
				} else {
					if (callback != null) {
						callback.onSuccess(result);
					}
				}
			}

			@Override
			public void onError(Throwable t) {
				if (callback != null) {
					callback.onError(t);
				}
			}
		});
		return ret;
	}

	public Future<MatcherGroup> getFirstOrAdd(String type, Qualifier key, String value, Callback<MatcherGroup> callback) {
		MatcherGroup ret = getFirst(type, key, value);
		if (ret == null) {
			ret = createMatcherGroup(type, Collections.<String, String>emptyMap(), ImmutableMultimap.of(key, value));
		}
		return ret;
	}

	/**
	 * Returns all known groups of the requested type.
	 *
	 * @param type The name of the type of section to look up
	 * @return Any matching groups. Empty if no values.
	 */
	public abstract Future<List<MatcherGroup>> getMatchingGroups(String type, Callback<List<MatcherGroup>> callback);

	/**
	 * Returns all known groups of the requested type with {@code qual} set to {@code qualValue}
	 *
	 * @param type      The type
	 * @param qual
	 * @param qualValue
	 * @return Any matching groups. Empty if no values.
	 */
	public abstract Future<List<MatcherGroup>> getMatchingGroups(String type, Qualifier qual, String qualValue, Callback<List<MatcherGroup>> callback);

	public abstract Future<MatcherGroup> createMatcherGroup(String type, Map<String, String> entries, Multimap<Qualifier, String> qualifiers, Callback<MatcherGroup> callback);

	public abstract Future<MatcherGroup> createMatcherGroup(String type, List<String> entries, Multimap<Qualifier, String> qualifiers, Callback<MatcherGroup> callback);

	public abstract Future<Collection<String>> getAllValues(Qualifier qualifier, Callback<Collection<String>> callback);

	public abstract Future<Boolean> hasAnyQualifier(Qualifier qualifier, String value, Callback<Boolean> callback);

	public abstract Future<Void> replaceQualifier(Qualifier qualifier, String old, String newVal);

	public abstract Future<List<MatcherGroup>> allWithQualifier(Qualifier qualifier, Callback<List<MatcherGroup>> callback);

	public Future<String> getName(final String uuid, final Callback<String> callback) {
		return execute(new Callable<String>() {
			@Override
			public String call() throws Exception {
				List<MatcherGroup> result = getMatchingGroups(MatcherGroup.UUID_ALIASES_KEY, null).get();
				for (MatcherGroup group : result) {
					String ret = group.getEntries().get(uuid);
					if (ret != null) {
						callback.onSuccess(ret);
					}
				}
				return uuid;
			}
		}, callback);
	}

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
			backend.getAllMatcherGroups(new Callback<Iterator<MatcherGroup>>() {
				@Override
				public void onSuccess(Iterator<MatcherGroup> result) {
					while (result.hasNext()) {
						MatcherGroup next = result.next();
						createMatcherGroup(next.getName(), next.getEntries(), next.getQualifiers(), null);
					}
				}

				@Override
				public void onError(Throwable t) {
				}
			});
		} finally {
			setPersistent(true);
		}
	}

	public void revertUUID() {
		this.setPersistent(false);
		try {
			Future<List<MatcherGroup>> ret = getMatchingGroups("uuid-mapping", new Callback<List<MatcherGroup>>() {
				@Override
				public void onSuccess(final List<MatcherGroup> uuids) {
					Future<?> res = getAllMatcherGroups(new Callback<Iterator<MatcherGroup>>() {
						@Override
						public void onSuccess(Iterator<MatcherGroup> result) {
							while (result.hasNext()) {
								MatcherGroup group = result.next();
								Multimap<Qualifier, String> qualifiers = group.getQualifiers();
								Collection<String> users = qualifiers.get(Qualifier.USER);
								if (!users.isEmpty()) {
									qualifiers = HashMultimap.create(qualifiers);
									List<String> newUsers = new LinkedList<>();
									for (String user : users) {
										for (MatcherGroup uuidGroup : uuids) {
											String name = uuidGroup.getEntries().get(user);
											newUsers.add(name == null ? user : name);
										}
									}
									qualifiers.replaceValues(Qualifier.USER, newUsers);
									group.setQualifiers(qualifiers);
								}
							}
							for (MatcherGroup matcher : uuids) {
								matcher.remove();
							}
						}

						@Override
						public void onError(Throwable t) {
							handleException(t, "reverting uuids");
						}
					});
					try {
						res.get();
					} catch (InterruptedException | ExecutionException e) {
						// Ignore, handled in callback
					}
				}

				@Override
				public void onError(Throwable t) {
					handleException(t, "reverting uuids");
				}
			});
			ret.get();
		} catch (InterruptedException | ExecutionException e) {
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
	 * Common error handling mechanism used for backends. When an exception is caught, this method takes care of it.
	 *
	 * @param t      The error thrown
	 * @param action The action that was being performed when the error occurred
	 */
	protected void handleException(Throwable t, String action) {
		getLogger().log(Level.SEVERE, "Error while " + action, t);
	}

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
	public static PermissionBackend getBackend(String backendName, PermissionManager manager, ConfigurationSection config, String fallBackBackend) throws PermissionBackendException {
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
