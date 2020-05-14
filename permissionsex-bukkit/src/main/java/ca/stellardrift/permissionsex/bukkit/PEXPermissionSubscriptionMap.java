/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2020 zml [at] stellardrift [dot] ca and PermissionsEx contributors
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

import com.google.common.collect.Sets;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.PluginManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * PermissibleMap for the permissions subscriptions data in Bukkit's {@link PluginManager} so we can put in our own data too.
 */
public class PEXPermissionSubscriptionMap extends HashMap<String, Map<Permissible, Boolean>> {
    private static FieldReplacer<PluginManager, Map> INJECTOR;
    private static final AtomicReference<PEXPermissionSubscriptionMap> INSTANCE = new AtomicReference<>();
    private final PermissionsExPlugin plugin;
    private final PluginManager manager;


    private PEXPermissionSubscriptionMap(PermissionsExPlugin plugin, PluginManager manager, Map<String, Map<Permissible, Boolean>> backing) {
        super(backing);
        this.plugin = plugin;
        this.manager = manager;
    }

    /**
     * Inject a PEX permission subscription map into the provided plugin manager.
     * This allows some PEX functions to work with the plugin manager.
     *
     * @param plugin plugin controlling
     * @param manager The manager to inject into
     * @return the injected map, or existing map if present
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static PEXPermissionSubscriptionMap inject(PermissionsExPlugin plugin, PluginManager manager) {
        PEXPermissionSubscriptionMap map = INSTANCE.get();
        if (map != null) {
            return map;
        }

        if (INJECTOR == null) {
            INJECTOR = new FieldReplacer<>(manager.getClass(), "permSubs", Map.class);
        }

        Map backing = INJECTOR.get(manager);
        if (backing instanceof PEXPermissionSubscriptionMap) {
            return (PEXPermissionSubscriptionMap) backing;
        }
        PEXPermissionSubscriptionMap wrappedMap = new PEXPermissionSubscriptionMap(plugin, manager, backing);
        if (INSTANCE.compareAndSet(null, wrappedMap)) {
            INJECTOR.set(manager, wrappedMap);
            return wrappedMap;
        } else {
            return INSTANCE.get();
        }
    }

    /**
     * Uninject this PEX map from its plugin manager
     */
    public void uninject() {
        if (INSTANCE.compareAndSet(this, null)) {
            Map<String, Map<Permissible, Boolean>> unwrappedMap = new HashMap<>(this.size());
            for (Map.Entry<String, Map<Permissible, Boolean>> entry : this.entrySet()) {
                if (entry.getValue() instanceof PEXSubscriptionValueMap) {
                    unwrappedMap.put(entry.getKey(), ((PEXSubscriptionValueMap) entry.getValue()).backing);
                }
            }
            INJECTOR.set(manager, unwrappedMap);
        }
    }

    @Override
    public Map<Permissible, Boolean> get(Object key) {
        if (key == null) {
            return null;
        }

        Map<Permissible, Boolean> result = super.get(key);
        if (result == null) {
            result = new PEXSubscriptionValueMap((String) key, new WeakHashMap<>());
            super.put((String) key, result);
        } else if (!(result instanceof PEXSubscriptionValueMap)) {
            result = new PEXSubscriptionValueMap((String) key, result);
            super.put((String) key, result);
        }
        return result;
    }

    @Override
    public Map<Permissible, Boolean> put(String key, Map<Permissible, Boolean> value) {
        if (!(value instanceof PEXSubscriptionValueMap)) {
            value = new PEXSubscriptionValueMap(key, value);
        }
        return super.put(key, value);
    }

    public class PEXSubscriptionValueMap implements Map<Permissible, Boolean> {
        private final String permission;
        private final Map<Permissible, Boolean> backing;

        public PEXSubscriptionValueMap(String permission, Map<Permissible, Boolean> backing) {
            this.permission = permission;
            this.backing = backing;
        }

        @Override
        public int size() {
            return backing.size();
        }

        @Override
        public boolean isEmpty() {
            return backing.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return backing.containsKey(key) || (key instanceof Permissible && ((Permissible) key).isPermissionSet(permission));
        }

        @Override
        public boolean containsValue(Object value) {
            return backing.containsValue(value);
        }

        @Override
        public Boolean put(Permissible key, Boolean value) {
            return backing.put(key, value);
        }

        @Override
        public Boolean remove(Object key) {
            return backing.remove(key);
        }

        @Override
        public void putAll(Map<? extends Permissible, ? extends Boolean> m) {
            backing.putAll(m);
        }

        @Override
        public void clear() {
            backing.clear();
        }

        @Override
        public Boolean get(Object key) {
            if (key instanceof Permissible) {
                Permissible p = (Permissible) key;
                if (p.isPermissionSet(permission)) {
                    return p.hasPermission(permission);
                }
            }
            return backing.get(key);
        }

        @Override
        public Set<Permissible> keySet() {
            Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();
            Set<Permissible> pexMatches = new HashSet<>(players.size());
            players.stream()
                    .filter(player -> player.hasPermission(permission))
                    .collect(Collectors.toCollection(() -> pexMatches));
            return Sets.union(pexMatches, backing.keySet());
        }

        @Override
        public Collection<Boolean> values() {
            return backing.values();
        }

        @Override
        public Set<Entry<Permissible, Boolean>> entrySet() {
            return backing.entrySet();
        }
    }
}
