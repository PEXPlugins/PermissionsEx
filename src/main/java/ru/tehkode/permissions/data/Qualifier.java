package ru.tehkode.permissions.data;

import ru.tehkode.permissions.EntityType;

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
	};
	public static final Qualifier GROUP = new Qualifier("group") {
		@Override
		public boolean matches(Context context, String value) {
			return context.getEntityType() == EntityType.GROUP && context.getEntityName().equals(value);
		}
	};
	public static final Qualifier WORLD = new Qualifier("world") {
		@Override
		public boolean matches(Context context, String value) {
			return context.getWorld() != null && context.getWorld().equals(value);
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
		return 0b1 << (30 - getId());
	}
}
