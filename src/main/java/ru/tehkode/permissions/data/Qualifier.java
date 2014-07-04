package ru.tehkode.permissions.data;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import ru.tehkode.permissions.EntityType;
import ru.tehkode.permissions.backends.PermissionBackend;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
* A qualifier for a match group.
*/
public abstract class Qualifier {
	private static final AtomicInteger REGISTERED_COUNT = new AtomicInteger(0);
	private static final ConcurrentMap<String, Qualifier> STRING_LOOKUP = new ConcurrentHashMap<>();

	public static final Qualifier USER = new Qualifier("user") {
		@Override
		public boolean matches(Context context, String value) {
			return context.getEntityType() == EntityType.USER && context.getEntityName().equals(value);
		}

		@Override
		protected Qualifier getInheritanceQualifier() {
			return GROUP;
		}

		@Override
		public String getInheritanceSectionName() {
			return MatcherGroup.INHERITANCE_KEY;
		}
	};
	public static final Qualifier GROUP = new Qualifier("group") {
		@Override
		public boolean matches(Context context, String value) {
			return context.getEntityType() == EntityType.GROUP && context.getEntityName().equals(value);
		}

		@Override
		public String getInheritanceSectionName() {
			return MatcherGroup.INHERITANCE_KEY;
		}
	};
	public static final Qualifier WORLD = new Qualifier("world") {
		@Override
		public boolean matches(Context context, String value) {
			return context.getWorld() != null && context.getWorld().equals(value);
		}

		@Override
		public String getInheritanceSectionName() {
			return MatcherGroup.WORLD_INHERITANCE_KEY;
		}
	};
	public static final Qualifier UNTIL = new Qualifier("until") {
		@Override
		public boolean matches(Context context, String value) {
			// null or stored until is after the context until means this matches
			return context.getUntil() == 0 || Long.parseLong(value) > context.getUntil();
		}
	};
	public static final Qualifier SERVER = new Qualifier("server") {
		@Override
		public boolean matches(Context context, String value) {
			return context.getServerTags().contains(value);
		}
	};

	private final String name;
	private final int id;

	public Qualifier(String name) {
		this.name = name;
		if (STRING_LOOKUP.putIfAbsent(name, this) == null) {
			this.id = REGISTERED_COUNT.getAndIncrement();
		} else {
			throw new IllegalStateException("A qualifier named " + this.name + " already exists!");
		}
	}

	public static int getRegisteredCount() {
		return REGISTERED_COUNT.get();
	}

	public static Qualifier fromString(String key) {
		return STRING_LOOKUP.get(key);
	}

	public abstract boolean matches(Context context, String value);

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}

	public int getFlag() {
		return 1 << getId();
	}

	protected String getInheritanceSectionName() {
		return null;
	}

	protected Qualifier getInheritanceQualifier() {
		return this;
	}

	public ListenableFuture<List<String>> getInheritedValues(PermissionBackend backend, String value) {
		final String inheritanceSectionName = getInheritanceSectionName();
		if (inheritanceSectionName == null) {
			return Futures.<List<String>>immediateFuture(ImmutableList.<String>of());
		}

		final List<String> inherited = new ArrayList<>();

		return Futures.transform(processInherited(backend, this, inherited, value), new Function<List<MatcherGroup>, List<String>>() {
			@Override
			public List<String> apply(@Nullable List<MatcherGroup> matcherGroups) {
				return inherited; // TODO Look at returning relevant matcher groups
			}
		});
	}

	private ListenableFuture<List<MatcherGroup>> processInherited(final PermissionBackend backend, final Qualifier qual, final List<String> inherited, String current) {
		return Futures.transform(backend.getMatchingGroups(qual.getInheritanceSectionName(), qual, current), new Function<List<MatcherGroup>, List<MatcherGroup>>() {
			@Override
			public List<MatcherGroup> apply(@Nullable List<MatcherGroup> matcherGroups) {
				final List<MatcherGroup> inheritanceGroups = new ArrayList<>();
				for (MatcherGroup match : matcherGroups) {
					if (!match.isList()) {
						continue;
					}
					if (match.getEntriesList().isEmpty()) {
						continue;
					}
					inheritanceGroups.add(match);
					for (String entry : match.getEntriesList()) {
						if (inherited.contains(entry)) {
							backend.getLogger().warning("Potential circular inheritance detected while iterating " + qual + " " + inherited.get(0));
							continue;
						}

						inherited.add(entry);
						inheritanceGroups.addAll(Futures.getUnchecked(processInherited(backend, qual.getInheritanceQualifier(), inherited, entry)));
					}
				}
				return inheritanceGroups;
			}
		});
	}
}
