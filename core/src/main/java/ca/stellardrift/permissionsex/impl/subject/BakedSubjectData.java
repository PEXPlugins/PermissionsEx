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
package ca.stellardrift.permissionsex.impl.subject;

import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.util.NodeTree;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Represents subject data that has had its hierarchy and contexts flattened
 */
final class BakedSubjectData {
    private final NodeTree permissions;
    private final List<SubjectRef<?>> parents;
    private final Map<String, String> options;

    BakedSubjectData(final NodeTree permissions, final List<SubjectRef<?>> parents, final Map<String, String> options) {
        requireNonNull(permissions, "permissions");
        requireNonNull(parents, "parents");
        requireNonNull(options, "options");
        this.permissions = permissions;
        this.parents = parents;
        this.options = options;
    }

    public NodeTree permissions() {
        return this.permissions;
    }

    public List<SubjectRef<?>> parents() {
        return this.parents;
    }

    public Map<String, String> options() {
        return this.options;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (!(other instanceof BakedSubjectData)) return false;

        final BakedSubjectData that = (BakedSubjectData) other;
        if (!this.options.equals(that.options)) return false;
        if (!this.parents.equals(that.parents)) return false;
        if (!this.permissions.equals(that.permissions)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = this.permissions.hashCode();
        result = 31 * result + this.parents.hashCode();
        result = 31 * result + this.options.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BakedSubjectData{" +
                "permissions=" + this.permissions +
                ", parents=" + this.parents +
                ", options=" + this.options +
                '}';
    }
}
