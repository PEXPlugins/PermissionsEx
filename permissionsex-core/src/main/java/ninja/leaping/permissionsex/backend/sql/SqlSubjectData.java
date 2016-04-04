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

import com.google.common.collect.ImmutableMap;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Data for SQL-backed subjects
 */
public class SqlSubjectData implements ImmutableSubjectData {
    private final Map<Set<Map.Entry<String, String>>, Segment> segments;

    SqlSubjectData() {
        this(ImmutableMap.of());
    }

    SqlSubjectData(Map<Set<Map.Entry<String, String>>, Segment> segments) {
        this.segments = segments;
    }

    @Override
    public Map<Set<Map.Entry<String, String>>, Map<String, String>> getAllOptions() {
        return null;
    }

    @Override
    public Map<String, String> getOptions(Set<Map.Entry<String, String>> contexts) {
        return null;
    }

    @Override
    public ImmutableSubjectData setOption(Set<Map.Entry<String, String>> contexts, String key, String value) {
        return null;
    }

    @Override
    public ImmutableSubjectData setOptions(Set<Map.Entry<String, String>> contexts, Map<String, String> values) {
        return null;
    }

    @Override
    public ImmutableSubjectData clearOptions(Set<Map.Entry<String, String>> contexts) {
        return null;
    }

    @Override
    public ImmutableSubjectData clearOptions() {
        return null;
    }

    @Override
    public Map<Set<Map.Entry<String, String>>, Map<String, Integer>> getAllPermissions() {
        return null;
    }

    @Override
    public Map<String, Integer> getPermissions(Set<Map.Entry<String, String>> contexts) {
        return null;
    }

    @Override
    public ImmutableSubjectData setPermission(Set<Map.Entry<String, String>> contexts, String permission, int value) {
        return null;
    }

    @Override
    public ImmutableSubjectData setPermissions(Set<Map.Entry<String, String>> contexts, Map<String, Integer> values) {
        return null;
    }

    @Override
    public ImmutableSubjectData clearPermissions() {
        return null;
    }

    @Override
    public ImmutableSubjectData clearPermissions(Set<Map.Entry<String, String>> contexts) {
        return null;
    }

    @Override
    public Map<Set<Map.Entry<String, String>>, List<Map.Entry<String, String>>> getAllParents() {
        return null;
    }

    @Override
    public List<Map.Entry<String, String>> getParents(Set<Map.Entry<String, String>> contexts) {
        return null;
    }

    @Override
    public ImmutableSubjectData addParent(Set<Map.Entry<String, String>> contexts, String type, String identifier) {
        return null;
    }

    @Override
    public ImmutableSubjectData removeParent(Set<Map.Entry<String, String>> contexts, String type, String identifier) {
        return null;
    }

    @Override
    public ImmutableSubjectData setParents(Set<Map.Entry<String, String>> contexts, List<Map.Entry<String, String>> parents) {
        return null;
    }

    @Override
    public ImmutableSubjectData clearParents() {
        return null;
    }

    @Override
    public ImmutableSubjectData clearParents(Set<Map.Entry<String, String>> contexts) {
        return null;
    }

    @Override
    public int getDefaultValue(Set<Map.Entry<String, String>> contexts) {
        return 0;
    }

    @Override
    public ImmutableSubjectData setDefaultValue(Set<Map.Entry<String, String>> contexts, int defaultValue) {
        return null;
    }

    /**
     * Gets the contexts we have data for
     *
     * @return
     */
    @Override
    public Iterable<Set<Map.Entry<String, String>>> getActiveContexts() {
        return null;
    }

    @Override
    public Map<Set<Map.Entry<String, String>>, Integer> getAllDefaultValues() {
        return null;
    }
}
