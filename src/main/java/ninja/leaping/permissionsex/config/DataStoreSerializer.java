/**
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ninja.leaping.permissionsex.config;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.InvalidTypeException;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import ninja.leaping.permissionsex.backend.DataStore;
import ninja.leaping.permissionsex.backend.DataStoreFactories;
import ninja.leaping.permissionsex.backend.DataStoreFactory;
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
