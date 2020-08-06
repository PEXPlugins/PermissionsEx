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

package ca.stellardrift.permissionsex.backend.sql;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.data.ImmutableSubjectData;
import ca.stellardrift.permissionsex.util.CheckedBiConsumer;
import ca.stellardrift.permissionsex.util.Util;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Data for SQL-backed subjects
 */
class SqlSubjectData implements ImmutableSubjectData {
    private final SubjectRef subject;
    private final Map<Set<ContextValue<?>>, Segment> segments;
    private final AtomicReference<ImmutableList<CheckedBiConsumer<SqlDao, SqlSubjectData, SQLException>>> updatesToPerform = new AtomicReference<>();

    SqlSubjectData(SubjectRef subject) {
        this(subject, ImmutableMap.of(), null);
    }

    SqlSubjectData(SubjectRef subject, Map<Set<ContextValue<?>>, Segment> segments, ImmutableList<CheckedBiConsumer<SqlDao, SqlSubjectData, SQLException>> updates) {
        this.subject = subject;
        this.segments = segments;
        this.updatesToPerform.set(updates);
    }

    protected final SqlSubjectData newWithUpdate(Map<Set<ContextValue<?>>, Segment> segments, CheckedBiConsumer<SqlDao, SqlSubjectData, SQLException> updateFunc) {
        return new SqlSubjectData(subject, segments, Util.appendImmutable(this.updatesToPerform.get(), updateFunc));
    }

