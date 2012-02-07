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

/**
 *
 * @author t3hk0d3
 */
public class PermissionEntityEvent extends PermissionEvent {

    protected PermissionEntity entity;
    
    protected Action action;

    public PermissionEntityEvent(PermissionEntity entity, Action action) {
        super(action.toString());

        this.entity = entity;
        this.action = action;
    }
    
    public Action getAction(){
        return this.action;
    }

    public PermissionEntity getEntity() {
        return entity;
    }
    
    public enum Action {
        PERMISSIONS_CHANGED,
        OPTIONS_CHANGED,
        INHERITANCE_CHANGED,
		@Deprecated
        INFO_CHANGED,
        TIMEDPERMISSION_EXPIRED,
        RANK_CHANGED,
        DEFAULTGROUP_CHANGED,
        WEIGHT_CHANGED,
        SAVED,
        REMOVED
    };
	
	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
