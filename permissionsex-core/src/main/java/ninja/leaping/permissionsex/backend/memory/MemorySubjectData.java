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
package ninja.leaping.permissionsex.backend.memory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.util.Util;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Map.Entry;
import static ninja.leaping.permissionsex.util.Util.updateImmutable;

public class MemorySubjectData implements ImmutableSubjectData {
    protected static final ObjectMapper<DataEntry> MAPPER;
    static {
        try {
            MAPPER = ObjectMapper.forClass(DataEntry.class);
        } catch (ObjectMappingException e) {
            throw new ExceptionInInitializerError(e); // This debug indicates a programming issue
        }
    }

    @ConfigSerializable
    protected static class DataEntry {
        @Nullable @Setting private Map<String, Integer> permissions;
        @Nullable @Setting private Map<String, String> options;
        @Nullable @Setting private List<String> parents;
        @Nullable @Setting("permissions-default") private Integer defaultValue;

        private DataEntry(@Nullable Map<String, Integer> permissions, @Nullable Map<String, String> options, @Nullable List<String> parents, @Nullable Integer defaultValue) {
            this.permissions = permissions;
            this.options = options;
            this.parents = parents;
            this.defaultValue = defaultValue;
        }

        private DataEntry() { // Objectmapper constructor
        }

        public DataEntry withOption(String key, String value) {
            return new DataEntry(permissions, updateImmutable(options, key, value), parents, defaultValue);
        }

        public DataEntry withoutOption(String key) {
            if (options == null || !options.containsKey(key)) {
                return this;
            }

            Map<String, String> newOptions = new HashMap<>(options);
            newOptions.remove(key);
            return new DataEntry(permissions, newOptions, parents, defaultValue);

        }

        public DataEntry withOptions(Map<String, String> values) {
            return new DataEntry(permissions, values == null ? null : ImmutableMap.copyOf(values), parents, defaultValue);
        }

        public DataEntry withoutOptions() {
            return new DataEntry(permissions, null, parents, defaultValue);
        }

        public DataEntry withPermission(String permission, int value) {
            return new DataEntry(updateImmutable(permissions, permission, value), options, parents, defaultValue);

        }

        public DataEntry withoutPermission(String permission) {
            if (permissions == null || !permissions.containsKey(permission)) {
                return this;
            }

            Map<String, Integer> newPermissions = new HashMap<>(permissions);
            newPermissions.remove(permission);
            return new DataEntry(newPermissions, options, parents, defaultValue);
        }

        public DataEntry withPermissions(Map<String, Integer> values) {
            return new DataEntry(ImmutableMap.copyOf(values), options, parents, defaultValue);
        }

        public DataEntry withoutPermissions() {
            return new DataEntry(null, options, parents, defaultValue);
        }

        public DataEntry withDefaultValue(Integer defaultValue) {
            return new DataEntry(permissions, options, parents, defaultValue);
        }

        public DataEntry withAddedParent(String parent) {
            ImmutableList.Builder<String> parents = ImmutableList.builder();
            parents.add(parent);
            if (this.parents != null) {
                parents.addAll(this.parents);
            }
            return new DataEntry(permissions, options, parents.build(), defaultValue);
        }

        public DataEntry withRemovedParent(String parent) {
            if (this.parents == null || this.parents.isEmpty()) {
                return this;
            }

            final List<String> newParents = new ArrayList<>(parents);
            newParents.remove(parent);
            return new DataEntry(permissions, options, newParents, defaultValue);
        }

        public DataEntry withParents(List<String> transform) {
            return new DataEntry(permissions, options, transform == null ? null : ImmutableList.copyOf(transform), defaultValue);
        }

        public DataEntry withoutParents() {
            return new DataEntry(permissions, options, null, defaultValue);
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

        public boolean isEmpty() {
            return (this.permissions == null || this.permissions.isEmpty())
                    && (this.options == null || this.options.isEmpty())
                    && (this.parents == null || this.parents.isEmpty())
                    && this.defaultValue == null;
        }
    }

