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

import ninja.leaping.permissionsex.util.Weighted;
import ninja.leaping.permissionsex.util.Tristate;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A segment of permissions data with specific flags
 */
public interface DataSegment extends Comparable<DataSegment>, Weighted {
    // -- Applicability flags
    Set<Map.Entry<String, String>> getContexts();

    DataSegment withContexts(Set<Map.Entry<String, String>> contexts);

    DataSegment withWeight(int weight);

    boolean isInheritable();

    DataSegment withInheritability(boolean inheritable);

    // -- The data itself

    Map<String, Tristate> getPermissions();

    Map<String, String> getOptions();

    List<SubjectRef> getParents();

    Tristate getPermissionDefault();

    DataSegment withOption(String key, String value);

    DataSegment withoutOption(String key);

    DataSegment withOptions(Map<String, String> values);

    DataSegment withoutOptions();

    DataSegment withPermission(String permission, Tristate value);

    DataSegment withoutPermission(String permission);

    DataSegment withPermissions(Map<String, Tristate> values);

    DataSegment withoutPermissions();

    DataSegment withDefaultValue(Tristate permissionDefault);

    DataSegment withAddedParent(SubjectRef parent);

    DataSegment withRemovedParent(SubjectRef parent);

    DataSegment withParents(List<SubjectRef> parents);

    DataSegment withoutParents();

    @Override
    default int compareTo(@Nonnull DataSegment o) {
        return Integer.compare(this.getWeight(), o.getWeight());
    }
}
