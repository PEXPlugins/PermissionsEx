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
package ca.stellardrift.permissionsex.subject;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class FixedEntriesSubjectTypeDefinition<AttachmentType> extends SubjectTypeDefinition<AttachmentType> {
    private final Map<String, Supplier<AttachmentType>> validEntries;

    public FixedEntriesSubjectTypeDefinition(String typeName, Map<String, Supplier<AttachmentType>> validEntries) {
        super(typeName);
        this.validEntries = ImmutableMap.copyOf(validEntries);
    }

    @Override
    public boolean isNameValid(String name) {
        return validEntries.containsKey(name);
    }

    @Override
    public Optional<String> getAliasForName(String name) {
        return Optional.empty();
    }

    @Override
    public Optional<AttachmentType> getAssociatedObject(String identifier) {
        Supplier<AttachmentType> type = validEntries.get(identifier);
        if (type != null) {
            return Optional.ofNullable(type.get());
        }
        return Optional.empty();
    }
}
