package ninja.leaping.permissionsex.backends;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;

public interface DataStoreFactory {
    public DataStore createDataStore(String identifier, ConfigurationNode config) throws PermissionsLoadingException;
}
