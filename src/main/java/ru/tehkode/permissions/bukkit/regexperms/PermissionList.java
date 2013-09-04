package ru.tehkode.permissions.bukkit.regexperms;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
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

    private static final Map<Class<?>, FieldReplacer<Permission, Map>> CHILDREN_MAPS = new HashMap<Class<?>, FieldReplacer<Permission, Map>>();
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

    private FieldReplacer<Permission, Map> getFieldReplacer(Permission perm) {
        FieldReplacer<Permission, Map> ret = CHILDREN_MAPS.get(perm.getClass());
        if (ret == null) {
            ret = new FieldReplacer<Permission, Map>(perm.getClass(), "children", Map.class);
            CHILDREN_MAPS.put(perm.getClass(), ret);
        }
        return ret;
    }

    private void injectPermission(Permission p) {

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
                if (it.next().getKey().equals(child)) {
                    it.remove();
                }
            }
        }

        @Override
        public Boolean put(String perm, Boolean val) {
            removeFromMapping(perm);
            childParentMapping.put(perm, new SimpleEntry<String, Boolean>(this.perm.getName(), val));
            return super.put(perm, val);
        }
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
        FieldReplacer<Permission, Map> repl = getFieldReplacer(v);
        repl.set(v, new NotifyingChildrenMap(v));
        injectPermission(v);
		return super.put(k, v);
	}

	@Override
	public Permission remove(Object k) {
		for (Iterator<Map.Entry<String, Map.Entry<String, Boolean>>> it = childParentMapping.entries().iterator(); it.hasNext();) {
			if (it.next().getValue().getKey().equals(k)) {
				it.remove();
			}
		}
		Permission ret = super.remove(k);
        if (ret != null) {
            getFieldReplacer(ret).set(ret, new LinkedHashMap<String, Boolean>(ret.getChildren()));
        }
        return ret;
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
