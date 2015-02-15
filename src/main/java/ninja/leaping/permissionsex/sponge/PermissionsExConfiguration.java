package ninja.leaping.permissionsex.sponge;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.permissionsex.backends.DataStore;

import java.util.Map;

public class PermissionsExConfiguration {
    @Setting("backends") private Map<String, DataStore> backends;
    @Setting("default-backend") private String defaultBackend;
    @Setting("debug") private boolean debugEnabled;

    public DataStore getDataStore(String name) {
        return backends.get(name);
    }

    public DataStore getDefaultDataStore() {
        return backends.get(defaultBackend);
    }
}
