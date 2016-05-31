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
package ninja.leaping.permissionsex.backend.sql;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.data.DataSegment;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.util.ThrowingBiConsumer;
import ninja.leaping.permissionsex.util.Util;
import ninja.leaping.permissionsex.util.WeightedImmutableSet;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Data for SQL-backed subjects
 */
class SqlSubjectData implements ImmutableSubjectData {
    private final SqlSubjectRef subject;
    private final Map<Set<Entry<String, String>>, Segment> segments;
    private final AtomicReference<ImmutableList<ThrowingBiConsumer<SqlDao, SqlSubjectData, SQLException>>> updatesToPerform = new AtomicReference<>();

    SqlSubjectData(SqlSubjectRef subject) {
        this(subject, ImmutableMap.of(), null);
    }

    SqlSubjectData(SqlSubjectRef subject, Map<Set<Entry<String, String>>, Segment> segments, ImmutableList<ThrowingBiConsumer<SqlDao, SqlSubjectData, SQLException>> updates) {
        this.subject = subject;
        this.segments = segments;
        this.updatesToPerform.set(updates);
    }

    protected final SqlSubjectData newWithUpdate(Map<Set<Entry<String, String>>, Segment> segments, ThrowingBiConsumer<SqlDao, SqlSubjectData, SQLException> updateFunc) {
        return new SqlSubjectData(subject, segments, Util.appendImmutable(this.updatesToPerform.get(), updateFunc));
    }

    protected final SqlSubjectData newWithUpdated(Set<Entry<String, String>> key, Segment val) {
        ThrowingBiConsumer<SqlDao, SqlSubjectData, SQLException> updateFunc;
        if (val.isEmpty()) { // then remove segment
            if (val.isUnallocated()) {
                updateFunc = (dao, data) -> {};
            } else {
                updateFunc = (dao, data) -> dao.removeSegment(val);
            }
        } else if (val.isUnallocated()) { // create new segment
            updateFunc = (dao, data) -> {
                Segment seg = data.segments.get(key);
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
            updateFunc = (dao, data) -> {
                val.doUpdates(dao);
            };
        }
        return newWithUpdate(Util.updateImmutable(segments, immutSet(key), val), updateFunc);
    }

    private Segment getSegmentOrNew(Set<Entry<String, String>> segments) {
        Segment res = this.segments.get(segments);
        if (res == null) {
            res = Segment.unallocated();
        }
        return res;
    }

    private <E> ImmutableSet<E> immutSet(Set<E> set) {
        return ImmutableSet.copyOf(set);
    }

    @Override
    public WeightedImmutableSet<DataSegment> getAllSegments() {
        return null;
    }

    @Override
    public WeightedImmutableSet<DataSegment> getAllSegments(Set<Entry<String, String>> contexts, boolean inheritable) {
        return null;
    }

    @Override
    public DataSegment getOrCreateSegment(Set<Entry<String, String>> contexts, int weight, boolean inheritable) {
        return null;
    }

    @Override
    public SqlSubjectData updateOrCreateSegment(Set<Entry<String, String>> contexts, int weight, boolean inheritable, Function<DataSegment, DataSegment> updateFunc) {
        return null;
    }

    @Override
    public SqlSubjectData clearOptions() {
        if (this.segments.isEmpty()) {
            return this;
        }

        Map<Set<Entry<String, String>>, Segment> newValue = Maps.transformValues(this.segments,
                dataEntry -> dataEntry == null ? null : dataEntry.withoutOptions());
        return newWithUpdate(newValue, createBulkUpdateFunc(newValue.keySet()));
    }

    private ThrowingBiConsumer<SqlDao, SqlSubjectData, SQLException> createBulkUpdateFunc(Collection<Set<Entry<String, String>>> keys) {
        return (dao, data) -> {
            for (Set<Entry<String, String>> key : keys) {
                Segment seg = data.segments.get(key);
                if (seg != null) {
                    if (seg.isEmpty()) {
                        dao.removeSegment(seg);
                    } else {
                        seg.doUpdates(dao);
                    }
                }
            }
        };
    }

    @Override
    public ImmutableSubjectData clearPermissions() {
        if (this.segments.isEmpty()) {
            return this;
        }

        Map<Set<Entry<String, String>>, Segment> newValue = Maps.transformValues(this.segments,
                dataEntry -> dataEntry == null ? null : dataEntry.withoutPermissions());
        return newWithUpdate(newValue, createBulkUpdateFunc(newValue.keySet()));
    }

    @Override
    public ImmutableSubjectData clearParents() {
        if (this.segments.isEmpty()) {
            return this;
        }

        Map<Set<Entry<String, String>>, Segment> newValue = Maps.transformValues(this.segments,
                dataEntry -> dataEntry == null ? null : dataEntry.withoutParents());
        return newWithUpdate(newValue, createBulkUpdateFunc(newValue.keySet()));
    }

    @Override
    public ImmutableSubjectData clearDefaultValues() {
        return null;
    }

    public void doUpdates(SqlDao dao) throws SQLException {
        dao.executeInTransaction(() -> {
            List<ThrowingBiConsumer<SqlDao, SqlSubjectData, SQLException>> updates = this.updatesToPerform.getAndSet(null);
            if (updates != null) {
                for (ThrowingBiConsumer<SqlDao, SqlSubjectData, SQLException> func : updates) {
                    func.accept(dao, this);
                }
            }
            return null;
        });
    }
}
