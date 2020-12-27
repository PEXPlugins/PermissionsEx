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

import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.subject.Segment;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.pcollections.PMap;
import org.pcollections.PSet;
import org.pcollections.PStack;
import org.pcollections.PVector;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

class SqlSegment implements Segment {
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final AtomicReferenceFieldUpdater<SqlSegment, PVector<CheckedBiConsumer<SqlDao, SqlSegment, SQLException>>> UPDATES_MUTATOR = (AtomicReferenceFieldUpdater) AtomicReferenceFieldUpdater.newUpdater(SqlSegment.class, PVector.class, "updatesToPerform");

    private volatile int id;
    private final PSet<ContextValue<?>> contexts;
    private final PMap<String, Integer> permissions;
    private final PMap<String, String> options;
    private final PVector<SqlSubjectRef<?>> parents;
    private final @Nullable Integer permissionDefault;
    private volatile PVector<CheckedBiConsumer<SqlDao, SqlSegment, SQLException>> updatesToPerform;

    SqlSegment(
            final int id,
            final PSet<ContextValue<?>> contexts,
            final PMap<String, Integer> permissions,
            final PMap<String, String> options,
            final PVector<SqlSubjectRef<?>> parents,
            final @Nullable Integer permissionDefault,
            final PVector<CheckedBiConsumer<SqlDao, SqlSegment, SQLException>> updates) {
        this.id = id;
        this.contexts = contexts;
        this.permissions = permissions;
        this.options = options;
        this.parents = parents;
        this.permissionDefault = permissionDefault;
        this.updatesToPerform = updates;
    }

    static SqlSegment empty(final int id) {
        return new SqlSegment(id, PCollections.set(), PCollections.map(), PCollections.map(), PCollections.vector(), null, PCollections.vector());
    }

    static SqlSegment empty(final int id, PSet<ContextValue<?>> contexts) {
        return new SqlSegment(id, contexts, PCollections.map(), PCollections.map(), PCollections.vector(), null, PCollections.vector());
    }

    private SqlSegment newWithUpdate(
            final PMap<String, Integer> permissions,
            final PMap<String, String> options,
            final PVector<SqlSubjectRef<?>> parents,
            final @Nullable Integer permissionDefault,
            final CheckedBiConsumer<SqlDao, SqlSegment, SQLException> updateFunc) {
        return new SqlSegment(this.id, this.contexts, permissions, options, parents, permissionDefault, this.updatesToPerform.plus(updateFunc));
    }

    static SqlSegment unallocated() {
        return SqlSegment.empty(SqlConstants.UNALLOCATED);
    }

    static SqlSegment unallocated(PSet<ContextValue<?>> contexts) {
        return SqlSegment.empty(SqlConstants.UNALLOCATED, contexts);
    }

    static SqlSegment from(final PSet<ContextValue<?>> contexts, final Segment other) {
        if (other instanceof SqlSegment) {
            return ((SqlSegment) other).contexts(contexts);
        } else {
            return new SqlSegment(
                    SqlConstants.UNALLOCATED,
                    contexts,
                    PCollections.asMap(other.permissions()),
                    PCollections.asMap(other.options()),
                    PCollections.asVector(other.parents(), SqlSubjectRef::from),
                    other.fallbackPermission() == 0 ? null : other.fallbackPermission(),
                    PCollections.vector()
            );
        }
    }

    public int id() {
        if (id == SqlConstants.UNALLOCATED) {
            throw new IllegalStateException("Unable to find issues");
        }
        return id;
    }

    boolean isUnallocated() {
        return this.id == SqlConstants.UNALLOCATED;
    }

    public void id(int id) {
        this.id = id;
    }

    public PSet<ContextValue<?>> contexts() {
        return this.contexts;
    }

    SqlSegment contexts(final PSet<ContextValue<?>> contexts) {
        if (contexts.equals(this.contexts)) {
            return this;
        } else {
            return new SqlSegment(
                    SqlConstants.UNALLOCATED,
                    contexts,
                    this.permissions,
                    this.options,
                    this.parents,
                    this.permissionDefault,
                    PCollections.vector()
            );
        }
    }

    @Override
    public Map<String, Integer> permissions() {
        return this.permissions;
    }

    @Override
    public Map<String, String> options() {
        return this.options;
    }

    @Override
    public List<SqlSubjectRef<?>> parents() {
        return this.parents;
    }

    @Override
    public int fallbackPermission() {
        return this.permissionDefault == null ? 0 : this.permissionDefault;
    }


    @Override
    public SqlSegment withOption(String key, String value) {
        return newWithUpdate(
                this.permissions,
                this.options.plus(key, value),
                this.parents,
                this.permissionDefault,
                (dao, seg) -> dao.setOption(seg, key, value));

    }

    @Override
    public SqlSegment withoutOption(final String key) {
        if (!this.options.containsKey(key)) {
            return this;
        }

        return newWithUpdate(
                this.permissions,
                this.options.minus(key),
                this.parents,
                this.permissionDefault,
                (dao, seg) -> dao.clearOption(seg, key));
    }

