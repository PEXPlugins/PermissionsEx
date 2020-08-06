/*
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

package ca.stellardrift.permissionsex.config;

import ca.stellardrift.permissionsex.backend.DataStore;
import ca.stellardrift.permissionsex.backend.DataStoreFactories;
import ca.stellardrift.permissionsex.backend.DataStoreFactory;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;

import java.util.Optional;

public class DataStoreSerializer implements TypeSerializer<DataStore> {
    @Override
    public DataStore deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
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
    public void serialize(TypeToken<?> type, DataStore obj, ConfigurationNode value) throws ObjectMappingException {
        try {
            value.getNode("type").setValue(obj.serialize(value));
        } catch (PermissionsLoadingException e) {
            throw new ObjectMappingException(e);
        }

    }
}
