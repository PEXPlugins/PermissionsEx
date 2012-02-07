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

import java.util.*;

public abstract class PermissionNode extends PermissionEntity {

	protected Map<String, List<PermissionGroup>> inheritance = new HashMap<String, List<PermissionGroup>>();
	protected Map<String, List<PermissionGroup>> inheritanceCache = new HashMap<String, List<PermissionGroup>>();

	public PermissionNode(String name, PermissionManager manager) {
		super(name, manager);
	}

	/**
	 * INHERITANCE =============================================================
	 */
	/**
	 *
	 * @param worldName
	 * @return
	 */
	public List<PermissionGroup> getOwnParents(String worldName) {
		this.checkInheritance();
		
		return inheritance.containsKey(worldName) ? inheritance.get(worldName) : new LinkedList<PermissionGroup>();
	}

	public List<PermissionGroup> getParents(String worldName) {
		if (!this.inheritanceCache.containsKey(worldName)) {
			this.inheritanceCache.put(worldName, this.calculateParents(worldName, true, true));
		}

		return this.inheritance.get(worldName);
	}

	public List<String> getParentNames(String worldName) {
		List<String> parents = new LinkedList<String>();

		for (PermissionGroup group : this.getParents(worldName)) {
			parents.add(group.getName());
		}

		return parents;
	}

	public boolean isChildOf(PermissionGroup group, String worldName) {
		for (PermissionGroup parentGroup : this.getOwnParents(worldName)) {
			if (parentGroup.equals(group)) {
				return true;
			}
		}

		return false;
	}

	public boolean isDescendantOf(PermissionGroup group, String worldName) {
		for (PermissionGroup parentGroup : this.getParents(worldName)) {
			if (parentGroup.equals(group)) {
				return true;
			}
		}

		return false;
	}

	public Map<String, List<PermissionGroup>> getParentMap() {
		return this.inheritance;
	}

	public void setParents(List<PermissionGroup> parentGroups, String worldName) {
		this.checkInheritance();

		this.inheritance.put(worldName, parentGroups);

		this.commit(false);
	}

	public void addParent(PermissionGroup parentGroup, String worldName) {
		this.checkInheritance();

		if (!this.inheritance.containsKey(worldName)) {
			this.inheritance.put(worldName, new LinkedList<PermissionGroup>());
		}

		if (this.inheritance.get(worldName).contains(parentGroup)) {
			this.inheritance.get(worldName).remove(parentGroup);
		}

		this.inheritance.get(worldName).add(0, parentGroup);

		this.commit(false);
	}

	public void removeParent(PermissionGroup parentGroup, String worldName) {
		this.checkInheritance();

		if (!this.inheritance.containsKey(worldName)) {
			return;
		}

		this.inheritance.get(worldName).add(parentGroup);

		this.commit(false);
	}

	/**
	 * HOOKS ===================================================================
	 */
	/**
	 * Permissions =============================================================
	 */
	@Override
	public List<String> getPermissionsList(String worldName) {
		if (!this.permissionsCache.containsKey(worldName)) {			
			this.permissionsCache.put(worldName, this.calculatePermissionInheritance(worldName));
		}
				
		return super.getPermissionsList(worldName);
	}

	public List<String> getOwnPermissionsList(String worldName) {
		return this.permissions.containsKey(worldName) ? this.permissions.get(worldName) : new LinkedList<String>();
	}

	/**
	 * OPTIONS =================================================================
	 */
	@Override
	public Map<String, String> getOptions(String worldName) {
		if (!this.permissionsCache.containsKey(worldName)) {
			Map<String, String> map = super.getOptions(worldName);

			// Group inheritance
			for (PermissionGroup group : this.getParents(worldName)) {
				this.copyMapNoRewrite(map, group.getOptions(worldName));
			}
		}

		return super.getOptions(worldName);
	}

	public String getOwnSuffix(String worldName) {
		return this.getOwnOption("prefix", worldName, null);
	}

	public String getOwnPrefix(String worldName) {
		return this.getOwnOption("suffix", worldName, null);
	}

	/**
	 * Return only non-inherited (no world/group inheritance) option
	 *
	 * @param option
	 * @param worldName
	 * @param defaultValue
	 * @return
	 */
	public String getOwnOption(String option, String worldName, String defaultValue) {
		this.checkOptions();

		if (!this.options.containsKey(worldName) || !this.options.get(worldName).containsKey(option)) {
			return defaultValue;
		}

		return this.options.get(worldName).get(option);
	}

	/**
	 * Return integer value for option
	 *
	 * @param optionName
	 * @param world
	 * @param defaultValue
	 * @return option value or defaultValue if option was not found or is not
	 * integer
	 */
	public int getOwnOptionInteger(String optionName, String world, int defaultValue) {
		try {
			return Integer.parseInt(this.getOwnOption(optionName, world, Integer.toString(defaultValue)));
		} catch (NumberFormatException e) {
		}

		return defaultValue;
	}

	/**
	 * Returns double value for option
	 *
	 * @param optionName
	 * @param world
	 * @param defaultValue
	 * @return option value or defaultValue if option was not found or is not
	 * double
	 */
	public double getOwnOptionDouble(String optionName, String world, double defaultValue) {
		String option = this.getOwnOption(optionName, world, Double.toString(defaultValue));

		try {
			return Double.parseDouble(option);
		} catch (NumberFormatException e) {
		}

		return defaultValue;
	}

	/**
	 * Returns boolean value for option
	 *
	 * @param optionName
	 * @param world
	 * @param defaultValue
	 * @return option value or defaultValue if option was not found or is not
	 * boolean
	 */
	public boolean getOwnOptionBoolean(String optionName, String world, boolean defaultValue) {
		String option = this.getOwnOption(optionName, world, Boolean.toString(defaultValue));

		if ("false".equalsIgnoreCase(option)) {
			return false;
		} else if ("true".equalsIgnoreCase(option)) {
			return true;
		}

		return defaultValue;
	}

	public abstract Map<String, List<PermissionGroup>> loadInheritance();

	protected void checkInheritance() {
		if (this.inheritance == null) {
			this.inheritance = this.loadInheritance();
		}
	}

	protected List<PermissionGroup> calculateParents(String worldName, boolean groupInheritance, boolean isRoot) {
		List<PermissionGroup> parentGroups = new LinkedList<PermissionGroup>(this.getOwnParents(worldName));

		// World inheritance
		if (worldName != null) {
			for (String parentWorld : this.manager.getWorldInheritanceList(worldName)) {
				parentGroups.addAll(this.calculateParents(parentWorld, false, false));
			}

			if (isRoot) {
				parentGroups.addAll(this.calculateParents(null, false, false));
			}
		}

		if (groupInheritance) {
			// Group inheritance
			parentGroups.addAll(this.calculateParentInheritance(worldName));
		}

		return parentGroups;
	}

	protected List<String> calculatePermissionInheritance(String worldName) {
		List<String> parentPermissions = this.calculatePermissions(worldName, true);

		for (PermissionGroup parentGroup : this.getParents(worldName)) {
			parentPermissions.addAll(parentGroup.getPermissionsList(worldName));
		}

		return parentPermissions;
	}
	
	protected List<PermissionGroup> calculateParentInheritance(String worldName) {
		List<PermissionGroup> parentGroups = new LinkedList<PermissionGroup>();

		for (PermissionGroup parentGroup : this.getOwnParents(worldName)) {
			parentGroups.addAll(parentGroup.getParents(worldName));
		}

		return parentGroups;
	}
}
