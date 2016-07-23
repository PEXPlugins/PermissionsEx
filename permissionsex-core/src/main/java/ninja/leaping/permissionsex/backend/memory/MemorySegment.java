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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ninja.leaping.permissionsex.data.DataSegment;
import ninja.leaping.permissionsex.data.SegmentKey;
import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.util.Tristate;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static ninja.leaping.permissionsex.util.Util.updateImmutable;

/**
 * In-memory data segment, with {@link ninja.leaping.configurate.objectmapping.ObjectMapper} annotations.
 */
@ConfigSerializable
public class MemorySegment implements DataSegment {
    private final SegmentKey key;
    @Nullable
    @Setting
    private Map<String, Tristate> permissions;
    @Nullable
    @Setting
    private Map<String, String> options;
    @Nullable
    @Setting
    private List<SubjectRef> parents;
    @Nullable
    @Setting("permissions-default")
    private Tristate defaultValue;

    private MemorySegment(SegmentKey key, @Nullable Map<String, Tristate> permissions, @Nullable Map<String, String> options, @Nullable List<SubjectRef> parents, @Nullable Tristate defaultValue) {
        this.key = key;
        this.permissions = permissions;
        this.options = options;
        this.parents = parents;
        this.defaultValue = defaultValue;
    }

    static MemorySegment fromSegment(DataSegment seg) {
        if (seg instanceof MemorySegment) {
            return (MemorySegment) seg;
        } else {
            return new MemorySegment(seg.getKey(),
                    seg.getPermissions(), seg.getOptions(), seg.getParents(), seg.getPermissionDefault());
        }
    }

    public MemorySegment(SegmentKey key) {
        this.key = key;
    }

    @Override
    public SegmentKey getKey() {
        return this.key;
    }

    @Override
    public MemorySegment withKey(SegmentKey key) {
        return new MemorySegment(checkNotNull(key, "key"), permissions, options, parents, defaultValue);
    }

    @Override
    public Map<String, Tristate> getPermissions() {
        return this.permissions == null ? ImmutableMap.of() : this.permissions;
    }

    @Override
    public Map<String, String> getOptions() {
        return this.options == null ? ImmutableMap.of() : this.options;
    }

    @Override
    public List<SubjectRef> getParents() {
        return this.parents == null ? ImmutableList.of() : this.parents;
    }

    @Override
    public Tristate getPermissionDefault() {
        return this.defaultValue == null ? Tristate.UNDEFINED : this.defaultValue;
    }

    @Override
    public MemorySegment withOption(String key, String value) {
        return new MemorySegment(this.key, permissions, updateImmutable(options, checkNotNull(key, "key"), value), parents, defaultValue);
    }

    @Override
    public MemorySegment withoutOption(String key) {
        checkNotNull(key, "key");
        if (options == null || !options.containsKey(key)) {
            return this;
        }

        Map<String, String> newOptions = new HashMap<>(options);
        newOptions.remove(key);
        return new MemorySegment(this.key, permissions, newOptions, parents, defaultValue);
    }

    @Override
    public MemorySegment withOptions(Map<String, String> values) {
        return new MemorySegment(this.key, permissions, values == null ? null : ImmutableMap.copyOf(values), parents, defaultValue);
    }

    @Override
    public MemorySegment withoutOptions() {
        return new MemorySegment(this.key, permissions, null, parents, defaultValue);
    }

    @Override
    public MemorySegment withPermission(String permission, Tristate value) {
        return new MemorySegment(this.key, updateImmutable(permissions, checkNotNull(permission, "permission"), value), options, parents, defaultValue);
    }

    @Override
    public MemorySegment withoutPermission(String permission) {
        checkNotNull(permission, "permission");
        if (permissions == null || !permissions.containsKey(permission)) {
            return this;
        }

        Map<String, Tristate> newPermissions = new HashMap<>(permissions);
        newPermissions.remove(permission);
        return new MemorySegment(this.key, newPermissions, options, parents, defaultValue);
    }

    @Override
    public MemorySegment withPermissions(Map<String, Tristate> values) {
        return new MemorySegment(this.key, ImmutableMap.copyOf(values), options, parents, defaultValue);
    }

    @Override
    public MemorySegment withoutPermissions() {
        return new MemorySegment(this.key, null, options, parents, defaultValue);
    }

    @Override
    public MemorySegment withDefaultValue(Tristate defaultValue) {
        return new MemorySegment(this.key, permissions, options, parents, defaultValue);
    }

    @Override
    public MemorySegment withAddedParent(SubjectRef parent) {
        ImmutableList.Builder<SubjectRef> parents = ImmutableList.builder();
        parents.add(parent);
        if (this.parents != null) {
            parents.addAll(this.parents);
        }
        return new MemorySegment(this.key, permissions, options, parents.build(), defaultValue);
    }

    @Override
    public MemorySegment withRemovedParent(SubjectRef parent) {
        if (this.parents == null || this.parents.isEmpty()) {
            return this;
        }

        final List<SubjectRef> newParents = new ArrayList<>(parents);
        newParents.remove(parent);
        return new MemorySegment(this.key, permissions, options, newParents, defaultValue);
    }

    @Override
    public MemorySegment withParents(List<SubjectRef> parents) {
        return new MemorySegment(this.key, permissions, options, parents == null ? null : ImmutableList.copyOf(parents), defaultValue);
    }

    @Override
    public MemorySegment withoutParents() {
        return new MemorySegment(this.key, permissions, options, null, defaultValue);
    }

    @Override
    public String toString() {
        return "DataEntry{" +
                "permissions=" + permissions +
                ", options=" + options +
                ", parents=" + parents +
                ", defaultValue=" + defaultValue +
                '}';
    }

    public boolean isEmpty() {
        return (this.permissions == null || this.permissions.isEmpty())
                && (this.options == null || this.options.isEmpty())
                && (this.parents == null || this.parents.isEmpty())
                && this.defaultValue == null;
    }
}
