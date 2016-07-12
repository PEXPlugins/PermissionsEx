/**
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ninja.leaping.permissionsex.bukkit;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.util.Tristate;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

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
    private final PermissionsExPlugin plugin;

    public PermissionList(PermissionsExPlugin plugin) {
        super();
        this.plugin = plugin;
    }

    public PermissionList(Map<? extends String, ? extends Permission> existing, PermissionsExPlugin plugin) {
        super(existing);
        this.plugin = plugin;
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
        for (Iterator<Map.Entry<String, Map.Entry<String, Boolean>>> it = childParentMapping.entries().iterator(); it.hasNext(); ) {
            if (it.next().getValue().getKey().equals(perm)) {
                it.remove();
            }
        }
    }

    public void uninject() {
        INJECTOR.set(plugin.getServer().getPluginManager(), new HashMap<>(this));

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
            for (Iterator<Map.Entry<String, Boolean>> it = childParentMapping.get(child).iterator(); it.hasNext(); ) {
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


    public static PermissionList inject(PermissionsExPlugin manager) {
        if (INJECTOR == null) {
            INJECTOR = new FieldReplacer<>(manager.getServer().getPluginManager().getClass(), "permissions", Map.class);
        }
        Map existing = INJECTOR.get(manager.getServer().getPluginManager());
        @SuppressWarnings("unchecked")
        PermissionList list = new PermissionList(existing, manager);
        INJECTOR.set(manager.getServer().getPluginManager(), list);
        return list;
    }

    @Override
    public Permission put(String k, final Permission v) {
        for (Map.Entry<String, Boolean> ent : v.getChildren().entrySet()) {
            childParentMapping.put(ent.getKey(), new SimpleEntry<>(v.getName(), ent.getValue()));
        }
        FieldReplacer<Permission, Map> repl = getFieldReplacer(v);
        repl.set(v, new NotifyingChildrenMap(v));
        if (v.getDefault() == PermissionDefault.TRUE || v.getDefault() == PermissionDefault.FALSE) {
            plugin.getManager().getSubjects(PermissionsEx.SUBJECTS_DEFAULTS)
                    .transientData()
                    .update(PermissionsEx.SUBJECTS_USER, input -> input.updateSegment(PermissionsEx.GLOBAL_CONTEXT, seg -> seg.withPermission(v.getName(), v.getDefault() == PermissionDefault.TRUE ? Tristate.TRUE : Tristate.FALSE)));
        }
        return super.put(k, v);
    }

    @Override
    public Permission remove(Object k) {
        final Permission ret = super.remove(k);
        if (ret != null) {
            removeAllChildren(k.toString());
            getFieldReplacer(ret).set(ret, new LinkedHashMap<>(ret.getChildren()));
            if (ret.getDefault() == PermissionDefault.TRUE || ret.getDefault() == PermissionDefault.FALSE) {
                plugin.getManager().getSubjects(PermissionsEx.SUBJECTS_DEFAULTS)
                        .transientData()
                        .update(PermissionsEx.SUBJECTS_USER, input -> input.updateSegment(PermissionsEx.GLOBAL_CONTEXT, seg -> seg.withoutPermission(ret.getName())));
            }
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
