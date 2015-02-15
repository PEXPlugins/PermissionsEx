package ninja.leaping.permissionsex.data;

import org.spongepowered.api.service.permission.Subject;

public interface Caching {
    void clearCache();

    void clearNodeCache(String node);

    void clearInheritanceCache(Subject subject);
}
