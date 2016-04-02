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
package ninja.leaping.permissionsex.backend;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.util.Util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Map.Entry;

final class SubjectDataImpl implements ImmutableSubjectData {

    static <K, V> Map<K, V> updateImmutable(Map<K, V> input, K newKey, V newVal) {
        if (input == null) {
            return ImmutableMap.of(newKey, newVal);
        }
        Map<K, V> ret = new HashMap<>(input);
        if (newVal == null) {
            ret.remove(newKey);
        } else {
            ret.put(newKey, newVal);
        }
        return Collections.unmodifiableMap(ret);
    }

    private final SubjectDataImpl newWithUpdated(Set<Entry<String, String>> key, DataEntry val) {
        if (val.isEmpty()) {
            val = null;
        }
        return newData(updateImmutable(contexts, immutSet(key), val));
    }

    protected SubjectDataImpl newData(Map<Set<Entry<String, String>>, DataEntry> contexts) {
        return new SubjectDataImpl(contexts);
    }

    protected final Map<Set<Entry<String, String>>, DataEntry> contexts;

    SubjectDataImpl() {
        this.contexts = ImmutableMap.of();
    }

    SubjectDataImpl(Map<Set<Entry<String, String>>, DataEntry> contexts) {
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
