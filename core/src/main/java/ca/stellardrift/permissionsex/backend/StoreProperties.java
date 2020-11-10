package ca.stellardrift.permissionsex.backend;

import ca.stellardrift.permissionsex.util.ImmutablesStyle;
import org.immutables.value.Value;

/**
 * Options to configure a DataStore
 *
 * @param <C> Type of the store configuration objcet
 * @since 2.0.0
 */
@Value.Immutable(builder = false)
public interface StoreProperties<C> {

    static <C> StoreProperties<C> of(final String identifier, final C config, final DataStoreFactory factory) {
        return ImmutableStoreProperties.of(identifier, config, factory);
    }

    /**
     * Identifier for a single data store instance.
     *
     * @return store identifier
     * @since 2.0.0
     */
    @Value.Parameter
    String identifier();

    /**
     * The object holding data store configuration.
     *
     * @return store configuration
     * @since 2.0.0
     */
    @Value.Parameter
    C config();

    /**
     * Factory for the type of data store used.
     *
     * @return factory instance
     * @since 2.0.0
     */
    @Value.Parameter
    DataStoreFactory factory();

}
