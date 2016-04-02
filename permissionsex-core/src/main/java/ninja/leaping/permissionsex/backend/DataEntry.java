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
package ninja.leaping.permissionsex.backend;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigSerializable
public class DataEntry {
    public static final ObjectMapper<DataEntry> MAPPER;
    static {
        try {
            MAPPER = ObjectMapper.forClass(DataEntry.class);
        } catch (ObjectMappingException e) {
            throw new ExceptionInInitializerError(e); // This debug indicates a programming issue
        }
    }

    @Nullable @Setting Map<String, Integer> permissions;
    @Nullable @Setting Map<String, String> options;
    @Nullable @Setting List<String> parents;
    @Nullable @Setting("permissions-default") Integer defaultValue;

    DataEntry(@Nullable Map<String, Integer> permissions, @Nullable Map<String, String> options, @Nullable List<String> parents, @Nullable Integer defaultValue) {
        this.permissions = permissions;
        this.options = options;
        this.parents = parents;
        this.defaultValue = defaultValue;
    }

    DataEntry() { // Objectmapper constructor
    }

    public DataEntry withOption(String key, String value) {
        return new DataEntry(permissions, SubjectDataImpl.updateImmutable(options, key, value), parents, defaultValue);
    }

    public DataEntry withoutOption(String key) {
        if (options == null || !options.containsKey(key)) {
            return this;
        }

        Map<String, String> newOptions = new HashMap<>(options);
        newOptions.remove(key);
        return new DataEntry(permissions, newOptions, parents, defaultValue);

    }

    public DataEntry withOptions(Map<String, String> values) {
        return new DataEntry(permissions, values == null ? null : ImmutableMap.copyOf(values), parents, defaultValue);
    }

    public DataEntry withoutOptions() {
        return new DataEntry(permissions, null, parents, defaultValue);
    }

    public DataEntry withPermission(String permission, int value) {
        return new DataEntry(SubjectDataImpl.updateImmutable(permissions, permission, value), options, parents, defaultValue);

    }

    public DataEntry withoutPermission(String permission) {
        if (permissions == null || !permissions.containsKey(permission)) {
            return this;
        }

        Map<String, Integer> newPermissions = new HashMap<>(permissions);
        newPermissions.remove(permission);
        return new DataEntry(newPermissions, options, parents, defaultValue);
    }

    public DataEntry withPermissions(Map<String, Integer> values) {
        return new DataEntry(ImmutableMap.copyOf(values), options, parents, defaultValue);
    }

    public DataEntry withoutPermissions() {
        return new DataEntry(null, options, parents, defaultValue);
    }

    public DataEntry withDefaultValue(Integer defaultValue) {
        return new DataEntry(permissions, options, parents, defaultValue);
    }

    public DataEntry withAddedParent(String parent) {
        ImmutableList.Builder<String> parents = ImmutableList.builder();
        parents.add(parent);
        if (this.parents != null) {
            parents.addAll(this.parents);
        }
        return new DataEntry(permissions, options, parents.build(), defaultValue);
    }

    public DataEntry withRemovedParent(String parent) {
        if (this.parents == null || this.parents.isEmpty()) {
            return this;
        }

        final List<String> newParents = new ArrayList<>(parents);
        newParents.remove(parent);
        return new DataEntry(permissions, options, newParents, defaultValue);
    }

    public DataEntry withParents(List<String> transform) {
        return new DataEntry(permissions, options, transform == null ? null : ImmutableList.copyOf(transform), defaultValue);
    }

    public DataEntry withoutParents() {
        return new DataEntry(permissions, options, null, defaultValue);
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
