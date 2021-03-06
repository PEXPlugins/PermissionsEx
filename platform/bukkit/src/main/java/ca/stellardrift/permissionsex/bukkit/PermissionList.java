/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2021 zml [at] stellardrift [dot] ca and PermissionsEx contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ca.stellardrift.permissionsex.bukkit;

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.legacy.LegacyConversions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import ca.stellardrift.permissionsex.impl.PermissionsEx;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Make Superperms' querying of all players with a given permission accurately follow PEX permission matching rules.
 */

@SuppressWarnings({"unchecked", "rawtypes"})
final class PermissionList extends HashMap<String, Permission> {
    private static final long serialVersionUID = 5211930944081173317L;

    private static @MonotonicNonNull FieldReplacer<PluginManager, Map> INJECTOR;

    private static final Map<Class<?>, FieldReplacer<Permission, Map>> CHILDREN_MAPS = new HashMap<>();
    /**
     * k = child permission
     * v.k = parent permission
     * v.v = value parent gives child
     */
    private final Multimap<String, Map.Entry<String, Boolean>> childParentMapping = Multimaps.synchronizedMultimap(HashMultimap.<String, Map.Entry<String, Boolean>>create());
    private final PermissionsExPlugin plugin;

    PermissionList(Map<? extends String, ? extends Permission> existing, PermissionsExPlugin plugin) {
        super(existing);
        this.plugin = plugin;
    }

    private FieldReplacer<Permission, Map> getFieldReplacer(final Permission perm) {
        FieldReplacer<Permission, Map> ret = CHILDREN_MAPS.get(perm.getClass());
        if (ret == null) {
            ret = new FieldReplacer<>(perm.getClass(), "children", Map.class);
            CHILDREN_MAPS.put(perm.getClass(), ret);
        }
        return ret;
    }

    private void removeAllChildren(final String perm) {
        this.childParentMapping.entries().removeIf(stringEntryEntry -> stringEntryEntry.getValue().getKey().equals(perm));
    }

    public void uninject() {
        INJECTOR.set(this.plugin.getServer().getPluginManager(), new HashMap<>(this));

    }

    private class NotifyingChildrenMap extends LinkedHashMap<String, Boolean> {
        private static final long serialVersionUID = -8012029306538729479L;

        private final Permission perm;

        public NotifyingChildrenMap(final Permission perm) {
            super(perm.getChildren());
            this.perm = perm;
        }

        @Override
        public Boolean remove(final Object perm) {
            removeFromMapping(String.valueOf(perm));
            return super.remove(perm);
        }

        private void removeFromMapping(final String child) {
            childParentMapping.get(child).removeIf(stringBooleanEntry -> stringBooleanEntry.getKey().equals(perm.getName()));
        }

        @Override
        public Boolean put(final String perm, final Boolean val) {
            //removeFromMapping(perm);
            PermissionList.this.childParentMapping.put(perm, new SimpleEntry<>(this.perm.getName(), val));
            return super.put(perm, val);
        }

        @Override
        public void clear() {
            removeAllChildren(this.perm.getName());
            super.clear();
        }
    }


    public static PermissionList inject(PermissionsExPlugin manager) {
        if (INJECTOR == null) {
            INJECTOR = new FieldReplacer<>(manager.getServer().getPluginManager().getClass(), "permissions", Map.class);
        }
        final Map existing = INJECTOR.get(manager.getServer().getPluginManager());
        final PermissionList list = new PermissionList(existing, manager);
        INJECTOR.set(manager.getServer().getPluginManager(), list);
        return list;
    }

    @Override
    public Permission put(String k, final Permission v) {
        for (final Map.Entry<String, Boolean> ent : v.getChildren().entrySet()) {
            childParentMapping.put(ent.getKey(), new SimpleEntry<>(v.getName(), ent.getValue()));
        }
        final FieldReplacer<Permission, Map> repl = getFieldReplacer(v);
        repl.set(v, new NotifyingChildrenMap(v));
        if (v.getDefault() == PermissionDefault.TRUE || v.getDefault() == PermissionDefault.FALSE) {
            this.plugin.engine().defaults()
                    .transientData()
                    .update(this.plugin.users().type(), input -> input.withSegment(PermissionsEx.GLOBAL_CONTEXT, s -> s.withPermission(v.getName(), v.getDefault() == PermissionDefault.TRUE ? 1 : -1)));
        }
        return super.put(k, v);
    }

    @Override
    public @Nullable Permission remove(final Object k) {
        final Permission ret = super.remove(k);
        if (ret != null) {
            removeAllChildren(k.toString());
            getFieldReplacer(ret).set(ret, new LinkedHashMap<>(ret.getChildren()));
            if (ret.getDefault() == PermissionDefault.TRUE || ret.getDefault() == PermissionDefault.FALSE) {
                this.plugin.engine().defaults()
                        .transientData()
                        .update(this.plugin.users().type(), input -> input.withSegment(PermissionsEx.GLOBAL_CONTEXT, s -> s.withPermission(ret.getName(), 0)));
            }
        }
        return ret;
    }

    @Override
    public void clear() {
        this.childParentMapping.clear();
        super.clear();
    }

    public Collection<Map.Entry<String, Boolean>> getParents(String permission) {
        return ImmutableSet.copyOf(this.childParentMapping.get(permission.toLowerCase()));
    }
}
