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

import org.bukkit.plugin.Plugin;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import ru.tehkode.permissions.PermissionCheckResult;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.BukkitPermissions;
import ru.tehkode.permissions.bukkit.PermissionsEx;
import static ru.tehkode.permissions.bukkit.superperms.PermissibleInjector.*;
import ru.tehkode.permissions.events.PermissionSystemEvent;
import ru.tehkode.permissions.exceptions.PermissionsNotAvailable;

public class PermissiblePEX extends PermissibleBase {

	protected static PermissibleInjector[] injectors = new PermissibleInjector[]{
		new ServerNamePermissibleInjector("net.glowstone.entity.GlowHumanEntity", "permissions", true, "Glowstone"),
		new ServerNamePermissibleInjector("org.getspout.server.entity.SpoutHumanEntity", "permissions", true, "Spout"),
		new ClassNameRegexPermissibleInjector("org.getspout.spout.player.SpoutCraftPlayer", "perm", false, "Spout"),
		new ServerNamePermissibleInjector("org.bukkit.craftbukkit.entity.CraftHumanEntity", "perm", true, "CraftBukkit"),
		new ServerNamePermissibleInjector("org.bukkit.craftbukkit.entity.CraftHumanEntity", "perm", true, "CraftBukkit++")
	};
	protected Player player = null;
	protected boolean strictMode = false;
	protected boolean injectMetadata = false;
	protected BukkitPermissions bridge;
	protected Map<String, PermissionCheckResult> cache = new HashMap<String, PermissionCheckResult>();

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
				Logger.getLogger("Minecraft").warning("[PermissionsEx] No Permissible injector found for your server implementation!");
			}

			permissible.recalculatePermissions();

			if (PermissionsEx.getPermissionManager().isDebug()) {
				Logger.getLogger("Minecraft").info("[PermissionsEx] Permissions handler for " + player.getName() + " successfuly injected");
			}
		} catch (Throwable e) {
			Logger.getLogger("Minecraft").warning("[PermissionsEx] Failed to inject own Permissible");
			e.printStackTrace();
		}
	}	

	public static void reinjectAll() {
		Logger.getLogger("Minecraft").warning("[PermissionsEx] Reinjecting all permissibles");
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

		if (!this.cache.containsKey(cid)) {
			PermissionCheckResult result = this.performCheck(permission, worldName);

			if (result == PermissionCheckResult.UNDEFINED) { // return default permission
				result = PermissionCheckResult.fromBoolean(super.hasPermission(permission));
			}

			this.cache.put(cid, result);
		}

		return this.cache.get(cid);
	}

	public PermissionCheckResult performCheck(String permission, String worldName) {
		try {
			PermissionUser user = PermissionsEx.getUser(this.player);

			// Check using PEX
			String expression = user.getMatchingExpression(permission.toLowerCase(), worldName);

			if (expression != null || this.strictMode) {
				if (user.isDebug()) {
					Logger.getLogger("Minecraft").info("User " + user.getName() + " checked for \"" + permission + "\", " + (expression == null ? "no permission found" : "\"" + expression + "\" found"));
				}

				return PermissionCheckResult.fromBoolean(user.explainExpression(expression));
			}

			// Pass check to superperms
			if (super.isPermissionSet(permission)) { // permission set by side plugin
				PermissionCheckResult result = PermissionCheckResult.fromBoolean(super.hasPermission(permission));

				if (user.isDebug()) {
					Logger.getLogger("Minecraft").info("User " + user.getName() + " checked for \"" + permission + "\" = " + result + ", found in superperms");
				}

				return result;
			}

			if (this.bridge.isEnableParentNodes()) {
				// check using parent nodes
				Map<String, Boolean> parentNodes = this.bridge.getChildPermissions().get(permission.toLowerCase());

				if (parentNodes != null) {
					for (String parentPermission : parentNodes.keySet()) {
						PermissionCheckResult result = this.checkPermission(parentPermission);
						if (result == PermissionCheckResult.UNDEFINED) {
							continue;
						}

						PermissionCheckResult anwser = PermissionCheckResult.fromBoolean(parentNodes.get(parentPermission).booleanValue() ^ !result.toBoolean());

						if (user.isDebug()) {
							Logger.getLogger("Minecraft").info("User " + user.getName() + " checked for \"" + permission + "\" = " + anwser + ",  found from \"" + parentPermission + "\"");
						}

						return anwser;
					}
				}
			}

			// No permission found
			if (user.isDebug()) {
				Logger.getLogger("Minecraft").info("User " + user.getName() + " checked for \"" + permission + "\", no permission found");
			}
		} catch (PermissionsNotAvailable e) {
			Logger.getLogger("Minecraft").warning("[PermissionsEx] Can't obtain PermissionsEx instance");
			reinjectAll();
		} catch (Throwable e) {
			// This should stay so if something will gone wrong user have chance to understand whats wrong actually
			e.printStackTrace();
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
		} catch (PermissionsNotAvailable e) {
			Logger.getLogger("Minecraft").warning("[PermissionsEx] Can't obtain PermissionsEx instance");
			reinjectAll();
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
			// do nothing
		}

		return infoSet;
	}

	private boolean isAvailable() {
		Plugin plugin = Bukkit.getPluginManager().getPlugin("PermissionsEx");

		return plugin != null && plugin instanceof PermissionsEx;
	}
	
}
