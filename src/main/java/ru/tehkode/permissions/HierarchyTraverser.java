package ru.tehkode.permissions;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Class created to simplify hierarchy traversal for entities
 */
public abstract class HierarchyTraverser<Return> {
	private final PermissionEntity start;
	private final String world;
	private final boolean traverseInheritance;

	public HierarchyTraverser(PermissionEntity entity, String world) {
		this(entity, world, true);
	}
	public HierarchyTraverser(PermissionEntity entity, String world, boolean traverseInheritance) {
		this.start = entity;
		this.world = world;
		this.traverseInheritance = traverseInheritance;
	}


	/**
	 * Performs a traversal of permissions hierarchy
	 *
	 * Ordering:
	 * For each entity (traversed depth-first):
	 * <ol>
	 *     <li>Chosen world</li>
	 *     <li>World inheritance for chosen world</li>
	 *     <li>Global scope</li>
	 * </ol>
	 *
	 * @return a value if any found
	 */
	public Return traverse() {
		LinkedList<PermissionEntity> entities = new LinkedList<>();
		Set<PermissionEntity> visited = new HashSet<>();
		entities.add(start);
		Return ret = null;
		while (!entities.isEmpty()) {
			PermissionEntity current = entities.removeFirst();
			// Circular inheritance detection
			if (visited.contains(current)) {
				if (current.isDebug()) {
					current.manager.getLogger().warning("Potential circular inheritance detected involving group " + current.getIdentifier() + " (when performing traversal for entity " + start + ")");
				}
				continue;
			}
			visited.add(current);

			// World-specific
			if (world != null) {
				ret = fetchLocal(current, world);
				if (ret != null) {
					break;
				}

				// World inheritance
				ret = traverseWorldInheritance(current);
				if (ret != null) {
					break;
				}
			}
			// Global scope
			ret = fetchLocal(current, null);
			if (ret != null) {
				break;
			}

			// Add parents
			if (traverseInheritance) {
				List<PermissionGroup> parents = current.getParents(world);
				for (int i = parents.size() - 1; i >= 0; --i) { // Add parents to be traversed in order provided by getParents
					entities.addFirst(parents.get(i));
				}
			}
		}
		return ret;
	}

	/**
	 * Traverses world inheritance depth-first.
	 *
	 * @param entity Entity to perform local action on
	 * @return Any detected results
	 */
	private Return traverseWorldInheritance(PermissionEntity entity) {
		List<String> worldInheritance = entity.manager.getWorldInheritance(world);
		if (worldInheritance.size() > 0) {
			Deque<String> worlds = new LinkedList<>(worldInheritance);
			Set<String> visitedWorlds = new HashSet<>();
			Return ret = null;
			while (!worlds.isEmpty()) {
				String current = worlds.removeFirst();
				if (visitedWorlds.contains(current)) {
					if (entity.isDebug()) {
						entity.manager.getLogger().warning("Potential circular inheritance detected with world inheritance for world " + current);
					}
					continue;
				}
				visitedWorlds.add(current);

				ret = fetchLocal(entity, current);
				if (ret != null) {
					break;
				}

				final List<String> nextLevel = entity.manager.getWorldInheritance(current);
				for (int i = nextLevel.size() - 1; i >= 0; --i) {
					worlds.add(nextLevel.get(i));
				}
			}
			return ret;
		}
		return null;
	}

	/**
	 * Collects the potential return value from a single entity
	 * @param entity Entity being checked in
	 * @param world World being checked in
	 * @return The value, or null if not present
	 */
	protected abstract Return fetchLocal(PermissionEntity entity, String world);
}
