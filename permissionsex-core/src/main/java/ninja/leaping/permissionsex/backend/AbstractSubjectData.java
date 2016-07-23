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
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.data.DataSegment;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.data.SegmentKey;
import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.util.Tristate;
import ninja.leaping.permissionsex.util.WeightedImmutableSet;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Map.Entry;

public abstract class AbstractSubjectData<R extends SubjectRef, S extends DataSegment> implements ImmutableSubjectData {

    private final R ref;
    protected final Map<SegmentKey, S> segments;

    protected AbstractSubjectData(R ref) {
        this.ref = ref;
        this.segments = ImmutableMap.of();
    }

    protected AbstractSubjectData(R ref, Map<SegmentKey, S> segments) {
        this.ref = ref;
        this.segments = segments;
    }

    public R getReference() {
        return this.ref;
    }

    protected abstract S fromSegment(DataSegment seg);
    protected abstract S newSegment(SegmentKey key);
    protected abstract AbstractSubjectData<R, S> newWithUpdated(SegmentKey oldKey, S newVal);
    protected abstract AbstractSubjectData<R, S> newData(Map<SegmentKey, S> segments);

    @Override
    public WeightedImmutableSet<DataSegment> getAllSegments() {
        return WeightedImmutableSet.copyOf(this.segments.values());
    }

    @Override
    public WeightedImmutableSet<DataSegment> getAllSegments(Set<Entry<String, String>> contexts, boolean inheritable) {
        return WeightedImmutableSet.ofStream(this.segments.values().stream()
                .filter(seg -> seg.getKey().getContexts().equals(contexts) && seg.getKey().isInheritable() == inheritable).map(Function.identity()));
    }

    @Override
    public DataSegment getSegment(SegmentKey key) {
        DataSegment ret = this.segments.get(key);
        if (ret != null) {
            return ret;
        }
        return newSegment(key);
    }

    @Override
    public ImmutableSubjectData updateSegment(SegmentKey key, Function<DataSegment, DataSegment> updateFunc) {
        DataSegment seg = this.segments.get(key);
        if (seg == null) {
            seg = newSegment(key);
        }
        DataSegment newSeg = updateFunc.apply(seg);
        if (newSeg == seg) {
            return this;
        }
        return newWithUpdated(seg.getKey(), fromSegment(newSeg));
    }


    @Override
    public ImmutableSubjectData clearOptions() {
        if (this.segments.isEmpty()) {
            return this;
        }

        return newData(Maps.transformValues(this.segments, seg -> fromSegment(seg.withoutOptions())));
    }

    @Override
    public ImmutableSubjectData clearPermissions() {
        if (this.segments.isEmpty()) {
            return this;
        }

        return newData(Maps.transformValues(this.segments, seg -> fromSegment(seg.withoutPermissions())));
    }

    @Override
    public ImmutableSubjectData clearParents() {
        if (this.segments.isEmpty()) {
            return this;
        }

        return newData(Maps.transformValues(this.segments, seg -> fromSegment(seg.withoutParents())));
    }

    @Override
    public ImmutableSubjectData clearDefaultValues() {
        if (this.segments.isEmpty()) {
            return this;
        }

        return newData(Maps.transformValues(this.segments, old -> fromSegment(old.withDefaultValue(Tristate.UNDEFINED))));
    }

    @Override
    public String toString() {
        return "AbstractSubjectData{" +
                "segments=" + segments +
                '}';
    }
}
