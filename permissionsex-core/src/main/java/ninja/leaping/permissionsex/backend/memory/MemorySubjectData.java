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

import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.permissionsex.backend.AbstractSubjectData;
import ninja.leaping.permissionsex.data.DataSegment;
import ninja.leaping.permissionsex.data.SegmentKey;
import ninja.leaping.permissionsex.data.SubjectRef;

import java.util.Map;

import static ninja.leaping.permissionsex.util.Util.updateImmutable;

public class MemorySubjectData extends AbstractSubjectData<SubjectRef, MemorySegment> {
    protected static final ObjectMapper<MemorySegment> MAPPER;
    static {
        try {
            MAPPER = ObjectMapper.forClass(MemorySegment.class);
        } catch (ObjectMappingException e) {
            throw new ExceptionInInitializerError(e); // This debug indicates a programming issue
        }
    }

    @Override
    protected MemorySubjectData newData(Map<SegmentKey, MemorySegment> segments) {
        return new MemorySubjectData(getReference(), segments);
    }


    protected MemorySubjectData(SubjectRef ref) {
        super(ref);
    }

    protected MemorySubjectData(SubjectRef ref, Map<SegmentKey, MemorySegment> segments) {
        super(ref, segments);
    }

    @Override
    protected MemorySegment fromSegment(DataSegment seg) {
        return MemorySegment.fromSegment(seg);
    }

    @Override
    protected MemorySegment newSegment(SegmentKey key) {
        return new MemorySegment(key);
    }

    @Override
    protected MemorySubjectData newWithUpdated(SegmentKey oldKey, MemorySegment newVal) {
        return new MemorySubjectData(getReference(), updateImmutable(segments, oldKey, newVal == null ? null : newVal.getKey(), newVal));
    }

    @Override
    public String toString() {
        return "MemorySubjectData{" +
                "segments=" + segments +
                '}';
    }
}
