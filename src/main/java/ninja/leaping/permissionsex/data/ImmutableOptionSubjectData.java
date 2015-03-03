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

import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ImmutableOptionSubjectData {
    Map<Set<Context>, Map<String, String>> getAllOptions();

    Map<String, String> getOptions(Set<Context> contexts);

    ImmutableOptionSubjectData setOption(Set<Context> contexts, String key, String value);

    ImmutableOptionSubjectData clearOptions(Set<Context> contexts);

    ImmutableOptionSubjectData clearOptions();

    Map<Set<Context>, Map<String, Integer>> getAllPermissions();

    Map<String, Integer> getPermissions(Set<Context> set);

    ImmutableOptionSubjectData setPermission(Set<Context> set, String s, int value);

    ImmutableOptionSubjectData clearPermissions();

    ImmutableOptionSubjectData clearPermissions(Set<Context> set);

    Map<Set<Context>, List<Subject>> getAllParents();

    List<Subject> getParents(Set<Context> set);

    ImmutableOptionSubjectData addParent(Set<Context> set, Subject subject);

    ImmutableOptionSubjectData removeParent(Set<Context> set, Subject subject);

    ImmutableOptionSubjectData clearParents();

    ImmutableOptionSubjectData clearParents(Set<Context> set);
}
