/*
 * PermissionsEx - Permissions plugin for Bukkit
 * Copyright (C) 2011 t3hk0d3 http://www.tehkode.ru
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package ru.tehkode.permissions.bukkit.superperms;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import ru.tehkode.permissions.PermissionCheckResult;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.bukkit.superperms.PermissibleInjector.ClassNameRegexPermissibleInjector;
import ru.tehkode.permissions.bukkit.superperms.PermissibleInjector.ServerNamePermissibleInjector;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static ru.tehkode.permissions.bukkit.CraftBukkitInterface.getCBClassName;

public class PermissiblePEX extends PermissibleBase {
	private static final AtomicBoolean LAST_CALL_ERRORED = new AtomicBoolean(false);
	private static final Logger LOGGER = Logger.getLogger(PermissiblePEX.class.getCanonicalName());
	protected static final PermissibleInjector[] injectors = new PermissibleInjector[] {
			new ServerNamePermissibleInjector("net.glowstone.entity.GlowHumanEntity", "permissions", true, "Glowstone"),
			new ServerNamePermissibleInjector("org.getspout.server.entity.SpoutHumanEntity", "permissions", true, "Spout"),
			new ClassNameRegexPermissibleInjector("org.getspout.spout.player.SpoutCraftPlayer", "perm", false, "Spout"),
			new ServerNamePermissibleInjector(getCBClassName("entity.CraftHumanEntity"), "perm", true, "CraftBukkit"),
			new ServerNamePermissibleInjector(getCBClassName("entity.CraftHumanEntity"), "perm", true, "CraftBukkit++")
	};
	protected final Player player;
	protected final PermissionsEx plugin;
	protected final Map<String, PermissionCheckResult> cache = new ConcurrentHashMap<String, PermissionCheckResult>();

	protected PermissiblePEX(Player player, PermissionsEx plugin) {
		super(player);
		this.player = player;
		this.plugin = plugin;
	}

	public static void inject(Player player, PermissionsEx plugin) {
		if (player.isPermissionSet("permissionsex.handler.injected") || player.hasPermission("permissionsex.disable")) { // already injected or this user shouldn't get permissionsex matching
			return;
		}

		try {
			Permissible permissible = new PermissiblePEX(player, plugin);

			boolean success = false;
			for (PermissibleInjector injector : injectors) {
				if (injector.isApplicable(player)) {
					if (injector.inject(player, permissible)) {
						success = true;
						break;
					}
				}
			}

			if (!success) {
				LOGGER.warning("[PermissionsEx] No Permissible injector found for your server implementation!");
			}

			permissible.recalculatePermissions();

			/*if (PermissionsEx.getPermissionManager().isDebug()) {
				LOGGER.info("[PermissionsEx] Permissions handler for " + player.getName() + " successfuly injected");
			}*/
		} catch (Throwable e) {
			e.printStackTrace();
			//ErrorReport.handleError("Unable to inject permissions handler for " + player.getName(), e);
		}
	}

	@Override
	public boolean hasPermission(String permission) {
		PermissionCheckResult res = permissionValue(permission);
		switch (res) {
			case TRUE:
			case FALSE:
				return res.toBoolean();
			case UNDEFINED:
			default:
				return super.hasPermission(permission);
		}
	}

	@Override
	public void recalculatePermissions() {
		super.recalculatePermissions();

		// clear cache
	}

	@Override
	public boolean isPermissionSet(String permission) {
		if (permission.equals("permissionsex.handler.injected")) {
			return true; // TODO: Do we even want to do it this way anymore?
		}

		// Check if any of the stored permissions are in cache
		return super.isPermissionSet(permission);
	}

	protected PermissionCheckResult permissionValue(String permission) {
		permission = permission.toLowerCase();
		return PermissionCheckResult.UNDEFINED; // TODO: Check all superperms-stored perms for the correct values
	}

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		return super.getEffectivePermissions(); // TODO: Does this need to be overridden/can this be efficiently done?
	}

	private boolean isAvailable() {
		Plugin plugin = Bukkit.getPluginManager().getPlugin("PermissionsEx");

		return plugin != null && plugin instanceof PermissionsEx;
	}

}
