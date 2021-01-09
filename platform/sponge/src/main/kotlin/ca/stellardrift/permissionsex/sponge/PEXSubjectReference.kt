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
package ca.stellardrift.permissionsex.sponge

import ca.stellardrift.permissionsex.subject.SubjectRef
import ca.stellardrift.permissionsex.subject.SubjectType
import java.util.concurrent.CompletableFuture
import org.spongepowered.api.service.permission.Subject
import org.spongepowered.api.service.permission.SubjectReference

/**
 * Convert an internal subject identifier into a Sponge-compatible representation.
 *
 * May return the same instance
 */
internal fun <I> SubjectRef<I>.asSponge(service: PermissionsExService): PEXSubjectReference<I> {
    return if (this is PEXSubjectReference<I>) {
        this
    } else {
        PEXSubjectReference(this.type(), this.identifier(), service)
    }
}

/**
 * Get the pex-internal representation of a subject reference.
 *
 * May or may not return the same instance.
 */
internal fun SubjectReference.asPex(service: PermissionsExService): PEXSubjectReference<*> {
    if (this is PEXSubjectReference<*>) {
        return this
    }
    val type = service.subjectTypeFromIdentifier(this.collectionIdentifier)

    return PEXSubjectReference(type, this.subjectIdentifier, service, true)
}

data class PEXSubjectReference<I> internal constructor(
    private val type: SubjectType<I>,
    private val ident: I,
    private val service: PermissionsExService
) : SubjectReference, SubjectRef<I> {
    override fun type(): SubjectType<I> = this.type
    override fun identifier(): I = this.ident

    internal constructor(
        type: SubjectType<I>,
        serialized: String,
        service: PermissionsExService,
        @Suppress("UNUSED_PARAMETER") resToAvoidAmbiguity: Boolean
    ) : this(type, type.parseIdentifier(serialized), service)

    override fun getCollectionIdentifier(): String {
        return this.type.name()
    }

    override fun getSubjectIdentifier(): String {
        return this.type.serializeIdentifier(this.ident)
    }

    override fun resolve(): CompletableFuture<Subject> {
        return service.loadCollection(this.type).thenCompose { it.loadSubject(this.subjectIdentifier) }
    }
}
