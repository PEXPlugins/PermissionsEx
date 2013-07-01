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

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachmentInfo;
import ru.tehkode.permissions.PermissionCheckResult;
import ru.tehkode.permissions.PermissionMatcher;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implements regex-based permission matching for superperms.
 * If a permission match is found using existing superperms methods, it takes priority.
 * However, if a match is not found we use a {@link PermissionMatcher} set in the plugin object to check for a permissions match, caching values.
 */
public class PermissiblePEX extends PermissibleBase {
	private static final AtomicBoolean LAST_CALL_ERRORED = new AtomicBoolean(false);

	protected final Player player;
	protected final PermissionsEx plugin;
	private Permissible previousPermissible = null;
	protected final Map<String, PermissionCheckResult> cache = new ConcurrentHashMap<String, PermissionCheckResult>();

	public PermissiblePEX(Player player, PermissionsEx plugin) {
		super(player);
		this.player = player;
		this.plugin = plugin;
	}

	public Permissible getPreviousPermissible() {
		return previousPermissible;
	}

	public void setPreviousPermissible(Permissible previousPermissible) {
		this.previousPermissible = previousPermissible;
	}

	@Override
	public boolean hasPermission(String permission) {
		if (super.isPermissionSet(permission)) {
			final boolean ret = super.hasPermission(permission);
			if (plugin.debugMode()) {
				plugin.getLogger().info("User " + player.getName() + " checked for permission '" + permission + "', superperms-matched a value of " + ret);
			}
			return ret;
		}

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
	public boolean hasPermission(Permission permission) {
		if (super.isPermissionSet(permission.getName())) {
			final boolean ret = super.hasPermission(permission);
			if (plugin.debugMode()) {
				plugin.getLogger().info("User " + player.getName() + " checked for permission '" + permission.getName() + "', superperms-matched a value of " + ret);
			}
			return ret;
		}

		PermissionCheckResult res = permissionValue(permission.getName());

		switch (res) {
			case TRUE:
			case FALSE:
				return res.toBoolean();
			case UNDEFINED:
			default:
				return permission.getDefault().getValue(player.isOp());
		}
	}

	@Override
	public void recalculatePermissions() {
		super.recalculatePermissions();
		if (cache != null) { // recalculatePermissions() is called from superclass constructor
			cache.clear();
		}
	}

	@Override
	public boolean isPermissionSet(String permission) {
		return super.isPermissionSet(permission) || permissionValue(permission) != PermissionCheckResult.UNDEFINED;
	}

	protected PermissionCheckResult permissionValue(String permission) {
		try {
			permission = permission.toLowerCase();
			if (cache.containsKey(permission)) {
				PermissionCheckResult res = cache.get(permission);
				if (plugin.debugMode()) {
					plugin.getLogger().info("User " + player.getName() + " checked for permission '" + permission + "', regex-matched a value of " + res + " from cache.");
				}
				return res;
			}
			PermissionCheckResult res = PermissionCheckResult.UNDEFINED;
			for (PermissionAttachmentInfo pai : getEffectivePermissions()) {
				if (plugin.getMatcher().matches(pai.getPermission(), permission)) {
					res = PermissionCheckResult.fromBoolean(pai.getValue());
					if (plugin.debugMode()) {
						plugin.getLogger().info("User " + player.getName() +
								" checked for permission '" + permission + "', regex-matched a value of "
								+ res + " from " + pai.getPermission() + " (CACHE MISS)");
					}
					break;
				}
			}
			cache.put(permission, res);
			LAST_CALL_ERRORED.set(false);
			return res;
		} catch (Throwable t) {
			if (LAST_CALL_ERRORED.compareAndSet(false, true)) {
				t.printStackTrace();
			}
			return PermissionCheckResult.UNDEFINED;
		}
	}
}
