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

import ca.stellardrift.permissionsex.datastore.DataStore;
import ca.stellardrift.permissionsex.datastore.DataStoreFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;

import java.lang.reflect.Type;

public class DataStoreSerializer implements TypeSerializer<DataStore> {
    @Override
    public DataStore deserialize(final Type type, final ConfigurationNode value) throws SerializationException {
        final String dataStoreType = value.node("type").getString(value.key().toString());
        final @Nullable DataStoreFactory factory = DataStoreFactory.forType(dataStoreType);
        if (factory == null) {
            throw new SerializationException("Unknown DataStore type " + dataStoreType);
        }
        try {
            return factory.create(value.key().toString(), value);
        } catch (PermissionsLoadingException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void serialize(final Type type, final DataStore store, final ConfigurationNode value) throws SerializationException {
        try {
            value.node("type").set(store.serialize(value));
        } catch (PermissionsLoadingException e) {
            throw new SerializationException(e);
        }

    }
}
