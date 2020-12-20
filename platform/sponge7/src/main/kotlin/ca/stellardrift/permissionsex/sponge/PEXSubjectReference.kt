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

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.subject.SubjectRef
import ca.stellardrift.permissionsex.subject.SubjectType
import java.util.concurrent.CompletableFuture
import org.spongepowered.api.service.permission.Subject
import org.spongepowered.api.service.permission.SubjectReference

data class PEXSubjectReference<I> internal constructor(
    private val type: SubjectType<I>,
    private val ident: I,
    private val pex: PermissionsExPlugin
) : SubjectReference, SubjectRef<I> {
    override fun type(): SubjectType<I> = this.type
    override fun identifier(): I = this.ident

    override fun getCollectionIdentifier(): String {
        return this.type.name()
    }

    override fun getSubjectIdentifier(): String {
        return this.type.serializeIdentifier(this.ident)
    }

    override fun resolve(): CompletableFuture<Subject> {
        return pex.loadCollection(this.type).thenCompose { it.loadSubject(this.subjectIdentifier) }
    }

    companion object {
        fun <I> of(input: SubjectRef<I>, pex: PermissionsExPlugin): PEXSubjectReference<I> {
            return if (input is PEXSubjectReference<I>) {
                input
            } else PEXSubjectReference(input.type(), input.identifier(), pex)
        }

        fun of(input: SubjectReference, pex: PermissionsExPlugin): PEXSubjectReference<*> {
            if (input is PEXSubjectReference<*>) {
                return input
            }
            val type = pex.subjectTypeFromIdentifier(input.collectionIdentifier)

            return of(type, input.subjectIdentifier, pex)
        }

        internal fun <I> of(type: SubjectType<I>, serialized: String, pex: PermissionsExPlugin): PEXSubjectReference<I> {
            return PEXSubjectReference(type, type.parseIdentifier(serialized), pex)
        }
    }
}