    protected final SqlSubjectData newWithUpdated(Set<ContextValue<?>> key, Segment val) {
        CheckedBiConsumer<SqlDao, SqlSubjectData, SQLException> updateFunc;
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

    private Segment getSegmentOrNew(Set<ContextValue<?>> segments) {
        Segment res = this.segments.get(segments);
        if (res == null) {
            res = Segment.unallocated(segments);
        }
        return res;
    }

    private <E> ImmutableSet<E> immutSet(Set<E> set) {
        return ImmutableSet.copyOf(set);
    }

    @Override
    public Map<Set<ContextValue<?>>, Map<String, String>> getAllOptions() {
        return Maps.filterValues(Maps.transformValues(segments,
                dataEntry -> dataEntry == null ? null : dataEntry.getOptions()), el -> el != null);
    }

    @Override
    public Map<String, String> getOptions(Set<ContextValue<?>> segments) {
        final Segment entry = this.segments.get(segments);
        return entry == null || entry.getOptions() == null ? Collections.emptyMap() : entry.getOptions();
    }

    @Override
    public ImmutableSubjectData setOption(Set<ContextValue<?>> segments, String key, String value) {
        if (value == null) {
            return newWithUpdated(segments, getSegmentOrNew(segments).withoutOption(key));
        } else {
            return newWithUpdated(segments, getSegmentOrNew(segments).withOption(key, value));
        }
    }

    @Override
    public ImmutableSubjectData setOptions(Set<ContextValue<?>> segments, Map<String, String> values) {
        return newWithUpdated(segments, getSegmentOrNew(segments).withOptions(values));
    }

    @Override
    public ImmutableSubjectData clearOptions(Set<ContextValue<?>> segments) {
        if (!this.segments.containsKey(segments)) {
            return this;
        }
        return newWithUpdated(segments, getSegmentOrNew(segments).withoutOptions());
    }

    @Override
    public ImmutableSubjectData clearOptions() {
        if (this.segments.isEmpty()) {
            return this;
        }

        Map<Set<ContextValue<?>>, Segment> newValue = Maps.transformValues(this.segments,
                dataEntry -> dataEntry == null ? null : dataEntry.withoutOptions());
        return newWithUpdate(newValue, createBulkUpdateFunc(newValue.keySet()));
    }

    private CheckedBiConsumer<SqlDao, SqlSubjectData, SQLException> createBulkUpdateFunc(Collection<Set<ContextValue<?>>> keys) {
        return (dao, data) -> {
            for (Set<ContextValue<?>> key : keys) {
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
    public Map<Set<ContextValue<?>>, Map<String, Integer>> getAllPermissions() {
        return Maps.filterValues(Maps.transformValues(segments,
                dataEntry -> dataEntry == null ? null : dataEntry.getPermissions()), o -> o != null);
    }

    @Override
    public Map<String, Integer> getPermissions(Set<ContextValue<?>> set) {
        final Segment entry = this.segments.get(set);
        return entry == null || entry.getPermissions()== null ? Collections.emptyMap() : entry.getPermissions();
    }

    @Override
    public ImmutableSubjectData setPermission(Set<ContextValue<?>> segments, String permission, int value) {
        if (value == 0) {
            return newWithUpdated(segments, getSegmentOrNew(segments).withoutPermission(permission));
        } else {
            return newWithUpdated(segments, getSegmentOrNew(segments).withPermission(permission, value));
        }
    }

    @Override
    public ImmutableSubjectData setPermissions(Set<ContextValue<?>> segments, Map<String, Integer> values) {
        return newWithUpdated(segments, getSegmentOrNew(segments).withPermissions(values));
    }

    @Override
    public ImmutableSubjectData clearPermissions() {
        if (this.segments.isEmpty()) {
            return this;
        }

        Map<Set<ContextValue<?>>, Segment> newValue = Maps.transformValues(this.segments,
                dataEntry -> dataEntry == null ? null : dataEntry.withoutPermissions());
        return newWithUpdate(newValue, createBulkUpdateFunc(newValue.keySet()));
    }

    @Override
    public ImmutableSubjectData clearPermissions(Set<ContextValue<?>> segments) {
        if (!this.segments.containsKey(segments)) {
            return this;
        }
        return newWithUpdated(segments, getSegmentOrNew(segments).withoutPermissions());

    }

    @Override
    public Map<Set<ContextValue<?>>, List<Entry<String, String>>> getAllParents() {
        return Maps.filterValues(Maps.transformValues(segments,
                dataEntry -> dataEntry == null ? null : dataEntry.getParents() == null ? null : ImmutableList.copyOf(dataEntry.getParents())), v -> v != null);
    }

    @Override
    public List<Entry<String, String>> getParents(Set<ContextValue<?>> segments) {
        Segment ent = this.segments.get(segments);
        return ent == null || ent.getParents() == null ? Collections.emptyList() : ImmutableList.copyOf(ent.getParents());
    }

    @Override
    public ImmutableSubjectData addParent(Set<ContextValue<?>> segments, String type, String ident) {
        Segment entry = getSegmentOrNew(segments);
        final SubjectRef parentIdent = SubjectRef.unresolved(type, ident);
        if (entry.getParents() != null && entry.getParents().contains(parentIdent)) {
            return this;
        }
        return newWithUpdated(segments, entry.withAddedParent(parentIdent));
    }

    @Override
    public ImmutableSubjectData removeParent(Set<ContextValue<?>> segments, String type, String identifier) {
        Segment ent = this.segments.get(segments);
        if (ent == null) {
            return this;
        }

        final SubjectRef parentIdent = SubjectRef.unresolved(type, identifier);
        if (ent.getParents() == null || !ent.getParents().contains(parentIdent)) {
            return this;
        }
        return newWithUpdated(segments, ent.withRemovedParent(parentIdent));
    }

    @Override
    public ImmutableSubjectData setParents(Set<ContextValue<?>> segments, List<Entry<String, String>> parents) {
        Segment entry = getSegmentOrNew(segments);
        return newWithUpdated(segments, entry.withParents(Lists.transform(parents, ent -> ent instanceof SubjectRef ? (SubjectRef) ent : SubjectRef.unresolved(ent.getKey(), ent.getValue()))));
    }

    @Override
    public ImmutableSubjectData clearParents() {
        if (this.segments.isEmpty()) {
            return this;
        }

        Map<Set<ContextValue<?>>, Segment> newValue = Maps.transformValues(this.segments,
                dataEntry -> dataEntry == null ? null : dataEntry.withoutParents());
        return newWithUpdate(newValue, createBulkUpdateFunc(newValue.keySet()));
    }

    @Override
    public ImmutableSubjectData clearParents(Set<ContextValue<?>> segments) {
        if (!this.segments.containsKey(segments)) {
            return this;
        }
        return newWithUpdated(segments, getSegmentOrNew(segments).withoutParents());
    }

    @Override
    public int getDefaultValue(Set<ContextValue<?>> segments) {
        Segment ent = this.segments.get(segments);
        return ent == null || ent.getPermissionDefault() == null ? 0 : ent.getPermissionDefault();
    }

    @Override
    public ImmutableSubjectData setDefaultValue(Set<ContextValue<?>> segments, int defaultValue) {
        return newWithUpdated(segments, getSegmentOrNew(segments).withDefaultValue(defaultValue));
    }

    @Override
    public Set<Set<ContextValue<?>>> getActiveContexts() {
        return segments.keySet();
    }

    @Override
    public Map<Set<ContextValue<?>>, Integer> getAllDefaultValues() {
        return Maps.filterValues(Maps.transformValues(segments,
                dataEntry -> dataEntry == null ? null : dataEntry.getPermissionDefault()), v -> v != null);
    }

    public void doUpdates(SqlDao dao) throws SQLException {
        dao.executeInTransaction(() -> {
            List<CheckedBiConsumer<SqlDao, SqlSubjectData, SQLException>> updates = this.updatesToPerform.getAndSet(null);
            if (updates != null) {
                for (CheckedBiConsumer<SqlDao, SqlSubjectData, SQLException> func : updates) {
                    func.accept(dao, this);
                }
            }
            return null;
        });
    }
}
