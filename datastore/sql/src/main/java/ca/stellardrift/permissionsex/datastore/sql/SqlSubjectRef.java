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
package ca.stellardrift.permissionsex.datastore.sql;

import ca.stellardrift.permissionsex.impl.PermissionsEx;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public class SqlSubjectRef<I> implements SubjectRef<I> {
    private volatile int id;
    private @Nullable volatile SubjectRef<I> resolved = null;
    private final PermissionsEx<?> pex;
    private final String type, identifier;

    SqlSubjectRef(final PermissionsEx<?> pex, final int id, final String type, final String identifier) {
        this.pex = pex;
        this.id = id;
        this.type = type;
        this.identifier = identifier;
    }

    SqlSubjectRef(final SubjectRef<I> existing) {
        this.id = SqlConstants.UNALLOCATED;
        this.pex = null;
        this.type = existing.type().name();
        this.identifier = null;
        this.resolved = SubjectRef.mapKeySafe(existing);
    }

    public static <I> SqlSubjectRef<I> unresolved(final PermissionsEx<?> pex, final String type, final String name) {
        return new SqlSubjectRef<>(pex, SqlConstants.UNALLOCATED, type, name);
    }

    public int id() {
        if (id == SqlConstants.UNALLOCATED) {
            throw new IllegalStateException("Unallocated SubjectRef tried to be used!");
        }
        return id;
    }

    public void id(int id) {
        this.id = id;
    }

    boolean isUnallocated() {
        return id == SqlConstants.UNALLOCATED;
    }

    public String rawType() {
        return type;
    }

    public String rawIdentifier() {
        if (this.identifier == null) {
            final @Nullable SubjectRef<I> resolved = this.resolved;
            if (resolved == null) {
                throw new IllegalStateException("Unable to get an identifier for this subject ref");
            }
            return resolved.serializedIdentifier();
        }
        return this.identifier;
    }

    @Override
    public String serializedIdentifier() {
        final @Nullable SubjectRef<?> resolved = this.resolved;
        if (resolved != null) {
            return resolved.serializedIdentifier();
        } else {
            return this.identifier;
        }
    }

    @Override
    public SubjectType<I> type() {
        return this.resolved().type();
    }

    @Override
    public I identifier() {
        return this.resolved().identifier();
    }

    @SuppressWarnings({"unchecked"})
    public SubjectRef<I> resolved() {
        if (this.resolved != null) {
            return this.resolved;
        }
        return this.resolved = (SubjectRef<I>) this.pex.deserializeSubjectRef(this.type, this.identifier);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (!(other instanceof SqlSubjectRef)) return false;
        final SqlSubjectRef<?> that = (SqlSubjectRef<?>) other;
        return Objects.equals(type, that.type) &&
                Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.identifier);
    }

    @Override
    public String toString() {
        return "SubjectRef{id=" + (this.id == SqlConstants.UNALLOCATED ? "<unallocated>" : this.id)
                + ",type=" + this.type
                + ",identifier=" + this.identifier
                + "}";
    }

    public static <I> SqlSubjectRef<I> from(final SubjectRef<I> existing) {
        if (existing instanceof SqlSubjectRef<?>) {
            return (SqlSubjectRef<I>) existing;
        } else {
            return new SqlSubjectRef<>(existing);
        }
    }
}
