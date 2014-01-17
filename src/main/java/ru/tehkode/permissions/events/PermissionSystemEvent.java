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

import java.util.UUID;

/**
 * @author t3hk0d3
 */
public class PermissionSystemEvent extends PermissionEvent {
	protected Action action;
	private static final HandlerList handlers = new HandlerList();

	public PermissionSystemEvent(UUID sourceUUID, Action action) {
		super(sourceUUID);
		this.action = action;
	}

	public Action getAction() {
		return this.action;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public enum Action {

		BACKEND_CHANGED,
		RELOADED,
		WORLDINHERITANCE_CHANGED,
		DEFAULTGROUP_CHANGED,
		DEBUGMODE_TOGGLE,
		REINJECT_PERMISSIBLES,
	}
}
