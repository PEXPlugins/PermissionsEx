package ninja.leaping.permissionsex.backends;

import com.google.common.base.Optional;
import ninja.leaping.permissionsex.backends.file.FileDataStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataStoreFactories {
    private static final Map<String, DataStoreFactory> REGISTRY = new ConcurrentHashMap<>();

    static {
        register("file", new FileDataStore.Factory());
    }

    private DataStoreFactories() {
    }

    public static void register(String type, DataStoreFactory factory) {
        REGISTRY.put(type, factory);
    }

    public static Optional<DataStoreFactory> get(String type) {
        return Optional.fromNullable(REGISTRY.get(type));
    }

}