    protected final MemorySubjectData newWithUpdated(Set<Entry<String, String>> key, DataEntry val) {
        if (val.isEmpty()) {
            val = null;
        }
        return newData(updateImmutable(contexts, immutSet(key), val));
    }

    protected MemorySubjectData newData(Map<Set<Entry<String, String>>, DataEntry> contexts) {
        return new MemorySubjectData(contexts);
    }

    protected final Map<Set<Entry<String, String>>, DataEntry> contexts;

    protected MemorySubjectData() {
        this.contexts = ImmutableMap.of();
    }

    protected MemorySubjectData(Map<Set<Entry<String, String>>, DataEntry> contexts) {
        this.contexts = contexts;
    }

    private DataEntry getDataEntryOrNew(Set<Entry<String, String>> contexts) {
        DataEntry res = this.contexts.get(contexts);
        if (res == null) {
            res = new DataEntry();
        }
        return res;
    }

    private <E> ImmutableSet<E> immutSet(Set<E> set) {
        return ImmutableSet.copyOf(set);
    }

    @Override
    public Map<Set<Entry<String, String>>, Map<String, String>> getAllOptions() {
        return Maps.filterValues(Maps.transformValues(contexts,
                dataEntry -> dataEntry == null ? null : dataEntry.options), el -> el != null);
    }

    @Override
    public Map<String, String> getOptions(Set<Entry<String, String>> contexts) {
        final DataEntry entry = this.contexts.get(contexts);
        return entry == null || entry.options == null ? Collections.<String, String>emptyMap() : entry.options;
    }

    @Override
    public ImmutableSubjectData setOption(Set<Entry<String, String>> contexts, String key, String value) {
        if (value == null) {
            return newWithUpdated(contexts, getDataEntryOrNew(contexts).withoutOption(key));
        } else {
            return newWithUpdated(contexts, getDataEntryOrNew(contexts).withOption(key, value));
        }
    }

    @Override
    public ImmutableSubjectData setOptions(Set<Entry<String, String>> contexts, Map<String, String> values) {
        return newWithUpdated(contexts, getDataEntryOrNew(contexts).withOptions(values));
    }

    @Override
    public ImmutableSubjectData clearOptions(Set<Entry<String, String>> contexts) {
        if (!this.contexts.containsKey(contexts)) {
            return this;
        }
        return newWithUpdated(contexts, getDataEntryOrNew(contexts).withoutOptions());
    }

    @Override
    public ImmutableSubjectData clearOptions() {
        if (this.contexts.isEmpty()) {
            return this;
        }

        Map<Set<Entry<String, String>>, DataEntry> newValue = Maps.transformValues(this.contexts,
                dataEntry -> dataEntry == null ? null : dataEntry.withoutOptions());
        return newData(newValue);
    }

    @Override
    public Map<Set<Entry<String, String>>, Map<String, Integer>> getAllPermissions() {
        return Maps.filterValues(Maps.transformValues(contexts,
                dataEntry -> dataEntry == null ? null : dataEntry.permissions), o -> o != null);
    }

    @Override
    public Map<String, Integer> getPermissions(Set<Entry<String, String>> set) {
        final DataEntry entry = this.contexts.get(set);
        return entry == null || entry.permissions == null ? Collections.<String, Integer>emptyMap() : entry.permissions;
    }

    @Override
    public ImmutableSubjectData setPermission(Set<Entry<String, String>> contexts, String permission, int value) {
        if (value == 0) {
            return newWithUpdated(contexts, getDataEntryOrNew(contexts).withoutPermission(permission));
        } else {
            return newWithUpdated(contexts, getDataEntryOrNew(contexts).withPermission(permission, value));
        }
    }

    @Override
    public ImmutableSubjectData setPermissions(Set<Entry<String, String>> contexts, Map<String, Integer> values) {
        return newWithUpdated(contexts, getDataEntryOrNew(contexts).withPermissions(values));
    }

