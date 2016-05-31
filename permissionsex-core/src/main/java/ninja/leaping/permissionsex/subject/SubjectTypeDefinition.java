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

import java.util.Optional;

/**
 * Provide metadata about a specific subject type
 *
 * Handles:
 * - name validation
 * - name alias handling
 * -
 * - what else does a subject type define?
 */
public abstract class SubjectTypeDefinition {
    private final String typeName;

    public static SubjectTypeDefinition defaultFor(String type) {
        return new DefaultSubjectTypeDefinition(type);
    }

    private static class DefaultSubjectTypeDefinition extends SubjectTypeDefinition {

        public DefaultSubjectTypeDefinition(String typeName) {
            super(typeName);
        }

        @Override
        public boolean isNameValid(String name) {
            return true;
        }

        @Override
        public Optional<String> getAliasForName(String name) {
            return Optional.empty();
        }
    }

    public SubjectTypeDefinition(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return this.typeName;
    }

    public abstract boolean isNameValid(String name);

    public abstract Optional<String> getAliasForName(String name);
}
