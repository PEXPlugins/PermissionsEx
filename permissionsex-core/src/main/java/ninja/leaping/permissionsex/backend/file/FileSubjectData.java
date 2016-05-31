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
package ninja.leaping.permissionsex.backend.file;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.permissionsex.backend.memory.MemorySegment;
import ninja.leaping.permissionsex.backend.memory.MemorySubjectData;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Map.Entry;
import static ninja.leaping.permissionsex.util.Translations.t;

public final class FileSubjectData extends MemorySubjectData {
    static final String KEY_CONTEXTS = "contexts";

    static FileSubjectData fromNode(ConfigurationNode node) throws ObjectMappingException, PermissionsLoadingException {
        ImmutableMap.Builder<Set<Entry<String, String>>, ContextSegments> map = ImmutableMap.builder();
        if (node.hasListChildren()) {
            for (ConfigurationNode child : node.getChildrenList()) {
                if (!child.hasMapChildren()) {
                    throw new PermissionsLoadingException(t("Each context section must be of map type! Check that no duplicate nesting has occurred."));
                }
                MemorySegment value = MAPPER.bindToNew().populate(child);
                map.put(value.getContexts(), value);
            }
        }
        return new FileSubjectData(map.build());
    }

    protected FileSubjectData() {
        super();
    }

    protected FileSubjectData(Map<Set<Entry<String, String>>, ContextSegments> contexts) {
        super(contexts);
    }

    @Override
    protected MemorySubjectData newData(Map<Set<Entry<String, String>>, ContextSegments> contexts) {
        return new FileSubjectData(contexts);
    }

    private static Set<Entry<String, String>> contextsFrom(ConfigurationNode node) {
        Set<Entry<String, String>> contexts = Collections.emptySet();
        ConfigurationNode contextsNode = node.getNode(KEY_CONTEXTS);
        if (contextsNode.hasMapChildren()) {
            contexts = ImmutableSet.copyOf(Collections2.transform(contextsNode.getChildrenMap().entrySet(), ent -> {
                    return Maps.immutableEntry(ent.getKey().toString(), String.valueOf(ent.getValue().getValue()));
            }));
        }
        return contexts;
    }

    void serialize(ConfigurationNode node) throws ObjectMappingException {
        if (!node.hasListChildren()) {
            node.setValue(null);
        }
        Map<Set<Entry<String, String>>, ConfigurationNode> existingSections = new HashMap<>();
        for (ConfigurationNode child : node.getChildrenList()) {
            existingSections.put(contextsFrom(child), child);
        }
        for (Map.Entry<Set<Entry<String, String>>, ContextSegments> ent : contexts.entrySet()) {
            ConfigurationNode contextSection = existingSections.remove(ent.getKey());
            if (contextSection == null) {
                contextSection = node.getAppendedNode();
                ConfigurationNode contextsNode = contextSection.getNode(KEY_CONTEXTS);
                for (Entry<String, String> context : ent.getKey()) {
                    contextsNode.getNode(context.getKey()).setValue(context.getValue());
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
