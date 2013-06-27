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
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import ru.tehkode.permissions.PermissionCheckResult;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.BukkitPermissions;
import ru.tehkode.permissions.bukkit.ErrorReport;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.bukkit.superperms.PermissibleInjector.ClassNameRegexPermissibleInjector;
import ru.tehkode.permissions.bukkit.superperms.PermissibleInjector.ServerNamePermissibleInjector;
import ru.tehkode.permissions.events.PermissionSystemEvent;
import ru.tehkode.permissions.exceptions.PermissionsNotAvailable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static ru.tehkode.permissions.bukkit.CraftBukkitInterface.getCBClassName;

public class PermissiblePEX extends PermissibleBase {
	private static final AtomicBoolean LAST_CALL_ERRORED = new AtomicBoolean(false);
	private static final Logger LOGGER = Logger.getLogger(PermissiblePEX.class.getCanonicalName());
	protected static PermissibleInjector[] injectors = new PermissibleInjector[]{
			new ServerNamePermissibleInjector("net.glowstone.entity.GlowHumanEntity", "permissions", true, "Glowstone"),
			new ServerNamePermissibleInjector("org.getspout.server.entity.SpoutHumanEntity", "permissions", true, "Spout"),
			new ClassNameRegexPermissibleInjector("org.getspout.spout.player.SpoutCraftPlayer", "perm", false, "Spout"),
			new ServerNamePermissibleInjector(getCBClassName("entity.CraftHumanEntity"), "perm", true, "CraftBukkit"),
			new ServerNamePermissibleInjector(getCBClassName("entity.CraftHumanEntity"), "perm", true, "CraftBukkit++"),
			new ServerNamePermissibleInjector(getCBClassName("entity.CraftHumanEntity"), "perm", true, "SportBukkit"),
	};
	protected Player player = null;
	protected boolean strictMode = false;
	protected boolean injectMetadata = false;
	protected BukkitPermissions bridge;
	protected Map<String, PermissionCheckResult> cache = new ConcurrentHashMap<String, PermissionCheckResult>();

	protected PermissiblePEX(Player player, BukkitPermissions bridge) {
		super(player);

		this.bridge = bridge;

		this.strictMode = bridge.isStrictMode();
		this.player = player;
	}

	public static void inject(Player player, BukkitPermissions bridge) {
		if (player.isPermissionSet("permissionsex.handler.injected")) { // already injected
			return;
		}
		try {
			Permissible permissible = new PermissiblePEX(player, bridge);

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

			if (PermissionsEx.getPermissionManager().isDebug()) {
				LOGGER.info("[PermissionsEx] Permissions handler for " + player.getName() + " successfuly injected");
			}
		} catch (Throwable e) {
			ErrorReport.handleError("Unable to inject permissions handler for " + player.getName(), e);
		}
	}

	public static void reinjectAll() {
		LOGGER.warning("[PermissionsEx] Reinjecting all permissibles");
		// Tell PEX to reinject all permissibles
		Bukkit.getPluginManager().callEvent(new PermissionSystemEvent(PermissionSystemEvent.Action.REINJECT_PERMISSIBLES));
	}

	@Override
	public boolean hasPermission(String permission) {
		if (permission.equals("permissionsex.handler.injected")) {
			return isAvailable() && PermissionsEx.isAvailable();
		}

		return this.checkPermission(permission).toBoolean();
	}

	public PermissionCheckResult checkPermission(String permission) {
		String worldName = player.getWorld().getName();
		String cid = worldName + ":" + permission;

		try {
			if (!this.cache.containsKey(cid)) {
				PermissionCheckResult result = this.performCheck(permission, worldName);

				if (result == PermissionCheckResult.UNDEFINED) { // return default permission
					result = PermissionCheckResult.fromBoolean(super.hasPermission(permission));
				}

				this.cache.put(cid, result);
			}

			PermissionCheckResult result = this.cache.get(cid);
			if (PermissionsEx.getUser(player).isDebug()) {
				LOGGER.info("User " + player.getName() + " checked for \"" + permission + "\", cache value " + result + " found.");
			}
			LAST_CALL_ERRORED.set(false);
			return result;
		} catch (Throwable t) {
			if (LAST_CALL_ERRORED.compareAndSet(false, true)) {
				ErrorReport.handleError("While checking permissions -- Issue getting PEX instance?", t);
			}
			return PermissionCheckResult.UNDEFINED;
		}
	}

