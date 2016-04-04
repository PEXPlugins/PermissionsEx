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

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Segment {
    private final int id;
    private final Set<Map.Entry<String, String>> contexts;
    private final Map<String, Integer> permissions;
    private final Map<String, String> options;
    private final List<SubjectRef> parents;
    private final Integer permissionDefault;

    Segment(int id, Set<Map.Entry<String, String>> contexts, Map<String, Integer> permissions, Map<String, String> options, List<SubjectRef> parents, Integer permissionDefault) {
        this.id = id;
        this.contexts = ImmutableSet.copyOf(contexts);
        this.permissions = ImmutableMap.copyOf(permissions);
        this.options = ImmutableMap.copyOf(options);
        this.parents = ImmutableList.copyOf(parents);
        this.permissionDefault = permissionDefault;
    }

    static Segment empty(int id) {
        return new Segment(id, ImmutableSet.of(), ImmutableMap.of(), ImmutableMap.of(), ImmutableList.of(), null);
    }

    public int getId() {
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
}
