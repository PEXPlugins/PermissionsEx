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
package ru.tehkode.permissions.events;

import org.bukkit.event.HandlerList;
import ru.tehkode.permissions.PermissionEntity;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.util.UUID;

/**
 * @author t3hk0d3
 */
public class PermissionEntityEvent extends PermissionEvent {

	private static final HandlerList handlers = new HandlerList();
	protected transient PermissionEntity entity;
	protected Action action;
	protected PermissionEntity.Type type;
	protected String entityName;

	public PermissionEntityEvent(UUID sourceUUID, PermissionEntity entity, Action action) {
		super(sourceUUID);
		this.entity = entity;
		this.entityName = entity.getIdentifier();
		this.type = entity.getType();
		this.action = action;
	}

	public Action getAction() {
		return this.action;
	}

	public PermissionEntity getEntity() {
		if (entity == null) {
			switch (type) {
				case GROUP:
					entity = PermissionsEx.getPermissionManager().getGroup(entityName);
					break;
				case USER:
					entity = PermissionsEx.getPermissionManager().getUser(entityName);
					break;
			}
		}
		return entity;
	}

	public String getEntityName() {
		return entityName;
	}

	public PermissionEntity.Type getType() {
		return type;
	}

	public enum Action {

		PERMISSIONS_CHANGED,
		OPTIONS_CHANGED,
		INHERITANCE_CHANGED,
		INFO_CHANGED,
		TIMEDPERMISSION_EXPIRED,
		RANK_CHANGED,
		DEFAULTGROUP_CHANGED,
		WEIGHT_CHANGED,
		SAVED,
		REMOVED,
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
