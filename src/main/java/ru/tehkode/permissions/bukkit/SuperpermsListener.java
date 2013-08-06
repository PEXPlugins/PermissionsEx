package ru.tehkode.permissions.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.ErrorReport;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.events.PermissionEntityEvent;
import ru.tehkode.permissions.events.PermissionSystemEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zml2008
 */
public class SuperpermsListener implements Listener {
	private final PermissionsEx plugin;
	private final Map<String, PermissionAttachment> attachments = new ConcurrentHashMap<String, PermissionAttachment>();

	public SuperpermsListener(PermissionsEx plugin) {
		this.plugin = plugin;
	}

	protected void updateAttachment(Player player) {
		PermissionAttachment prevAttach = attachments.get(player.getName());
		if (prevAttach != null) {
			prevAttach.remove();
		}

		final PermissionAttachment attach = player.addAttachment(plugin);
		attachments.put(player.getName(), attach);
		PermissionUser user = plugin.getPermissionsManager().getUser(player);
		if (user != null) {
			final String[] perms = user.getPermissions(player.getWorld().getName());
			final String worldName = player.getWorld().getName();
			final String[] groups = user.getGroupsNames(worldName);
			final Map<String, String> options = user.getOptions(worldName);
			final String prefix = user.getPrefix(worldName), suffix = user.getSuffix(worldName);

			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
				@Override
				public void run() {
					for (String perm : perms) {
						boolean value = true;
						if (perm.startsWith("-")) {
							value = false;
							perm = perm.substring(1);
						}
						attach.setPermission(perm, value);
					}

					// Metadata
					// Groups
					for (String group : groups) {
						attach.setPermission("groups." + group, true);
						attach.setPermission("group." + group, true);
					}

					// Options
					for (Map.Entry<String, String> option : options.entrySet()) {
						attach.setPermission("options." + option.getKey() + "." + option.getValue(), true);
					}

					// Prefix and Suffix
					attach.setPermission("prefix." + prefix, true);
					attach.setPermission("suffix." + suffix, true);
				}
			});
		}
	}

	protected void removeAttachment(Player player) {
		PermissionAttachment attach = attachments.remove(player.getName());
		if (attach != null) {
			attach.remove();
		}
	}

	public void onDisable() {
		for (PermissionAttachment attach : attachments.values()) {
			attach.remove();
		}
		attachments.clear();
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
		try {
			updateAttachment(event.getPlayer());
		} catch (Throwable t) {
			ErrorReport.handleError("Superperms event login", t);
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		try {
			removeAttachment(event.getPlayer());
		} catch (Throwable t) {
			ErrorReport.handleError("Superperms event quit", t);
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onEntityEvent(PermissionEntityEvent event) {
		try {
			if (event.getEntity() instanceof PermissionUser) { // update user only
				final Player p = plugin.getServer().getPlayerExact(event.getEntity().getName());
				if (p != null) {
					updateAttachment(p);
				}
			} else if (event.getEntity() instanceof PermissionGroup) { // update all members of group, might be resource hog
				for (PermissionUser user : plugin.getPermissionsManager().getUsers(event.getEntity().getName(), true)) {
					final Player p = plugin.getServer().getPlayerExact(user.getName());
					if (p != null) {
						updateAttachment(p);
					}
				}
			}
		} catch (Throwable t) {
			ErrorReport.handleError("Superperms event permission entity", t);
		}
	}

	@EventHandler
	public void onWorldChanged(PlayerChangedWorldEvent event) {
		try {
			updateAttachment(event.getPlayer());
		} catch (Throwable t) {
			ErrorReport.handleError("Superperms event world change", t);
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onSystemEvent(PermissionSystemEvent event) {
		try {
			if (event.getAction() == PermissionSystemEvent.Action.DEBUGMODE_TOGGLE) {
				return;
			}

			for (Player p : plugin.getServer().getOnlinePlayers()) {
				updateAttachment(p);
			}
		} catch (Throwable t) {
			ErrorReport.handleError("Superperms event permission system event", t);
		}
	}
}
