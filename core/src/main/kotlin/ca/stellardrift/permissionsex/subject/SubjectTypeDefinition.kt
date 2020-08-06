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

package ca.stellardrift.permissionsex.subject

/**
 * Provide metadata about a specific type of attachment.
 *
 * @param <A> type of attached value
 */
abstract class SubjectTypeDefinition<A> @JvmOverloads constructor(
    val typeName: String,
    private val transientHasPriority: Boolean = true
) {
    /**
     * Return whether or not transient data takes priority over persistent for this subject type.
     *
     * @return Whether or not transient data has priority.
     */
    fun transientHasPriority(): Boolean {
        return transientHasPriority
    }

    /**
     * Check if a name is a valid identifier for a given subject collection
     *
     * @param name The identifier to check
     * @return Whether or not the given name is a valid identifier
     */
    abstract fun isNameValid(name: String): Boolean

    /**
     * Return the internal identifier to be used for a subject given its friendly name.
     * If the given name is already a valid identifier, this method may return an empty optional.
     *
     * @param name The friendly name that may be used
     * @return A standard representation of the subject identifier
     */
    abstract fun getAliasForName(name: String): String?

    /**
     * The native object that may be held
     *
     * @param identifier type
     * @return A native object that has its permissions defined by this subject
     */
    abstract fun getAssociatedObject(identifier: String): A?
}

class FixedEntriesSubjectTypeDefinition<A> internal constructor(
    typeName: String,
    private val validEntries: Map<String, () -> A>
) : SubjectTypeDefinition<A>(typeName) {
    override fun isNameValid(name: String): Boolean {
        return this.validEntries.containsKey(name)
    }

    override fun getAliasForName(name: String): String? = null

    override fun getAssociatedObject(identifier: String): A? {
        return this.validEntries[identifier]?.invoke()
    }
}

private class DefaultSubjectTypeDefinition(typeName: String, transientHasPriority: Boolean) :
    SubjectTypeDefinition<Unit>(typeName, transientHasPriority) {

    override fun isNameValid(name: String): Boolean = true
    override fun getAliasForName(name: String): String? = null
    override fun getAssociatedObject(identifier: String): Unit? = null
}

@JvmOverloads
fun subjectType(type: String, transientHasPriority: Boolean = true): SubjectTypeDefinition<Unit> {
    return DefaultSubjectTypeDefinition(type, transientHasPriority)
}

fun <A> subjectType(type: String, vararg validEntries: Pair<String, () -> A>): SubjectTypeDefinition<A> {
    return FixedEntriesSubjectTypeDefinition(type, mapOf(*validEntries))
}
