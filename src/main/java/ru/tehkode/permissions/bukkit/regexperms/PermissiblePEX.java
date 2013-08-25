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
package ru.tehkode.permissions.bukkit.regexperms;

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachmentInfo;
import ru.tehkode.permissions.PermissionCheckResult;
import ru.tehkode.permissions.PermissionMatcher;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.utils.FieldReplacer;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implements regex-based permission matching for superperms.
 * If a permission match is found using existing superperms methods, it takes priority.
 * However, if a match is not found we use a {@link PermissionMatcher} set in the plugin object to check for a permissions match, caching values.
 * <p/>
 * Class should be thread-safe
 */
public class PermissiblePEX extends PermissibleBase {
	private static final FieldReplacer<PermissibleBase, Map> PERMISSIONS_FIELD = new FieldReplacer<PermissibleBase, Map>(PermissibleBase.class, "permissions", Map.class);
	private final Map<String, PermissionAttachmentInfo> permissions;
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private static final AtomicBoolean LAST_CALL_ERRORED = new AtomicBoolean(false);

	protected final Player player;
	protected final PermissionsEx plugin;
	private Permissible previousPermissible = null;
	protected final Map<String, PermissionCheckResult> cache = new ConcurrentHashMap<String, PermissionCheckResult>();

	@SuppressWarnings("unchecked")
	public PermissiblePEX(Player player, PermissionsEx plugin) {
		super(player);
		this.player = player;
		this.plugin = plugin;
		permissions = new LinkedHashMap<String, PermissionAttachmentInfo>() {
			/**
			 * Customized put() useable ONLY for this permissible. It's pretty weird otherwise.
			 * (It's more of a putIfAbsent, but it needs to override PermissibleBase behavior, so it's put())
			 * Basically:
			 * If the permission is already present in the map, only allow it to be reset if the original has no attachment.
			 *
			 * @param k The key
			 * @param v The value
			 * @return The previous/existing permission at this location
			 */
			@Override
			public PermissionAttachmentInfo put(String k, PermissionAttachmentInfo v) {
				PermissionAttachmentInfo existing = this.get(k);
				if (existing != null) {
					if (existing.getAttachment() == null) {
						this.remove(k);
					} else {
						return existing;
					}
				}
				super.put(k, v);
				return existing;
			}
		};
		PERMISSIONS_FIELD.set(this, permissions);
	}

	public Permissible getPreviousPermissible() {
		return previousPermissible;
	}

	public void setPreviousPermissible(Permissible previousPermissible) {
		this.previousPermissible = previousPermissible;
	}

	@Override
	public boolean hasPermission(String permission) {
		lock.readLock().lock();
		try {
			PermissionCheckResult res = permissionValue(permission);

			switch (res) {
				case TRUE:
				case FALSE:
					return res.toBoolean();
				case UNDEFINED:
				default:
					if (super.isPermissionSet(permission)) {
						final boolean ret = super.hasPermission(permission);
						if (plugin.isDebug()) {
							plugin.getLogger().info("User " + player.getName() + " checked for permission '" + permission + "', superperms-matched a value of " + ret);
						}
						return ret;
					} else {
						return false;
					}
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public boolean hasPermission(Permission permission) {
		lock.readLock().lock();
		try {
			PermissionCheckResult res = permissionValue(permission.getName());

			switch (res) {
				case TRUE:
				case FALSE:
					return res.toBoolean();
				case UNDEFINED:
				default:
					if (super.isPermissionSet(permission.getName())) {
						final boolean ret = super.hasPermission(permission);
						if (plugin.isDebug()) {
							plugin.getLogger().info("User " + player.getName() + " checked for permission '" + permission.getName() + "', superperms-matched a value of " + ret);
						}
						return ret;
					} else {
						return permission.getDefault().getValue(player.isOp());
					}
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void recalculatePermissions() {
		if (lock != null) { // recalculatePermissions() is called from superclass constructor
			lock.writeLock().lock();
			try {
				super.recalculatePermissions();
				cache.clear();
			} finally {
				lock.writeLock().unlock();
			}
		} else {
			super.recalculatePermissions();
		}
	}

	@Override
	public boolean isPermissionSet(String permission) {
		lock.readLock().lock();
		try {
			return super.isPermissionSet(permission) || permissionValue(permission) != PermissionCheckResult.UNDEFINED;
		} finally {
			lock.readLock().unlock();
		}
	}


	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		return new LinkedHashSet<PermissionAttachmentInfo>(permissions.values());
	}

	private PermissionCheckResult checkSingle(String expression, String permission, boolean value) {
		if (plugin.getPermissionsManager().getPermissionMatcher().isMatches(expression, permission)) {
			PermissionCheckResult res = PermissionCheckResult.fromBoolean(value);
			if (plugin.isDebug()) {
				plugin.getLogger().info("User " + player.getName() +
						" checked for permission '" + permission + "', regex-matched a value of "
						+ res + " from " + expression + " (CACHE MISS)");
			}
			return res;
		}
		return PermissionCheckResult.UNDEFINED;
	}

	protected PermissionCheckResult permissionValue(String permission) {
		lock.readLock().lock();
		try {
			permission = permission.toLowerCase();
			PermissionCheckResult res = cache.get(permission);
			if (res != null) {
				if (plugin.isDebug()) {
					plugin.getLogger().info("User " + player.getName() + " checked for permission '" + permission + "', regex-matched a value of " + res + " from cache.");
				}
				return res;
			}

			res = PermissionCheckResult.UNDEFINED;

			for (PermissionAttachmentInfo pai : permissions.values()) {
				if ((res = checkSingle(pai.getPermission(), permission, pai.getValue())) != PermissionCheckResult.UNDEFINED) {
					break;
				}
			}
			if (res == PermissionCheckResult.UNDEFINED) {
				for (Map.Entry<String, Boolean> ent : plugin.getRegexPerms().getPermissionList().getParents(permission)) {
						if ((res = permissionValue(ent.getKey())) != PermissionCheckResult.UNDEFINED) {
							res = PermissionCheckResult.fromBoolean(!(res.toBoolean() ^ ent.getValue()));
							plugin.getLogger().info("User " + player.getName() + " checked for permission '" + permission + "', match from parent '" + ent.getKey() + "' (CACHE MISS)");
							break;
						}
					}
			}
			cache.put(permission, res);
			if (res == PermissionCheckResult.UNDEFINED && plugin.isDebug()) {
				plugin.getLogger().info("User " + player.getName() + " checked for permission '" + permission + "', no match found (CACHE MISS)");
			}
			LAST_CALL_ERRORED.set(false);
			return res;
		} catch (Throwable t) {
			if (!plugin.isDebug() && LAST_CALL_ERRORED.compareAndSet(false, true)) {
				t.printStackTrace();
			}
			return PermissionCheckResult.UNDEFINED;
		} finally {
			lock.readLock().unlock();
		}
	}
}
