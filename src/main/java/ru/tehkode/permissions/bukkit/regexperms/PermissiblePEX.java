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

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import ru.tehkode.permissions.PermissionCheckResult;
import ru.tehkode.permissions.PermissionMatcher;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.ErrorReport;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.utils.FieldReplacer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implements regex-based permission matching for superperms.
 * If a permission match is found using existing superperms methods, it takes priority.
 * However, if a match is not found we use a {@link PermissionMatcher} set in the plugin object to check for a permissions match, caching values.
 *
 * Class should be thread-safe
 */
public class PermissiblePEX extends PermissibleBase {
	private static final FieldReplacer<PermissibleBase, Map> PERMISSIONS_FIELD = new FieldReplacer<>(PermissibleBase.class, "permissions", Map.class);
	private static final FieldReplacer<PermissibleBase, List> ATTACHMENTS_FIELD = new FieldReplacer<>(PermissibleBase.class, "attachments", List.class);
	private static final Method CALC_CHILD_PERMS_METH;

	static {
		try {
			CALC_CHILD_PERMS_METH = PermissibleBase.class.getDeclaredMethod("calculateChildPermissions", Map.class, boolean.class, PermissionAttachment.class);
		} catch (NoSuchMethodException e) {
			throw new ExceptionInInitializerError(e);
		}
		CALC_CHILD_PERMS_METH.setAccessible(true);
	}

	private final Map<String, PermissionAttachmentInfo> permissions;
	private final List<PermissionAttachment> attachments;
	private static final AtomicBoolean LAST_CALL_ERRORED = new AtomicBoolean(false);

	protected final Player player;
	protected final PermissionsEx plugin;
	private Permissible previousPermissible = null;
	protected final Map<String, PermissionCheckResult> cache = new ConcurrentHashMap<>();
	private final Object permissionsLock = new Object();

	@SuppressWarnings("unchecked")
	public PermissiblePEX(Player player, PermissionsEx plugin) {
		super(player);
		this.player = player;
		this.plugin = plugin;
		permissions = new LinkedHashMap<String, PermissionAttachmentInfo>() {
			/**
			 * Customized put() useable ONLY for this permissible. It's pretty weird otherwise.
			 * It'd be better as a putIfAbsent, but it needs to be called from the superclass so it's not.
			 *
			 * @param k The key
			 * @param v The value
			 * @return The previous/existing permission at this location
			 */
			@Override
			public PermissionAttachmentInfo put(String k, PermissionAttachmentInfo v) {
				PermissionAttachmentInfo existing = this.get(k);
				if (existing != null) {
					return existing;
				}
				return super.put(k, v);
			}
		};
		PERMISSIONS_FIELD.set(this, permissions);
		this.attachments = ATTACHMENTS_FIELD.get(this);
		recalculatePermissions();
	}

	public Permissible getPreviousPermissible() {
		return previousPermissible;
	}

	public void setPreviousPermissible(Permissible previousPermissible) {
		this.previousPermissible = previousPermissible;
	}

