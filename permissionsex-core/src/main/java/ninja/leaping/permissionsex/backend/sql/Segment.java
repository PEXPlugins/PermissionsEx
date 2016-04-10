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
import ninja.leaping.permissionsex.util.ThrowingBiConsumer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static ninja.leaping.permissionsex.util.Util.appendImmutable;
import static ninja.leaping.permissionsex.util.Util.updateImmutable;

class Segment {
    private volatile int id;
    private final Set<Map.Entry<String, String>> contexts;
    private final Map<String, Integer> permissions;
    private final Map<String, String> options;
    private final List<SubjectRef> parents;
    private final Integer permissionDefault;
    private final AtomicReference<ImmutableList<ThrowingBiConsumer<SqlDao, Segment, SQLException>>> updatesToPerform = new AtomicReference<>();

    Segment(int id, Set<Map.Entry<String, String>> contexts, Map<String, Integer> permissions, Map<String, String> options, List<SubjectRef> parents, Integer permissionDefault, ImmutableList<ThrowingBiConsumer<SqlDao, Segment, SQLException>> updates) {
        this.id = id;
        this.contexts = ImmutableSet.copyOf(contexts);
        this.permissions = permissions;
        this.options = options;
        this.parents = parents;
        this.permissionDefault = permissionDefault;
        this.updatesToPerform.set(updates);
    }

    static Segment empty(int id) {
        return new Segment(id, ImmutableSet.of(), ImmutableMap.of(), ImmutableMap.of(), ImmutableList.of(), null, null);
    }

    static Segment empty(int id, Set<Map.Entry<String, String>> contexts) {
        return new Segment(id, contexts, ImmutableMap.of(), ImmutableMap.of(), ImmutableList.of(), null, null);
    }

    private Segment newWithUpdate(Map<String, Integer> permissions, Map<String, String> options, List<SubjectRef> parents, Integer permissionDefault, ThrowingBiConsumer<SqlDao, Segment, SQLException> updateFunc) {
        return new Segment(this.id, this.contexts, permissions, options, parents, permissionDefault, appendImmutable(this.updatesToPerform.get(), updateFunc));
    }

    static Segment unallocated() {
        return Segment.empty(-1);
    }

    static Segment unallocated(Set<Map.Entry<String, String>> contexts) {
        return Segment.empty(-1, contexts);
    }

    public int getId() {
        if (id == SqlConstants.UNALLOCATED) {
            throw new IllegalStateException("Unable to find issues");
        }
        return id;
    }

    public Set<Map.Entry<String, String>> getContexts() {
        return contexts;
    }

    public Map<String, Integer> getPermissions() {
        return permissions;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public List<SubjectRef> getParents() {
        return parents;
    }

    public Integer getPermissionDefault() {
        return permissionDefault;
    }


    public Segment withOption(String key, String value) {
        return newWithUpdate(this.permissions, updateImmutable(this.options, key, value), this.parents, this.permissionDefault, (dao, seg) -> {
            dao.setOption(seg, key, value);
        });

    }

    public Segment withoutOption(String key) {
        if (options == null || !options.containsKey(key)) {
            return this;
        }

        Map<String, String> newOptions = new HashMap<>(options);
        newOptions.remove(key);
        return newWithUpdate(this.permissions, newOptions, this.parents, this.permissionDefault, (dao, seg) -> {
            dao.clearOption(seg, key);
        });
    }

    public Segment withOptions(Map<String, String> values) {
        Map<String, String> immValues = values == null ? null : ImmutableMap.copyOf(values);
        return newWithUpdate(permissions, immValues, parents, permissionDefault, (dao, seg) -> {
            dao.setOptions(seg, immValues);
        });
    }

    public Segment withoutOptions() {
        return newWithUpdate(permissions, null, parents, permissionDefault, (dao, seg) -> dao.setOptions(seg, null));
    }

    public Segment withPermission(String permission, int value) {
        return newWithUpdate(updateImmutable(permissions, permission, value), options, parents, permissionDefault, (dao, seg) -> dao.setPermission(seg, permission, value));

    }

    public Segment withoutPermission(String permission) {
        if (permissions == null || !permissions.containsKey(permission)) {
            return this;
        }

        Map<String, Integer> newPermissions = new HashMap<>(permissions);
        newPermissions.remove(permission);
        return newWithUpdate(newPermissions, options, parents, permissionDefault, (dao, seg) -> dao.clearPermission(seg, permission));
    }

    public Segment withPermissions(Map<String, Integer> values) {
        Map<String, Integer> immValues = values == null ? null : ImmutableMap.copyOf(values);
        return newWithUpdate(immValues, options, parents, permissionDefault, (dao, seg) -> dao.setPermissions(seg, immValues));
    }

    public Segment withoutPermissions() {
        return newWithUpdate(null, options, parents, permissionDefault, (dao, seg) -> dao.setPermissions(seg, null));
    }

    public Segment withDefaultValue(Integer permissionDefault) {
        return newWithUpdate(permissions, options, parents, permissionDefault, (dao, seg) -> dao.setDefaultValue(seg, permissionDefault));
    }

    public Segment withAddedParent(SubjectRef parent) {
        ImmutableList.Builder<SubjectRef> parents = ImmutableList.builder();
        parents.add(parent);
        if (this.parents != null) {
            parents.addAll(this.parents);
        }
        return newWithUpdate(permissions, options, parents.build(), permissionDefault, (dao, seg) -> dao.addParent(seg, parent));
    }

    public Segment withRemovedParent(SubjectRef parent) {
        if (this.parents == null || this.parents.isEmpty()) {
            return this;
        }

        final List<SubjectRef> newParents = new ArrayList<>(parents);
        newParents.remove(parent);
        return newWithUpdate(permissions, options, parents, permissionDefault, (dao, seg) -> dao.removeParent(seg, parent));
    }

    public Segment withParents(List<SubjectRef> parents) {
        List<SubjectRef> immValues = parents == null ? null : ImmutableList.copyOf(parents);
        return newWithUpdate(permissions, options, immValues, permissionDefault, (dao, seg) -> dao.setParents(seg, immValues));
    }

    public Segment withoutParents() {
        return newWithUpdate(permissions, options, null, permissionDefault, (dao, seg) -> dao.setParents(seg, null));
    }

    List<ThrowingBiConsumer<SqlDao, Segment, SQLException>> popUpdates() {
        return this.updatesToPerform.getAndSet(null);
    }

    void doUpdates(SqlDao dao) throws SQLException {
        List<ThrowingBiConsumer<SqlDao, Segment, SQLException>> updateFuncs = popUpdates();
        if (updateFuncs != null) {
            for (ThrowingBiConsumer<SqlDao, Segment, SQLException> consumer : updateFuncs) {
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
