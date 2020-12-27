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
package ca.stellardrift.permissionsex.impl.backend.file;

import ca.stellardrift.permissionsex.impl.backend.memory.MemorySubjectData;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import org.pcollections.PMap;
import org.pcollections.PSet;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class FileSubjectData extends MemorySubjectData {
    static final String KEY_CONTEXTS = "contexts";

    static FileSubjectData fromNode(ConfigurationNode node) throws SerializationException, PermissionsLoadingException {
        PMap<PSet<ContextValue<?>>, MemorySegment> map = PCollections.map();
        if (node.isList()) {
            for (ConfigurationNode child : node.childrenList()) {
                if (!child.isMap()) {
                    throw new PermissionsLoadingException(Messages.FILE_LOAD_CONTEXT.tr());
                }
                final PSet<ContextValue<?>> contexts = contextsFrom(child);
                MemorySegment value = MAPPER.load(child);
                map = map.plus(contexts, value);
            }
        }
        return new FileSubjectData(map);
    }

    FileSubjectData() {
        super();
    }

    FileSubjectData(final PMap<PSet<ContextValue<?>>, MemorySegment> contexts) {
        super(contexts);
    }

    @Override
    protected MemorySubjectData newData(final PMap<PSet<ContextValue<?>>, MemorySegment> contexts) {
        return new FileSubjectData(contexts);
    }

    private static PSet<ContextValue<?>> contextsFrom(final ConfigurationNode node) {
        PSet<ContextValue<?>> contexts = PCollections.set();
        final ConfigurationNode contextsNode = node.node(KEY_CONTEXTS);
        if (contextsNode.isMap()) {
            contexts = contextsNode.childrenMap().entrySet().stream()
                    .map(ent -> new ContextValue<>(ent.getKey().toString(), ent.getValue().getString()))
                    .collect(PCollections.toPSet());
        }
        return contexts;
    }

    void serialize(final ConfigurationNode node) throws SerializationException {
        if (!node.isList()) {
            node.raw(null);
        }
        final Map<Set<ContextValue<?>>, ConfigurationNode> existingSections = new HashMap<>();
        for (final ConfigurationNode child : node.childrenList()) {
            existingSections.put(contextsFrom(child), child);
        }
        for (final Map.Entry<PSet<ContextValue<?>>, MemorySegment> ent : this.segments.entrySet()) {
            ConfigurationNode contextSection = existingSections.remove(ent.getKey());
            if (contextSection == null) {
                contextSection = node.appendListNode();
                final ConfigurationNode contextsNode = contextSection.node(KEY_CONTEXTS);
                for (final ContextValue<?> context : ent.getKey()) {
                    contextsNode.node(context.key()).set(context.rawValue());
                }
            }
            MAPPER.save(ent.getValue(), contextSection);
        }
        for (final ConfigurationNode unused : existingSections.values()) {
            unused.raw(null);
        }
    }

    @Override
    public String toString() {
        return "FileSubjectData{" +
                "segments=" + segments +
                '}';
    }
}
