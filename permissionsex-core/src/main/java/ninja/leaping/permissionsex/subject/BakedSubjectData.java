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
package ninja.leaping.permissionsex.subject;

import com.google.common.base.Preconditions;
import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.util.NodeTree;

import java.util.List;
import java.util.Map;

/**
 * Represents subject data that has had its hierarchy and contexts flattened
 */
class BakedSubjectData {
    private final NodeTree permissions;
    private final List<SubjectRef> parents;
    private final Map<String, String> options;

    public BakedSubjectData(NodeTree permissions, List<SubjectRef> parents, Map<String, String> options) {
        Preconditions.checkNotNull(permissions, "permissions");
        Preconditions.checkNotNull(parents, "parents");
        Preconditions.checkNotNull(options, "options");
        this.permissions = permissions;
        this.parents = parents;
        this.options = options;
    }

    public NodeTree getPermissions() {
        return permissions;
    }

    public List<SubjectRef> getParents() {
        return parents;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BakedSubjectData that = (BakedSubjectData) o;

        if (!options.equals(that.options)) return false;
        if (!parents.equals(that.parents)) return false;
        if (!permissions.equals(that.permissions)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = permissions.hashCode();
        result = 31 * result + parents.hashCode();
        result = 31 * result + options.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BakedSubjectData{" +
                "permissions=" + permissions +
                ", parents=" + parents +
                ", options=" + options +
                '}';
    }
}
