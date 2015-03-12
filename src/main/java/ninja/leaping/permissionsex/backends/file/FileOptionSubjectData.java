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
package ninja.leaping.permissionsex.backends.file;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.permissionsex.backends.memory.MemoryOptionSubjectData;
import org.spongepowered.api.service.permission.context.Context;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FileOptionSubjectData extends MemoryOptionSubjectData {
    private static final String KEY_CONTEXTS = "context";

    static FileOptionSubjectData fromNode(ConfigurationNode node) throws ObjectMappingException {
        ImmutableMap.Builder<Set<Context>, DataEntry> map = ImmutableMap.builder();
        if (node.hasListChildren()) {
            for (ConfigurationNode child : node.getChildrenList()) {
                Set<Context> contexts = contextsFrom(child);
                DataEntry value = MAPPER.bindToNew().populate(child);
                map.put(contexts, value);
            }
        }
        return new FileOptionSubjectData(map.build());
    }

    protected FileOptionSubjectData(Map<Set<Context>, DataEntry> contexts) {
        super(contexts);
    }

    protected MemoryOptionSubjectData newData(Map<Set<Context>, DataEntry> contexts) {
        return new FileOptionSubjectData(contexts);
    }

    private static Set<Context> contextsFrom(ConfigurationNode node) {
        Set<Context> contexts = Collections.emptySet();
        ConfigurationNode contextsNode = node.getNode(KEY_CONTEXTS);
        if (contextsNode.hasMapChildren()) {
            contexts = ImmutableSet.copyOf(Collections2.transform(contextsNode.getChildrenMap().entrySet(), new Function<Map.Entry<Object, ? extends ConfigurationNode>, Context>() {
                @Nullable
                @Override
                public Context apply(Map.Entry<Object, ? extends ConfigurationNode> ent) {
                    return new Context(ent.getKey().toString(), ent.getValue().getString());
                }
            }));
        }
        return contexts;
    }

    void serialize(ConfigurationNode node) throws ObjectMappingException {
        if (!node.hasListChildren()) {
            node.setValue(null);
        }
        Map<Set<Context>, ConfigurationNode> existingSections = new HashMap<>();
        for (ConfigurationNode child : node.getChildrenList()) {
            existingSections.put(contextsFrom(child), child);
        }
        for (Map.Entry<Set<Context>, DataEntry> ent : contexts.entrySet()) {
            ConfigurationNode contextSection = existingSections.remove(ent.getKey());
            if (contextSection == null) {
                contextSection = node.getAppendedNode();
                ConfigurationNode contextsNode = contextSection.getNode(KEY_CONTEXTS);
                for (Context context : ent.getKey()) {
                    contextsNode.getNode(context.getType()).setValue(context.getName());
                }
            }
            MAPPER.bind(ent.getValue()).serialize(contextSection);
        }
        for (ConfigurationNode unused : existingSections.values()) {
            unused.setValue(null);
        }
    }

    @Override
    public String toString() {
        return "FileOptionSubjectData{" +
                "contexts=" + contexts +
                '}';
    }
}
