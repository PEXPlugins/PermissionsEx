package ru.tehkode.permissions.bukkit.superperms;

import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.PluginManager;
import ru.tehkode.permissions.PermissionCheckResult;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.BukkitPermissions;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PermissibleMap for the permissions subscriptions data in Bukkit's {@link PluginManager} so we can put in our own data too.
 */
public class PEXPermissionSubscriptionMap extends HashMap<String, Map<Permissible, Boolean>> {
    private static final Logger LOGGER = Logger.getLogger(PEXPermissionSubscriptionMap.class.getCanonicalName());
    private final BukkitPermissions superms;

    public PEXPermissionSubscriptionMap(BukkitPermissions superms, Map<String, Map<Permissible, Boolean>> backing) {
        super(backing);
        this.superms = superms;
    }

    /**
     * Inject a PEX permission subscription map into the provided plugin manager.
     * This allows some PEX functions to work with the plugin manager.
     *
     * @param manager The manager to inject into
     */
    @SuppressWarnings("unchecked")
    public static PEXPermissionSubscriptionMap inject(BukkitPermissions superms, PluginManager manager) {
        try {
            Field field = manager.getClass().getDeclaredField("permSubs");
            field.setAccessible(true);
            Map backing = (Map) field.get(manager);
            if (backing instanceof PEXPermissionSubscriptionMap) {
                return (PEXPermissionSubscriptionMap) backing;
            }
            PEXPermissionSubscriptionMap wrappedMap = new PEXPermissionSubscriptionMap(superms, backing);
            field.set(manager, wrappedMap);
            return wrappedMap;
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, "[PermissionsEx] Error injecting permissible map!", e);
            return null;
        } catch (NoSuchFieldException e) {
            LOGGER.severe("[PermissionsEx] No permission subscriptions field in plugin manager! " +
                    "Permission-based broadcasts will not work.");
            return null;
        }
    }

    /**
     * Uninject a PEX map from the provided plugin manager.
     *
     * @param manager The manager to uninject
     */
    public static void uninject(PluginManager manager) {
        try {
            Field field = manager.getClass().getDeclaredField("permSubs");
            field.setAccessible(true);
            Map backing = (Map) field.get(manager);
            if (backing instanceof PEXPermissionSubscriptionMap) {
                PEXPermissionSubscriptionMap wrappedMap = (PEXPermissionSubscriptionMap) backing;
                Map<String, Map<Permissible, Boolean>> unwrappedMap = new HashMap<String, Map<Permissible, Boolean>>(backing.size());
                for (Map.Entry<String, Map<Permissible, Boolean>> entry : wrappedMap.entrySet()) {
                    if (entry.getValue() instanceof PEXSubscriptionValueMap) {
                        unwrappedMap.put(entry.getKey(), ((PEXSubscriptionValueMap) entry.getValue()).backing);
                    }
                }
                field.set(manager, unwrappedMap);
            }
        } catch (IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, "[PermissionsEx] Error uninjecting permissible map!", e);
        } catch (NoSuchFieldException e) {
            // Wasn't injected anyway, no need to notify
        }
    }

    @Override
    public Map<Permissible, Boolean> get(Object key) {
        if (key == null) {
            return null;
        }

        Map<Permissible, Boolean> result = super.get(key);
        if (PermissionsEx.isAvailable()) {
            if (result == null) {
                result = new PEXSubscriptionValueMap((String) key, new WeakHashMap<Permissible, Boolean>());
                super.put((String) key, result);
            } else if (!(result instanceof PEXSubscriptionValueMap)) {
                result = new PEXSubscriptionValueMap((String) key, result);
                super.put((String) key, result);
            }
        }
        return result;
    }

    @Override
    public Map<Permissible, Boolean> put(String key, Map<Permissible, Boolean> value) {
        if (!(value instanceof PEXSubscriptionValueMap) && PermissionsEx.isAvailable()) {
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
            return backing.containsKey(key);
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
            if (key instanceof Player) {
                PermissionUser user = PermissionsEx.getUser((Player) key);
                if (user != null) {
                    String match = user.getMatchingExpression(permission, ((Player) key).getWorld().getName());
                    if (match != null) {
                        return user.explainExpression(match);
                    }
                }
            }
            return backing.get(key);
        }

        @Override
        public Set<Permissible> keySet() {
            Player[] players = superms.getPlugin().getServer().getOnlinePlayers();
            Set<Permissible> pexMatches = new HashSet<Permissible>(players.length);
            for (Player player : players) {
                PermissionUser user = PermissionsEx.getUser(player);
                if (user != null) {
                    String match = user.getMatchingExpression(permission, player.getWorld().getName());
                    if (match != null) {
                        pexMatches.add(player);
                    }
                }
            }
            return Sets.union(pexMatches, backing.keySet());
        }

        @Override
        public Collection<Boolean> values() {
            return backing.values();
        }

        @Override
        public Set<Entry<Permissible, Boolean>> entrySet() {
            return entrySet();
        }
    }
}
