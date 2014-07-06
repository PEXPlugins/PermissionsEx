package ru.tehkode.permissions.query;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.data.MatcherGroup;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Query used to set permissions data
 */
public class SetQuery extends PermissionQuery<SetQuery> {
	public SetQuery(PermissionManager manager) {
		super(manager);
		followInheritance(false); // When setting, we usually don't want to go up in the inheritance tree to look for sections to modify
	}

	public ListenableFuture<Boolean> addPermission(final String permission) {
		return Futures.chain(performQuery(MatcherGroup.PERMISSIONS_KEY, true), new Function<List<MatcherGroup>, ListenableFuture<? extends Boolean>>() {
			@Override
			public ListenableFuture<? extends Boolean> apply(List<MatcherGroup> matcherGroups) {
				for (MatcherGroup group : matcherGroups) {
					if (group.isList()) {
						return Futures.transform(group.addEntry(permission), new Function<MatcherGroup, Boolean>() {
							@Override
							public Boolean apply(@Nullable MatcherGroup matcherGroup) {
								return matcherGroup != null;
							}
						});
					}
				}
				return Futures.immediateFuture(false);
			}
		});
	}

	public ListenableFuture<Boolean> removePermission(final String permission) {
		return Futures.chain(performQuery(MatcherGroup.PERMISSIONS_KEY, false), new Function<List<MatcherGroup>, ListenableFuture<? extends Boolean>>() {
			@Override
			public ListenableFuture<? extends Boolean> apply(List<MatcherGroup> matcherGroups) {
				boolean successful = false;
				for (MatcherGroup group : matcherGroups) {
					if (group.isList()) {
						group.removeEntry(permission);
						successful = true;
					}
				}
				return Futures.immediateFuture(successful);
			}
		});
	}

	public ListenableFuture<Boolean> setPermissions(final List<String> permissions) {
		return Futures.chain(performQuery(MatcherGroup.PERMISSIONS_KEY, true), new Function<List<MatcherGroup>, ListenableFuture<? extends Boolean>>() {
			@Override
			public ListenableFuture<? extends Boolean> apply(List<MatcherGroup> matcherGroups) {
				for (MatcherGroup group : matcherGroups) {
					if (group.isList()) {
						return Futures.transform(group.setEntries(permissions), new Function<MatcherGroup, Boolean>() {
							@Override
							public Boolean apply(@Nullable MatcherGroup matcherGroup) {
								return matcherGroup != null;
							}
						});
					}
				}
				return Futures.immediateFuture(false);
			}
		});
	}

	public ListenableFuture<Boolean> setOption(final String key, final String value) {
		return Futures.chain(performQuery(MatcherGroup.OPTIONS_KEY, true), new Function<List<MatcherGroup>, ListenableFuture<? extends Boolean>>() {
			@Override
			public ListenableFuture<? extends Boolean> apply(List<MatcherGroup> matcherGroups) {
				for (MatcherGroup group : matcherGroups) {
					if (group.isMap()) {
						return Futures.transform(group.putEntry(key, value), new Function<MatcherGroup, Boolean>() {
							@Override
							public Boolean apply(@Nullable MatcherGroup matcherGroup) {
								return matcherGroup != null;
							}
						});
					}
				}
				return Futures.immediateFuture(false);
			}
		});
	}

	public ListenableFuture<Boolean> removeOption(final String key) {
		return Futures.chain(performQuery(MatcherGroup.OPTIONS_KEY, false), new Function<List<MatcherGroup>, ListenableFuture<? extends Boolean>>() {
			@Override
			public ListenableFuture<? extends Boolean> apply(List<MatcherGroup> matcherGroups) {
				boolean successful = false;
				for (MatcherGroup group : matcherGroups) {
					if (group.isMap()) {
						group.removeEntry(key);
						successful = true;
					}
				}
				return Futures.immediateFuture(successful);
			}
		});
	}

	public ListenableFuture<Boolean> addParent(final String parent) {
		return Futures.chain(performQuery(MatcherGroup.INHERITANCE_KEY, true), new Function<List<MatcherGroup>, ListenableFuture<? extends Boolean>>() {
			@Override
			public ListenableFuture<? extends Boolean> apply(List<MatcherGroup> matcherGroups) {
				for (MatcherGroup group : matcherGroups) {
					if (group.isList()) {
						return Futures.transform(group.addEntry(parent), new Function<MatcherGroup, Boolean>() {
							@Override
							public Boolean apply(@Nullable MatcherGroup matcherGroup) {
								return matcherGroup != null;
							}
						});
					}
				}
				return Futures.immediateFuture(false);
			}
		});
	}

	public ListenableFuture<Boolean> removeParent(final String parent) {
		return Futures.chain(performQuery(MatcherGroup.INHERITANCE_KEY, false), new Function<List<MatcherGroup>, ListenableFuture<? extends Boolean>>() {
			@Override
			public ListenableFuture<? extends Boolean> apply(List<MatcherGroup> matcherGroups) {
				boolean successful = false;
				for (MatcherGroup group : matcherGroups) {
					if (group.isList()) {
						group.removeEntry(parent);
						successful = true;
					}
				}
				return Futures.immediateFuture(successful);
			}
		});
	}

	public ListenableFuture<Boolean> setParents(final List<String> parents) {
		return Futures.chain(performQuery(MatcherGroup.INHERITANCE_KEY, true), new Function<List<MatcherGroup>, ListenableFuture<? extends Boolean>>() {
			@Override
			public ListenableFuture<? extends Boolean> apply(List<MatcherGroup> matcherGroups) {
				for (MatcherGroup group : matcherGroups) {
					if (group.isList()) {
						return Futures.transform(group.setEntries(parents), new Function<MatcherGroup, Boolean>() {
							@Override
							public Boolean apply(@Nullable MatcherGroup matcherGroup) {
								return matcherGroup != null;
							}
						});
					}
				}
				return Futures.immediateFuture(false);
			}
		});
	}
}
