package ru.tehkode.permissions.query;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.tehkode.permissions.EntityType;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.data.Context;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Object holding state for a permission query about to happen.
 *
 * @param <T> the type of this class (should be the subclass)
 */
public abstract class PermissionQuery<T extends PermissionQuery<T>> implements Context {
	private final PermissionManager manager;
	/**
	 * This is present as an ugly hack to get fancy generic'd return types without casting each time.
	 */
	@SuppressWarnings("unchecked")
	private final T self = (T) this;
	protected Qualifier primaryKey;
	protected String primaryValue;
	private String world;
	private long until;
	private boolean inheritance = true;

	protected PermissionQuery(PermissionManager manager) {
		this.manager = manager;
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
		primaryKey = Qualifier.USER;
		primaryValue = identifier;
		return self;
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
		this.world = world;
		if (this.primaryKey == null || primaryKey == Qualifier.WORLD) {
			this.primaryKey = Qualifier.WORLD;
			this.primaryValue = world;
		}
		return self;
	}

	public T world(World world) {
		return world(world.getName());
	}

	public T group(String group) {
		primaryKey = Qualifier.GROUP;
		primaryValue = group;
		return self;
	}

	public T until(Date until) {
		this.until = until.getTime();
		return self;
	}

	/**
	 * Performs this query, traversing inheritance if necessary.
	 * Duplicate groups may be present in the result if the same group is relevant in multiple places in the inheritance hierarchy.
	 * @param sectionName The name of the section being looked up in this query.
	 * @return the relevant matcher groups
	 */
	protected ListenableFuture<List<MatcherGroup>> performQuery(String sectionName) {
		ListenableFuture<List<MatcherGroup>> query;
		if (primaryKey != null) {
			query = this.manager.getBackend().getMatchingGroups(sectionName, primaryKey, primaryValue);
		} else {
			query = this.manager.getBackend().getMatchingGroups(sectionName);
		}
		return Futures.transform(query, new Function<List<MatcherGroup>, List<MatcherGroup>>() {
			@Override
			public List<MatcherGroup> apply(List<MatcherGroup> result) {
				if (result != null) {
					for (Iterator<MatcherGroup> it = result.iterator(); it.hasNext(); ) {
						if (!it.next().matches(PermissionQuery.this)) {
							it.remove();
						}
					}
					Collections.sort(result);
				}
				return result;
			}
		});
	}

	// Context methods

	@Override
	public Set<String> getServerTags() {
		return manager.getConfiguration().getServerTags();
	}

	@Override
	public String getEntityName() {
		return primaryValue;
	}

	@Override
	public EntityType getEntityType() {
		if (primaryKey == Qualifier.USER) {
			return EntityType.USER;
		} else if (primaryKey == Qualifier.GROUP) {
			return EntityType.GROUP;
		} else {
			return null;
		}
	}

	@Override
	public String getWorld() {
		return world;
	}

	@Override
	public long getUntil() {
		return until;
	}
}
