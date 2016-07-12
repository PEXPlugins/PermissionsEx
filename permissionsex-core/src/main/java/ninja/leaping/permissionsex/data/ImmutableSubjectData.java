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


import ninja.leaping.permissionsex.util.WeightedImmutableSet;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static ninja.leaping.permissionsex.data.DataSegment.DEFAULT_CONTEXTS;
import static ninja.leaping.permissionsex.data.DataSegment.DEFAULT_INHERITABILITY;
import static ninja.leaping.permissionsex.data.DataSegment.DEFAULT_WEIGHT;

public interface ImmutableSubjectData {

    /**
     * Return an immutable list of all data segments this subject data object has an attachment to, sorted by weight
     *
     * @return The segments of data to get
     */
    WeightedImmutableSet<DataSegment> getAllSegments();

    WeightedImmutableSet<DataSegment> getAllSegments(Set<Map.Entry<String, String>> contexts, boolean inheritable);

    default ImmutableSubjectData withSegment(DataSegment segment) {
        return updateSegment(segment.getContexts(), segment.getWeight(), segment.isInheritable(),
                old -> old.withPermissions(segment.getPermissions())
                .withOptions(segment.getOptions())
                .withParents(segment.getParents())
                .withDefaultValue(segment.getPermissionDefault()));
    }

    /**
     * Get a segment with the specified parameters. If such a segment does not exist, a new one will be instantiated to edit.
     * @param contexts The contexts of the segment
     * @param weight The segment's weight
     * @param inheritable Whether or not the segment is inheritable
     * @return The segment matching these parameters
     */
    DataSegment getSegment(Set<Map.Entry<String, String>> contexts, int weight, boolean inheritable);

    default DataSegment getSegment(Set<Map.Entry<String, String>> contexts, int weight) {
        return getSegment(contexts, weight, DEFAULT_INHERITABILITY);
    }

    default DataSegment getSegment(Set<Map.Entry<String, String>> contexts, boolean inheritable) {
        return getSegment(contexts, DEFAULT_WEIGHT, inheritable);
    }

    default DataSegment getSegment(int weight, boolean inheritable) {
        return getSegment(DEFAULT_CONTEXTS, weight, inheritable);
    }

    default DataSegment getSegment(Set<Map.Entry<String, String>> contexts) {
        return getSegment(contexts, DEFAULT_WEIGHT, DEFAULT_INHERITABILITY);
    }

    default DataSegment getSegment(int weight) {
        return getSegment(DEFAULT_CONTEXTS, weight, DEFAULT_INHERITABILITY);
    }

    default DataSegment getSegment(boolean inheritable) {
        return getSegment(DEFAULT_CONTEXTS, DEFAULT_WEIGHT, inheritable);
    }

    ImmutableSubjectData updateSegment(Set<Map.Entry<String, String>> contexts, int weight, boolean inheritable, Function<DataSegment, DataSegment> updateFunc);

    default ImmutableSubjectData updateSegment(Set<Map.Entry<String, String>> contexts, int weight, Function<DataSegment, DataSegment> updateFunc) {
        return updateSegment(contexts, weight, DEFAULT_INHERITABILITY, updateFunc);
    }

    default ImmutableSubjectData updateSegment(Set<Map.Entry<String, String>> contexts, boolean inheritable, Function<DataSegment, DataSegment> updateFunc) {
        return updateSegment(contexts, DEFAULT_WEIGHT, inheritable, updateFunc);
    }

    default ImmutableSubjectData updateSegment(int weight, boolean inheritable, Function<DataSegment, DataSegment> updateFunc) {
        return updateSegment(DEFAULT_CONTEXTS, weight, inheritable, updateFunc);
    }

    default ImmutableSubjectData updateSegment(Set<Map.Entry<String, String>> contexts, Function<DataSegment, DataSegment> updateFunc) {
        return updateSegment(contexts, DEFAULT_WEIGHT, DEFAULT_INHERITABILITY, updateFunc);
    }

    default ImmutableSubjectData updateSegment(int weight, Function<DataSegment, DataSegment> updateFunc) {
        return updateSegment(DEFAULT_CONTEXTS, weight, DEFAULT_INHERITABILITY, updateFunc);
    }

    default ImmutableSubjectData updateSegment(boolean inheritable, Function<DataSegment, DataSegment> updateFunc) {
        return updateSegment(DEFAULT_CONTEXTS, DEFAULT_WEIGHT, inheritable, updateFunc);
    }

    ImmutableSubjectData clearOptions();

    ImmutableSubjectData clearPermissions();

    ImmutableSubjectData clearParents();

    ImmutableSubjectData clearDefaultValues();

}