	public PermissionCheckResult performCheck(String permission, String worldName) {
		try {
			PermissionUser user = PermissionsEx.getUser(this.player);

			// Check using PEX
			String expression = user.getMatchingExpression(permission.toLowerCase(), worldName);

			if (expression != null || this.strictMode) {
				if (user.isDebug()) {
					LOGGER.info("User " + user.getName() + " checked for \"" + permission + "\", " + (expression == null ? "no permission found" : "\"" + expression + "\" found"));
				}

				LAST_CALL_ERRORED.set(false);
				return PermissionCheckResult.fromBoolean(user.explainExpression(expression));
			}

			// Pass check to superperms
			if (super.isPermissionSet(permission)) { // permission already set by side plugin
				PermissionCheckResult result = PermissionCheckResult.fromBoolean(super.hasPermission(permission));

				if (user.isDebug()) {
					LOGGER.info("User " + user.getName() + " checked for \"" + permission + "\" = " + result + ", found in superperms");
				}

				LAST_CALL_ERRORED.set(false);
				return result;
			}

			// check using parent nodes
			Map<String, Boolean> parentNodes = this.bridge.getChildPermissions().get(permission.toLowerCase());

			if (parentNodes != null) {
				for (String parentPermission : parentNodes.keySet()) {
					PermissionCheckResult result = this.checkPermission(parentPermission);
					if (result == PermissionCheckResult.UNDEFINED) {
						continue;
					}

					PermissionCheckResult anwser = PermissionCheckResult.fromBoolean(parentNodes.get(parentPermission) ^ !result.toBoolean());

					if (user.isDebug()) {
						LOGGER.info("User " + user.getName() + " checked for \"" + permission + "\" = " + anwser + ",  found from \"" + parentPermission + "\"");
					}

					LAST_CALL_ERRORED.set(false);
					return anwser;
				}
			}


			// No permission found
			if (user.isDebug()) {
				LOGGER.info("User " + user.getName() + " checked for \"" + permission + "\", no permission found");
			}
			LAST_CALL_ERRORED.set(false);
		} catch (PermissionsNotAvailable e) {
			LOGGER.warning("[PermissionsEx] Can't obtain PermissionsEx instance");
			reinjectAll();
		} catch (Throwable e) {
			if (!LAST_CALL_ERRORED.compareAndSet(false, true)) {
				ErrorReport.handleError("While performing PEX check", e);
			}
			return PermissionCheckResult.UNDEFINED;
		}
		return PermissionCheckResult.UNDEFINED;
	}

	@Override
	public void recalculatePermissions() {
		super.recalculatePermissions();

		if (this.cache != null) {
			this.cache.clear();
		}

		if (bridge != null) {
			bridge.checkAllParentPermissions(false);
		}
	}

	@Override
	public boolean hasPermission(Permission perm) {
		return this.hasPermission(perm.getName().toLowerCase());
	}

	@Override
	public boolean isPermissionSet(String permission) {
		if (permission.equals("permissionsex.handler.injected")) {
			return PermissionsEx.isAvailable();
		}

		try {
			PermissionUser user = PermissionsEx.getUser(this.player);

			if (user != null && user.getMatchingExpression(permission, this.player.getWorld().getName()) != null) {
				return true;
			}
			LAST_CALL_ERRORED.set(false);
		} catch (PermissionsNotAvailable e) {
			LOGGER.warning("[PermissionsEx] Can't obtain PermissionsEx instance");
			reinjectAll();
		} catch (Throwable t) {
			if (LAST_CALL_ERRORED.compareAndSet(false, true)) {
				ErrorReport.handleError("Error checking isPermissionSet for " + player.getName(), t);
			}
		}
		return super.isPermissionSet(permission);
	}

	@Override
	public boolean isPermissionSet(Permission perm) {
		return this.isPermissionSet(perm.getName().toLowerCase());
	}

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		if (!this.injectMetadata || !PermissionsEx.isAvailable()) {
			return super.getEffectivePermissions();
		}

		Set<PermissionAttachmentInfo> infoSet = super.getEffectivePermissions();

		// Injecting metadata into output
		try {
			PermissionUser user = PermissionsEx.getUser(this.player);
			String world = this.player.getWorld().getName();

			PermissionAttachment attachment = new PermissionAttachment(PermissionsEx.getPlugin(), this.player);

			// Groups
			for (PermissionGroup group : user.getGroups(world)) {
				infoSet.add(new PermissionAttachmentInfo(this.player, "groups." + group.getName(), attachment, true));
				infoSet.add(new PermissionAttachmentInfo(this.player, "group." + group.getName(), attachment, true));
			}

			// Options
			for (Map.Entry<String, String> option : user.getOptions(world).entrySet()) {
				infoSet.add(new PermissionAttachmentInfo(this.player, "options." + option.getKey() + "." + option.getValue(), attachment, true));
			}

			// Prefix and Suffix
			infoSet.add(new PermissionAttachmentInfo(this.player, "prefix." + user.getPrefix(world), attachment, true));
			infoSet.add(new PermissionAttachmentInfo(this.player, "suffix." + user.getSuffix(world), attachment, true));

			// Permissions
			for (Permission perm : Bukkit.getServer().getPluginManager().getPermissions()) {
				if (super.isPermissionSet(perm)) {
					continue;
				}

				String expression = user.getMatchingExpression(perm.getName(), world);
				if (expression != null) {
					infoSet.add(new PermissionAttachmentInfo(this.player, perm.getName(), attachment, user.explainExpression(expression)));
				}
			}

		} catch (Throwable e) {
			ErrorReport.handleError("Error building metadata for " + player.getName(), e);
		}

		return infoSet;
	}

	private boolean isAvailable() {
		Plugin plugin = Bukkit.getPluginManager().getPlugin("PermissionsEx");

		return plugin != null && plugin instanceof PermissionsEx;
	}

}
