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
package ca.stellardrift.permissionsex.datastore.sql;

import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData;
import ca.stellardrift.permissionsex.subject.Segment;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.pcollections.PMap;
import org.pcollections.PSet;
import org.pcollections.PStack;
import org.pcollections.PVector;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Data for SQL-backed subjects
 */
class SqlSubjectData implements ImmutableSubjectData {
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final AtomicReferenceFieldUpdater<SqlSubjectData, PVector<CheckedBiConsumer<SqlDao, SqlSubjectData, SQLException>>> UPDATES_MUTATOR = (AtomicReferenceFieldUpdater) AtomicReferenceFieldUpdater.newUpdater(SqlSubjectData.class, PVector.class, "updatesToPerform");
    private final SqlSubjectRef<?> subject;
    private final PMap<PSet<ContextValue<?>>, SqlSegment> segments;
    private volatile PVector<CheckedBiConsumer<SqlDao, SqlSubjectData, SQLException>> updatesToPerform;

    SqlSubjectData(final SqlSubjectRef<?> subject) {
        this(subject, PCollections.map(), PCollections.vector());
    }

    SqlSubjectData(final SqlSubjectRef<?> subject, final PMap<PSet<ContextValue<?>>, SqlSegment> segments, final PVector<CheckedBiConsumer<SqlDao, SqlSubjectData, SQLException>> updates) {
        this.subject = subject;
        this.segments = segments;
        this.updatesToPerform = updates;
    }

    protected final SqlSubjectData newWithUpdate(PMap<PSet<ContextValue<?>>, SqlSegment> segments, CheckedBiConsumer<SqlDao, SqlSubjectData, SQLException> updateFunc) {
        return new SqlSubjectData(subject, segments, this.updatesToPerform.plus(updateFunc));
    }

    @Override
    public Map<Set<ContextValue<?>>, Segment> segments() {
        return PCollections.narrow(this.segments);
    }

    @Override
    public ImmutableSubjectData withSegments(BiFunction<Set<ContextValue<?>>, Segment, Segment> transformer) {
        PMap<PSet<ContextValue<?>>, SqlSegment> segments = this.segments;
        PStack<CheckedBiConsumer<SqlDao, SqlSubjectData, SQLException>> updates = PCollections.stack();

        for (final Map.Entry<PSet<ContextValue<?>>, SqlSegment> entry : this.segments.entrySet()) {
            SqlSegment modified = SqlSegment.from(entry.getKey(), transformer.apply(entry.getKey(), entry.getValue()));
            if (modified != entry.getValue()) {
                segments = segments.plus(entry.getKey(), modified);
                updates = updates.plus(makeUpdater(entry.getKey(), modified));
            }
        }

        if (segments != this.segments) {
            return new SqlSubjectData(this.subject, segments, this.updatesToPerform.plusAll(updates));
        } else {
            return this;
        }
    }

    @Override
    public ImmutableSubjectData withSegment(final Set<ContextValue<?>> rawContexts, final UnaryOperator<Segment> operation) {
        final PSet<ContextValue<?>> contexts = PCollections.asSet(rawContexts);
        final SqlSegment input = this.segment(contexts);
        final SqlSegment output = SqlSegment.from(contexts, operation.apply(input));
        if (input != output) {
            return withSegment(contexts, output);
        } else {
            return this;
        }
    }

    @Override
    public <V> Map<Set<ContextValue<?>>, V> mapSegmentValues(final Function<Segment, V> mapper) {
        return PCollections.asMap(this.segments, (k, v) -> k, (k, v) -> mapper.apply(v));
    }

    @Override
    public <V> @Nullable V mapSegment(final Set<ContextValue<?>> contexts, final Function<Segment, V> mapper) {
        final SqlSegment segment = this.segments.get(PCollections.asSet(contexts));
        if (segment != null) {
            return mapper.apply(segment);
        }
        return null;
    }

    @Override
    public SqlSegment segment(final Set<ContextValue<?>> contexts) {
        final PSet<ContextValue<?>> pContexts = PCollections.asSet(contexts);
        SqlSegment res = this.segments.get(pContexts);
        if (res == null) {
            res = SqlSegment.unallocated(pContexts);
        }
        return res;
    }

    @Override
    public ImmutableSubjectData withSegment(final Set<ContextValue<?>> rawContexts, final Segment rawSegment) {
        final PSet<ContextValue<?>> contexts = PCollections.asSet(rawContexts);
        final SqlSegment segment = SqlSegment.from(contexts, rawSegment);
        return newWithUpdate(this.segments.plus(contexts, segment), makeUpdater(contexts, segment));
    }

    private CheckedBiConsumer<SqlDao, SqlSubjectData, SQLException> makeUpdater(final PSet<ContextValue<?>> contexts, final SqlSegment segment) {
        if (segment.empty()) { // then remove segment
            if (segment.isUnallocated()) {
                return (dao, data) -> {};
            } else {
                return (dao, data) -> dao.removeSegment(segment);
            }
        } else if (segment.isUnallocated()) { // create new segment
            return (dao, data) -> {
                SqlSegment seg = data.segments.get(contexts);
                if (seg != null) {
                    if (seg.isUnallocated()) {
                        seg.popUpdates();
                        dao.updateFullSegment(data.subject, seg);
                    } else {
                        seg.doUpdates(dao);
                    }
                }
            };
        } else { // just run updates
            return (dao, data) -> segment.doUpdates(dao);
        }
    }

    @Override
    public Set<Set<ContextValue<?>>> activeContexts() {
        return this.segments().keySet();
    }

    public void doUpdates(final SqlDao dao) throws SQLException {
        dao.executeInTransaction(() -> {
            final PVector<CheckedBiConsumer<SqlDao, SqlSubjectData, SQLException>> updates = UPDATES_MUTATOR.getAndSet(this, PCollections.vector());
            for (final CheckedBiConsumer<SqlDao, SqlSubjectData, SQLException> func : updates) {
                func.accept(dao, this);
            }
            return null;
        });
    }
}
