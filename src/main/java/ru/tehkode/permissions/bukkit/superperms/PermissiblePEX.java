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
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.ServerOperator;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

public class PermissiblePEX extends PermissibleBase {

    protected Player player = null;
    protected boolean strictMode = false;

    protected PermissiblePEX(ServerOperator opable, boolean disableUnmatched) {
        super(opable);

        this.strictMode = disableUnmatched;
        
        if (opable instanceof Player) {
            this.player = (Player) opable;
        }
    }

    public static void inject(Player player, boolean strictMode) {
        if (player.hasPermission("permissionsex.handler.injected")) { // already injected
            Logger.getLogger("Minecraft").warning("[PermissionsEx] Already injected");
            return;
        }

        try {
            Class humanEntity = Class.forName("org.bukkit.craftbukkit.entity.CraftHumanEntity");

            if (player.getClass().isAssignableFrom(humanEntity)) { // Not CraftBukkit?
                return;
            }

            Field permField = humanEntity.getDeclaredField("perm");
            // Make it public for reflection
            permField.setAccessible(true);

            PermissibleBase oldBase = (PermissibleBase) permField.get(player);
            PermissibleBase newBase = new PermissiblePEX(player, strictMode);

            // Copy permissions and attachments from old Permissible

            // Attachments
            Field attachmentField = PermissibleBase.class.getDeclaredField("attachments");
            attachmentField.setAccessible(true);
            attachmentField.set(newBase, attachmentField.get(oldBase));

            // Permissions
            Field permissionsField = PermissibleBase.class.getDeclaredField("permissions");
            permissionsField.setAccessible(true);
            permissionsField.set(newBase, permissionsField.get(oldBase));

            // Inject permissible
            permField.set(player, newBase);

        } catch (Throwable e) {
            Logger.getLogger("Minecraft").warning("[PermissionsEx] Failed to inject own Permissible");
        }
    }

    @Override
    public boolean hasPermission(String inName) {
        if (inName.equals("permissionsex.handler.injected")) {
            return true;
        }

        if (this.player != null) {
            try {
                PermissionUser user = PermissionsEx.getUser(this.player);
                if (user != null) {

                    
                    String expression = user.getMatchingExpression(inName, player.getWorld().getName());
                    
                    if (user.isDebug()) {
                        Logger.getLogger("Minecraft").info("User " + user.getName() + " checked for \"" + inName + "\", " + (expression == null ? "no permission found" : "\"" + expression + "\" found"));
                    }

                    if (expression != null || strictMode) {
                        return user.explainExpression(expression);
                    }
                }
            } catch (Throwable e) { // pex failed
            }
        }

        return super.hasPermission(inName);
    }

    @Override
    public boolean isPermissionSet(String name) {
        if (this.player != null) {
            try {
                PermissionUser user = PermissionsEx.getUser(this.player);
                if (user != null && user.getMatchingExpression(name, player.getWorld().getName()) != null) {
                    return true;
                }
            } catch (Throwable e) { // pex failed
            }
        }

        return super.isPermissionSet(name);
    }

    @Override
    public boolean hasPermission(Permission perm) {
        return this.hasPermission(perm.getName().toLowerCase());
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        return this.isPermissionSet(perm.getName().toLowerCase());
    }
}
