package ru.tehkode.permissions.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Event called whenever a matcher group is created.
 */
public class MatcherGroupEvent extends PermissionEvent {
	private final MatcherGroup oldGroup, newGroup;
	private final Action action;
	private static final HandlerList HANDLERS = new HandlerList();

	public MatcherGroupEvent(UUID id, MatcherGroup oldGroup, MatcherGroup newGroup, Action action) {
		super(id, true);
		this.oldGroup = oldGroup;
		this.newGroup = newGroup;
		this.action = action;
	}

	public MatcherGroup getOldGroup() {
		return oldGroup;
	}

	public MatcherGroup getNewGroup() {
		return newGroup;
	}

	public Action getAction() {
		return action;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	/**
	 * Returns online players affected by the changes in this event
	 *
	 *
	 * @return online affected players
	 */
	public Iterable<Player> getChangedPlayers() {
		final Set<Player> changedPlayers = new HashSet<>();
		if (oldGroup != null) {
			addPlayers(changedPlayers, oldGroup);
		}

		if (newGroup != null) {
			addPlayers(changedPlayers, newGroup);
		}
		return changedPlayers;
	}

	private void addPlayers(Set<Player> changedPlayers, MatcherGroup group) {
		for (String name : group.getQualifiers().get(Qualifier.USER)) {
			Player player = getPlayer(name);
			if (player != null) {
				changedPlayers.add(player);
			}
		}
		// TODO Implement inheritance resolution
	}

	private Player getPlayer(String identifier) {
		try {
			return Bukkit.getServer().getPlayer(UUID.fromString(identifier));
		} catch (Throwable ex) { // Not a UUID or method not implemented in server build
			return Bukkit.getServer().getPlayerExact(identifier);
		}
	}

	public String getGroupType() {
		if (newGroup != null) {
			return newGroup.getName();
		} else {
			return oldGroup.getName();
		}
	}

	public static enum Action {
		CHANGE_QUALIFIERS,
		CHANGE_ENTRIES,
		REMOVE,
		CREATE
	}
}
