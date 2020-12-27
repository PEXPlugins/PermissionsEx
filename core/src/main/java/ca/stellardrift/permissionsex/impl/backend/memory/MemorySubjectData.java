/*
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
package ca.stellardrift.permissionsex.impl.backend.memory;

import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData;
import ca.stellardrift.permissionsex.subject.Segment;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.pcollections.PMap;
import org.pcollections.PSet;
import org.pcollections.PVector;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

public class MemorySubjectData implements ImmutableSubjectData {
    protected static final ObjectMapper<MemorySegment> MAPPER;
    static {
        try {
            MAPPER = ObjectMapper.factory().get(MemorySegment.class);
        } catch (SerializationException e) {
            throw new ExceptionInInitializerError(e); // This debug indicates a programming issue
        }
    }


    protected final PMap<PSet<ContextValue<?>>, MemorySegment> segments;

    protected MemorySubjectData() {
        this.segments = PCollections.map();
    }

    protected MemorySubjectData(final Map<PSet<ContextValue<?>>, MemorySegment> segments) {
        this.segments = PCollections.asMap(segments);
    }

    protected MemorySubjectData newData(final PMap<PSet<ContextValue<?>>, MemorySegment> contexts) {
        if (contexts == this.segments) {
            return this;
        }
        return new MemorySubjectData(contexts);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map<Set<ContextValue<?>>, Segment> segments() {
        return (Map) this.segments;
    }

    @Override
    public ImmutableSubjectData withSegments(final BiFunction<Set<ContextValue<?>>, Segment, Segment> transformer) {
        PMap<PSet<ContextValue<?>>, MemorySegment> output = this.segments;
        for (final Map.Entry<PSet<ContextValue<?>>, MemorySegment> entry : this.segments.entrySet()) {
            final MemorySegment mapped = MemorySegment.from(transformer.apply(entry.getKey(), entry.getValue()));
            if (mapped != entry.getValue()) {
                if (mapped.empty()) {
                    output = output.minus(entry.getKey());
                } else {
                    output = output.plus(entry.getKey(), mapped);
                }
            }
        }
        return newData(output);
    }

    @Override
    public ImmutableSubjectData withSegment(final Set<ContextValue<?>> contexts, final UnaryOperator<Segment> operation) {
        final PSet<ContextValue<?>> pContexts = PCollections.asSet(contexts);
        final Segment original = this.segments.get(pContexts);
        final MemorySegment mapped = MemorySegment.from(operation.apply(original == null ? MemorySegment.create() : original));
        if (original != mapped) {
            return newData(mapped.empty() ? this.segments.minus(pContexts) : this.segments.plus(pContexts, mapped));
        } else {
            return this;
        }
    }

    @Override
    public <V> Map<Set<ContextValue<?>>, V> mapSegmentValues(final Function<Segment, V> mapper) {
        requireNonNull(mapper, "mapper");
        return PCollections.asMap(this.segments, (k, v) -> k, (k, v) -> mapper.apply(v));
    }

    @Override
    public <V> @Nullable V mapSegment(final Set<ContextValue<?>> contexts, final Function<Segment, V> mapper) {
        requireNonNull(mapper, "mapper");
        final MemorySegment segment = this.segments.get(PCollections.asSet(requireNonNull(contexts, "contexts")));
        if (segment != null) {
            return mapper.apply(segment);
        }
        return null;
    }

    @Override
    public MemorySegment segment(final Set<ContextValue<?>> contexts) {
        MemorySegment res = this.segments.get(PCollections.asSet(contexts));
        if (res == null) {
            res = MemorySegment.create();
        }
        return res;
    }

    @Override
    public ImmutableSubjectData withSegment(final Set<ContextValue<?>> contexts, final Segment segment) {
        PMap<PSet<ContextValue<?>>, MemorySegment> segments;
        if (segment.empty()) {
            segments = this.segments.minus(PCollections.asSet(contexts));
        } else {
            segments = this.segments.plus(PCollections.asSet(contexts), MemorySegment.from(segment));
        }
        return segments == this.segments ? this : newData(segments);
    }

    @Override
    public Set<PSet<ContextValue<?>>> activeContexts() {
        return this.segments.keySet();
    }

    @Override
    public String toString() {
        return "MemorySubjectData{" +
                "segments=" + this.segments +
                '}';
    }

    @ConfigSerializable
    protected static class MemorySegment implements Segment {
        @Setting private PMap<String, Integer> permissions;
        @Setting private PMap<String, String> options;
        @Setting private PVector<SubjectRef<?>> parents;
        @Nullable @Setting("permissions-default") private Integer defaultValue;

        static MemorySegment create() {
            return new MemorySegment(
                    PCollections.map(),
                    PCollections.map(),
                    PCollections.vector(),
                    null
            );
        }

        static MemorySegment from(final Segment other) {
            if (other instanceof MemorySegment) {
                return (MemorySegment) other;
            } else {
                return new MemorySegment(
                        PCollections.asMap(other.permissions()),
                        PCollections.asMap(other.options()),
                        PCollections.narrow(PCollections.asVector(other.parents())),
                        other.fallbackPermission()
                );
            }
        }

        MemorySegment(final PMap<String, Integer> permissions, final PMap<String, String> options, final PVector<SubjectRef<?>> parents, final @Nullable Integer defaultValue) {
            this.permissions = permissions;
            this.options = options;
            this.parents = parents;
            this.defaultValue = defaultValue;
        }


        private MemorySegment() { // Objectmapper constructor
        }

        @Override
        public Map<String, String> options() {
            return this.options;
        }

        @Override
        public MemorySegment withOption(final String key, final String value) {
            return new MemorySegment(this.permissions, this.options.plus(key, value), this.parents, this.defaultValue);
        }

        @Override
        public MemorySegment withoutOption(final String key) {
            if (!this.options.containsKey(key)) {
                return this;
            }
            return new MemorySegment(this.permissions, this.options.minus(key), this.parents, this.defaultValue);

        }

        @Override
        public MemorySegment withOptions(final Map<String, String> values) {
            return new MemorySegment(this.permissions, PCollections.asMap(values), this.parents, this.defaultValue);
        }

        @Override
        public MemorySegment withoutOptions() {
            return new MemorySegment(this.permissions, PCollections.map(), this.parents, this.defaultValue);
        }

        @Override
        public Map<String, Integer> permissions() {
            return this.permissions;
        }

        @Override
        public MemorySegment withPermission(final String permission, final int value) {
            return new MemorySegment(
                    value == 0 ? this.permissions.minus(permission) : this.permissions.plus(permission, value),
                    this.options,
                    this.parents,
                    this.defaultValue);

        }

        @Override
        public MemorySegment withPermissions(final Map<String, Integer> values) {
            return new MemorySegment(PCollections.asMap(values), this.options, this.parents, this.defaultValue);
        }

        @Override
        public MemorySegment withoutPermissions() {
            return new MemorySegment(PCollections.map(), this.options, this.parents, this.defaultValue);
        }

        @Override
        public List<SubjectRef<?>> parents() {
            return this.parents;
        }

        @Override
        public <I> MemorySegment plusParent(final SubjectRef<I> parent) {
            return new MemorySegment(this.permissions, this.options, this.parents.plus(0, SubjectRef.mapKeySafe(parent)), this.defaultValue);
        }

        @Override
        public <I> MemorySegment minusParent(final SubjectRef<I> parent) {
            if (this.parents.isEmpty()) {
                return this;
            }

            return new MemorySegment(this.permissions, this.options, this.parents.minus(SubjectRef.mapKeySafe(parent)), this.defaultValue);
        }

        @Override
        public MemorySegment withParents(List<SubjectRef<?>> parents) {
            return new MemorySegment(this.permissions, this.options, PCollections.asVector(parents), this.defaultValue);
        }

        @Override
        public MemorySegment withoutParents() {
            return new MemorySegment(this.permissions, this.options, PCollections.vector(), this.defaultValue);
        }

        @Override
        public int fallbackPermission() {
            return this.defaultValue == null ? 0 : this.defaultValue;
        }

        @Override
        public Segment withFallbackPermission(int defaultValue) {
            return new MemorySegment(this.permissions, this.options, this.parents, defaultValue);
        }

        @Override
        public Segment cleared() {
            return new MemorySegment(PCollections.map(), PCollections.map(), PCollections.vector(), null);
        }

        @Override
        public String toString() {
            return "DataEntry{" +
                    "permissions=" + this.permissions +
                    ", options=" + this.options +
                    ", parents=" + this.parents +
                    ", defaultValue=" + this.defaultValue +
                    '}';
        }

        @Override
        public boolean empty() {
            return this.permissions.isEmpty()
                    && this.options.isEmpty()
                    && this.parents.isEmpty()
                    && this.defaultValue == null;
        }
    }
}
