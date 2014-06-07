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
				to.setOption(option.getKey(), option.getValue(), entry.getKey());
			}
		}

		to.setParents(from.getParents(null), null);
		for (String world : from.getWorlds()) {
			List<String> groups =  from.getParents(world);
			if (groups == null || groups.isEmpty()) {
				continue;
			}
			to.setParents(groups, world);
		}
	}

	public static void transferGroup(PermissionsGroupData from, PermissionsGroupData to) {
		transferBase(from, to);
	}

	public static void transferUser(PermissionsUserData from, PermissionsUserData to) {
		transferBase(from, to);
	}
}