    @Override
    public ImmutableSubjectData clearPermissions() {
        if (this.contexts.isEmpty()) {
            return this;
        }

        Map<Set<Entry<String, String>>, DataEntry> newValue = Maps.transformValues(this.contexts,
                dataEntry -> dataEntry == null ? null : dataEntry.withoutPermissions());
        return newData(newValue);
    }

    @Override
    public ImmutableSubjectData clearPermissions(Set<Entry<String, String>> contexts) {
        if (!this.contexts.containsKey(contexts)) {
            return this;
        }
        return newWithUpdated(contexts, getDataEntryOrNew(contexts).withoutPermissions());

    }

    @Override
    public Map<Set<Entry<String, String>>, List<Entry<String, String>>> getAllParents() {
        return Maps.filterValues(Maps.transformValues(contexts,
                dataEntry -> dataEntry == null ? null : dataEntry.parents == null ? null : Lists.transform(dataEntry.parents, Util::subjectFromString)), v -> v != null);
    }

    @Override
    public List<Map.Entry<String, String>> getParents(Set<Entry<String, String>> contexts) {
        DataEntry ent = this.contexts.get(contexts);
        return ent == null || ent.parents == null ? Collections.<Map.Entry<String, String>>emptyList() : Lists.transform(ent.parents, Util::subjectFromString);
    }

    @Override
    public ImmutableSubjectData addParent(Set<Entry<String, String>> contexts, String type, String ident) {
        DataEntry entry = getDataEntryOrNew(contexts);
        final String parentIdent = type + ":" + ident;
        if (entry.parents != null && entry.parents.contains(parentIdent)) {
            return this;
        }
        return newWithUpdated(contexts, entry.withAddedParent(parentIdent));
    }

    @Override
    public ImmutableSubjectData removeParent(Set<Entry<String, String>> contexts, String type, String identifier) {
        DataEntry ent = this.contexts.get(contexts);
        if (ent == null) {
            return this;
        }

        final String combined = type + ":" + identifier;
        if (ent.parents == null || !ent.parents.contains(combined)) {
            return this;
        }
        return newWithUpdated(contexts, ent.withRemovedParent(combined));
    }

    @Override
    public ImmutableSubjectData setParents(Set<Entry<String, String>> contexts, List<Entry<String, String>> parents) {
        DataEntry entry = getDataEntryOrNew(contexts);
        return newWithUpdated(contexts, entry.withParents(Lists.transform(parents, Util::subjectToString)));
    }

    @Override
    public ImmutableSubjectData clearParents() {
        if (this.contexts.isEmpty()) {
            return this;
        }

        Map<Set<Entry<String, String>>, DataEntry> newValue = Maps.transformValues(this.contexts,
                dataEntry -> dataEntry == null ? null : dataEntry.withoutParents());
        return newData(newValue);
    }

    @Override
    public ImmutableSubjectData clearParents(Set<Entry<String, String>> contexts) {
        if (!this.contexts.containsKey(contexts)) {
            return this;
        }
        return newWithUpdated(contexts, getDataEntryOrNew(contexts).withoutParents());
    }

    @Override
    public int getDefaultValue(Set<Entry<String, String>> contexts) {
        DataEntry ent = this.contexts.get(contexts);
        return ent == null || ent.defaultValue == null ? 0 : ent.defaultValue;
    }

    @Override
    public ImmutableSubjectData setDefaultValue(Set<Entry<String, String>> contexts, int defaultValue) {
        return newWithUpdated(contexts, getDataEntryOrNew(contexts).withDefaultValue(defaultValue));
    }

    @Override
    public Iterable<Set<Entry<String, String>>> getActiveContexts() {
        return contexts.keySet();
    }

    @Override
    public Map<Set<Entry<String, String>>, Integer> getAllDefaultValues() {
        return Maps.filterValues(Maps.transformValues(contexts,
                dataEntry -> dataEntry == null ? null : dataEntry.defaultValue), v -> v != null);
    }

    @Override
    public String toString() {
        return "MemoryOptionSubjectData{" +
                "contexts=" + contexts +
                '}';
    }
}
