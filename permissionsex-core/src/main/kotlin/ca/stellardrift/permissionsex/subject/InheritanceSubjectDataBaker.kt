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

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_DEFAULTS
import ca.stellardrift.permissionsex.context.ContextFlux
import ca.stellardrift.permissionsex.context.ContextSet
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.data.ImmutableSubjectData
import ca.stellardrift.permissionsex.util.NodeTree
import ca.stellardrift.permissionsex.util.SubjectIdentifier
import ca.stellardrift.permissionsex.util.Translations.t
import ca.stellardrift.permissionsex.util.glob.GlobParseException
import ca.stellardrift.permissionsex.util.glob.Globs
import com.google.common.collect.HashMultiset
import com.google.common.collect.Maps
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.util.stream.Collectors
import kotlin.math.abs

private const val CIRCULAR_INHERITANCE_THRESHOLD = 3

object InheritanceSubjectDataBaker : SubjectDataBaker {
    private suspend fun processContexts(pex: PermissionsEx, rawContexts: ContextSet): ContextFlux {
        val ctxInheritance =  pex.contextInheritance.awaitSingle()
        val seen = mutableSetOf<ContextValue<*>>()

        return rawContexts.toFlux().expand { // we want to work breadth-first
            if (it !in seen) {
                seen.add(it)
                ctxInheritance.getParents(it).toFlux()
            } else {
                Flux.empty()
            }
        }
    }

    override fun bake(data: CalculatedSubject, contexts: ContextSet): Mono<BakedSubjectData> = mono {
        val subject = data.identifier
        val processedContexts = processContexts(data.manager, contexts).collect(Collectors.toSet()).awaitSingle()
        val state = BakeState(data, data.manager, processedContexts)

        state.visitSubject(subject, 0)
        val defIdent = data.data().cache.defaultIdentifier
        if (subject != defIdent) {
            state.visitSubject(defIdent, 1)
            state.visitSubject(Maps.immutableEntry(SUBJECTS_DEFAULTS, SUBJECTS_DEFAULTS), 2)
        }
        BakedSubjectData(NodeTree.of(state.combinedPermissions, state.defaultValue), state.parents, state.options)
    }
}

private data class BakeState(val base: CalculatedSubject, val pex: PermissionsEx, val activeContexts: ContextSet) {
    val combinedPermissions = mutableMapOf<String, Int>()
    val parents = mutableListOf<SubjectIdentifier>()
    val options  = mutableMapOf<String, String>()
    val visitedSubjects: HashMultiset<SubjectIdentifier> = HashMultiset.create()
    var defaultValue: Int = 0

    internal suspend fun visitSubject(subject: SubjectIdentifier, inheritanceLevel: Int) {
        if (visitedSubjects.count(subject) > CIRCULAR_INHERITANCE_THRESHOLD) {
            pex.logger.warn(t("Potential circular inheritance found while traversing inheritance for %s when visiting %s", base.identifier, subject))
            return
        }
        visitedSubjects.add(subject)
        val type = pex.getSubjects(subject.key)

        val persistentData = type.persistentData().getData(subject.value, base).awaitSingle()
        val transientData = type.transientData().getData(subject.value, base).awaitSingle()

        processSubjectContexts(persistentData.activeContexts, transientData.activeContexts).forEach {
            if (type.typeInfo.transientHasPriority()) {
                visitSubjectSingle(transientData, it, inheritanceLevel)
                visitSubjectSingle(persistentData, it, inheritanceLevel)
            } else {
                visitSubjectSingle(persistentData, it, inheritanceLevel)
                visitSubjectSingle(transientData, it, inheritanceLevel)
            }
        }
    }

    suspend fun visitSubjectSingle(data: ImmutableSubjectData, specificCombination: ContextSet, inheritanceLevel: Int) {
        visitSingle(data, specificCombination, inheritanceLevel)
        data.getParents(specificCombination).forEach {
            visitSubject(it, inheritanceLevel + 1)
        }
    }

    fun visitSingle(data: ImmutableSubjectData, specificCombination: ContextSet, inheritanceLevel: Int) {
        data.getPermissions(specificCombination).forEach { (_perm, value) ->
            var perm = _perm
            if (perm.startsWith('#')) {
                if (inheritanceLevel > 1) {
                    return@forEach
                }
                perm = perm.substring(1)
            }

            try {
                Globs.parse(perm).forEach { combinedPermissions.putIfNecessary(it, value) }
            } catch (ex: GlobParseException) {
                combinedPermissions.putIfNecessary(perm, value)
            }
        }

        parents.addAll(data.getParents(specificCombination).map { (k, v) -> pex.createSubjectIdentifier(k, v) })

        data.getOptions(specificCombination).forEach { (_option, value) ->
            var option = _option
            if (option.startsWith('#')) {
                if (inheritanceLevel > 1) {
                    return@forEach
                }
                option = option.substring(1)
            }

            options.putIfAbsent(option, value)
        }

        data.getDefaultValue(specificCombination).run {
            if (abs(this) > abs(defaultValue)) {
                defaultValue = this
            }
        }
    }

    fun MutableMap<String, Int>.putIfNecessary(perm: String, value: Int) {
        val existing = this[perm]
        if (existing == null || abs(value) > abs(existing)) {
            this[perm] = value
        }
    }

    internal fun processSubjectContexts(persistentPossibilities: Set<ContextSet>, transientPossibilities: Set<ContextSet>): List<ContextSet> {
        val ret = mutableListOf<ContextSet>()

        fun <T> ContextValue<T>.matches(other: ContextValue<*>): Boolean {
            if (key != other.key || !tryResolve(pex)) {
                return false
            }
            val definition = this.definition!!
            @Suppress("UNCHECKED_CAST")
            return definition.matches(this, (other as ContextValue<T>).getParsedValue(definition))
        }

        /**
         * Add every context set used for a segment in this subject where for a given set,
         * every context matches at least one of the active contexts provided for the query
         */
        fun processSingleContexts(possibilities: Set<ContextSet>) {
            nextSegment@ for (segmentContexts in possibilities) {
                for (value in segmentContexts) {
                    var matched = false
                    for (possibility in activeContexts) {
                        if (value.matches(possibility)) {
                            matched = true
                            break
                        }
                    }

                    if (!matched) {
                        continue@nextSegment
                    }
                }
                ret.add(segmentContexts)
            }
        }

        processSingleContexts(persistentPossibilities)
        processSingleContexts(transientPossibilities)
        ret.sortWith(Comparator.comparingInt<ContextSet> {it.size }.reversed())
        return ret
    }
}
