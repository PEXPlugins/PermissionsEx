package ru.tehkode.permissions.bukkit.regexperms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import ru.tehkode.utils.FieldReplacer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author zml2008
 */
public class PermissionList extends HashMap<String, Permission> {
	private static FieldReplacer<PluginManager, Map> INJECTOR;
	/**
	 * k = child permission
	 * v.k = parent permission
	 * v.v = value parent gives child
	 */
	private final Multimap<String, Map.Entry<String, Boolean>> childParentMapping = HashMultimap.create();

	public PermissionList() {
		super();
	}

	public PermissionList(Map<? extends String, ? extends Permission> existing) {
		super(existing);

	}


	public static PermissionList inject(PluginManager manager) {
		if (INJECTOR == null) {
			INJECTOR = new FieldReplacer<PluginManager, Map>(manager.getClass(), "permissions", Map.class);
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
			childParentMapping.put(ent.getKey(), new SimpleEntry<String, Boolean>(v.getName(), ent.getValue()));
		}
		return super.put(k, v);
	}

	@Override
	public Permission remove(Object k) {
		for (Iterator<Map.Entry<String, Map.Entry<String, Boolean>>> it = childParentMapping.entries().iterator(); it.hasNext();) {
			if (it.next().getValue().getKey().equals(k)) {
				it.remove();
			}
		}
		return super.remove(k);
	}

	@Override
	public void clear() {
		childParentMapping.clear();
		super.clear();
	}

	public Collection<Map.Entry<String, Boolean>> getParents(String permission) {
		return childParentMapping.get(permission.toLowerCase());
	}
}
