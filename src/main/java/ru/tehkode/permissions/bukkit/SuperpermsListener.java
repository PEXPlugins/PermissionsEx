package ru.tehkode.permissions.bukkit;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
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
import ru.tehkode.permissions.events.PermissionEntityEvent;
import ru.tehkode.permissions.events.PermissionSystemEvent;
import ru.tehkode.permissions.query.GetQuery;

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

			if (plugin.getPermissionsManager().isDebug(player)) {
				plugin.getLogger().info("Updating superperms for player " + player.getName());
			}
			updatePlayerPermission(playerPerm, player, worldName);
			updatePlayerMetadata(playerOptionPerm, player, worldName);
			player.recalculatePermissions();
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
			perm = new Permission(name, "Internal permission for PEX. DO NOT SET DIRECTLY", PermissionDefault.FALSE);
			plugin.getServer().getPluginManager().addPermission(perm);
		}

		return perm;

	}

	private ListenableFuture<?> updatePlayerPermission(final Permission permission, final Player user, final String worldName) {
		return Futures.chain(plugin.getPermissionsManager().get().user(user).world(worldName).permissions(), new Function<List<String>, ListenableFuture<?>>() {
			@Override
			public ListenableFuture<?> apply(List<String> permissions) {
				permission.getChildren().clear();
				for (String perm : permissions) {
					boolean value = true;
					if (perm.startsWith("-")) {
						value = false;
						perm = perm.substring(1);
					}
					if (!permission.getChildren().containsKey(perm)) {
						permission.getChildren().put(perm, value);
					}
				}
				return Futures.<Void>immediateFuture(null);
			}
		}, PermissionsEx.mainThreadExecutor());
	}

	private ListenableFuture<?> updatePlayerMetadata(final Permission rootPermission, Player user, String worldName) {
		GetQuery query = plugin.getPermissionsManager().get().user(user).world(worldName);
		return Futures.transform(Futures.allAsList(query.parents(), query.options()), new Function<List<Object>, Object>() {
			@Override
			public Object apply(List<Object> objects) {
				rootPermission.getChildren().clear();
				@SuppressWarnings("unchecked")
				final List<String> parents = (List<String>) objects.get(0);
				@SuppressWarnings("unchecked")
				final Map<String, String> options = (Map<String, String>) objects.get(1);

				// Metadata
				// Groups
				for (String group : parents) {
					rootPermission.getChildren().put("groups." + group, true);
					rootPermission.getChildren().put("group." + group, true);
				}

				// Options
				for (Map.Entry<String, String> option : options.entrySet()) {
					rootPermission.getChildren().put("options." + option.getKey() + "." + option.getValue(), true);
				}

				// Prefix and Suffix
				final String prefix = options.get("prefix"),
						suffix = options.get("suffix");
				rootPermission.getChildren().put("prefix." + (prefix == null ? "" : prefix), true);
				rootPermission.getChildren().put("suffix." + (suffix == null ? "" : suffix), true);
				return null;
			}
		}, PermissionsEx.mainThreadExecutor());
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

	private void updateSelective(PermissionEntityEvent event, Player p) {
			switch (event.getAction()) {
				case SAVED:
					break;

				case PERMISSIONS_CHANGED:
				case TIMEDPERMISSION_EXPIRED:
					updatePlayerPermission(getCreateWrapper(p, ""), p, p.getWorld().getName());
					p.recalculatePermissions();
					break;

				case OPTIONS_CHANGED:
				case INFO_CHANGED:
					updatePlayerMetadata(getCreateWrapper(p, ".options"), p, p.getWorld().getName());
					p.recalculatePermissions();
					break;

				default:
					updateAttachment(p);
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
