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
package ninja.leaping.permissionsex.backends.file;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FileOptionSubjectData implements ImmutableOptionSubjectData {
    static class DataEntry {
        @Setting("permissions") private Map<String, Integer> nodes;
        @Setting("options") private Map<String, String> options;
        private Multimap<String, String> parents;
        @Setting("permissions-default") private int defaultValue;

        public DataEntry(Map<String, Integer> nodes, Map<String, String> options, Multimap<String, String> parents, Integer defaultValue) {
            this.nodes = nodes;
            this.options = options;
            this.parents = parents;
            this.defaultValue = defaultValue;
        }
    }
    private final Map<Set<Context>, DataEntry> contexts;

    FileOptionSubjectData(Map<Set<Context>, DataEntry> contexts) {
        this.contexts = contexts;
    }

    @Override
    public Map<Set<Context>, Map<String, String>> getAllOptions() {
        return Maps.transformValues(contexts, new Function<DataEntry, Map<String, String>>() {
            @Nullable
            @Override
            public Map<String, String> apply(@Nullable DataEntry dataEntry) {
                return dataEntry.options;
            }
        });
    }

    @Override
    public Map<String, String> getOptions(Set<Context> contexts) {
        final DataEntry entry = this.contexts.get(contexts);
        return entry == null ? null : entry.options;
    }

    @Override
    public ImmutableOptionSubjectData setOption(Set<Context> contexts, String key, String value) {
        return null;
    }

    @Override
    public ImmutableOptionSubjectData clearOptions(Set<Context> contexts) {
        return null;
    }

    @Override
    public ImmutableOptionSubjectData clearOptions() {
        return null;
    }

    @Override
    public Map<Set<Context>, Map<String, Integer>> getAllPermissions() {
        return Maps.transformValues(contexts, new Function<DataEntry, Map<String, Integer>>() {
            @Nullable
            @Override
            public Map<String, Integer> apply(@Nullable DataEntry dataEntry) {
                return dataEntry.nodes;
            }
        });
    }

    @Override
    public Map<String, Integer> getPermissions(Set<Context> set) {
        final DataEntry entry = this.contexts.get(set);
        return entry == null ? null : entry.nodes;
    }

    @Override
    public ImmutableOptionSubjectData setPermission(Set<Context> contexts, String permission, int value) {
        return null;
    }

    @Override
    public ImmutableOptionSubjectData clearPermissions() {
        return null;
    }

    @Override
    public ImmutableOptionSubjectData clearPermissions(Set<Context> set) {
        return null;
    }

    @Override
    public Map<Set<Context>, List<Subject>> getAllParents() {
        return null;
    }

    @Override
    public List<Subject> getParents(Set<Context> set) {
        return null;
    }

    @Override
    public ImmutableOptionSubjectData addParent(Set<Context> set, Subject subject) {
        return null;
    }

    @Override
    public ImmutableOptionSubjectData removeParent(Set<Context> set, Subject subject) {
        return null;
    }

    @Override
    public ImmutableOptionSubjectData clearParents() {
        return null;
    }

    @Override
    public ImmutableOptionSubjectData clearParents(Set<Context> set) {
        return null;
    }
}
