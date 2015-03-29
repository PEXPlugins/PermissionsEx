package ru.tehkode.permissions.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionDefault;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.events.PermissionEntityEvent;
import ru.tehkode.permissions.events.PermissionSystemEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PEX permissions database integration with superperms
 */
public class SuperpermsListener implements Listener {
	private final PermissionsEx plugin;
	private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

	public SuperpermsListener(PermissionsEx plugin) {
		this.plugin = plugin;
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			updateAttachment(player);
		}
	}

	protected void updateAttachment(Player player) {
		updateAttachment(player, player.getWorld().getName());
	}

	protected void updateAttachment(Player player, String worldName) {
		PermissionAttachment attach = attachments.get(player.getUniqueId());
		Permission playerPerm = getCreateWrapper(player, "");
		Permission playerOptionPerm = getCreateWrapper(player, ".options");
		if (attach == null) {
			attach = player.addAttachment(plugin);
			attachments.put(player.getUniqueId(), attach);
			attach.setPermission(playerPerm, true);
			attach.setPermission(playerOptionPerm, true);
		}

		PermissionUser user = plugin.getPermissionsManager().getUser(player);
		if (user != null) {
			if (user.isDebug()) {
				plugin.getLogger().info("Updating superperms for player " + player.getName());
			}
			updatePlayerPermission(playerPerm, user, worldName);
			updatePlayerMetadata(playerOptionPerm, user, worldName);
			player.recalculatePermissions();
		}
	}

	private String permissionName(Player player, String suffix) {
		return "permissionsex.player." + player.getUniqueId().toString() + suffix;
	}

	private void removePEXPerm(Player player, String suffix) {
		plugin.getServer().getPluginManager().removePermission(permissionName(player, suffix));
	}

	private Permission getCreateWrapper(Player player, String suffix) {
		final String name = permissionName(player, suffix);
		Permission perm = plugin.getServer().getPluginManager().getPermission(name);
		if (perm == null) {
			perm = new Permission(name, "Internal permission for PEX. DO NOT SET DIRECTLY", PermissionDefault.FALSE) {
				@Override
				public void recalculatePermissibles() {
					// no-op
				}
			};
			plugin.getServer().getPluginManager().addPermission(perm);
		}

		return perm;

	}

	private void updatePlayerPermission(Permission permission, PermissionUser user, String worldName) {
		permission.getChildren().clear();
		for (String perm : user.getPermissions(worldName)) {
			boolean value = true;
			if (perm.startsWith("-")) {
				value = false;
				perm = perm.substring(1);
			}
			if (!permission.getChildren().containsKey(perm)) {
				permission.getChildren().put(perm, value);
			}
		}
	}

	private void updatePlayerMetadata(Permission rootPermission, PermissionUser user, String worldName) {
		rootPermission.getChildren().clear();
		final List<String> groups = user.getParentIdentifiers(worldName);
		final Map<String, String> options = user.getOptions(worldName);
		// Metadata
		// Groups
		for (String group : groups) {
			rootPermission.getChildren().put("groups." + group, true);
			rootPermission.getChildren().put("group." + group, true);
		}

		// Options
		for (Map.Entry<String, String> option : options.entrySet()) {
			rootPermission.getChildren().put("options." + option.getKey() + "." + option.getValue(), true);
		}

		// Prefix and Suffix
		rootPermission.getChildren().put("prefix." + user.getPrefix(worldName), true);
		rootPermission.getChildren().put("suffix." + user.getSuffix(worldName), true);

	}

	protected void removeAttachment(Player player) {
		PermissionAttachment attach = attachments.remove(player.getUniqueId());
		if (attach != null) {
			attach.remove();
		}

		removePEXPerm(player, "");
		removePEXPerm(player, ".options");
	}

	public void onDisable() {
		for (PermissionAttachment attach : attachments.values()) {
			attach.remove();
		}
		attachments.clear();
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		try {
			updateAttachment(event.getPlayer());
		} catch (Throwable t) {
			ErrorReport.handleError("Superperms event join", t);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
		if (!plugin.requiresLateUserSetup()) {
			handleLogin(event);
		}
	}

	@EventHandler
	public void onPlayerLoginLate(PlayerLoginEvent event) {
		if (plugin.requiresLateUserSetup()) {
			handleLogin(event);
		}
	}

	private void handleLogin(PlayerLoginEvent event) {
		try {
			final Player player = event.getPlayer();
			// Because player world is inaccurate in the login event (at least with MV), start with null world and then reset to the real world in join event
			removeAttachment(player);
			updateAttachment(player, null);
		} catch (Throwable t) {
			ErrorReport.handleError("Superperms event login", t);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void playerLoginDeny(PlayerLoginEvent event) {
		if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
			try {
				removeAttachment(event.getPlayer());
				Player player = plugin.getServer().getPlayer(event.getPlayer().getUniqueId());
				if (player != null && player.isOnline()) {
					updateAttachment(player);
				}
			} catch (Throwable t) {
				ErrorReport.handleError("Superperms event login denied", t);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	// Technically not supposed to use MONITOR for this, but we don't want to remove before other plugins are done checking permissions
	public void onPlayerQuit(PlayerQuitEvent event) {
		try {
			removeAttachment(event.getPlayer());
		} catch (Throwable t) {
			ErrorReport.handleError("Superperms event quit", t);
		}
	}

	private void updateSelective(PermissionEntityEvent event, PermissionUser user) {
		final Player p = user.getPlayer();
		if (p != null) {
			switch (event.getAction()) {
				case SAVED:
					break;

				case PERMISSIONS_CHANGED:
				case TIMEDPERMISSION_EXPIRED:
					if (user.isDebug()) {
						plugin.getLogger().info("Updating superperms permissions for player " + p.getName());
					}
					updatePlayerPermission(getCreateWrapper(p, ""), user, p.getWorld().getName());
					p.recalculatePermissions();
					break;

				case OPTIONS_CHANGED:
				case INFO_CHANGED:
					if (user.isDebug()) {
						plugin.getLogger().info("Updating superperms metadata for player " + p.getName());
					}
					updatePlayerMetadata(getCreateWrapper(p, ".options"), user, p.getWorld().getName());
					p.recalculatePermissions();
					break;

				default:
					updateAttachment(p);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onEntityEvent(PermissionEntityEvent event) {
		try {
			if (event.getEntity() instanceof PermissionUser) { // update user only
				updateSelective(event, (PermissionUser) event.getEntity());
			} else if (event.getEntity() instanceof PermissionGroup) { // update all members of group, might be resource hog
				for (PermissionUser user : ((PermissionGroup) event.getEntity()).getActiveUsers(true)) {
					updateSelective(event, user);
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
			switch (event.getAction()) {
				case DEBUGMODE_TOGGLE:
				case REINJECT_PERMISSIBLES:
					return;
				default:
					for (Player p : plugin.getServer().getOnlinePlayers()) {
						updateAttachment(p);
					}
			}
		} catch (Throwable t) {
			ErrorReport.handleError("Superperms event permission system event", t);
		}
	}
}
