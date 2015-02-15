package ninja.leaping.permissionsex.util;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.InvalidTypeException;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import ninja.leaping.permissionsex.backends.DataStore;
import ninja.leaping.permissionsex.backends.DataStoreFactories;
import ninja.leaping.permissionsex.backends.DataStoreFactory;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;

public class DataStoreSerializer implements TypeSerializer {
    private static final TypeToken<DataStore> DATA_STORE_TYPE = TypeToken.of(DataStore.class);

    @Override
    public boolean isApplicable(TypeToken<?> type) {
        return DATA_STORE_TYPE.isAssignableFrom(type);
    }

    @Override
    public Object deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
        if (!isApplicable(type)) {
            throw new InvalidTypeException(type);
        }
        String dataStoreType = value.getNode("type").getString(value.getKey().toString());
        Optional<DataStoreFactory> factory = DataStoreFactories.get(dataStoreType);
        if (!factory.isPresent()) {
            throw new ObjectMappingException("Unknown DataStore type " + dataStoreType);
        }
        try {
            return factory.get().createDataStore(value.getKey().toString(), value);
        } catch (PermissionsLoadingException e) {
            throw new ObjectMappingException(e);
        }
    }

    @Override
    public void serialize(TypeToken<?> type, Object obj, ConfigurationNode value) throws ObjectMappingException {
        if (!isApplicable(type)) {
            throw new InvalidTypeException(type);
        }
        if (!(obj instanceof DataStore)) {
            throw new ObjectMappingException("Object provided to serializer was a " + (obj == null ? null : obj.getClass()) + "; expected a DataStore");
        }
        try {
            value.getNode("type").setValue(((DataStore) obj).serialize(value));
        } catch (PermissionsLoadingException e) {
            throw new ObjectMappingException(e);
        }

    }
}
