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

import javax.annotation.concurrent.Immutable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ImmutableOptionSubjectData {
    /**
     * Provide this set to indicate global contexts
     */
    Set<Map.Entry<String, String>> GLOBAL_CTX = ImmutableSet.of();

    Map<Set<Map.Entry<String, String>>, Map<String, String>> getAllOptions();

    Map<String, String> getOptions(Set<Map.Entry<String, String>> contexts);

    ImmutableOptionSubjectData setOption(Set<Map.Entry<String, String>> contexts, String key, String value);

    ImmutableOptionSubjectData setOptions(Set<Map.Entry<String, String>> contexts, Map<String, String> values);

    ImmutableOptionSubjectData clearOptions(Set<Map.Entry<String, String>> contexts);

    ImmutableOptionSubjectData clearOptions();

    Map<Set<Map.Entry<String, String>>, Map<String, Integer>> getAllPermissions();

    Map<String, Integer> getPermissions(Set<Map.Entry<String, String>> contexts);

    ImmutableOptionSubjectData setPermission(Set<Map.Entry<String, String>> contexts, String permission, int value);

    ImmutableOptionSubjectData setPermissions(Set<Map.Entry<String, String>> contexts, Map<String, Integer> values);

    ImmutableOptionSubjectData clearPermissions();

    ImmutableOptionSubjectData clearPermissions(Set<Map.Entry<String, String>> contexts);

    Map<Set<Map.Entry<String, String>>, List<Map.Entry<String, String>>> getAllParents();

    List<Map.Entry<String, String>> getParents(Set<Map.Entry<String, String>> contexts);

    ImmutableOptionSubjectData addParent(Set<Map.Entry<String, String>> contexts, String type, String identifier);

    ImmutableOptionSubjectData removeParent(Set<Map.Entry<String, String>> contexts, String type, String identifier);

    ImmutableOptionSubjectData setParents(Set<Map.Entry<String, String>> contexts, List<Map.Entry<String, String>> parents);

    ImmutableOptionSubjectData clearParents();

    ImmutableOptionSubjectData clearParents(Set<Map.Entry<String, String>> contexts);

    int getDefaultValue(Set<Map.Entry<String, String>> contexts);

    ImmutableOptionSubjectData setDefaultValue(Set<Map.Entry<String, String>> contexts, int defaultValue);

    /**
     * Gets the contexts we have data for
     * @return
     */
    Iterable<Set<Map.Entry<String, String>>> getActiveContexts();

    Map<Set<Map.Entry<String, String>>, Integer> getAllDefaultValues();
}
