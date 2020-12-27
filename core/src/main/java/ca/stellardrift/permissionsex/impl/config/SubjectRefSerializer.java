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

import ca.stellardrift.permissionsex.impl.PermissionsEx;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectType;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Serializer for {@link SubjectRef} instances.
 */
public final class SubjectRefSerializer implements TypeSerializer<SubjectRef<?>> {
    public static final TypeToken<SubjectRef<?>> TYPE = new TypeToken<SubjectRef<?>>() {};
    private final PermissionsEx<?> engine;
    private final @Nullable SubjectType<?> defaultType;

    public SubjectRefSerializer(final PermissionsEx<?> engine, final @Nullable SubjectType<?> defaultType) {
        this.engine = engine;
        this.defaultType = defaultType;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public SubjectRef<?> deserialize(final Type type, final ConfigurationNode node) throws SerializationException {
        final @Nullable String value = node.getString();
        if (value == null) {
            throw new SerializationException(node, type, "Node value was not a string");
        }

        final String[] entries = value.split(":", 2);
        if (entries.length == 1) {
            if (this.defaultType == null) {
                throw new SerializationException(node, type, "No default type was specified but subject '" + value + "' has no specified type!");
            }
            return SubjectRef.subject((SubjectType) this.defaultType, this.defaultType.parseIdentifier(entries[0]));
        } else {
            return this.engine.lazySubjectRef(entries[0], entries[1]);
        }
    }

    @Override
    public void serialize(final Type type, final @Nullable SubjectRef<?> obj, final ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.set(null);
        } else if (Objects.equals(obj.type(), this.defaultType)) {
            node.set(obj.serializedIdentifier());
        } else {
            node.set(obj.type().name() + ':' + obj.serializedIdentifier());
        }
    }
}
