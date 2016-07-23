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
import ninja.leaping.permissionsex.backend.AbstractSubjectData;
import ninja.leaping.permissionsex.data.DataSegment;
import ninja.leaping.permissionsex.data.SegmentKey;
import ninja.leaping.permissionsex.util.ThrowingBiConsumer;
import ninja.leaping.permissionsex.util.Util;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Data for SQL-backed subjects
 */
class SqlSubjectData extends AbstractSubjectData<SqlSubjectRef, SqlDataSegment> {
    private final AtomicReference<ImmutableList<ThrowingBiConsumer<SqlDao, SqlSubjectData, SQLException>>> updatesToPerform = new AtomicReference<>();

    SqlSubjectData(SqlSubjectRef subject) {
        this(subject, ImmutableMap.of(), null);
    }

    @Override
    protected SqlDataSegment fromSegment(DataSegment seg) {
        return SqlDataSegment.fromSegment(seg);
    }

    @Override
    protected SqlDataSegment newSegment(SegmentKey key) {
        return SqlDataSegment.unallocated(key);
    }

    SqlSubjectData(SqlSubjectRef subject, Map<SegmentKey, SqlDataSegment> segments, ImmutableList<ThrowingBiConsumer<SqlDao, SqlSubjectData, SQLException>> updates) {
        super(subject, segments);
        this.updatesToPerform.set(updates);
    }

    protected final SqlSubjectData newWithUpdate(Map<SegmentKey, SqlDataSegment> segments, ThrowingBiConsumer<SqlDao, SqlSubjectData, SQLException> updateFunc) {
        return new SqlSubjectData(getReference(), segments, Util.appendImmutable(this.updatesToPerform.get(), updateFunc));
    }

    @Override
    protected final SqlSubjectData newWithUpdated(SegmentKey oldKey, SqlDataSegment newVal) {
        ThrowingBiConsumer<SqlDao, SqlSubjectData, SQLException> updateFunc;
        if (newVal == null || newVal.isEmpty()) { // then remove segment
            /*if (newVal != null && newVal.isUnallocated()) */{
                updateFunc = (dao, data) -> {};
            /*} else {
                updateFunc = (dao, data) -> dao.removeSegment(oldKey);*/
            }
        } else if (newVal.isUnallocated()) { // create new segment
            updateFunc = (dao, data) -> {
                SqlDataSegment seg = data.segments.get(newVal.getKey());
                if (seg != null) {
                    if (seg.isUnallocated()) {
                        seg.popUpdates();
                        dao.updateFullSegment(data.getReference(), seg);
                    } else {
                        seg.doUpdates(dao);
                    }
                }
            };
        } else { // just run updates
            updateFunc = (dao, data) -> {
                newVal.doUpdates(dao);
            };
        }
        return newWithUpdate(Util.updateImmutable(segments, oldKey, newVal == null ? null : newVal.getKey(), newVal), updateFunc);
    }

    @Override
    protected AbstractSubjectData<SqlSubjectRef, SqlDataSegment> newData(Map<SegmentKey, SqlDataSegment> segments) {
        return newWithUpdate(segments, (dao, data) -> {
            for (SegmentKey key : segments.keySet()) {
                SqlDataSegment seg = data.segments.get(key);
                if (seg != null) {
                    if (seg.isEmpty()) {
                        dao.removeSegment(seg);
                    } else {
                        seg.doUpdates(dao);
                    }
                }
            }
        });
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
