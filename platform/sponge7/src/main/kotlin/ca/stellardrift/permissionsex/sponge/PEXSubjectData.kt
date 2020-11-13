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

import ca.stellardrift.permissionsex.subject.ImmutableSubjectData
import ca.stellardrift.permissionsex.subject.SubjectRef
import ca.stellardrift.permissionsex.util.Change
import ca.stellardrift.permissionsex.util.ContextSet
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.function.Consumer
import org.spongepowered.api.service.context.Context
import org.spongepowered.api.service.permission.SubjectData
import org.spongepowered.api.service.permission.SubjectReference
import org.spongepowered.api.util.Tristate

/**
 * Wrapper around ImmutableSubjectData that writes to backend each change
 */
class PEXSubjectData(private val data: SubjectRef, private val plugin: PermissionsExPlugin) : SubjectData {
    private val parentsCache: ConcurrentMap<ContextSet, List<SubjectReference>> = ConcurrentHashMap()

    init {
        data.onUpdate { clearCache() }
    }

    /**
     * Provide a boolean representation of success for the Sponge returns.
     *
     * @return Whether or not the old data object is different from the new data object
     */
    private fun CompletableFuture<Change<ImmutableSubjectData?>>.boolSuccess(): CompletableFuture<Boolean> {
        return thenApply { it.old() != it.current() }
    }

    private fun clearCache() {
        synchronized(parentsCache) { parentsCache.clear() }
    }

    override fun getAllOptions(): Map<Set<Context>, Map<String, String>> {
        return data.get().allOptions.keysToSponge()
    }

    override fun getOptions(contexts: Set<Context>): Map<String, String> {
        return data.get().getOptions(contexts.toPex(plugin.manager))
    }

    override fun setOption(contexts: Set<Context>, key: String, value: String?): CompletableFuture<Boolean> {
        return data.update { it.setOption(contexts.toPex(plugin.manager), key, value) }.boolSuccess()
    }

    override fun clearOptions(contexts: Set<Context>): CompletableFuture<Boolean> {
        return data.update { it.clearOptions(contexts.toPex(plugin.manager)) }.boolSuccess()
    }

    override fun clearOptions(): CompletableFuture<Boolean> {
        return data.update { obj: ImmutableSubjectData -> obj.clearOptions() }.boolSuccess()
    }

    override fun getAllPermissions(): Map<Set<Context>, Map<String, Boolean>> {
        return data.get().allPermissions
            .keysToSponge()
            .mapValues { (_, v) -> v.mapValues { it.value > 0 } }
    }

    override fun getPermissions(contexts: Set<Context>): Map<String, Boolean> {
        return data.get().getPermissions(contexts.toPex(plugin.manager))
            .mapValues { (_, v) -> v > 0 }
    }

    override fun setPermission(contexts: Set<Context>, permission: String, state: Tristate): CompletableFuture<Boolean> {
        val value = when (state) {
            Tristate.TRUE -> 1
            Tristate.FALSE -> -1
            Tristate.UNDEFINED -> 0
            else -> throw IllegalStateException("Unknown tristate provided $state")
        }
        return data.update { it.setPermission(contexts.toPex(plugin.manager), permission, value) }.thenApply { true }
    }

    override fun clearPermissions(): CompletableFuture<Boolean> {
        return data.update { it.clearPermissions() }.boolSuccess()
    }

    override fun clearPermissions(contexts: Set<Context>): CompletableFuture<Boolean> {
        return data.update { it.clearPermissions(contexts.toPex(plugin.manager)) }.boolSuccess()
    }

    override fun getAllParents(): Map<Set<Context>, List<SubjectReference>> {
        synchronized(parentsCache) {
            data.get().activeContexts.forEach(Consumer { getParentsInternal(it) })
            return parentsCache.keysToSponge()
        }
    }

    override fun getParents(contexts: Set<Context>): List<SubjectReference> {
        return getParentsInternal(contexts.toPex(plugin.manager))
    }

    private fun getParentsInternal(contexts: ContextSet): List<SubjectReference> {
        val existing = parentsCache[contexts]
        if (existing != null) {
            return existing
        }
        val parents: List<SubjectReference>
        synchronized(parentsCache) {
            val rawParents = data.get().getParents(contexts)
            parents = if (rawParents == null) {
                emptyList()
            } else {
                rawParents.map { (k, v) -> plugin.newSubjectReference(k, v) }
            }
            val existingParents = parentsCache.putIfAbsent(contexts, parents)
            if (existingParents != null) {
                return existingParents
            }
        }
        return parents
    }

    override fun addParent(contexts: Set<Context>, subject: SubjectReference): CompletableFuture<Boolean> {
        PEXSubjectReference.of(subject, plugin) // validate subject reference
        return data.update {
            it.addParent(contexts.toPex(plugin.manager), subject.collectionIdentifier, subject.subjectIdentifier)
        }.boolSuccess()
    }

    override fun removeParent(set: Set<Context>, subject: SubjectReference): CompletableFuture<Boolean> {
        return data.update {
            it.removeParent(set.toPex(plugin.manager), subject.collectionIdentifier, subject.subjectIdentifier)
        }.boolSuccess()
    }

    override fun clearParents(): CompletableFuture<Boolean> {
        return data.update { it.clearParents() }.boolSuccess()
    }

    override fun clearParents(set: Set<Context>): CompletableFuture<Boolean> {
        return data.update { it.clearParents(set.toPex(plugin.manager)) }.boolSuccess()
    }
}

private fun <T> Map<ContextSet, T>.keysToSponge(): Map<Set<Context>, T> {
    return mapKeys { (key, _) -> key.toSponge() }
}
