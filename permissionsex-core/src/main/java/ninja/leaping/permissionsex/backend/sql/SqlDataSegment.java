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
import ninja.leaping.permissionsex.data.DataSegment;
import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.util.GuavaCollectors;
import ninja.leaping.permissionsex.util.ThrowingBiConsumer;
import ninja.leaping.permissionsex.util.Tristate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static ninja.leaping.permissionsex.util.Util.appendImmutable;
import static ninja.leaping.permissionsex.util.Util.updateImmutable;

class SqlDataSegment implements DataSegment {
    private volatile int id;
    private final Set<Map.Entry<String, String>> contexts;
    private final int weight;
    private final boolean inheritable;
    private final Map<String, Tristate> permissions;
    private final Map<String, String> options;
    private final List<SqlSubjectRef> parents;
    private final Tristate permissionDefault;
    private final AtomicReference<ImmutableList<ThrowingBiConsumer<SqlDao, SqlDataSegment, SQLException>>> updatesToPerform = new AtomicReference<>();

    SqlDataSegment(int id, Set<Map.Entry<String, String>> contexts, int weight, boolean inheritable, Map<String, Tristate> permissions, Map<String, String> options, List<SqlSubjectRef> parents, Tristate permissionDefault, ImmutableList<ThrowingBiConsumer<SqlDao, SqlDataSegment, SQLException>> updates) {
        this.id = id;
        this.contexts = ImmutableSet.copyOf(contexts);
        this.weight = weight;
        this.inheritable = inheritable;
        this.permissions = permissions;
        this.options = options;
        this.parents = parents;
        this.permissionDefault = permissionDefault;
        this.updatesToPerform.set(updates);
    }

    static SqlDataSegment empty(int id) {
        return new SqlDataSegment(id, ImmutableSet.of(), 0, true, ImmutableMap.of(), ImmutableMap.of(), ImmutableList.of(), null, null);
    }

    static SqlDataSegment empty(int id, Set<Map.Entry<String, String>> contexts) {
        return new SqlDataSegment(id, contexts, 0, true, ImmutableMap.of(), ImmutableMap.of(), ImmutableList.of(), null, null);
    }

    private SqlDataSegment newWithUpdate(Map<String, Tristate> permissions, Map<String, String> options, List<SqlSubjectRef> parents, Tristate permissionDefault, ThrowingBiConsumer<SqlDao, SqlDataSegment, SQLException> updateFunc) {
        return new SqlDataSegment(this.id, this.contexts, this.weight, this.inheritable, permissions, options, parents, permissionDefault, appendImmutable(this.updatesToPerform.get(), updateFunc));
    }

    private SqlDataSegment newWithUpdate(Set<Map.Entry<String, String>> contexts, int weight, boolean inheritable, ThrowingBiConsumer<SqlDao, SqlDataSegment, SQLException> updateFunc) {
        return new SqlDataSegment(this.id, contexts, weight, inheritable, this.permissions, this.options, this.parents, this.permissionDefault, appendImmutable(this.updatesToPerform.get(), updateFunc));
    }

    static SqlDataSegment unallocated() {
        return SqlDataSegment.empty(-1);
    }

    static SqlDataSegment unallocated(Set<Map.Entry<String, String>> contexts) {
        return SqlDataSegment.empty(-1, contexts);
    }

    public int getId() {
        if (id == SqlConstants.UNALLOCATED) {
            throw new IllegalStateException("Unable to find issues");
        }
        return id;
    }

    @Override
    public Set<Map.Entry<String, String>> getContexts() {
        return contexts;
    }

    @Override
    public DataSegment withContexts(Set<Map.Entry<String, String>> contexts) {
        return newWithUpdate(contexts, this.weight, this.inheritable, (dao, seg) -> dao.setContexts(seg, contexts));
    }

    @Override
    public int getWeight() {
        return this.weight;
    }

    @Override
    public DataSegment withWeight(int weight) {
        return newWithUpdate(this.contexts, weight, this.inheritable, SqlDao::updateMetadata);
    }

    @Override
    public boolean isInheritable() {
        return this.inheritable;
    }

    @Override
    public DataSegment withInheritability(boolean inheritable) {
        return newWithUpdate(this.contexts, this.weight, inheritable, SqlDao::updateMetadata);
    }

    @Override
    public Map<String, Tristate> getPermissions() {
        return permissions;
    }

    @Override
    public Map<String, String> getOptions() {
        return options;
    }

    @Override
    public List<SubjectRef> getParents() {
        return ImmutableList.copyOf(parents);
    }

    List<SqlSubjectRef> getSqlParents() {
        return this.parents;
    }

    @Override
    public Tristate getPermissionDefault() {
        return permissionDefault;
    }


    @Override
    public SqlDataSegment withOption(String key, String value) {
        return newWithUpdate(this.permissions, updateImmutable(this.options, key, value), this.parents, this.permissionDefault, (dao, seg) -> {
            dao.setOption(seg, key, value);
        });

    }

