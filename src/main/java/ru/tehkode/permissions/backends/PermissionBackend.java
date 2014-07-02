package ru.tehkode.permissions.backends;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.ErrorReport;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

import javax.annotation.Nullable;
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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
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

	protected PermissionBackend(PermissionManager manager, ConfigurationSection backendConfig, ExecutorService asyncExecutor) throws PermissionBackendException {
		this.manager = manager;
		this.backendConfig = backendConfig;
		this.asyncExecutor = asyncExecutor;
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

	protected <V> ListenableFuture<V> execute(Callable<V> func) {
		ListenableFutureTask<V> ret = ListenableFutureTask.create(func);
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
	public abstract ListenableFuture<Iterator<MatcherGroup>> getAll();

	public ListenableFuture<MatcherGroup> getOne(final String type, final Qualifier qual, final String qualValue) {
		return Futures.chain(getMatchingGroups(type, qual, qualValue), new Function<List<MatcherGroup>, ListenableFuture<? extends MatcherGroup>>() {
			@Override
			public ListenableFuture<? extends MatcherGroup> apply(List<MatcherGroup> matcherGroups) {
				return Futures.immediateFuture(matcherGroups.size() == 1 ? matcherGroups.get(0) : null);
			}
		});
	}

	public ListenableFuture<MatcherGroup> getFirst(final String type, final Qualifier qual, final String qualValue) {
		return Futures.chain(getMatchingGroups(type, qual, qualValue), new Function<List<MatcherGroup>, ListenableFuture<? extends MatcherGroup>>() {
			@Override
			public ListenableFuture<? extends MatcherGroup> apply(List<MatcherGroup> matcherGroups) {
				if (!matcherGroups.isEmpty()) {
					return Futures.immediateFuture(matcherGroups.get(0));
				} else {
					return Futures.immediateFuture(null);
				}
			}
		});
	}

	public ListenableFuture<MatcherGroup> getOne(final String type) {
		return Futures.transform(getMatchingGroups(type), new Function<List<MatcherGroup>, MatcherGroup>() {
			@Override
			public MatcherGroup apply(List<MatcherGroup> matcherGroups) {
				return matcherGroups != null && matcherGroups.size() == 1 ? matcherGroups.get(0) : null;
			}
		});
	}

	public ListenableFuture<MatcherGroup> getFirst(final String type) {
		return Futures.transform(getMatchingGroups(type), new Function<List<MatcherGroup>, MatcherGroup>() {
			@Override
			public MatcherGroup apply(@Nullable List<MatcherGroup> matcherGroups) {
				return matcherGroups == null || matcherGroups.isEmpty() ? null : matcherGroups.get(0);
			}
		});
	}

	public ListenableFuture<MatcherGroup> getFirstOrAdd(final String type) {
		return Futures.chain(getFirst(type), new Function<MatcherGroup, ListenableFuture<? extends MatcherGroup>>() {
			@Override
			public ListenableFuture<? extends MatcherGroup> apply(@Nullable MatcherGroup matcherGroup) {
				if (matcherGroup == null) {
					return createMatcherGroup(type, Collections.<String, String>emptyMap(), ImmutableMultimap.<Qualifier, String>of());
				} else {
					return Futures.immediateFuture(matcherGroup);
				}
			}
		});
	}

	public ListenableFuture<MatcherGroup> getFirstOrAdd(final String type, final Qualifier key, final String value) {
		return Futures.chain(getFirst(type, key, value), new Function<MatcherGroup, ListenableFuture<? extends MatcherGroup>>() {
			@Override
			public ListenableFuture<? extends MatcherGroup> apply(@Nullable MatcherGroup matcherGroup) {
				if (matcherGroup == null) {
					return createMatcherGroup(type, Collections.<String, String>emptyMap(), ImmutableMultimap.<Qualifier, String>of(key, value));
				} else {
					return Futures.immediateFuture(matcherGroup);
				}
			}
		});
	}

	/**
	 * Returns all known groups of the requested type.
	 *
	 * @param type The name of the type of section to look up
	 * @return Any matching groups. Empty if no values.
	 */
	public abstract ListenableFuture<List<MatcherGroup>> getMatchingGroups(String type);

	/**
	 * Returns all known groups of the requested type with {@code qual} set to {@code qualValue}
	 *
	 * @param type      The type
	 * @param qual
	 * @param qualValue
	 * @return Any matching groups. Empty if no values.
	 */
	public abstract ListenableFuture<List<MatcherGroup>> getMatchingGroups(String type, Qualifier qual, String qualValue);

	public abstract ListenableFuture<MatcherGroup> createMatcherGroup(String type, Map<String, String> entries, Multimap<Qualifier, String> qualifiers);

	public abstract ListenableFuture<MatcherGroup> createMatcherGroup(String type, List<String> entries, Multimap<Qualifier, String> qualifiers);

	public abstract ListenableFuture<Collection<String>> getAllValues(Qualifier qualifier);

	public abstract ListenableFuture<Boolean> hasAnyQualifier(Qualifier qualifier, String value);

	public abstract ListenableFuture<Void> replaceQualifier(Qualifier qualifier, String old, String newVal);

	public abstract ListenableFuture<List<MatcherGroup>> allWithQualifier(Qualifier qualifier);

	public ListenableFuture<String> getName(final String uuid) {
		return Futures.transform(getMatchingGroups(MatcherGroup.UUID_ALIASES_KEY), new Function<List<MatcherGroup>, String>() {
			@Override
			public String apply(List<MatcherGroup> matcherGroups) {
				for (MatcherGroup group : matcherGroups) {
					String ret = group.getEntries().get(uuid);
					if (ret != null) {
						return ret;
					}
				}
				return uuid;
			}
		});
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
			Futures.addCallback(backend.getAll(), new FutureCallback<Iterator<MatcherGroup>>() {
				@Override
				public void onSuccess(Iterator<MatcherGroup> result) {
					List<ListenableFuture<MatcherGroup>> toWait = new LinkedList<>();
					setPersistent(false);
					while (result.hasNext()) {
						MatcherGroup next = result.next();
						toWait.add(createMatcherGroup(next.getName(), next.getEntries(), next.getQualifiers()));
					}
					Futures.allAsList(toWait).addListener(new Runnable() {
						@Override
						public void run() {
							setPersistent(true);
						}
					}, asyncExecutor);
				}

				@Override
				public void onFailure(Throwable throwable) {

				}
			});
	}

	public void revertUUID() {
		Futures.addCallback(Futures.allAsList(getMatchingGroups(MatcherGroup.UUID_ALIASES_KEY), getAll()), new FutureCallback<List<Object>>() {
			@Override
			public void onSuccess(List<Object> objects) {
				List<MatcherGroup> uuids = (List<MatcherGroup>) objects.get(0);
				Iterator<MatcherGroup> all = (Iterator<MatcherGroup>) objects.get(1);

				setPersistent(false);
				try {
					while (all.hasNext()) {
						MatcherGroup group = all.next();
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
				} finally {
					setPersistent(true);
				}
			}

			@Override
			public void onFailure(Throwable throwable) {
				handleException(throwable, "reverting uuids");
			}
		});
	}

	public void setPersistent(boolean persistent) {
		if (persistent) {
			this.activeExecutor = asyncExecutor;
		} else {
			this.activeExecutor = onThreadExecutor;
		}
	}

	/**
	 * This method is called when the file the permissions config is supposed to save to
	 * does not exist yet,This adds default permissions & stuff
	 */
	protected void initializeDefaultConfiguration() throws PermissionBackendException {
				// Load default permissions
				createMatcherGroup(MatcherGroup.INHERITANCE_KEY, Collections.singletonList("default"), ImmutableMultimap.<Qualifier, String>of());
				createMatcherGroup(MatcherGroup.PERMISSIONS_KEY, ImmutableList.of("modifyworld.*"), ImmutableMultimap.of(Qualifier.GROUP, "default"));
				setSchemaVersion(getLatestSchemaVersion());
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
