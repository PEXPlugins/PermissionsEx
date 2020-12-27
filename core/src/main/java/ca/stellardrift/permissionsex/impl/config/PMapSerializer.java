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
package ca.stellardrift.permissionsex.impl.config;

import ca.stellardrift.permissionsex.impl.util.PCollections;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.pcollections.PMap;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

// Lifted almost entirely from Configurate's implementation of MapSerializer
// with slight modifications for persistent collections
final class PMapSerializer implements TypeSerializer<PMap<?, ?>> {
    public static final TypeToken<PMap<?, ?>> TYPE = new TypeToken<PMap<?, ?>>() {};
    public static final PMapSerializer INSTANCE = new PMapSerializer();

    private PMapSerializer() {
    }

    @Override
    public PMap<?, ?> deserialize(final Type type, final ConfigurationNode node) throws SerializationException {
        PMap<Object, Object> ret = PCollections.map();
        if (node.isMap()) {
            if (!(type instanceof ParameterizedType)) {
                throw new SerializationException(type, "Raw types are not supported for collections");
            }
            final ParameterizedType param = (ParameterizedType) type;
            if (param.getActualTypeArguments().length != 2) {
                throw new SerializationException(type, "Map expected two type arguments!");
            }
            final Type key = param.getActualTypeArguments()[0];
            final Type value = param.getActualTypeArguments()[1];
            final @Nullable TypeSerializer<?> keySerial = node.options().serializers().get(key);
            final @Nullable TypeSerializer<?> valueSerial = node.options().serializers().get(value);

            if (keySerial == null) {
                throw new SerializationException(type, "No type serializer available for key type " + key);
            }

            if (valueSerial == null) {
                throw new SerializationException(type, "No type serializer available for value type " + value);
            }

            final BasicConfigurationNode keyNode = BasicConfigurationNode.root(node.options());

            for (Map.Entry<Object, ? extends ConfigurationNode> ent : node.childrenMap().entrySet()) {
                ret = ret.plus(requireNonNull(keySerial.deserialize(key, keyNode.set(ent.getKey())), "key"),
                        requireNonNull(valueSerial.deserialize(value, ent.getValue()), "value"));
            }
        }
        return ret;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void serialize(final Type type, final @Nullable PMap<?, ?> obj, final ConfigurationNode node) throws SerializationException {
        if (!(type instanceof ParameterizedType)) {
            throw new SerializationException(type, "Raw types are not supported for collections");
        }
        final ParameterizedType param = (ParameterizedType) type;
        if (param.getActualTypeArguments().length != 2) {
            throw new SerializationException(type, "Map expected two type arguments!");
        }
        final Type key = param.getActualTypeArguments()[0];
        final Type value = param.getActualTypeArguments()[1];
        final @Nullable TypeSerializer keySerial = node.options().serializers().get(key);
        final @Nullable TypeSerializer valueSerial = node.options().serializers().get(value);

        if (keySerial == null) {
            throw new SerializationException(type, "No type serializer available for key type " + key);
        }

        if (valueSerial == null) {
            throw new SerializationException(type, "No type serializer available for value type " + value);
        }

        if (obj == null || obj.isEmpty()) {
            node.set(Collections.emptyMap());
        } else {
            final Set<Object> unvisitedKeys;
            if (node.empty()) {
                node.raw(Collections.emptyMap());
                unvisitedKeys = Collections.emptySet();
            } else {
                unvisitedKeys = new HashSet<>(node.childrenMap().keySet());
            }
            final BasicConfigurationNode keyNode = BasicConfigurationNode.root(node.options());
            for (Map.Entry<?, ?> ent : obj.entrySet()) {
                keySerial.serialize(key, ent.getKey(), keyNode);
                final Object keyObj = requireNonNull(keyNode.raw(), "Key must not be null!");
                final ConfigurationNode child = node.node(keyObj);
                try {
                    valueSerial.serialize(value, ent.getValue(), child);
                } catch (final SerializationException ex) {
                    ex.initPath(child::path);
                } finally {
                    unvisitedKeys.remove(keyObj);
                }
            }

            for (Object unusedChild : unvisitedKeys) {
                node.removeChild(unusedChild);
            }
        }
    }

    @Override
    public @Nullable PMap<?, ?> emptyValue(Type specificType, ConfigurationOptions options) {
        return PCollections.map();
    }
}
