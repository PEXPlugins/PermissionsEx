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

package ru.tehkode.permissions.backends.sql;

/**
 *
 * @author t3hk0d3
 */
public class SQLEntity {
/*
	public enum Type {

		GROUP, USER
	}
	protected SQLConnection db;
	protected Map<String, List<String>> worldsPermissions = null;
	protected Map<String, Map<String, String>> worldsOptions = null;
	protected List<String> commonPermissions = null;
	protected Map<String, String> commonOptions = null;
	protected Map<String, Set<String>> parents = null;
	protected Type type;
	protected String prefix;
	protected String suffix;

	public SQLEntity(String name, PermissionManager manager, SQLEntity.Type type, SQLConnection db) {
		super(name, manager);
		this.db = db;
		this.type = type;

		this.fetchInfo();
		this.fetchPermissions();
		this.fetchInheritance();
	}

	public static String[] getEntitiesNames(SQLConnection sql, Type type, boolean defaultOnly) {
		try {
			List<String> entities = new ArrayList<String>();

			ResultSet result = sql.selectQuery("SELECT name FROM `permissions_entity` WHERE `type` = ? " + (defaultOnly ? " AND `default` = 1" : ""), type.ordinal());
			while (result.next()) {
				entities.add(result.getString("name"));
			}

			return entities.toArray(new String[0]);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Set<String> getWorldsList() {
		Set<String> worlds = new HashSet<String>();

		worlds.addAll(worldsOptions.keySet());
		worlds.addAll(worldsPermissions.keySet());
		worlds.addAll(parents.keySet());

		return worlds;
	}

	@Override
	public String getPrefix(String worldName) {
		return (worldName == null || worldName.isEmpty()) ? this.prefix : this.getOption("prefix", worldName);
	}

	@Override
	public String getSuffix(String worldName) {
		return (worldName == null || worldName.isEmpty()) ? this.suffix : this.getOption("suffix", worldName);
	}

	@Override
	public void setPrefix(String prefix, String worldName) {
		if (worldName == null || worldName.isEmpty()) {
			this.prefix = prefix;
			this.updateInfo();
		} else {
			this.setOption("prefix", prefix, worldName);
		}
	}

	@Override
	public void setSuffix(String suffix, String worldName) {
		if (worldName == null || worldName.isEmpty()) {
			this.suffix = suffix;
			this.updateInfo();
		} else {
			this.setOption("suffix", suffix, worldName);
		}
	}

	public String[] getParentNames(String worldName) {
		if (this.parents == null) {
			this.fetchInheritance();
		}

		if (this.parents.containsKey(worldName)) {
			return this.parents.get(worldName).toArray(new String[0]);
		}

		return new String[0];
	}

	@Override
	public String[] getPermissions(String world) {
		List<String> permissions = new ArrayList<String>();

		if (commonPermissions == null) {
			this.fetchPermissions();
		}

		if (world != null && !world.isEmpty()) {
			List<String> worldPermissions = this.worldsPermissions.get(world);
			if (worldPermissions != null) {
				permissions.addAll(worldPermissions);
			}
		} else {
			permissions = commonPermissions;
		}

		return permissions.toArray(new String[0]);
	}

	@Override
	public String getOption(String option, String world, String defaultValue) {
		if (world != null && !world.isEmpty() && this.worldsOptions.containsKey(world)) {
			if (this.worldsOptions.get(world).containsKey(option)) {
				return this.worldsOptions.get(world).get(option);
			}
		}

		if ((world == null || world.isEmpty()) && this.commonOptions.containsKey(option)) {
			return this.commonOptions.get(option);
		}

		return defaultValue;
	}

	@Override
	public void setOption(String option, String value, String world) {
		if (option == null || option.isEmpty()) {
			return;
		}

		if (world == null) {
			world = "";
		}

		if (value == null || value.isEmpty()) {
			this.db.updateQuery("DELETE FROM `permissions` WHERE `name` = ? AND `permission` = ? AND `type` = ? AND `world` = ?", this.getName(), option, this.type.ordinal(), world);

			if (!world.isEmpty() && this.worldsOptions.containsKey(world)) {
				this.worldsOptions.get(world).remove(option);
			} else {
				this.commonOptions.remove(option);
			}

			return;
		}

		Boolean newOption = true;
		if (this.commonOptions == null) {
			this.fetchPermissions();
		}

		if (!world.isEmpty() && worldsOptions.containsKey(world) && worldsOptions.get(world).containsKey(option)) {
			newOption = false;
		} else if (world.isEmpty() && commonOptions.containsKey(option)) {
			newOption = false;
		}

		if (newOption) {
			this.db.updateQuery("INSERT INTO `permissions` (`name`, `permission`, `value`, `world`, `type`) VALUES (?, ?, ?, ?, ?)", this.getName(), option, value, world, this.type.ordinal());
		} else {
			this.db.updateQuery("UPDATE `permissions` SET `value` = ? WHERE `name` = ? AND `type` = ? AND `permission` = ?", value, this.getName(), this.type.ordinal(), option);
		}

		if (this.isVirtual()) {
			this.save();
		}

		// Refetch options
		this.fetchPermissions();
	}

	public void setParents(String[] parentGroups, String worldName) {
		try {
			// Clean out existing records
			if (worldName != null) { // damn NULL
				this.db.updateQuery("DELETE FROM `permissions_inheritance` WHERE `child` = ? AND `type` = ? AND `world` = ?", this.getName(), this.type.ordinal(), worldName);
			} else {
				this.db.updateQuery("DELETE FROM `permissions_inheritance` WHERE `child` = ? AND `type` = ? AND IFNULL(`world`, 1)", this.getName(), this.type.ordinal());
			}


			List<Object[]> rows = new ArrayList<Object[]>();
			for (String group : parentGroups) {
				if (group == null || group.isEmpty()) {
					continue;
				}

				rows.add(new Object[]{this.getName(), group, this.type.ordinal(), worldName});
			}

			this.db.insert("permissions_inheritance", new String[]{"child", "parent", "type", "world"}, rows);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		if (this.isVirtual()) {
			this.save();
		}

		//reload inherirance
		this.parents = null;
		this.fetchInheritance();
	}

	@Override
	public Map<String, String> getOptions(String world) {
		Map<String, String> options = new HashMap<String, String>();

		// put common options
		options.putAll(this.commonOptions);
		// override them with world-specific
		if (this.worldsOptions.containsKey(world)) {
			options.putAll(this.worldsOptions.get(world));
		}

		return options;
	}

	@Override
	public Map<String, String[]> getAllPermissions() {
		Map<String, String[]> allPermissions = new HashMap<String, String[]>();

		allPermissions.put(null, this.commonPermissions.toArray(new String[0]));

		for (Map.Entry<String, List<String>> entry : this.worldsPermissions.entrySet()) {
			allPermissions.put(entry.getKey(), entry.getValue().toArray(new String[0]));
		}

		return allPermissions;
	}

	@Override
	public Map<String, Map<String, String>> getAllOptions() {
		Map<String, Map<String, String>> allOptions = new HashMap<String, Map<String, String>>();

		allOptions.put(null, this.commonOptions);

		for (Map.Entry<String, Map<String, String>> entry : this.worldsOptions.entrySet()) {
			allOptions.put(entry.getKey(), entry.getValue());
		}

		return allOptions;
	}

	@Override
	public void setPermissions(String[] permissions, String world) {
		if (world == null) {
			world = "";
		}

		this.db.updateQuery("DELETE FROM `permissions` WHERE `name` = ? AND `type` = ? AND `world` = ? AND `value` = ''", this.getName(), this.type.ordinal(), world);

		for (int i = permissions.length - 1; i >= 0; i--) { // insert in reverse order
			this.db.updateQuery("INSERT INTO `permissions` (`name`, `permission`, `value`, `world`, `type`) VALUES (?, ?, '', ?, ?)", this.getName(), permissions[i], world, this.type.ordinal());
		}

		if (this.isVirtual()) {
			this.save();
		}

		this.fetchPermissions();
	}

	@Override
	public void save() {
		this.updateInfo();
	}

	@Override
	public void remove() {
		// clear inheritance info
		this.db.updateQuery("DELETE FROM `permissions_inheritance` WHERE `child` = ? AND `type` = ?", this.getName(), this.type.ordinal());
		// clear permissions
		this.db.updateQuery("DELETE FROM `permissions` WHERE `name` = ? AND `type` = ?", this.getName(), this.type.ordinal());
		// clear info
		this.db.updateQuery("DELETE FROM `permissions_entity` WHERE `name` = ? AND `type` = ?", this.getName(), this.type.ordinal());

		this.virtual = true;
		this.commonOptions.clear();
		this.commonPermissions.clear();
		this.worldsOptions.clear();
		this.worldsPermissions.clear();
		this.parents.clear();
	}

	protected void updateInfo() {
		String sql;
		if (this.isVirtual()) { // This section are suspicious, here was problem which are resolved mysticaly. Keep eye on it.
			sql = "INSERT INTO `permissions_entity` (`prefix`, `suffix`, `name`, `type`) VALUES (?, ?, ?, ?)";
		} else {
			sql = "UPDATE `permissions_entity` SET `prefix` = ?, `suffix` = ? WHERE `name` = ? AND `type` = ?";
		}

		try {
			this.db.updateQuery(sql, this.prefix, this.suffix, this.getName(), this.type.ordinal());
		} catch (RuntimeException e) {
			if (e.getCause() instanceof SQLException && this.isVirtual()) {
				this.virtual = false;
				this.updateInfo(); // try update
			}

			throw e;
		}

		this.virtual = false;
	}

	protected final void fetchPermissions() {
		this.worldsOptions = new HashMap<String, Map<String, String>>();
		this.worldsPermissions = new HashMap<String, List<String>>();
		this.commonOptions = new HashMap<String, String>();
		this.commonPermissions = new ArrayList<String>();

		try {
			ResultSet results = this.db.selectQuery("SELECT `permission`, `world`, `value` FROM `permissions` WHERE `name` = ? AND `type` = ? ORDER BY `id` DESC", this.getName(), this.type.ordinal());
			while (results.next()) {
				String permission = results.getString("permission").trim();
				String world = results.getString("world").trim();
				String value = results.getString("value").trim();

				// @TODO: to this in more optimal way
				if (value.isEmpty()) {
					if (!world.isEmpty()) {
						List<String> worldPermissions = this.worldsPermissions.get(world);
						if (worldPermissions == null) {
							worldPermissions = new ArrayList<String>();
							this.worldsPermissions.put(world, worldPermissions);
						}

						worldPermissions.add(permission);
					} else {
						this.commonPermissions.add(permission);
					}
				} else {
					if (!world.isEmpty()) {
						Map<String, String> worldOptions = this.worldsOptions.get(world);
						if (worldOptions == null) {
							worldOptions = new HashMap<String, String>();
							worldsOptions.put(world, worldOptions);
						}

						worldOptions.put(permission, value);
					} else {
						commonOptions.put(permission, value);
					}
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	protected final void fetchInheritance() {
		try {
			this.parents = new HashMap<String, Set<String>>();

			ResultSet results = this.db.selectQuery("SELECT `parent`, `world` FROM `permissions_inheritance` WHERE `child` = ? AND `type` = ? ORDER BY `id` DESC", this.getName(), this.type.ordinal());

			while (results.next()) {
				String parentName = results.getString(1);
				String worldName = results.getString(2);

				if (!this.parents.containsKey(worldName)) {
					this.parents.put(worldName, new LinkedHashSet<String>());
				}

				this.parents.get(worldName).add(parentName);
			}

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	protected final void fetchInfo() {
		try {
			ResultSet result = this.db.selectQuery("SELECT `name`, `prefix`, `suffix` FROM `permissions_entity` WHERE `name` LIKE ? AND `type` = ? LIMIT 1", this.getName(), this.type.ordinal());
			if (result.next()) {
				this.prefix = result.getString("prefix");
				this.suffix = result.getString("suffix");

				// For teh case-insensetivity
				this.setName(result.getString("name"));

				this.virtual = false;
			} else {
				this.prefix = "";
				this.suffix = "";
				this.virtual = true;
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	* */
}
