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

import java.lang.reflect.Field;
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
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.BukkitPermissions;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class PermissiblePEX extends PermissibleBase {

	protected Player player = null;
	protected boolean strictMode = false;
	protected boolean injectMetadata = true;
	protected BukkitPermissions bridge;
	protected Map<String, Boolean> cache = new HashMap<String, Boolean>();

	protected PermissiblePEX(Player player, BukkitPermissions bridge) {
		super(player);

		this.bridge = bridge;

		this.strictMode = bridge.isStrictMode();
		this.player = player;
	}

	public static void inject(Player player, BukkitPermissions bridge) {
		if (player.hasPermission("permissionsex.handler.injected")) { // already injected
			return;
		}

		try {
			Permissible permissible = new PermissiblePEX(player, bridge);

			if (player.getClass().getName().contains("Spout")) { // we have spout installed
				injectSpout(player, permissible);
			} else {
				injectCraftBukkit(player, permissible);
			}

			permissible.recalculatePermissions();

			// Logger.getLogger("Minecraft").info("[PermissionsEx] Permissions handler for " + player.getName() + " successfuly injected");
		} catch (Throwable e) {
			Logger.getLogger("Minecraft").warning("[PermissionsEx] Failed to inject own Permissible");
			e.printStackTrace();
		}
	}

	protected static void injectCraftBukkit(Player player, Permissible permissible) throws Throwable {
		Class humanEntity = Class.forName("org.bukkit.craftbukkit.entity.CraftHumanEntity");

		Field permField = humanEntity.getDeclaredField("perm");
		// Make it public for reflection
		permField.setAccessible(true);

		PermissibleBase oldBase = (PermissibleBase) permField.get(player);

		// Copy permissions and attachments from old Permissible

		// Attachments
		Field attachmentField = PermissibleBase.class.getDeclaredField("attachments");
		attachmentField.setAccessible(true);
		attachmentField.set(permissible, attachmentField.get(oldBase));

		// Permissions
		Field permissionsField = PermissibleBase.class.getDeclaredField("permissions");
		permissionsField.setAccessible(true);
		permissionsField.set(permissible, permissionsField.get(oldBase));

		// Inject permissible
		permField.set(player, permissible);
	}

	protected static void injectSpout(Player player, Permissible permissible) throws Throwable {
		Class humanEntity = Class.forName("org.getspout.spout.player.SpoutCraftPlayer");
		Field permField = humanEntity.getDeclaredField("perm");
		permField.setAccessible(true);

		permField.set(player, permissible);
	}

	@Override
	public boolean hasPermission(String permission) {
		String worldName = player.getWorld().getName();
		String cid = worldName + ":" + permission;

		if (!this.cache.containsKey(cid)) {
			this.cache.put(cid, this.performCheck(permission, worldName));
		}

		return this.cache.get(cid);
	}

	public boolean performCheck(String permission, String worldName) {
		if (permission.equals("permissionsex.handler.injected")) {
			return PermissionsEx.isAvailable();
		}

		try {
			PermissionUser user = PermissionsEx.getUser(this.player);
			if (user == null) {
				return super.hasPermission(permission);
			}

			// Check using PEX
			String expression = user.getMatchingExpression(permission.toLowerCase(), worldName);

			if (expression != null || this.strictMode) {
				if (user.isDebug()) {
					Logger.getLogger("Minecraft").info("User " + user.getName() + " checked for \"" + permission + "\", " + (expression == null ? "no permission found" : "\"" + expression + "\" found"));
				}

				return user.explainExpression(expression);
			}

			if (this.bridge.isEnableParentNodes()) {
				// check using parent nodes
				Map<String, Boolean> parentNodes = this.bridge.getChildPermissions().get(permission.toLowerCase());

				if (parentNodes != null) {
					for (String parentPermission : parentNodes.keySet()) {
						if (this.hasPermission(parentPermission)) {
							if (user.isDebug()) {
								Logger.getLogger("Minecraft").info("User " + user.getName() + " checked for \"" + permission + "\", " + (expression == null ? "no permission found" : " found from \"" + parentPermission + "\""));
							}

							return parentNodes.get(parentPermission).booleanValue();
						}
					}
				}
			}

			// Pass check to superperms
			if (super.hasPermission(permission)) {
				if (user.isDebug()) {
					Logger.getLogger("Minecraft").info("User " + user.getName() + " checked for \"" + permission + "\", found in superperms");
				}

				return true;
			}

			// No permission found
			if (user.isDebug()) {
				Logger.getLogger("Minecraft").info("User " + user.getName() + " checked for \"" + permission + "\", no permission found");
			}
		} catch (Throwable e) {
			// This should stay so if something will gone wrong user have chance to understand whats wrong actually
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public void recalculatePermissions() {
		super.recalculatePermissions();

		if (this.cache != null) {
			this.cache.clear();
		}
	}

	@Override
	public boolean hasPermission(Permission perm) {
		return this.hasPermission(perm.getName().toLowerCase());
	}

	@Override
	public boolean isPermissionSet(String permission) {
		PermissionUser user = PermissionsEx.getUser(this.player);

		if (user != null && user.getMatchingExpression(permission, this.player.getWorld().getName()) != null) {
			return true;
		}

		return super.isPermissionSet(permission);
	}

	@Override
	public boolean isPermissionSet(Permission perm) {
		return this.isPermissionSet(perm.getName().toLowerCase());
	}

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		if (!this.injectMetadata) {
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
}
