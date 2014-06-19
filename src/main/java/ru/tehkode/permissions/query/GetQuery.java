package ru.tehkode.permissions.query;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.callback.Callback;
import ru.tehkode.permissions.data.MatcherGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Query type used to get permissions data.
 */
public class GetQuery extends PermissionQuery<GetQuery> {
	public GetQuery(PermissionManager manager) {
		super(manager);
	}

	public void has(String permission, final Callback<Boolean> callback) {
		permissions(new Callback<List<String>>() {
			@Override
			public void onSuccess(List<String> result) {
				for (String expression : result) {
					if (getManager().getPermissionMatcher().isMatches(expression, permission)) {
						if (callback != null) {
							callback.onSuccess(true);
						}
					}
				}
				if (callback != null) {
					callback.onSuccess(false);
				}

			}

			@Override
			public void onError(Throwable t) {
				if (callback != null) {
					callback.onError(t);
				}
			}
		});
	}

	public void permissions(final Callback<List<String>> callback) {
		performQuery(MatcherGroup.PERMISSIONS_KEY, new Callback<List<MatcherGroup>>() {
			@Override
			public void onSuccess(List<MatcherGroup> result) {
				List<String> ret = new ArrayList<>();
				for (MatcherGroup match : result) {
					ret.addAll(match.getEntriesList());
				}
				if (callback != null) {
					callback.onSuccess(ret);
				}

			}

			@Override
			public void onError(Throwable t) {
				if (callback != null) {
					callback.onError(t);
				}
			}
		});
	}

	protected void getInheritedChildPermissions(String perm, List<String> list) {
		getInheritedChildPermissions(perm, list, false);
	}

	protected void getInheritedChildPermissions(String perm, List<String> list, boolean invert) {

		if (perm.startsWith("-")) {
			invert = !invert;
			perm = perm.substring(1);
		}
		getInheritedChildPermissions(Bukkit.getPluginManager().getPermission(perm), list, invert);
	}

	protected void getInheritedChildPermissions(Permission perm, List<String> list, boolean invert) {
		if (perm == null) {
			return;
		}
		for (Map.Entry<String, Boolean> entry : perm.getChildren().entrySet()) {
			boolean has = entry.getValue() ^ invert;
			String node = (has ? "" : "-") + entry.getKey();
			if (!list.contains(node)) {
				list.add(node);
				getInheritedChildPermissions(node, list, !has);
			}
		}
	}

	public String option(String option) {
		return option(option, null);
	}

	public String option(String option, String defaultValue) {
		for (MatcherGroup match : performQuery(MatcherGroup.OPTIONS_KEY)) {
			if (match.getEntries().containsKey(option)) {
				return match.getEntries().get(option);
			}
		}
		return defaultValue;
	}

	public Map<String, String> options() {
		Map<String, String> options = new HashMap<>();
		List<MatcherGroup> result = performQuery(MatcherGroup.OPTIONS_KEY);
		for (ListIterator<MatcherGroup> it = result.listIterator(result.size() - 1); it.hasPrevious();) {
			options.putAll(it.previous().getEntries());
		}
		return options;
	}

	public List<String> parents() {
		List<String> ret = new LinkedList<>();
		for (MatcherGroup match : performQuery(MatcherGroup.INHERITANCE_KEY)) {
			ret.addAll(match.getEntriesList());
		}
		return ret;
	}

	public List<String> worldParents() {
		List<String> ret = new LinkedList<>();
		for (MatcherGroup match : performQuery(MatcherGroup.WORLD_INHERITANCE_KEY)) {
			ret.addAll(match.getEntriesList());
		}
		return ret;
	}
}