	public boolean isDebug() {
		boolean debug = plugin.isDebug();
		/*try {
			PermissionUser user = plugin.getPermissionsManager().getUser(this.player);
			if (user != null) {
				debug |= user.isDebug();
			}
		} catch (Throwable ignore) {
		}*/
		return debug;
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
				if (super.isPermissionSet(permission)) {
					final boolean ret = super.hasPermission(permission);
					if (isDebug()) {
						plugin.getLogger().info("User " + player.getName() + " checked for permission '" + permission + "', superperms-matched a value of " + ret);
					}
					return ret;
				} else {
					Permission perm = player.getServer().getPluginManager().getPermission(permission);
					return perm == null ? Permission.DEFAULT_PERMISSION.getValue(player.isOp()) : perm.getDefault().getValue(player.isOp());
				}
		}
	}

	@Override
	public boolean hasPermission(Permission permission) {
		PermissionCheckResult res = permissionValue(permission.getName());

		switch (res) {
			case TRUE:
			case FALSE:
				return res.toBoolean();
			case UNDEFINED:
			default:
				if (super.isPermissionSet(permission.getName())) {
					final boolean ret = super.hasPermission(permission);
					if (isDebug()) {
						plugin.getLogger().info("User " + player.getName() + " checked for permission '" + permission.getName() + "', superperms-matched a value of " + ret);
					}
					return ret;
				} else {
					return permission.getDefault().getValue(player.isOp());
				}
		}
	}

	@Override
	public void recalculatePermissions() {
		if (cache != null && permissions != null && attachments != null) {
			synchronized (permissionsLock) {
				clearPermissions();
				cache.clear();
				for (ListIterator<PermissionAttachment> it = this.attachments.listIterator(this.attachments.size()); it.hasPrevious(); ) {
					PermissionAttachment attach = it.previous();
					calculateChildPerms(attach.getPermissions(), false, attach);
				}

				for (Permission p : player.getServer().getPluginManager().getDefaultPermissions(isOp())) {
					this.permissions.put(p.getName(), new PermissionAttachmentInfo(player, p.getName(), null, true));
					calculateChildPerms(p.getChildren(), false, null);
				}
			}
		}
	}

	protected void calculateChildPerms(Map<String, Boolean> children, boolean invert, PermissionAttachment attachment) {
		try {
			CALC_CHILD_PERMS_METH.invoke(this, children, invert, attachment);
		} catch (IllegalAccessException e) {
			// Shouldn't happen, we setAccessible earlier
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isPermissionSet(String permission) {
		return super.isPermissionSet(permission) || permissionValue(permission) != PermissionCheckResult.UNDEFINED;
	}


	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		synchronized (permissionsLock) {
			return new LinkedHashSet<>(permissions.values());
		}
	}

	private PermissionCheckResult checkSingle(String expression, String permission, boolean value) {
		if (plugin.getPermissionsManager().getPermissionMatcher().isMatches(expression, permission)) {
			PermissionCheckResult res = PermissionCheckResult.fromBoolean(value);
			if (isDebug()) {
				plugin.getLogger().info("User " + player.getName() +
						" checked for permission '" + permission + "', regex-matched a value of "
						+ res + " from " + expression + " (CACHE MISS)");
			}
			return res;
		}
		return PermissionCheckResult.UNDEFINED;
	}

	protected PermissionCheckResult permissionValue(String permission) {
		try {
			Validate.notNull(permission, "Permissions being checked must not be null!");
			permission = permission.toLowerCase();
			PermissionCheckResult res = cache.get(permission);
			if (res != null) {
				if (isDebug()) {
					plugin.getLogger().info("User " + player.getName() + " checked for permission '" + permission + "', regex-matched a value of " + res + " from cache.");
				}
				return res;
			}

			res = PermissionCheckResult.UNDEFINED;

			synchronized (permissionsLock) {
				for (PermissionAttachmentInfo pai : permissions.values()) {
					if ((res = checkSingle(pai.getPermission(), permission, pai.getValue())) != PermissionCheckResult.UNDEFINED) {
						break;
					}
				}
			}
			if (res == PermissionCheckResult.UNDEFINED) {
				for (Map.Entry<String, Boolean> ent : plugin.getRegexPerms().getPermissionList().getParents(permission)) {
					if ((res = permissionValue(ent.getKey())) != PermissionCheckResult.UNDEFINED) {
						res = PermissionCheckResult.fromBoolean(!(res.toBoolean() ^ ent.getValue()));
						if (isDebug()) {
							plugin.getLogger().info("User " + player.getName() + " checked for permission '" + permission + "', match from parent '" + ent.getKey() + "' (CACHE MISS)");
						}
						break;
					}
				}
			}
			cache.put(permission, res);
			if (res == PermissionCheckResult.UNDEFINED && isDebug()) {
				plugin.getLogger().info("User " + player.getName() + " checked for permission '" + permission + "', no match found (CACHE MISS)");
			}
			LAST_CALL_ERRORED.set(false);
			return res;
		} catch (Throwable t) {
			if (LAST_CALL_ERRORED.compareAndSet(false, true)) {
				ErrorReport.handleError("Permissible permissionValue for " + player.getName(), t);
			}
			return PermissionCheckResult.UNDEFINED;
		}
	}
}
