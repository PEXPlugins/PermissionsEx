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
import com.google.common.util.concurrent.MoreExecutors;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.ErrorReport;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;
import ru.tehkode.permissions.exceptions.PermissionBackendException;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;
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

	/**
	 * Add a schema update to the list of updates that will be considered for {@link #performSchemaUpdate()}
	 *
	 * @param update The update to add
	 */
	protected void addSchemaUpdate(SchemaUpdate update) {
		schemaUpdates.add(update);
		Collections.sort(schemaUpdates);
	}

	/**
	 * Actually perform the schema update using updates registered with {@link #addSchemaUpdate(SchemaUpdate)}
	 *
	 * This will update the schema from its current version to the latest version with an associated updater.
	 * It makes no attempt to roll back partially failed updates, but will update the saved schema version
	 * to the latest update which completed successfully.
	 */
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
	 * -1 indicates that the schema version is unknown or this backend does not use schema versioning (For example in a transient backend).
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

	/**
	 * Returns the latest schema version known for this backend, or -1 if no schema updates are present.
	 *
	 * @return The latest known schema version
	 */
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

	protected ListenableFuture<Void> execute(Runnable run) {
		ListenableFutureTask<Void> ret = ListenableFutureTask.create(run, null);
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


	/**
	 * Clear any cached data for this backend.
	 *
	 * @throws PermissionBackendException If an error occurs with accessing the updated data.
	 * Implementations may try to fall back to using existing cached data.
	 */
	public abstract void reload() throws PermissionBackendException;

	/**
	 * Return friendly names of known users. These may not
	 *
	 * @return Names associated with users
	 */
	public abstract Collection<String> getUserNames();


	// -- World inheritance

	/**
	 * Returns an iterator that will produce all matcher groups known to this backend. This may be an extremely long list.
	 * This iterable may not be completely filled. The backend may choose to asynchronously load results.
	 * In this case, calls to {@link Iterator#next()} may block.
	 *
	 * @return An iterable that will provide all matcher groups.
	 */
	public abstract Iterable<MatcherGroup> getAll();

	/**
	 * Returns a matcher group only if there is only a single result. If multiple groups match, or no groups match, null is returned.
	 *
	 * @param type The name of the section.
	 * @param qual The primary-key qualifier
	 * @param qualValue The value of the qualifier used.
	 * @return the result if only one result is present, otherwise null
	 */
	public ListenableFuture<MatcherGroup> getOne(final String type, final Qualifier qual, final String qualValue) {
		return Futures.transform(getMatchingGroups(type, qual, qualValue), new Function<List<MatcherGroup>, MatcherGroup>() {
			@Override
			public MatcherGroup apply(List<MatcherGroup> matcherGroups) {
				return matcherGroups.size() == 1 ? matcherGroups.get(0) : null;
			}
		});
	}

	/**
	 * Returns the first known matcher group, or null if no groups match.
	 * Ordering of groups is backend-dependant currently.
	 * This may be changed to be based on a sorted list of matching groups.
	 *
	 * @param type The name of the section
	 * @param qual The primary-key qualifier
	 * @param qualValue The value of the qualifier used.
	 * @return the result, or null if no groups matched
	 */
	public ListenableFuture<MatcherGroup> getFirst(final String type, final Qualifier qual, final String qualValue) {
		return Futures.transform(getMatchingGroups(type, qual, qualValue), new Function<List<MatcherGroup>, MatcherGroup>() {
			@Override
			public MatcherGroup apply(List<MatcherGroup> matcherGroups) {
				if (!matcherGroups.isEmpty()) {
					return matcherGroups.get(0);
				} else {
					return null;
				}
			}
		});
	}

	/**
	 * Returns a matcher group only if there is only a single result. If multiple groups match, or no groups match, null is returned.
	 *
	 * @param type The name of the section
	 * @return the result if only one result is present, otherwise null
	 */
	public ListenableFuture<MatcherGroup> getOne(final String type) {
		return Futures.transform(getMatchingGroups(type), new Function<List<MatcherGroup>, MatcherGroup>() {
			@Override
			public MatcherGroup apply(List<MatcherGroup> matcherGroups) {
				return matcherGroups != null && matcherGroups.size() == 1 ? matcherGroups.get(0) : null;
			}
		});
	}

	/**
	 * Returns the first known matcher group, or null if no groups match.
	 * Ordering of groups is backend-dependant currently.
	 * This may be changed to be based on a sorted list of matching groups.
	 *
	 * @param type The name of the section
	 * @return the result, or null if no groups matched
	 */	public ListenableFuture<MatcherGroup> getFirst(final String type) {
		return Futures.transform(getMatchingGroups(type), new Function<List<MatcherGroup>, MatcherGroup>() {
			@Override
			public MatcherGroup apply(@Nullable List<MatcherGroup> matcherGroups) {
				return matcherGroups == null || matcherGroups.isEmpty() ? null : matcherGroups.get(0);
			}
		});
	}

	/**
	 * Returns the first known matcher group, or creates a new group if none match.
	 * What 'first' means is entirely backend-dependant.
	 *
	 * @param type The name of the section
	 * @return Either an existing or new group, never null.
	 */
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

	/**
	 * Returns the first known matcher group, or creates a new group if none match.
	 * What 'first' means is entirely backend-dependant.
	 *
	 * @param type The name of the section
	 * @param key The primary-key qualifier
	 * @param value The qualifier's value
	 * @return Either an existing or new group, never null.
	 */
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
	 * @param type The type of group to look up
	 * @return Any matching groups. Empty if no values.
	 */
	public abstract ListenableFuture<List<MatcherGroup>> getMatchingGroups(String type);

	/**
	 * Returns all known groups of the requested type with {@code qual} set to {@code qualValue}
	 *
	 * @param type The type of group to look up
	 * @param qual A primary-key qualifier
	 * @param qualValue A value for the primary-key qualifier
	 * @return Any matching groups. Empty if no values.
	 */
	public abstract ListenableFuture<List<MatcherGroup>> getMatchingGroups(String type, Qualifier qual, String qualValue);

	/**
	 * Creates a new matcher group with the provided parameters.
	 * Null values are not permitted in the entries list.
	 *
	 * @param type The type of group to create.
	 * @param entries The entries the created group will have
	 * @param qualifiers The qualifiers the created group will have
	 * @return Future of the created group, result never null
	 */
	public abstract ListenableFuture<MatcherGroup> createMatcherGroup(String type, Map<String, String> entries, Multimap<Qualifier, String> qualifiers);

	/**
	 * Creates a new matcher group with the provided parameters.
	 * Null values are not permitted in the entries list.
	 *
	 * @param type The type of group to create.
	 * @param entries The entries the created group will have
	 * @param qualifiers The qualifiers the created group will have
	 * @return Future of the created group, result never null
	 */
	public abstract ListenableFuture<MatcherGroup> createMatcherGroup(String type, List<String> entries, Multimap<Qualifier, String> qualifiers);

	/**
	 * Returns all values this qualifier has across every matcher group.
	 * The result of the future will never be null, instead returning an empty collection representing no values.
	 *
	 * @param qualifier The qualifier to get values for
	 * @return Future of all values, no duplicates.
	 */
	public abstract ListenableFuture<Collection<String>> getAllValues(Qualifier qualifier);

	/**
	 * Returns whether any group has the gives qualifier set to the given value.
	 *
	 * @param qualifier The qualifier to check
	 * @param value The value to check.
	 * @return Future of the result
	 */
	public abstract ListenableFuture<Boolean> hasAnyQualifier(Qualifier qualifier, String value);

	/**
	 * Replace all occurrences of the given qualifier set to {@code old} with {@code newVal}.
	 *
	 * @param qualifier The qualifier to operate on
	 * @param old The old value
	 * @param newVal The new value
	 * @return A value-less future, really only useful for completion listeners
	 */
	public abstract ListenableFuture<Void> replaceQualifier(Qualifier qualifier, String old, String newVal);

	/**
	 * Return any matcher group containing the given qualifier with any value.
	 * The result is never null.
	 *
	 * @param qualifier The qualifier to check
	 * @return Future of the list of matcher groups.
	 */
	public abstract ListenableFuture<List<MatcherGroup>> allWithQualifier(Qualifier qualifier);

	/**
	 * Return the stored name for a given uuid, or null if the uuid is unknown.
	 *
	 * @param uuid The uuid to check
	 * @return The name if known, otherwise {@code uuid}
	 */
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

	/**
	 * Performs any cleanup necessary to shut down this backend.
	 *
	 * @throws PermissionBackendException if shutdown does not occur successfully
	 */
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
	public void loadFrom(final PermissionBackend backend) {
		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				List<ListenableFuture<MatcherGroup>> toWait = new LinkedList<>();
				setPersistent(false);
				for (MatcherGroup next : backend.getAll()) {
					toWait.add(createMatcherGroup(next.getName(), next.getEntries(), next.getQualifiers()));
				}
				Futures.allAsList(toWait).addListener(new Runnable() {
					@Override
					public void run() {
						setPersistent(true);
					}
				}, MoreExecutors.sameThreadExecutor());
			}
		});
	}

	/**
	 * Revert any occurrences of UUID-using data from the PEX database. This allows recovering from any incorrect UUID assignments.
	 */
	public void revertUUID() {
		Futures.addCallback(getMatchingGroups(MatcherGroup.UUID_ALIASES_KEY), new FutureCallback<List<MatcherGroup>>() {
			@Override
			public void onSuccess(@Nonnull List<MatcherGroup> uuids) {
				setPersistent(false);
				try {
					List<ListenableFuture<?>> toWaitFor = new LinkedList<>();
					for (MatcherGroup group : getAll()) {
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
							toWaitFor.add(group.setQualifiers(qualifiers));
						}
					}
					for (MatcherGroup matcher : uuids) {
						toWaitFor.add(matcher.remove());
					}

					for (ListenableFuture<?> future : toWaitFor) {
						Futures.getUnchecked(future);
					}
				} finally {
					setPersistent(true);
				}
			}

			@Override
			public void onFailure(@Nonnull Throwable throwable) {
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
	@SuppressWarnings("unchecked")
	protected void initializeDefaultConfiguration() throws PermissionBackendException {
				// Load default permissions
				Futures.getUnchecked(Futures.allAsList(createMatcherGroup(MatcherGroup.INHERITANCE_KEY, Collections.singletonList("default"), ImmutableMultimap.<Qualifier, String>of()),
				createMatcherGroup(MatcherGroup.PERMISSIONS_KEY, ImmutableList.of("modifyworld.*"), ImmutableMultimap.of(Qualifier.GROUP, "default"))));
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
	 * @param alias The short name to look the backend up by
	 * @return The backend class, if any.
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
	 * @param alias The short name that the provided class can be looked up by
	 * @param backendClass The class to be aliased
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
	 * @param backendClass The backend class to get the alias for.
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
