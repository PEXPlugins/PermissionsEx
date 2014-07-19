package ru.tehkode.permissions.query;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.data.Context;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;
import ru.tehkode.permissions.data.StaticContext;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * Object holding state for a permission query about to happen.
 *
 * @param <T> the type of this class (should be the subclass)
 */
public abstract class PermissionQuery<T extends PermissionQuery<T>> implements Context {
	private final PermissionManager manager;
	private final ConcurrentMap<CacheKey, CacheElement> cache;
	/**
	 * This is present as an ugly hack to get fancy generic'd return types without casting each time.
	 */
	@SuppressWarnings("unchecked")
	private final T self = (T) this;
	private boolean inheritance = true;
	private final Multimap<Qualifier, String> implicitValues = HashMultimap.create(), explicitValues = HashMultimap.create();

	protected PermissionQuery(PermissionManager manager, ConcurrentMap<CacheKey, CacheElement> cache) {
		this.manager = manager;
		this.cache = cache;
	}

	protected PermissionManager getManager() {
		return manager;
	}

	public T followInheritance(boolean inheritance) {
		this.inheritance = inheritance;
		return self;
	}

	public T user(UUID user) {
		return user(user.toString());
	}

	public T user(String identifier) {
		return putQualifier(Qualifier.USER, identifier);
	}

	public T user(Player user) {
		return user(user.getUniqueId());
	}

	public T userAndWorld(Player user) {
		user(user);
		world(user.getWorld());
		return self;
	}

	public T world(String world) {
		return putQualifier(Qualifier.WORLD, world);
	}

	public T world(World world) {
		return world(world.getName());
	}

	public T group(String group) {
		return putQualifier(Qualifier.GROUP, group);
	}

	public T until(Date until) {
		return putQualifier(Qualifier.UNTIL, until.getTime());
	}

	protected T putQualifier(Qualifier qual, Object value) {
		if (value == null) {
			explicitValues.removeAll(qual);
			implicitValues.removeAll(qual);
		} else {
			explicitValues.put(qual, String.valueOf(value));
		}
		return self;
	}

	protected void updateImplicitValues() {
		implicitValues.clear();
		implicitValues.putAll(Qualifier.SERVER, manager.getConfiguration().getServerTags());
		implicitValues.put(Qualifier.UNTIL, String.valueOf(new Date().getTime()));
	}


	protected ListenableFuture<List<MatcherGroup>> performQuery(String sectionName) {
		return performQuery(sectionName, false);
	}

	/**
	 * Performs this query, traversing inheritance if necessary.
	 * Duplicate groups may be present in the result if the same group is relevant in multiple places in the inheritance hierarchy.
	 * @param sectionName The name of the section being looked up in this query.
	 * @param createIfEmpty Create a new matcher group if none match
	 * @return the relevant matcher groups
	 */
	protected ListenableFuture<List<MatcherGroup>> performQuery(final String sectionName, final boolean createIfEmpty) {
		final CacheKey rawContext = new CacheKey(sectionName, StaticContext.of(explicitValues), inheritance);
		CacheElement cacheVal = cache.get(rawContext);

		while (true) {
			if (cacheVal != null) {
				if (!cacheVal.isValid()) {
					cache.remove(rawContext, cacheVal);
				} else {
					break;
				}
			} else {
				cacheVal = fetchUncached(sectionName, createIfEmpty);
				cache.put(rawContext, cacheVal);
			}
		}

		return cacheVal.result;
	}

	/**
	 * Calculate inheritance for any qualifier involved in inheritance
	 *
	 * @return Futures of inheritance for inheritable qualifiers
	 */
	private Map<Qualifier, ListenableFuture<List<String>>> calculateInheritance() {
		final Map<Qualifier, ListenableFuture<List<String>>> parentFutures = new HashMap<>();
		if (inheritance) {
			// TODO: Inheritance for all used/supported qualifiers -- not too important because nothing else uses inheritance now, but makes expansion easier
			if (explicitValues.containsKey(Qualifier.USER)) {
				parentFutures.put(Qualifier.USER.getInheritanceQualifier(), Qualifier.USER.getInheritedValues(manager.getBackend(), explicitValues.get(Qualifier.USER).iterator().next()));
			} else if (explicitValues.containsKey(Qualifier.GROUP)) {
				parentFutures.put(Qualifier.GROUP.getInheritanceQualifier(), Qualifier.GROUP.getInheritedValues(manager.getBackend(), explicitValues.get(Qualifier.GROUP).iterator().next()));
			}
			if (explicitValues.containsKey(Qualifier.WORLD)) {
				parentFutures.put(Qualifier.WORLD.getInheritanceQualifier(), Qualifier.WORLD.getInheritedValues(manager.getBackend(), explicitValues.get(Qualifier.WORLD).iterator().next()));
			}
		}
		return parentFutures;
	}


