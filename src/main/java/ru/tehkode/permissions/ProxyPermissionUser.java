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
package ru.tehkode.permissions;

import java.util.Map;
import ru.tehkode.permissions.events.PermissionEntityEvent;

public abstract class ProxyPermissionUser extends PermissionUser {

    protected PermissionEntity backendEntity;

    public ProxyPermissionUser(PermissionEntity backendEntity) {
        super(backendEntity.getName(), backendEntity.manager);

        this.backendEntity = backendEntity;

        this.setName(backendEntity.getName());

        this.virtual = backendEntity.isVirtual();

    }

    @Override
    public String[] getWorlds() {
        return backendEntity.getWorlds();
    }

    @Override
    public String getOwnPrefix(String worldName) {
        return backendEntity.getPrefix(worldName);
    }

    @Override
    public String getOwnSuffix(String worldName) {
        return backendEntity.getSuffix(worldName);
    }

    @Override
    public void setPrefix(String prefix, String worldName) {
        this.backendEntity.setPrefix(prefix, worldName);
    }

    @Override
    public void setSuffix(String suffix, String worldName) {
        this.backendEntity.setSuffix(suffix, worldName);
    }

    @Override
    public boolean isVirtual() {
        return backendEntity.isVirtual();
    }

    @Override
    public String[] getOwnPermissions(String world) {
        return this.backendEntity.getPermissions(world);
    }

    @Override
    public void addPermission(String permission, String world) {
        this.backendEntity.addPermission(permission, world);

        this.clearCache();

        this.callEvent(PermissionEntityEvent.Action.PERMISSIONS_CHANGED);
    }

    @Override
    public Map<String, String[]> getAllPermissions() {
        return this.backendEntity.getAllPermissions();
    }

    @Override
    public void setPermissions(String[] permissions, String world) {
        this.backendEntity.setPermissions(permissions, world);

        this.callEvent(PermissionEntityEvent.Action.PERMISSIONS_CHANGED);
    }

    @Override
    public void removePermission(String permission, String world) {
        this.backendEntity.removePermission(permission, world);

        this.clearCache();
        this.callEvent(PermissionEntityEvent.Action.PERMISSIONS_CHANGED);
    }

    @Override
    public Map<String, Map<String, String>> getAllOptions() {
        return this.backendEntity.getAllOptions();
    }

    @Override
    public String getOwnOption(String option, String world, String defaultValue) {
        return this.backendEntity.getOption(option, world, defaultValue);
    }

    @Override
    public String getOption(String optionName, String worldName, String defaultValue) {
        String option = this.getOwnOption(optionName, worldName, null);

        if (option == null) {
            for (PermissionGroup group : this.getGroups(worldName)) {
                option = group.getOption(optionName, worldName, null);
                if (option != null) {
                    return option;
                }
            }

            option = defaultValue;
        }

        return option;
    }

    @Override
    public Map<String, String> getOptions(String world) {
        return this.backendEntity.getOptions(world);
    }

    @Override
    public void setOption(String permission, String value, String world) {
        this.backendEntity.setOption(permission, value, world);

        this.callEvent(PermissionEntityEvent.Action.OPTIONS_CHANGED);
    }

    @Override
    public void save() {
        this.backendEntity.save();
        super.save();
    }

    @Override
    public void remove() {
        this.backendEntity.remove();
        super.remove();
    }
}
