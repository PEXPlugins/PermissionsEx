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


import com.google.common.base.Preconditions;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Subject type information
 */
public class SubjectRef {
    private final String type, identifier;

    protected SubjectRef(String type, String identifier) {
        this.type = type;
        this.identifier = identifier;
    }

    public static SubjectRef of(String type, String identifier) {
        return new SubjectRef(checkNotNull(type, "type"), checkNotNull(identifier, "identifier"));
    }

    public String getType() {
        return this.type;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubjectRef)) return false;
        SubjectRef that = (SubjectRef) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, identifier);
    }

    @Override
    public String toString() {
        return this.type + ":" + this.identifier;
    }
}
