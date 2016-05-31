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
package ninja.leaping.permissionsex.data;


import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.util.WeightedImmutableSet;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public interface ImmutableSubjectData {
    Set<Map.Entry<String, String>> DEFAULT_CONTEXTS = PermissionsEx.GLOBAL_CONTEXT;
    int DEFAULT_WEIGHT = 0;
    boolean DEFAULT_INHERITABILITY = true;

    /**
     * Return an immutable list of all data segments this subject data object has an attachment to, sorted by weight
     *
     * @return The segments of data to get
     */
    WeightedImmutableSet<DataSegment> getAllSegments();

    WeightedImmutableSet<DataSegment> getAllSegments(Set<Map.Entry<String, String>> contexts, boolean inheritable);

    default ImmutableSubjectData withSegment(DataSegment segment) {
        return updateOrCreateSegment(segment.getContexts(), segment.getWeight(), segment.isInheritable(),
                old -> old.withPermissions(segment.getPermissions())
                .withOptions(segment.getOptions())
                .withParents(segment.getParents())
                .withDefaultValue(segment.getPermissionDefault()));
    }

    DataSegment getOrCreateSegment(Set<Map.Entry<String, String>> contexts, int weight, boolean inheritable);

    default DataSegment getOrCreateSegment(Set<Map.Entry<String, String>> contexts, int weight) {
        return getOrCreateSegment(contexts, weight, DEFAULT_INHERITABILITY);
    }

    default DataSegment getOrCreateSegment(Set<Map.Entry<String, String>> contexts, boolean inheritable) {
        return getOrCreateSegment(contexts, DEFAULT_WEIGHT, inheritable);
    }

    default DataSegment getOrCreateSegment(int weight, boolean inheritable) {
        return getOrCreateSegment(DEFAULT_CONTEXTS, weight, inheritable);
    }

    default DataSegment getOrCreateSegment(Set<Map.Entry<String, String>> contexts) {
        return getOrCreateSegment(contexts, DEFAULT_WEIGHT, DEFAULT_INHERITABILITY);
    }

    default DataSegment getOrCreateSegment(int weight) {
        return getOrCreateSegment(DEFAULT_CONTEXTS, weight, DEFAULT_INHERITABILITY);
    }

    default DataSegment getOrCreateSegment(boolean inheritable) {
        return getOrCreateSegment(DEFAULT_CONTEXTS, DEFAULT_WEIGHT, inheritable);
    }

    ImmutableSubjectData updateOrCreateSegment(Set<Map.Entry<String, String>> contexts, int weight, boolean inheritable, Function<DataSegment, DataSegment> updateFunc);

    default ImmutableSubjectData updateOrCreateSegment(Set<Map.Entry<String, String>> contexts, int weight, Function<DataSegment, DataSegment> updateFunc) {
        return updateOrCreateSegment(contexts, weight, DEFAULT_INHERITABILITY, updateFunc);
    }

    default ImmutableSubjectData updateOrCreateSegment(Set<Map.Entry<String, String>> contexts, boolean inheritable, Function<DataSegment, DataSegment> updateFunc) {
        return updateOrCreateSegment(contexts, DEFAULT_WEIGHT, inheritable, updateFunc);
    }

    default ImmutableSubjectData updateOrCreateSegment(int weight, boolean inheritable, Function<DataSegment, DataSegment> updateFunc) {
        return updateOrCreateSegment(DEFAULT_CONTEXTS, weight, inheritable, updateFunc);
    }

    default ImmutableSubjectData updateOrCreateSegment(Set<Map.Entry<String, String>> contexts, Function<DataSegment, DataSegment> updateFunc) {
        return updateOrCreateSegment(contexts, DEFAULT_WEIGHT, DEFAULT_INHERITABILITY, updateFunc);
    }

    default ImmutableSubjectData updateOrCreateSegment(int weight, Function<DataSegment, DataSegment> updateFunc) {
        return updateOrCreateSegment(DEFAULT_CONTEXTS, weight, DEFAULT_INHERITABILITY, updateFunc);
    }

    default ImmutableSubjectData updateOrCreateSegment(boolean inheritable, Function<DataSegment, DataSegment> updateFunc) {
        return updateOrCreateSegment(DEFAULT_CONTEXTS, DEFAULT_WEIGHT, inheritable, updateFunc);
    }


    ImmutableSubjectData clearOptions();

    ImmutableSubjectData clearPermissions();

    ImmutableSubjectData clearParents();

    ImmutableSubjectData clearDefaultValues();

}
