/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2020 zml [at] stellardrift [dot] ca and PermissionsEx contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ca.stellardrift.permissionsex.bukkit;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class BukkitConfiguration {
    @Setting(value = "fallback-op", comment = "Whether to fall back to checking op status when a permission is unset in PEX")
    private boolean fallbackOp = true;

    public boolean shouldFallbackOp() {
        return fallbackOp;
    }
}
