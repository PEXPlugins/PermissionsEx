package ru.tehkode.permissions;

/**
 * Matcher using shell-style glob expansion
 * Expressions can use the following expansion:
 * * - wildcard
 * {a,b,...} - matches either a or b
 * ? - previous character or match group is optional
 */
public class ShellGlobMatcher implements PermissionMatcher {
	@Override
	public boolean isMatches(String expression, String permission) {
		if (permission.equals(expression)) {
			return true;
		}
		int permissionIndex = 0;
		boolean match;
		for (String split : expression.split("\\*")) {

			if (permissionIndex == 0) {
				match = permission.startsWith(split);
				permissionIndex += split.length();
			} else {
				int matchIndex = permission.indexOf(split, permissionIndex);
				match = matchIndex != -1;
				permissionIndex = matchIndex + split.length();
			}

			if (!match) {
				return false;
			}
		}
		return true;
	}
}
