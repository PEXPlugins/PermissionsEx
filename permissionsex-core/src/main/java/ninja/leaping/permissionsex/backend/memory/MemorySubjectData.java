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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.permissionsex.data.DataSegment;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.util.Tristate;
import ninja.leaping.permissionsex.util.WeightedImmutableSet;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Map.Entry;
import static ninja.leaping.permissionsex.util.Util.updateImmutable;

public class MemorySubjectData implements ImmutableSubjectData {
    protected static final ObjectMapper<MemorySegment> MAPPER;
    static {
        try {
            MAPPER = ObjectMapper.forClass(MemorySegment.class);
        } catch (ObjectMappingException e) {
            throw new ExceptionInInitializerError(e); // This debug indicates a programming issue
        }
    }

    protected static class ContextSegments {
        private WeightedImmutableSet<MemorySegment> inheritable, notInheritable;

        public ContextSegments(WeightedImmutableSet<MemorySegment> inheritable, WeightedImmutableSet<MemorySegment> notInheritable) {
            this.inheritable = inheritable;
            this.notInheritable = notInheritable;
        }

        public ContextSegments withInheritableSegments(WeightedImmutableSet<MemorySegment> inheritable) {
            return new ContextSegments(inheritable, this.notInheritable);
        }

        public ContextSegments withNonInheritableSegments(WeightedImmutableSet<MemorySegment> notInheritable) {
            return new ContextSegments(this.inheritable, notInheritable);
        }

        public WeightedImmutableSet<MemorySegment> getInheritableSegments() {
            return this.inheritable;
        }

        public WeightedImmutableSet<MemorySegment> getNonInheritableSegments() {
            return this.notInheritable;
        }

        public ContextSegments withBothUpdated(Function<WeightedImmutableSet<MemorySegment>, WeightedImmutableSet<MemorySegment>> updateFunc) {
            return withBothUpdated(updateFunc, updateFunc);
        }

        public ContextSegments withBothUpdated(Function<WeightedImmutableSet<MemorySegment>, WeightedImmutableSet<MemorySegment>> updateInheritable, Function<WeightedImmutableSet<MemorySegment>, WeightedImmutableSet<MemorySegment>> updateNotInheritable) {
            WeightedImmutableSet<MemorySegment> inherit = this.inheritable, notInherit = this.notInheritable;
            if (inherit != null) {
                inherit = updateInheritable.apply(inherit);
            }
            if (notInherit != null) {
                notInherit = updateNotInheritable.apply(notInherit);
            }
            return new ContextSegments(inherit, notInherit);
        }
    }

    protected MemorySubjectData newData(Map<Set<Entry<String, String>>, ContextSegments> segments) {
        return new MemorySubjectData(segments);
    }

    protected final Map<Set<Entry<String, String>>, ContextSegments> contexts;

    /**
     * Data structure for segments:
     * contexts, inheritable t/f, sortedset
     */

    protected MemorySubjectData() {
        this.contexts = ImmutableMap.of();
    }

    protected MemorySubjectData(Map<Set<Entry<String, String>>, ContextSegments> contexts) {
        this.contexts = contexts;
    }

    private <E> ImmutableSet<E> immutSet(Set<E> set) {
        return ImmutableSet.copyOf(set);
    }

    @Override
    public WeightedImmutableSet<DataSegment> getAllSegments() {
        return contexts.values().stream().collect(() -> new ArrayList<>(contexts.size() * 2), (acc, el ) -> {
            if (el.getInheritableSegments() != null) {
                acc.addAll(el.getInheritableSegments().asSet()); // TODO: Make more efficient
            }
            if (el.getNonInheritableSegments() != null) {
                acc.addAll(el.getNonInheritableSegments().asSet());
            }
        }, ArrayList::addAll);
    }

    @Override
    public WeightedImmutableSet<DataSegment> getAllSegments(Set<Entry<String, String>> contexts, boolean inheritable) {
        ContextSegments segs = this.contexts.get(contexts);
        if (segs == null) {
            return WeightedImmutableSet.of();
        }
        WeightedImmutableSet<MemorySegment> ret = inheritable ? segs.getInheritableSegments() : segs.getNonInheritableSegments();
        if (ret == null) {
            return WeightedImmutableSet.of();
        }
        return WeightedImmutableSet.copyOf(ret);
    }