    @Override
    public SqlDataSegment withoutOption(String key) {
        if (options == null || !options.containsKey(key)) {
            return this;
        }

        Map<String, String> newOptions = new HashMap<>(options);
        newOptions.remove(key);
        return newWithUpdate(this.permissions, newOptions, this.parents, this.permissionDefault, (dao, seg) -> {
            dao.clearOption(seg, key);
        });
    }

    @Override
    public SqlDataSegment withOptions(Map<String, String> values) {
        Map<String, String> immValues = values == null ? null : ImmutableMap.copyOf(values);
        return newWithUpdate(permissions, immValues, parents, permissionDefault, (dao, seg) -> {
            dao.setOptions(seg, immValues);
        });
    }

    @Override
    public SqlDataSegment withoutOptions() {
        return newWithUpdate(permissions, null, parents, permissionDefault, (dao, seg) -> dao.setOptions(seg, null));
    }

    @Override
    public SqlDataSegment withPermission(String permission, Tristate value) {
        return newWithUpdate(updateImmutable(permissions, permission, value), options, parents, permissionDefault, (dao, seg) -> dao.setPermission(seg, permission, value));

    }

    @Override
    public SqlDataSegment withoutPermission(String permission) {
        if (permissions == null || !permissions.containsKey(permission)) {
            return this;
        }

        Map<String, Tristate> newPermissions = new HashMap<>(permissions);
        newPermissions.remove(permission);
        return newWithUpdate(newPermissions, options, parents, permissionDefault, (dao, seg) -> dao.clearPermission(seg, permission));
    }

    @Override
    public SqlDataSegment withPermissions(Map<String, Tristate> values) {
        Map<String, Tristate> immValues = values == null ? null : ImmutableMap.copyOf(values);
        return newWithUpdate(immValues, options, parents, permissionDefault, (dao, seg) -> dao.setPermissions(seg, immValues));
    }

    @Override
    public SqlDataSegment withoutPermissions() {
        return newWithUpdate(null, options, parents, permissionDefault, (dao, seg) -> dao.setPermissions(seg, null));
    }

    @Override
    public SqlDataSegment withDefaultValue(Tristate permissionDefault) {
        return newWithUpdate(permissions, options, parents, permissionDefault, SqlDao::updateMetadata);
    }

    @Override
    public SqlDataSegment withAddedParent(SubjectRef parent) {
        ImmutableList.Builder<SqlSubjectRef> parents = ImmutableList.builder();
        SqlSubjectRef sqlParent = SqlSubjectRef.of(parent);
        parents.add(sqlParent);
        if (this.parents != null) {
            parents.addAll(this.parents);
        }
        return newWithUpdate(permissions, options, parents.build(), permissionDefault, (dao, seg) -> dao.addParent(seg, sqlParent));
    }

    @Override
    public SqlDataSegment withRemovedParent(SubjectRef parent) {
        if (this.parents == null || this.parents.isEmpty()) {
            return this;
        }
        SqlSubjectRef sqlParent = SqlSubjectRef.of(parent);

        final List<SqlSubjectRef> newParents = new ArrayList<>(parents);
        if (!newParents.remove(sqlParent)) {
            return this;
        }

        return newWithUpdate(permissions, options, newParents, permissionDefault, (dao, seg) -> dao.removeParent(seg, sqlParent));
    }

    @Override
    public SqlDataSegment withParents(List<SubjectRef> parents) {
        List<SqlSubjectRef> immValues = parents == null ? null : parents.stream().map(SqlSubjectRef::of).collect(GuavaCollectors.toImmutableList());
        return newWithUpdate(permissions, options, immValues, permissionDefault, (dao, seg) -> dao.setParents(seg, immValues));
    }

    @Override
    public SqlDataSegment withoutParents() {
        return newWithUpdate(permissions, options, null, permissionDefault, (dao, seg) -> dao.setParents(seg, null));
    }

    List<ThrowingBiConsumer<SqlDao, SqlDataSegment, SQLException>> popUpdates() {
        return this.updatesToPerform.getAndSet(null);
    }

    void doUpdates(SqlDao dao) throws SQLException {
        List<ThrowingBiConsumer<SqlDao, SqlDataSegment, SQLException>> updateFuncs = popUpdates();
        if (updateFuncs != null) {
            for (ThrowingBiConsumer<SqlDao, SqlDataSegment, SQLException> consumer : updateFuncs) {
                consumer.accept(dao, this);
            }
        }
    }

    @Override
    public String toString() {
        return "Segment{" +
                "id=" + id +
                ", contexts=" + contexts +
                ", permissions=" + permissions +
                ", options=" + options +
                ", parents=" + parents +
                ", permissionDefault=" + permissionDefault +
                '}';
    }

    public boolean isEmpty() {
        return (this.permissions == null || this.permissions.isEmpty())
                && (this.options == null || this.options.isEmpty())
                && (this.parents == null || this.parents.isEmpty())
                && this.permissionDefault == null;
    }

    boolean isUnallocated() {
        return this.id == SqlConstants.UNALLOCATED;
    }

    public void setId(int id) {
        this.id = id;
    }
}
