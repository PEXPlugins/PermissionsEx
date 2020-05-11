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

package ca.stellardrift.permissionsex.subject;

import java.util.Optional;

/**
 * Provide metadata about a specific type of attachment
 * @param <AttachmentType> type of attached value
 */
public abstract class SubjectTypeDefinition<AttachmentType> {
    private final String typeName;
    private final boolean transientHasPriority;

    /**
     * Create a new subject type definition that represents the default options for a subject type
     *
     * @param type The name of the type being checked
     * @return The type definition
     */
    public static SubjectTypeDefinition<Void> defaultFor(String type) {
        return new DefaultSubjectTypeDefinition(type);
    }


    public static SubjectTypeDefinition<Void> defaultFor(String type, boolean transientHasPriority) {
        return new DefaultSubjectTypeDefinition(type, transientHasPriority);
    }

    private static class DefaultSubjectTypeDefinition extends SubjectTypeDefinition<Void> {

        public DefaultSubjectTypeDefinition(String typeName) {
            super(typeName);
        }

        public DefaultSubjectTypeDefinition(String typeName, boolean transientHasPriority) {
            super(typeName, transientHasPriority);
        }

        @Override
        public boolean isNameValid(String name) {
            return true;
        }

        @Override
        public Optional<String> getAliasForName(String name) {
            return Optional.empty();
        }

        @Override
        public Optional<Void> getAssociatedObject(String identifier) {
            return Optional.empty();
        }
    }

    public SubjectTypeDefinition(String typeName) {
        this(typeName, true);
    }

    public SubjectTypeDefinition(String typeName, boolean transientHasPriority) {
        this.typeName = typeName;
        this.transientHasPriority = transientHasPriority;
    }

    public String getTypeName() {
        return this.typeName;
    }

    /**
     * Return whether or not transient data takes priority over persistent for this subject type.
     *
     * @return Whether or not transient data has priority.
     */
    public boolean transientHasPriority() {
        return this.transientHasPriority;
    }

    /**
     * Check if a name is a valid identifier for a given subject collection
     *
     * @param name The identifier to check
     * @return Whether or not the given name is a valid identifier
     */
    public abstract boolean isNameValid(String name);

    /**
     * Return the internal identifier to be used for a subject given its friendly name.
     * If the given name is already a valid identifier, this method may return an empty optional.
     *
     * @param name The friendly name that may be used
     * @return A standard representation of the subject identifier
     */
    public abstract Optional<String> getAliasForName(String name);

    /**
     * The native object that may be held
     *
     * @return A native object that has its permissions defined by this subject
     */
    public abstract Optional<AttachmentType> getAssociatedObject(String identifier);
}