    @Override
    public DataSegment getOrCreateSegment(Set<Entry<String, String>> contexts, int weight, boolean inheritable) {
        ContextSegments contextSegs = this.contexts.get(contexts);
        if (contextSegs != null) {
            WeightedImmutableSet<MemorySegment> entries = inheritable ? contextSegs.getInheritableSegments() : contextSegs.getNonInheritableSegments();
            if (entries != null && !entries.isEmpty()) {
                MemorySegment ret = entries.get(weight);
                if (ret != null) {
                    return ret;
                }

            }
        }
        return new MemorySegment(contexts, weight, inheritable);
    }

    @Override
    public ImmutableSubjectData updateOrCreateSegment(Set<Entry<String, String>> contexts, int weight, boolean inheritable, Function<DataSegment, DataSegment> updateFunc) {
        contexts = immutSet(contexts);
        ContextSegments contextSegs = this.contexts.get(contexts);
        if (contextSegs != null) {
            WeightedImmutableSet<MemorySegment> entries = inheritable ? contextSegs.getInheritableSegments() : contextSegs.getNonInheritableSegments(), newEntries;
            if (entries != null && !entries.isEmpty()) {
                MemorySegment ret = entries.get(weight);
                DataSegment newSeg;
                if (ret != null) {
                    newSeg = updateFunc.apply(ret);
                } else {
                    newSeg = updateFunc.apply(new MemorySegment(contexts, weight, inheritable));
                }
                newEntries = entries.with(MemorySegment.fromSegment(newSeg));
            } else {
                DataSegment newSeg = updateFunc.apply(new MemorySegment(contexts, weight, inheritable));
                newEntries = WeightedImmutableSet.of(MemorySegment.fromSegment(updateFunc.apply(newSeg)));
            }
            if (inheritable) {
                contextSegs = contextSegs.withInheritableSegments(newEntries);
            } else {
                contextSegs = contextSegs.withNonInheritableSegments(newEntries);
            }
        } else {
            DataSegment newSeg = updateFunc.apply(new MemorySegment(contexts, weight, inheritable));
            WeightedImmutableSet<MemorySegment> entry = WeightedImmutableSet.of(MemorySegment.fromSegment(updateFunc.apply(newSeg)));
            if (inheritable) {
                contextSegs = new ContextSegments(entry, null);
            } else {
                contextSegs = new ContextSegments(null, entry);
            }
        }
        return newData(updateImmutable(this.contexts, contexts, contextSegs));
    }


    @Override
    public ImmutableSubjectData clearOptions() {
        if (this.contexts.isEmpty()) {
            return this;
        }

        Map<Set<Entry<String, String>>, ContextSegments> newValue = Maps.transformValues(this.contexts,
                dataEntry -> dataEntry.withBothUpdated(oldList -> oldList.map(MemorySegment::withoutOptions)));
        return newData(newValue);
    }

    @Override
    public ImmutableSubjectData clearPermissions() {
        if (this.contexts.isEmpty()) {
            return this;
        }

        Map<Set<Entry<String, String>>, ContextSegments> newValue = Maps.transformValues(this.contexts,
                dataEntry -> dataEntry.withBothUpdated(oldList -> oldList.map(MemorySegment::withoutPermissions)));
        return newData(newValue);
    }

    @Override
    public ImmutableSubjectData clearParents() {
        if (this.contexts.isEmpty()) {
            return this;
        }

        Map<Set<Entry<String, String>>, ContextSegments> newValue = Maps.transformValues(this.contexts,
                dataEntry -> dataEntry.withBothUpdated(oldList -> oldList.map(MemorySegment::withoutParents)));
        return newData(newValue);
    }

    @Override
    public ImmutableSubjectData clearDefaultValues() {
        if (this.contexts.isEmpty()) {
            return this;
        }

        Map<Set<Entry<String, String>>, ContextSegments> newValue = Maps.transformValues(this.contexts,
                dataEntry -> dataEntry.withBothUpdated(oldList-> oldList.map(entry -> entry.withDefaultValue(Tristate.UNDEFINED))));
        return newData(newValue);
    }

    @Override
    public String toString() {
        return "MemoryOptionSubjectData{" +
                "contexts=" + contexts +
                '}';
    }
}