    @Override
    public SqlSegment withOptions(final Map<String, String> values) {
        final PMap<String, String> immValues = PCollections.asMap(values);
        return newWithUpdate(
                this.permissions,
                immValues,
                this.parents,
                this.permissionDefault,
                (dao, seg) -> dao.setOptions(seg, immValues));
    }

    @Override
    public SqlSegment withoutOptions() {
        return newWithUpdate(
                this.permissions,
                PCollections.map(),
                this.parents,
                this.permissionDefault,
                (dao, seg) -> dao.setOptions(seg, null));
    }

    @Override
    public SqlSegment withPermission(final String permission, final int value) {
        if (value == 0 && !this.permissions.containsKey(permission)) {
            return this;
        }
        return newWithUpdate(
                value == 0 ? this.permissions.minus(permission) : this.permissions.plus(permission, value),
                this.options,
                this.parents,
                this.permissionDefault,
                (dao, seg) -> dao.setPermission(seg, permission, value));

    }

    @Override
    public SqlSegment withPermissions(final Map<String, Integer> values) {
        final PMap<String, Integer> immValues = PCollections.asMap(values);
        return newWithUpdate(
                immValues,
                this.options,
                this.parents,
                this.permissionDefault,
                (dao, seg) -> dao.setPermissions(seg, immValues));
    }

    @Override
    public SqlSegment withoutPermissions() {
        return newWithUpdate(
                PCollections.map(),
                options,
                parents,
                permissionDefault,
                (dao, seg) -> dao.setPermissions(seg, null));
    }

    @Override
    public SqlSegment withFallbackPermission(final int permissionDefault) {
        return newWithUpdate(
                this.permissions,
                this.options,
                this.parents,
                permissionDefault == 0 ? null : permissionDefault,
                (dao, seg) -> dao.setDefaultValue(seg, permissionDefault));
    }

    @Override
    public <I> SqlSegment plusParent(final SubjectRef<I> parent) {
        final SqlSubjectRef<?> sqlParent = SqlSubjectRef.from(parent);
        return newWithUpdate(
                this.permissions,
                this.options,
                this.parents.plus(0, sqlParent),
                this.permissionDefault,
                (dao, seg) -> dao.addParent(seg, sqlParent));
    }

    @Override
    public <I> SqlSegment minusParent(final SubjectRef<I> parent) {
        if (this.parents.isEmpty()) {
            return this;
        }

        final SqlSubjectRef<I> sqlParent = SqlSubjectRef.from(parent);
        return newWithUpdate(
                this.permissions,
                this.options,
                this.parents.minus(sqlParent),
                this.permissionDefault,
                (dao, seg) -> dao.removeParent(seg, sqlParent));
    }

    @Override
    public SqlSegment withParents(final List<SubjectRef<?>> parents) {
        final PVector<SqlSubjectRef<?>> immValues = PCollections.asVector(parents, SqlSubjectRef::from);
        return newWithUpdate(
                this.permissions,
                this.options,
                immValues,
                this.permissionDefault,
                (dao, seg) -> dao.setParents(seg, immValues));
    }

    @Override
    public SqlSegment withoutParents() {
        return newWithUpdate(
                this.permissions,
                this.options,
                PCollections.vector(),
                this.permissionDefault,
                (dao, seg) -> dao.setParents(seg, null));
    }

    @Override
    public Segment cleared() {
        return newWithUpdate(
                PCollections.map(),
                PCollections.map(),
                PCollections.vector(),
                null,
                SqlDao::removeSegment);
    }

    @Override
    public SqlSegment mergeFrom(final Segment other) {
        if (this.empty() && other instanceof SqlSegment) {
            // super would just return other
            return ((SqlSegment) other).contexts(PCollections.set());
        }

        return (SqlSegment) Segment.super.mergeFrom(other);
    }

    PVector<CheckedBiConsumer<SqlDao, SqlSegment, SQLException>> popUpdates() {
        return UPDATES_MUTATOR.getAndSet(this, PCollections.vector());
    }

    void doUpdates(final SqlDao dao) throws SQLException {
        final PVector<CheckedBiConsumer<SqlDao, SqlSegment, SQLException>> updateFuncs = popUpdates();
        for (final CheckedBiConsumer<SqlDao, SqlSegment, SQLException> consumer : updateFuncs) {
            consumer.accept(dao, this);
        }
    }


    @Override
    public String toString() {
        return "Segment{" +
                "id=" + this.id +
                ", contexts=" + this.contexts +
                ", permissions=" + this.permissions +
                ", options=" + this.options +
                ", parents=" + this.parents +
                ", permissionDefault=" + this.permissionDefault +
                '}';
    }

    @Override
    public boolean empty() {
        return this.permissions.isEmpty()
                && this.options.isEmpty()
                && this.parents.isEmpty()
                && this.permissionDefault == null;
    }
}
