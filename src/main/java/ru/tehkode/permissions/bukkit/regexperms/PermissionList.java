package ru.tehkode.permissions.bukkit.regexperms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import ru.tehkode.utils.FieldReplacer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author zml2008
 */
public class PermissionList extends HashMap<String, Permission> {
	private static FieldReplacer<PluginManager, Map> INJECTOR;

	private static final Map<Class<?>, FieldReplacer<Permission, Map>> CHILDREN_MAPS = new HashMap<>();
	/**
	 * k = child permission
	 * v.k = parent permission
	 * v.v = value parent gives child
	 */
	private final Multimap<String, Map.Entry<String, Boolean>> childParentMapping = Multimaps.synchronizedMultimap(HashMultimap.<String, Map.Entry<String, Boolean>>create());

	public PermissionList() {
		super();
	}

	public PermissionList(Map<? extends String, ? extends Permission> existing) {
		super(existing);
	}

	private FieldReplacer<Permission, Map> getFieldReplacer(Permission perm) {
		FieldReplacer<Permission, Map> ret = CHILDREN_MAPS.get(perm.getClass());
		if (ret == null) {
			ret = new FieldReplacer<>(perm.getClass(), "children", Map.class);
			CHILDREN_MAPS.put(perm.getClass(), ret);
		}
		return ret;
	}

	private void removeAllChildren(String perm) {
		for (Iterator<Map.Entry<String, Map.Entry<String, Boolean>>> it = childParentMapping.entries().iterator(); it.hasNext();) {
			if (it.next().getValue().getKey().equals(perm)) {
				it.remove();
			}
		}
	}

	private class NotifyingChildrenMap extends LinkedHashMap<String, Boolean> {
		private final Permission perm;
		public NotifyingChildrenMap(Permission perm) {
			super(perm.getChildren());
			this.perm = perm;
		}
		@Override
		public Boolean remove(Object perm) {
			removeFromMapping(String.valueOf(perm));
			return super.remove(perm);
		}

		private void removeFromMapping(String child) {
			for (Iterator<Map.Entry<String, Boolean>> it = childParentMapping.get(child).iterator(); it.hasNext();) {
				if (it.next().getKey().equals(perm.getName())) {
					it.remove();
				}
			}
		}

		@Override
		public Boolean put(String perm, Boolean val) {
			//removeFromMapping(perm);
			childParentMapping.put(perm, new SimpleEntry<>(this.perm.getName(), val));
			return super.put(perm, val);
		}

		@Override
		public void clear() {
			removeAllChildren(perm.getName());
			super.clear();
		}
	}


	public static PermissionList inject(PluginManager manager) {
		if (INJECTOR == null) {
			INJECTOR = new FieldReplacer<>(manager.getClass(), "permissions", Map.class);
		}
		Map existing = INJECTOR.get(manager);
		@SuppressWarnings("unchecked")
		PermissionList list = new PermissionList(existing);
		INJECTOR.set(manager, list);
		return list;
	}

	@Override
	public Permission put(String k, Permission v) {
		for (Map.Entry<String, Boolean> ent : v.getChildren().entrySet()) {
			childParentMapping.put(ent.getKey(), new SimpleEntry<>(v.getName(), ent.getValue()));
		}
		FieldReplacer<Permission, Map> repl = getFieldReplacer(v);
		repl.set(v, new NotifyingChildrenMap(v));
		return super.put(k, v);
	}

	@Override
	public Permission remove(Object k) {
		Permission ret = super.remove(k);
		if (ret != null) {
			removeAllChildren(k.toString());
			getFieldReplacer(ret).set(ret, new LinkedHashMap<>(ret.getChildren()));
		}
		return ret;
	}

	@Override
	public void clear() {
		childParentMapping.clear();
		super.clear();
	}

	public Collection<Map.Entry<String, Boolean>> getParents(String permission) {
		return ImmutableSet.copyOf(childParentMapping.get(permission.toLowerCase()));
	}
}
