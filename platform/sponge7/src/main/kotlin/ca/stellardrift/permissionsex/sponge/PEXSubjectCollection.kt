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

import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.subject.SubjectType
import ca.stellardrift.permissionsex.subject.SubjectTypeCollection
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collectors
import org.spongepowered.api.service.context.Context
import org.spongepowered.api.service.permission.Subject
import org.spongepowered.api.service.permission.SubjectCollection
import org.spongepowered.api.service.permission.SubjectReference
import org.spongepowered.api.util.Tristate

/**
 * Subject collection
 */
class PEXSubjectCollection<I> private constructor(private val identifier: SubjectType<I>, val plugin: PermissionsExPlugin) :
    SubjectCollection {
    val type: SubjectTypeCollection<I> = plugin.manager.subjects(identifier)
    private var defaults: PEXSubject? = null
    private val subjectCache: AsyncLoadingCache<String, PEXSubject> = Caffeine.newBuilder()
        .executor(plugin.manager.asyncExecutor())
        .buildAsync { key, _ -> PEXSubject.load(key, this@PEXSubjectCollection) }

    companion object {
        fun <I> load(identifier: SubjectType<I>, plugin: PermissionsExPlugin): CompletableFuture<PEXSubjectCollection<I>> {
            val ret = PEXSubjectCollection(identifier, plugin)
            val defaultFuture =
                if (identifier.equals(plugin.manager.defaults().type())) {
                    ret.loadSubject(plugin.manager.defaults().type().name())
                } else {
                    plugin.loadCollection(plugin.manager.defaults().type()).thenCompose { it.loadSubject(identifier.name()) }
                }
            return defaultFuture.thenApply {
                ret.defaults = it as PEXSubject?
                ret
            }
        }
    }

    override fun getIdentifier(): String {
        return this.identifier.name()
    }

    override fun getIdentifierValidityPredicate(): Predicate<String> {
        return Predicate(this.identifier::isIdentifierValid)
    }

    override fun getSubject(identifier: String): Optional<Subject> {
        return try {
            val future = subjectCache.getIfPresent(identifier)
            future?.get().optionally()
        } catch (e: InterruptedException) {
            Optional.empty()
        } catch (e: ExecutionException) {
            Optional.empty()
        }
    }

    override fun hasSubject(identifier: String): CompletableFuture<Boolean> {
        return type.persistentData().isRegistered(this.identifier.parseIdentifier(identifier))
    }

    override fun loadSubject(identifier: String): CompletableFuture<Subject> {
        return subjectCache[identifier].thenApply { x: PEXSubject? -> x }
    }

    override fun loadSubjects(identifiers: Set<String>): CompletableFuture<Map<String, Subject>> {
        val subjs = identifiers.associateWith { loadSubject(it) }
        return CompletableFuture.allOf(*subjs.values.toTypedArray<CompletableFuture<*>>())
            .thenApply { _: Void? ->
                subjs.mapValues { (_, value) -> value.join() }
            }
    }

    override fun newSubjectReference(subjectIdentifier: String): SubjectReference {
        return PEXSubjectReference(this.identifier, this.identifier.parseIdentifier(subjectIdentifier), plugin)
    }

    override fun suggestUnload(identifier: String) {
        subjectCache.synchronous().invalidate(identifier)
        type.uncache(this.identifier.parseIdentifier(identifier))
    }

    override fun getAllIdentifiers(): CompletableFuture<Set<String>> {
        return CompletableFuture.completedFuture(
            type.allIdentifiers()
            .map(this.identifier::serializeIdentifier)
            .collect(Collectors.toSet())
        )
    }

    override fun getLoadedSubjects(): Collection<Subject> {
        return ImmutableSet.copyOf<Subject>(activeSubjects)
    }

    val activeSubjects: Iterable<PEXSubject>
        get() = subjectCache.synchronous().asMap().values

    override fun getLoadedWithPermission(permission: String): Map<Subject, Boolean> {
        return getLoadedWithPermission(null, permission)
    }

    override fun getLoadedWithPermission(contexts: Set<Context>?, permission: String): Map<Subject, Boolean> {
        val ret = ImmutableMap.builder<Subject, Boolean>()
        for (subject in subjectCache.synchronous().asMap().values) {
            val permissionValue = subject.getPermissionValue(contexts ?: subject.activeContexts, permission)
            if (permissionValue !== Tristate.UNDEFINED) {
                ret.put(subject, permissionValue.asBoolean())
            }
        }
        return ret.build()
    }

    override fun getAllWithPermission(permission: String): CompletableFuture<Map<SubjectReference, Boolean>> {
        return getAllWithPermission(null, permission)
    }

    override fun getAllWithPermission(
        contexts: Set<Context>?,
        permission: String
    ): CompletableFuture<Map<SubjectReference, Boolean>> {
        val raw = type.allIdentifiers()
        val futures: Array<CompletableFuture<CalculatedSubject>> = raw.map { type[it] }.collect(Collectors.toList()).toTypedArray()
        return CompletableFuture.allOf(*futures).thenApply(Function { _: Void? ->
            futures.asSequence()
                .map { it.join() }
                .map {
                    val perm = it.permission(contexts?.toPex(plugin.manager) ?: it.activeContexts(), permission)
                    var bPerm: Boolean? = null
                    if (perm > 0) {
                        bPerm = true
                    } else if (perm < 0) {
                        bPerm = false
                    }
                    if (bPerm == null) {
                        null
                    } else {
                        Pair(it.identifier() as SubjectReference, bPerm)
                    }
                }
                .filterNotNull()
                .toMap()
        })
    }

    /**
     * Get the subject that provides defaults for subjects of this type. This subject is placed at the root of any inheritance tree involving subjects of this type.
     *
     * @return The subject holding defaults
     */
    override fun getDefaults(): Subject {
        return defaults!!
    }

    fun getCalculatedSubject(identifier: String): CompletableFuture<CalculatedSubject> {
        return type[this.identifier.parseIdentifier(identifier)]
    }
}
