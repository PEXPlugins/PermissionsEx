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

import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ImmutableSubjectData {
    Map<Set<Map.Entry<String, String>>, Map<String, String>> getAllOptions();

    Map<String, String> getOptions(Set<Map.Entry<String, String>> contexts);

    ImmutableSubjectData setOption(Set<Map.Entry<String, String>> contexts, String key, String value);

    ImmutableSubjectData setOptions(Set<Map.Entry<String, String>> contexts, Map<String, String> values);

    ImmutableSubjectData clearOptions(Set<Map.Entry<String, String>> contexts);

    ImmutableSubjectData clearOptions();

    Map<Set<Map.Entry<String, String>>, Map<String, Integer>> getAllPermissions();

    Map<String, Integer> getPermissions(Set<Map.Entry<String, String>> contexts);

    ImmutableSubjectData setPermission(Set<Map.Entry<String, String>> contexts, String permission, int value);

    ImmutableSubjectData setPermissions(Set<Map.Entry<String, String>> contexts, Map<String, Integer> values);

    ImmutableSubjectData clearPermissions();

    ImmutableSubjectData clearPermissions(Set<Map.Entry<String, String>> contexts);

    Map<Set<Map.Entry<String, String>>, List<Map.Entry<String, String>>> getAllParents();

    List<Map.Entry<String, String>> getParents(Set<Map.Entry<String, String>> contexts);

    ImmutableSubjectData addParent(Set<Map.Entry<String, String>> contexts, String type, String identifier);

    ImmutableSubjectData removeParent(Set<Map.Entry<String, String>> contexts, String type, String identifier);

    ImmutableSubjectData setParents(Set<Map.Entry<String, String>> contexts, List<Map.Entry<String, String>> parents);

    ImmutableSubjectData clearParents();

    ImmutableSubjectData clearParents(Set<Map.Entry<String, String>> contexts);

    int getDefaultValue(Set<Map.Entry<String, String>> contexts);

    ImmutableSubjectData setDefaultValue(Set<Map.Entry<String, String>> contexts, int defaultValue);

    /**
     * Gets the contexts we have data for
     * @return
     */
    Iterable<Set<Map.Entry<String, String>>> getActiveContexts();

    Map<Set<Map.Entry<String, String>>, Integer> getAllDefaultValues();
}
