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
package ca.stellardrift.permissionsex.sponge;

import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

final class PEXSubjectReference<I> implements SubjectReference, SubjectRef<I> {
    private final SubjectType<I> type;
    private final I identifier;
    private final PermissionsExService service;

    /**
     * Convert an internal subject identifier into a Sponge-compatible representation.
     *
     * May return the same instance
     */
    static <I> PEXSubjectReference<I> asSponge(final SubjectRef<I> ref, final PermissionsExService service) {
        if (ref instanceof PEXSubjectReference<?>) {
            return (PEXSubjectReference<I>) ref;
        } else {
            return new PEXSubjectReference<>(ref.type(), ref.identifier(), service);
        }
    }

    /**
     * Get the pex-internal representation of a subject reference.
     *
     * May or may not return the same instance.
     */
    static PEXSubjectReference<?> asPex(final SubjectReference reference, final PermissionsExService service) {
        if (reference instanceof PEXSubjectReference<?>) {
            return (PEXSubjectReference<?>) reference;
        }
        final SubjectType<?> type = service.subjectTypeFromIdentifier(reference.getCollectionIdentifier());

        return new PEXSubjectReference<>(type, reference.getSubjectIdentifier(), service);
    }

    PEXSubjectReference(final SubjectType<I> type, final I identifier, final PermissionsExService service) {
        this.type = type;
        this.identifier = identifier;
        this.service = service;
    }

    PEXSubjectReference(final SubjectType<I> type, final String identifier, final PermissionsExService service) {
        this(type, type.parseIdentifier(identifier), service);
    }

    @Override
    public SubjectType<I> type() {
        return this.type;
    }

    @Override
    public I identifier() {
        return this.identifier;
    }

    @Override
    public String getCollectionIdentifier() {
        return this.type.name();
    }

    @Override
    public String getSubjectIdentifier() {
        return this.type.serializeIdentifier(this.identifier);
    }

    @Override
    public CompletableFuture<Subject> resolve() {
        return service.loadCollection(this.type.name())
            .thenCompose(it -> it.loadSubject(this.getSubjectIdentifier()));
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        final PEXSubjectReference<?> that = (PEXSubjectReference<?>) other;
        return this.type.equals(that.type)
            && this.identifier.equals(that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.identifier);
    }

    @Override
    public String toString() {
        return "PEXSubjectReference{" +
            "type=" + this.type +
            ", identifier=" + this.identifier +
            '}';
    }

}
