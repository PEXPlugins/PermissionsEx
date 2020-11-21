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

import java.util.concurrent.CompletableFuture
import org.spongepowered.api.service.permission.Subject
import org.spongepowered.api.service.permission.SubjectReference

data class PEXSubjectReference internal constructor(
    override val key: String,
    override val value: String,
    private val pex: PermissionsExPlugin
) : SubjectReference, MutableMap.MutableEntry<String, String> {

    init {
        require(pex.manager.getSubjects(key).typeInfo.isNameValid(value)) { "Name '$value' was not a valid name for a subject in collection '$key'!" }
    }

    override fun getCollectionIdentifier(): String {
        return key
    }

    override fun getSubjectIdentifier(): String {
        return value
    }

    override fun resolve(): CompletableFuture<Subject> {
        return pex.loadCollection(key).thenCompose { it.loadSubject(value) }
    }

    override fun setValue(newValue: String): String {
        throw UnsupportedOperationException("immutable")
    }

    companion object {
        fun of(input: Map.Entry<String, String>, pex: PermissionsExPlugin): PEXSubjectReference {
            return if (input is PEXSubjectReference) {
                input
            } else PEXSubjectReference(input.key, input.value, pex)
        }

        fun of(input: SubjectReference, pex: PermissionsExPlugin): PEXSubjectReference {
            if (input is PEXSubjectReference) {
                return input
            }
            return PEXSubjectReference(input.collectionIdentifier, input.subjectIdentifier, pex)
        }
    }
}
