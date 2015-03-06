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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import org.spongepowered.api.service.permission.context.Context;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FileOptionSubjectData implements ImmutableOptionSubjectData {
    private static final String KEY_CONTEXTS = "contexts";
    @ConfigSerializable
    static class DataEntry {
        private static final ObjectMapper<DataEntry> MAPPER;

        static {
            try {
                MAPPER = ObjectMapper.forClass(DataEntry.class);
            } catch (ObjectMappingException e) {
                throw new ExceptionInInitializerError(e); // This error indicates a programming issue
            }
        }

        @Setting private Map<String, Integer> permissions;
        @Setting private Map<String, String> options;
        @Setting private Map<String, List<String>> parents;
        @Setting("permissions-default") private int defaultValue;

        private DataEntry(Map<String, Integer> permissions, Map<String, String> options, Map<String, List<String>> parents, int defaultValue) {
            this.permissions = permissions;
            this.options = options;
            this.parents = parents;
            this.defaultValue = defaultValue;
        }

        private DataEntry() { // Objectmapper constructor
        }

        public DataEntry withOption(String key, String value) {
            return new DataEntry(permissions, ImmutableMap.<String, String>builder().putAll(options).put(key, value).build(), parents, defaultValue);
        }

        public DataEntry withoutOption(String key) {
            if (!options.containsKey(key)) {
                return this;
            }

            Map<String, String> newOptions = new HashMap<>(options);
            newOptions.remove(key);
            return new DataEntry(permissions, newOptions, parents, defaultValue);

        }

        public DataEntry withoutOptions() {
            return new DataEntry(permissions, ImmutableMap.<String, String>of(), parents, defaultValue);
        }

        public DataEntry withPermission(String permission, int value) {
            return new DataEntry(ImmutableMap.<String, Integer>builder().putAll(permissions).put(permission, value).build(), options, parents, defaultValue);

        }

        public DataEntry withoutPermission(String permission) {
            if (!permissions.containsKey(permission)) {
                return this;
            }

            Map<String, Integer> newPermissions = new HashMap<>(permissions);
            newPermissions.remove(permission);
            return new DataEntry(newPermissions, options, parents, defaultValue);
        }

        public DataEntry withoutPermissions() {
            return new DataEntry(ImmutableMap.<String, Integer>of(), options, parents, defaultValue);
        }

        public DataEntry withDefaultValue(int defaultValue) {
            return new DataEntry(permissions, options, parents, defaultValue);
        }

        public DataEntry withParents(String type, List<String> parents) {
            if (parents == null) {
                Map<String, List<String>> newParents = new HashMap<>(this.parents);
                newParents.remove(type);
                return new DataEntry(permissions, options, newParents, defaultValue);
            } else {
                return new DataEntry(permissions, options, ImmutableMap.<String, List<String>>builder().putAll(this.parents).put(type, parents).build(), defaultValue);
            }
        }

        public DataEntry withoutParents() {
            return new DataEntry(permissions, options, ImmutableMap.<String, List<String>>of(), defaultValue);
        }

        @Override
        public String toString() {
            return "DataEntry{" +
                    "permissions=" + permissions +
                    ", options=" + options +
                    ", parents=" + parents +
                    ", defaultValue=" + defaultValue +
                    '}';
        }
    }
    private final Map<Set<Context>, DataEntry> contexts;

    FileOptionSubjectData(ConfigurationNode node) throws ObjectMappingException {
        ImmutableMap.Builder<Set<Context>, DataEntry> map = ImmutableMap.builder();
        if (node.hasListChildren()) {
            for (ConfigurationNode child : node.getChildrenList()) {
                Set<Context> contexts = contextsFrom(child);
                DataEntry value = DataEntry.MAPPER.bindToNew().populate(child);
                map.put(contexts, value);
            }
        }
        this.contexts = map.build();
    }

    private FileOptionSubjectData(Map<Set<Context>, DataEntry> contexts) {
        this.contexts = contexts;
    }

    private DataEntry getDataEntryOrNew(Set<Context> contexts) {
        DataEntry res = this.contexts.get(contexts);
        if (res == null) {
            res = new DataEntry();
        }
        return res;
    }

    private Set<Context> contextsFrom(ConfigurationNode node) {
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

    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions() {
        return Maps.transformValues(contexts, new Function<DataEntry, Map<String, String>>() {
            @Nullable
            @Override
            public Map<String, String> apply(@Nullable DataEntry dataEntry) {
                return dataEntry.options;
            }
        });
    }

    @Override
    public Map<String, String> getOptions(Set<Context> contexts) {
        final DataEntry entry = this.contexts.get(contexts);
        return entry == null ? null : entry.options;
    }

    @Override
    public ImmutableOptionSubjectData setOption(Set<Context> contexts, String key, String value) {
        if (value == null) {
            return new FileOptionSubjectData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(contexts, getDataEntryOrNew(contexts).withoutOption(key)).build());
        } else {
            return new FileOptionSubjectData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(contexts, getDataEntryOrNew(contexts).withOption(key, value)).build());
        }
    }

    @Override
    public ImmutableOptionSubjectData clearOptions(Set<Context> contexts) {
        if (!this.contexts.containsKey(contexts)) {
            return this;
        }
        return new FileOptionSubjectData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(contexts, getDataEntryOrNew(contexts).withoutOptions()).build());
    }

    @Override
    public ImmutableOptionSubjectData clearOptions() {
        if (this.contexts.isEmpty()) {
            return this;
        }

        Map<Set<Context>, DataEntry> newValue = Maps.transformValues(this.contexts, new Function<DataEntry, DataEntry>() {
            @Nullable
            @Override
            public DataEntry apply(@Nullable DataEntry dataEntry) {
                return dataEntry.withoutOptions();
            }
        });
        return new FileOptionSubjectData(newValue);
    }

    @Override
    public Map<Set<Context>, Map<String, Integer>> getAllPermissions() {
        return Maps.transformValues(contexts, new Function<DataEntry, Map<String, Integer>>() {
            @Nullable
            @Override
            public Map<String, Integer> apply(@Nullable DataEntry dataEntry) {
                return dataEntry.permissions;
            }
        });
    }

    @Override
    public Map<String, Integer> getPermissions(Set<Context> set) {
        final DataEntry entry = this.contexts.get(set);
        return entry == null ? null : entry.permissions;
    }

    @Override
    public ImmutableOptionSubjectData setPermission(Set<Context> contexts, String permission, int value) {
        if (value == 0) {
            return new FileOptionSubjectData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(contexts, getDataEntryOrNew(contexts).withoutPermission(permission)).build());
        } else {
            return new FileOptionSubjectData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(contexts, getDataEntryOrNew(contexts).withPermission(permission, value)).build());
        }
    }

    @Override
    public ImmutableOptionSubjectData clearPermissions() {
        if (this.contexts.isEmpty()) {
            return this;
        }

        Map<Set<Context>, DataEntry> newValue = Maps.transformValues(this.contexts, new Function<DataEntry, DataEntry>() {
            @Nullable
            @Override
            public DataEntry apply(@Nullable DataEntry dataEntry) {
                return dataEntry.withoutPermissions();
            }
        });
        return new FileOptionSubjectData(newValue);
    }

    @Override
    public ImmutableOptionSubjectData clearPermissions(Set<Context> contexts) {
        if (!this.contexts.containsKey(contexts)) {
            return this;
        }
        return new FileOptionSubjectData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(contexts, getDataEntryOrNew(contexts).withoutPermissions()).build());

    }

    @Override
    public Map<Set<Context>, List<Map.Entry<String, String>>> getAllParents() {
        return Maps.transformValues(contexts, new Function<DataEntry, List<Map.Entry<String, String>>>() {
            @Nullable
            @Override
            public List<Map.Entry<String, String>> apply(@Nullable DataEntry dataEntry) {
                return unwrapNestedMap(dataEntry.parents);
            }
        });
    }

    private <K, V> List<Map.Entry<K, V>> unwrapNestedMap(Map<K, List<V>> map) {
        List<Map.Entry<K, V>> ret = new ArrayList<>();
        for (Map.Entry<K, List<V>> ent : map.entrySet()) {
            for (V val : ent.getValue()) {
                ret.add(Maps.immutableEntry(ent.getKey(), val));
            }
        }
        return ret;
    }

    @Override
    public List<Map.Entry<String, String>> getParents(Set<Context> contexts) {
        DataEntry ent = this.contexts.get(contexts);
        return ent == null ? null : unwrapNestedMap(ent.parents);
    }

    private <T> List<T> addToCopyOrNew(List<T> orig, T addItem) {
        ImmutableList.Builder<T> build = ImmutableList.builder();
        if (orig != null) {
            build.addAll(orig);
        }
        build.add(addItem);
        return build.build();
    }

    @Override
    public ImmutableOptionSubjectData addParent(Set<Context> contexts, String type, String ident) {
        DataEntry entry = getDataEntryOrNew(contexts);
        return new FileOptionSubjectData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(contexts, entry.withParents(type, addToCopyOrNew(entry.parents.get(type), ident))).build());
    }

    @Override
    public ImmutableOptionSubjectData removeParent(Set<Context> contexts, String type, String identifier) {
        DataEntry ent = this.contexts.get(contexts);
        if (ent == null) {
            return this;
        }

        List<String> oldParents = ent.parents.get(type), newParents;
        if (oldParents == null) {
            return this;
        }
        newParents = new ArrayList<>(oldParents);
        newParents.remove(identifier);
        if (newParents.isEmpty()) {
            newParents = null;
        }
        return new FileOptionSubjectData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(contexts, ent.withParents(type, newParents)).build());
    }

    @Override
    public ImmutableOptionSubjectData clearParents() {
        if (this.contexts.isEmpty()) {
            return this;
        }

        Map<Set<Context>, DataEntry> newValue = Maps.transformValues(this.contexts, new Function<DataEntry, DataEntry>() {
            @Nullable
            @Override
            public DataEntry apply(@Nullable DataEntry dataEntry) {
                return dataEntry.withoutParents();
            }
        });
        return new FileOptionSubjectData(newValue);
    }

    @Override
    public ImmutableOptionSubjectData clearParents(Set<Context> contexts) {
        if (!this.contexts.containsKey(contexts)) {
            return this;
        }
        return new FileOptionSubjectData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(contexts, getDataEntryOrNew(contexts).withoutParents()).build());
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
            DataEntry.MAPPER.bind(ent.getValue()).serialize(contextSection);
        }
        for (ConfigurationNode unused : existingSections.values()) {
            unused.setValue(null);
        }
    }

    public int getDefaultValue(Set<Context> contexts) {
        DataEntry ent = this.contexts.get(contexts);
        return ent == null ? 0 : ent.defaultValue;
    }

    public ImmutableOptionSubjectData setDefaultValue(Set<Context> contexts, int defaultValue) {
        return new FileOptionSubjectData(ImmutableMap.<Set<Context>, DataEntry>builder().putAll(this.contexts).put(contexts, getDataEntryOrNew(contexts).withDefaultValue(defaultValue)).build());
    }

    @Override
    public Iterable<Set<Context>> getActiveContexts() {
        return contexts.keySet();
    }

    @Override
    public String toString() {
        return "FileOptionSubjectData{" +
                "contexts=" + contexts +
                '}';
    }
}
