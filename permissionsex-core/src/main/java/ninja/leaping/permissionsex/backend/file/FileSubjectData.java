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
import ninja.leaping.permissionsex.data.SegmentKey;
import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Map.Entry;
import static ninja.leaping.permissionsex.util.Translations.t;

public final class FileSubjectData extends MemorySubjectData {
    static final String KEY_CONTEXTS = "contexts";
    static final String KEY_WEIGHT = "weight";
    static final String KEY_INHERITABLE = "inheritable";

    static FileSubjectData fromNode(SubjectRef ref, ConfigurationNode node) throws ObjectMappingException, PermissionsLoadingException {
        ImmutableMap.Builder<SegmentKey, MemorySegment> map = ImmutableMap.builder();
        if (node.hasListChildren()) {
            for (ConfigurationNode child : node.getChildrenList()) {
                if (!child.hasMapChildren()) {
                    throw new PermissionsLoadingException(t("Each context section must be of map type! Check that no duplicate nesting has occurred."));
                }
                SegmentKey key = keyFrom(child);
                MemorySegment segment = new MemorySegment(key);
                MAPPER.bind(segment).populate(child);
                map.put(key, segment);
            }
        }
        return new FileSubjectData(ref, map.build());
    }

    protected FileSubjectData(SubjectRef ref) {
        super(ref);
    }

    protected FileSubjectData(SubjectRef ref, Map<SegmentKey, MemorySegment> segments) {
        super(ref, segments);
    }

    @Override
    protected MemorySubjectData newData(Map<SegmentKey, MemorySegment> segments) {
        return new FileSubjectData(getReference(), segments);
    }

    static SegmentKey keyFrom(ConfigurationNode node) {
        Set<Entry<String, String>> contexts = Collections.emptySet();
        ConfigurationNode contextsNode = node.getNode(KEY_CONTEXTS);
        if (contextsNode.hasMapChildren()) {
            contexts = ImmutableSet.copyOf(Collections2.transform(contextsNode.getChildrenMap().entrySet(), ent -> {
                return Maps.immutableEntry(ent.getKey().toString(), String.valueOf(ent.getValue().getValue()));
            }));
        }
        int weight = node.getNode(KEY_WEIGHT).getInt(SegmentKey.DEFAULT_WEIGHT);
        boolean inheritable = node.getNode(KEY_INHERITABLE).getBoolean(SegmentKey.DEFAULT_INHERITABILITY);
        return SegmentKey.of(contexts, weight, inheritable);

    }

    static void keyTo(SegmentKey key, ConfigurationNode node) {
        ConfigurationNode contextsNode = node.getNode(KEY_CONTEXTS),
                weightNode = node.getNode(KEY_WEIGHT),
                inheritableNode = node.getNode(KEY_INHERITABLE);
        contextsNode.setValue(null);
        for (Entry<String, String> context : key.getContexts()) {
            contextsNode.getNode(context.getKey()).setValue(context.getValue());
        }
        weightNode.setValue(null);
        if (key.getWeight() != SegmentKey.DEFAULT_WEIGHT) {
            weightNode.setValue(key.getWeight());
        }
        inheritableNode.setValue(null);
        if (key.isInheritable() != SegmentKey.DEFAULT_INHERITABILITY) {
            inheritableNode.setValue(key.isInheritable());
        }
    }

    void serialize(ConfigurationNode node) throws ObjectMappingException {
        if (!node.hasListChildren()) {
            node.setValue(null);
        }
        Map<SegmentKey, ConfigurationNode> existingSections = new HashMap<>();
        for (ConfigurationNode child : node.getChildrenList()) {
            existingSections.put(keyFrom(child), child);
        }
        for (Map.Entry<SegmentKey, MemorySegment> ent : segments.entrySet()) {
            ConfigurationNode contextSection = existingSections.remove(ent.getKey());
            if (contextSection == null) {
                contextSection = node.getAppendedNode();
                keyTo(ent.getKey(), contextSection);
            }
            MAPPER.bind(ent.getValue()).serialize(contextSection);
        }
        for (ConfigurationNode unused : existingSections.values()) {
            unused.setValue(null);
        }
    }

    @Override
    public String toString() {
        return "FileSubjectData{" +
                "segments=" + segments +
                '}';
    }
}