	private CacheElement fetchUncached(final String sectionName, final boolean createIfEmpty) {
		updateImplicitValues();
		final Map<Qualifier, ListenableFuture<List<String>>> parentFutures = calculateInheritance();
		ListenableFuture<List<MatcherGroup>> result = Futures.chain(Futures.allAsList(parentFutures.values()), new Function<List<List<String>>, ListenableFuture<? extends List<MatcherGroup>>>() {
			@Override
			public ListenableFuture<? extends List<MatcherGroup>> apply(List<List<String>> lists) {
				Multimap<Qualifier, String> contextMultimap = HashMultimap.create(/*implicitValues*/);
				contextMultimap.putAll(explicitValues);
				if (!parentFutures.isEmpty()) {
					for (Map.Entry<Qualifier, ListenableFuture<List<String>>> ent : parentFutures.entrySet()) {
						contextMultimap.putAll(ent.getKey(), Futures.getUnchecked(ent.getValue()));
					}
				}
				return Futures.chain(manager.getBackend().getMatchingGroups(sectionName, StaticContext.of(contextMultimap)), new Function<List<MatcherGroup>, ListenableFuture<List<MatcherGroup>>>() {
					@Override
					public ListenableFuture<List<MatcherGroup>> apply(List<MatcherGroup> matcherGroups) {
						if (matcherGroups.isEmpty() && createIfEmpty) {
							return Futures.transform(manager.getBackend().createMatcherGroup(sectionName, ImmutableMap.<String, String>of(), explicitValues), new Function<MatcherGroup, List<MatcherGroup>>() {
								@Override
								public List<MatcherGroup> apply(MatcherGroup matcherGroup) {
									return ImmutableList.of(matcherGroup);
								}
							});
						} else {
							if (!parentFutures.isEmpty()) {
								Collections.sort(matcherGroups, new InheritanceAwareComparator(parentFutures));
							} else {
								Collections.sort(matcherGroups);
							}
							return Futures.immediateFuture(matcherGroups);
						}
					}
				});
			}
		});
		return new CacheElement(result, Futures.immediateFuture(Collections.<MatcherGroup>emptyList())); // TODO: Keep track of groups involved in inheritance
	}

	/**
	Special InheritanceAware comparator
	map<string, int> for inheritance level
	 */
	private static class InheritanceAwareComparator implements Comparator<MatcherGroup> {
		private final Map<Qualifier, ListenableFuture<List<String>>> parentFutures;

		private InheritanceAwareComparator(Map<Qualifier, ListenableFuture<List<String>>> parentFutures) {
			this.parentFutures = parentFutures;
		}

		@Override
		public int compare(MatcherGroup a, MatcherGroup b) {
			final int me = a.getQualifierTypeMask();
			final int other = b.getQualifierTypeMask();
			if (me != other) {
				return me > other ? 1 : -1;
			}

			return 0; // TODO: Take inheritance into account
		}
	}

	public static final class CacheKey {
		private final String sectionName;
		private final Context context;
		private final boolean inheritance;

		private CacheKey(String sectionName, Context context, boolean inheritance) {
			this.sectionName = sectionName;
			this.context = context;
			this.inheritance = inheritance;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			CacheKey cacheKey = (CacheKey) o;

			if (!context.equals(cacheKey.context)) return false;
			if (!sectionName.equals(cacheKey.sectionName)) return false;
			if (!inheritance == cacheKey.inheritance) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = sectionName.hashCode();
			result = 31 * result + context.hashCode();
			result = 7 * result + (inheritance ? 1 : 0);
			return result;
		}
	}

	public static class CacheElement {
		private final ListenableFuture<List<MatcherGroup>> result, inheritanceGroups;

		private CacheElement(ListenableFuture<List<MatcherGroup>> result, ListenableFuture<List<MatcherGroup>> inheritanceGroups) {
			this.result = result;
			this.inheritanceGroups = inheritanceGroups;
		}

		public boolean isValid() {
			if (!result.isDone() || !inheritanceGroups.isDone()) {
				return true;
			}
			try {
				for (MatcherGroup group : inheritanceGroups.get()) {
					if (!group.isValid()) {
						return false;
					}
				}
				for (MatcherGroup group : result.get()) {
					if (!group.isValid()) {
						return false;
					}
				}
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
	}

	// Context methods

	@Override
	public Collection<String> getValues(Qualifier qual) {
		Collection<String> ret = explicitValues.get(qual);
		if (ret.isEmpty()) {
			ret = implicitValues.get(qual);
		}
		return ret;
	}

	@Override
	public boolean hasValue(Qualifier qual) {
		return explicitValues.containsKey(qual) || implicitValues.containsKey(qual);
	}

	@Override
	public ImmutableMultimap<Qualifier, String> getValues() {
		ImmutableMultimap.Builder<Qualifier, String> build = ImmutableMultimap.builder();
		build.putAll(implicitValues);
		build.putAll(explicitValues);
		return build.build();
	}
}
