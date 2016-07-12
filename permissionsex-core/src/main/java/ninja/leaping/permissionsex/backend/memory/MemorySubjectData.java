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
import ninja.leaping.permissionsex.data.SegmentKey;
import ninja.leaping.permissionsex.util.Tristate;
import ninja.leaping.permissionsex.util.WeightedImmutableSet;

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

    protected MemorySubjectData newData(Map<SegmentKey, DataSegment> segments) {
        return new MemorySubjectData(segments);
    }

    protected final Map<SegmentKey, DataSegment> segments;

    protected MemorySubjectData() {
        this.segments = ImmutableMap.of();
    }

    protected MemorySubjectData(Map<SegmentKey, DataSegment> segments) {
        this.segments = segments;
    }

    private <E> ImmutableSet<E> immutSet(Set<E> set) {
        return ImmutableSet.copyOf(set);
    }

    @Override
    public WeightedImmutableSet<DataSegment> getAllSegments() {
        return WeightedImmutableSet.copyOf(this.segments.values());
    }

    @Override
    public WeightedImmutableSet<DataSegment> getAllSegments(Set<Entry<String, String>> contexts, boolean inheritable) {
        return WeightedImmutableSet.ofStream(this.segments.values().stream()
                .filter(seg -> seg.getContexts().equals(contexts) && seg.isInheritable() == inheritable));
    }

    @Override
    public DataSegment getSegment(Set<Entry<String, String>> contexts, int weight, boolean inheritable) {
        DataSegment ret = this.segments.get(SegmentKey.of(contexts, weight, inheritable));
        if (ret != null) {
            return ret;
        }
        return new MemorySegment(contexts, weight, inheritable);
    }

    @Override
    public ImmutableSubjectData updateSegment(Set<Entry<String, String>> contexts, int weight, boolean inheritable, Function<DataSegment, DataSegment> updateFunc) {
        contexts = immutSet(contexts);
        SegmentKey key = SegmentKey.of(contexts, weight, inheritable);
        DataSegment seg = this.segments.get(key);
        if (seg == null) {
            seg = new MemorySegment(contexts, weight, inheritable);
        }
        DataSegment newSeg = updateFunc.apply(seg);
        if (newSeg == seg) {
            return this;
        }

        return newData(updateImmutable(this.segments, key, newSeg.getKey(), newSeg));
    }


    @Override
    public ImmutableSubjectData clearOptions() {
        if (this.segments.isEmpty()) {
            return this;
        }

        return newData(Maps.transformValues(this.segments, DataSegment::withoutOptions));
    }

    @Override
    public ImmutableSubjectData clearPermissions() {
        if (this.segments.isEmpty()) {
            return this;
        }

        return newData(Maps.transformValues(this.segments, DataSegment::withoutPermissions));
    }

    @Override
    public ImmutableSubjectData clearParents() {
        if (this.segments.isEmpty()) {
            return this;
        }

        return newData(Maps.transformValues(this.segments, DataSegment::withoutParents));
    }

    @Override
    public ImmutableSubjectData clearDefaultValues() {
        if (this.segments.isEmpty()) {
            return this;
        }

        return newData(Maps.transformValues(this.segments, old -> old.withDefaultValue(Tristate.UNDEFINED)));
    }

    @Override
    public String toString() {
        return "MemoryOptionSubjectData{" +
                "segments=" + segments +
                '}';
    }
}
