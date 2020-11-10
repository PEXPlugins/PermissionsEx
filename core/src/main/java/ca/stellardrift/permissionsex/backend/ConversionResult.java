package ca.stellardrift.permissionsex.backend;

import net.kyori.adventure.text.Component;
import org.immutables.value.Value;

/**
 * A possible result of a conversion lookup.
 *
 * @since 2.0.0
 */
@Value.Immutable
public interface ConversionResult {

    /**
     * Create a new builder for a conversion result.
     *
     * @return new builder
     * @since 2.0.0
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * A short description of the data that will be converted.
     *
     * @return conversion description
     * @since 2.0.0
     */
    Component description();

    /**
     * The data store, configured based on the discovered environment but not yet initialized.
     *
     * @return convertible data store
     * @since 2.0.0
     */
    DataStore store();

    /**
     * Builder for a conversion result.
     *
     * @since 2.0.0
     */
    class Builder extends ImmutableConversionResult.Builder {
        Builder() {}
    }

}
