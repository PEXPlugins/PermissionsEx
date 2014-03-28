package ru.tehkode.permissions.backends;

import ru.tehkode.permissions.PermissionsData;
import ru.tehkode.permissions.PermissionsGroupData;
import ru.tehkode.permissions.PermissionsUserData;

import java.util.List;
import java.util.Map;

/**
 * Helper class to hold static methods relating to import/export between backends.
 * Should be refactored to be interface methods in jdk8
 */
public class BackendDataTransfer {
	private BackendDataTransfer() {
		// NO NO NO
	}

	private static void transferBase(PermissionsData from, PermissionsData to) {
		for (Map.Entry<String, List<String>> entry : from.getPermissionsMap().entrySet()) {
			to.setPermissions(entry.getValue(), entry.getKey());
		}

		for (Map.Entry<String, Map<String, String>> entry : from.getOptionsMap().entrySet()) {
			for (Map.Entry<String, String> option : entry.getValue().entrySet()) {
				to.setOption(option.getKey(), entry.getKey(), option.getValue());
			}
		}

		to.setPrefix(from.getPrefix(null), null);
		to.setSuffix(from.getSuffix(null), null);
		for (String world : from.getWorlds()) {
			to.setPrefix(from.getPrefix(world), world);
			to.setSuffix(from.getSuffix(world), world);
		}
	}

	public static void transferGroup(PermissionsGroupData from, PermissionsGroupData to) {
		transferBase(from, to);
		to.setParents(from.getParents(null), null);
		to.setDefault(from.isDefault(null), null);
		for (String world : from.getWorlds()) {
			List<String> groups =  from.getParents(world);
			if (groups == null || groups.isEmpty()) {
				continue;
			}
			to.setParents(groups, world);
			to.setDefault(from.isDefault(world), world);
		}
	}

	public static void transferUser(PermissionsUserData from, PermissionsUserData to) {
		transferBase(from, to);
		to.setGroups(from.getGroups(null), null);
		for (String world : from.getWorlds()) {
			List<String> groups =  from.getGroups(world);
			if (groups == null || groups.isEmpty()) {
				continue;
			}
			to.setGroups(groups, world);
		}
	}
}
