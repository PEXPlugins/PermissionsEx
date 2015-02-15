package ninja.leaping.permissionsex.backends;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import ninja.leaping.permissionsex.sponge.option.OptionSubjectData;

/**
 * Data type abstraction for permissions data
 */
public interface DataStore {
    OptionSubjectData getData(String type, String identifier, Caching listener);

    boolean isRegistered(String type, String identifier);

    Iterable<OptionSubjectData> getAll(String type);

    /**
     * Return the type name for this data store
     *
     * @return The type name of this data store
     */
    public String getTypeName();

    /**
     * Serialize the configuration state of this data store to a configuration node
     *
     * @param node The node to serialize state to
     * @return The type name of this data store
     */
    public String serialize(ConfigurationNode node) throws PermissionsLoadingException;
}
