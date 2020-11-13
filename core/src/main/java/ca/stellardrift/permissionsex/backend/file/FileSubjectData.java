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

package ca.stellardrift.permissionsex.backend.file;

import ca.stellardrift.permissionsex.backend.Messages;
import ca.stellardrift.permissionsex.backend.memory.MemorySubjectData;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class FileSubjectData extends MemorySubjectData {
    static final String KEY_CONTEXTS = "contexts";

    static FileSubjectData fromNode(ConfigurationNode node) throws SerializationException, PermissionsLoadingException {
        Map<Set<ContextValue<?>>, DataEntry> map = new HashMap<>();
        if (node.isList()) {
            for (ConfigurationNode child : node.childrenList()) {
                if (!child.isMap()) {
                    throw new PermissionsLoadingException(Messages.FILE_LOAD_CONTEXT.toComponent());
                }
                Set<ContextValue<?>> contexts = contextsFrom(child);
                DataEntry value = MAPPER.load(child);
                map.put(contexts, value);
            }
        }
        return new FileSubjectData(Collections.unmodifiableMap(map));
    }

    FileSubjectData() {
        super();
    }

    FileSubjectData(Map<Set<ContextValue<?>>, DataEntry> contexts) {
        super(contexts);
    }

    @Override
    protected MemorySubjectData newData(Map<Set<ContextValue<?>>, DataEntry> contexts) {
        return new FileSubjectData(contexts);
    }

    private static Set<ContextValue<?>> contextsFrom(ConfigurationNode node) {
        Set<ContextValue<?>> contexts = Collections.emptySet();
        ConfigurationNode contextsNode = node.node(KEY_CONTEXTS);
        if (contextsNode.isMap()) {
            contexts = ImmutableSet.copyOf(Collections2.transform(contextsNode.childrenMap().entrySet(), ent -> {
                    return new ContextValue<>(ent.getKey().toString(), ent.getValue().getString());
            }));
        }
        return contexts;
    }

    void serialize(ConfigurationNode node) throws SerializationException {
        if (!node.isList()) {
            node.raw(null);
        }
        Map<Set<ContextValue<?>>, ConfigurationNode> existingSections = new HashMap<>();
        for (ConfigurationNode child : node.childrenList()) {
            existingSections.put(contextsFrom(child), child);
        }
        for (Map.Entry<Set<ContextValue<?>>, DataEntry> ent : contexts.entrySet()) {
            ConfigurationNode contextSection = existingSections.remove(ent.getKey());
            if (contextSection == null) {
                contextSection = node.appendListNode();
                ConfigurationNode contextsNode = contextSection.node(KEY_CONTEXTS);
                for (ContextValue<?> context : ent.getKey()) {
                    contextsNode.node(context.key()).set(context.rawValue());
                }
            }
            MAPPER.save(ent.getValue(), contextSection);
        }
        for (ConfigurationNode unused : existingSections.values()) {
            unused.raw(null);
        }
    }

    @Override
    public String toString() {
        return "FileOptionSubjectData{" +
                "contexts=" + contexts +
                '}';
    }
}
