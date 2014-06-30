package ru.tehkode.permissions.query;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import ru.tehkode.permissions.PermissionManager;
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

	public ListenableFuture<Boolean> has(final String permission) {
		return Futures.transform(permissions(), new Function<List<String>, Boolean>() {
			@Override
			public Boolean apply(List<String> result) {
				for (String expression : result) {
					if (getManager().getPermissionMatcher().isMatches(expression, permission)) {
						return true;
					}
				}
				return false;
			}
		});
	}

	public ListenableFuture<List<String>> permissions() {
		return Futures.transform(performQuery(MatcherGroup.PERMISSIONS_KEY), new Function<List<MatcherGroup>, List<String>>() {
			@Override
			public List<String> apply(List<MatcherGroup> result) {
				List<String> ret = new ArrayList<>();
				for (MatcherGroup match : result) {
					ret.addAll(match.getEntriesList());
				}
				return ret;
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

	public ListenableFuture<String> option(String option) {
		return option(option, null);
	}

	public ListenableFuture<String> option(final String option, final String defaultValue) {
		return Futures.transform(performQuery(MatcherGroup.OPTIONS_KEY), new Function<List<MatcherGroup>, String>() {
			@Override
			public String apply(List<MatcherGroup> matcherGroups) {
				for (MatcherGroup match : matcherGroups) {
					if (match.getEntries().containsKey(option)) {
						return match.getEntries().get(option);
					}
				}
				return defaultValue;
			}
		});
	}

	public ListenableFuture<Map<String, String>> options() {
		return Futures.transform(performQuery(MatcherGroup.OPTIONS_KEY), new Function<List<MatcherGroup>, Map<String, String>>() {
			@Override
			public Map<String, String> apply(List<MatcherGroup> result) {
				Map<String, String> options = new HashMap<>();
			for (ListIterator<MatcherGroup> it = result.listIterator(result.size() - 1); it.hasPrevious();) {
			options.putAll(it.previous().getEntries());
		}
		return options;
			}
		});
	}

	public ListenableFuture<List<String>> parents() {
		return Futures.transform(performQuery(MatcherGroup.INHERITANCE_KEY), new Function<List<MatcherGroup>, List<String>>() {
			@Override
			public List<String> apply(List<MatcherGroup> matcherGroups) {
				List<String> ret = new LinkedList<>();
				for (MatcherGroup match : matcherGroups) {
					ret.addAll(match.getEntriesList());
				}
				return ret;
			}
		});
	}

	public ListenableFuture<List<String>> worldParents() {
		return Futures.transform(performQuery(MatcherGroup.WORLD_INHERITANCE_KEY), new Function<List<MatcherGroup>, List<String>>() {
			@Override
			public List<String> apply(List<MatcherGroup> matcherGroups) {
				List<String> ret = new LinkedList<>();
				for (MatcherGroup match : matcherGroups) {
					ret.addAll(match.getEntriesList());
				}
				return ret;
			}
		});
	}
}
