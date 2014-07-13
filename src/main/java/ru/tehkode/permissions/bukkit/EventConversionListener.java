package ru.tehkode.permissions.bukkit;

import org.apache.commons.lang.ObjectUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.tehkode.permissions.EntityType;
import ru.tehkode.permissions.data.MatcherGroup;
import ru.tehkode.permissions.data.Qualifier;
import ru.tehkode.permissions.events.MatcherGroupEvent;
import ru.tehkode.permissions.events.PermissionEntityEvent;

/**
 * Event transformer that will
 */
public class EventConversionListener implements Listener {
	private final PermissionsEx plugin;

	public EventConversionListener(PermissionsEx plugin) {
		this.plugin = plugin;
	}
	/*
		TIMEDPERMISSION_EXPIRED,
		DEFAULTGROUP_CHANGED,
	 */
	/**
	 *
	 * @param event
	 */
	@EventHandler
	@SuppressWarnings("deprecation")
	public void handleMatcherGroup(final MatcherGroupEvent event) {
		if (PermissionEntityEvent.getHandlerList().getRegisteredListeners().length == 0) { // No listeners, don't pass on the event
			return;
		}

		PermissionEntityEvent.Action legacyEvent = null;
		final EntityType type;
		final String identifier;
		final MatcherGroup activeGroup = getActiveGroup(event);
		if (activeGroup.getQualifiers().containsKey(Qualifier.USER)) {
			type = EntityType.USER;
			identifier = activeGroup.getQualifiers().get(Qualifier.USER).iterator().next();
		} else if (activeGroup.getQualifiers().containsKey(Qualifier.GROUP)) {
			type = EntityType.GROUP;
			identifier = activeGroup.getQualifiers().get(Qualifier.GROUP).iterator().next();
		} else {
			return;
		}

		switch (event.getAction()) {
			case REMOVE:
				legacyEvent = PermissionEntityEvent.Action.REMOVED;
				break;
			case CREATE:
			case CHANGE_ENTRIES:
				if (event.getNewGroup().getEntries().isEmpty()) {
					legacyEvent = PermissionEntityEvent.Action.SAVED;
				} else {
					switch (event.getNewGroup().getName()) {
						case MatcherGroup.INHERITANCE_KEY:
							legacyEvent = PermissionEntityEvent.Action.INHERITANCE_CHANGED;
							break;
						case MatcherGroup.OPTIONS_KEY:
							legacyEvent = PermissionEntityEvent.Action.OPTIONS_CHANGED;
							if (difference(event, "prefix") || difference(event, "suffix")) {
								legacyEvent = PermissionEntityEvent.Action.INFO_CHANGED;
							} else if (difference(event, "rank") || difference(event, "rank-ladder")) {
								legacyEvent = PermissionEntityEvent.Action.RANK_CHANGED;
							} else if (difference(event, "weight")) {
								legacyEvent = PermissionEntityEvent.Action.WEIGHT_CHANGED;
							}
							break;
						case MatcherGroup.PERMISSIONS_KEY:
							legacyEvent = PermissionEntityEvent.Action.PERMISSIONS_CHANGED;
							break;
					}
				}
				break;
			case CHANGE_QUALIFIERS:
				break;
		}

		if (legacyEvent != null) {
			if (event.isAsynchronous()) {
				final PermissionEntityEvent.Action finalLegacyEvent = legacyEvent;
				plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
					@Override
					public void run() {
						plugin.getServer().getPluginManager().callEvent(new PermissionEntityEvent(event.getSourceUUID(), identifier, type, finalLegacyEvent));
					}
				});
			} else {
				plugin.getServer().getPluginManager().callEvent(new PermissionEntityEvent(event.getSourceUUID(), identifier, type, legacyEvent));
			}
		}
	}

	private MatcherGroup getActiveGroup(MatcherGroupEvent event) {
		return event.getNewGroup() == null ? event.getOldGroup() : event.getNewGroup();
	}

	private boolean difference(MatcherGroupEvent event, String key) {
		if (event.getOldGroup() == null) {
			return event.getNewGroup().getEntries().containsKey(key);
		} else if (event.getNewGroup() == null) {
			return event.getOldGroup().getEntries().containsKey(key);
		} else {
			return !ObjectUtils.equals(event.getOldGroup().getEntries().get(key), event.getNewGroup().getEntries().get(key));
		}
	}
}
