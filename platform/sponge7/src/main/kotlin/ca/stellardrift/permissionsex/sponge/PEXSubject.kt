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
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import ca.stellardrift.permissionsex.util.CachingValue
import ca.stellardrift.permissionsex.util.optionally
import java.util.Objects
import java.util.Optional
import java.util.concurrent.CompletableFuture
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.service.context.Context
import org.spongepowered.api.service.permission.Subject
import org.spongepowered.api.service.permission.SubjectReference
import org.spongepowered.api.util.Tristate
import org.spongepowered.configurate.util.UnmodifiableCollections.immutableMapEntry

/**
 * Permissions subject implementation
 */
class PEXSubject(private val baked: CalculatedSubject, private val collection: PEXSubjectCollection<*>) : Subject {
    private val data = PEXSubjectData(baked.data(), collection.plugin)
    private val transientData = PEXSubjectData(baked.transientData(), collection.plugin)

    private val ref: SubjectReference = this.baked.identifier as SubjectReference
    private val activeContexts: CachingValue<ActiveContextsHolder>

    private data class ActiveContextsHolder(val spongeContexts: Set<Context>, val pexContexts: ContextSet)

    init {
        this.activeContexts = collection.plugin.tickBasedCachingValue(1L) {
            time.getActiveContexts.time {
                val pexContexts = baked.activeContexts
                val spongeContexts: MutableSet<Context> = pexContexts.toSponge()
                val spongeContextsAccum: MutableSet<Context> = mutableSetOf()
                for (spongeCalc in this.collection.plugin.contextCalculators) {
                    spongeCalc.accumulateContexts(this, spongeContextsAccum)
                }
                spongeContexts.addAll(spongeContextsAccum)
                pexContexts.addAll(spongeContextsAccum.toPex(manager))
                ActiveContextsHolder(spongeContexts, pexContexts)
            }
        }
    }

    private val time get() = collection.plugin.timings

    override fun getIdentifier(): String {
        return ref.subjectIdentifier
    }

    override fun getFriendlyIdentifier(): Optional<String> {
        return Optional.empty()
    }

    val manager: PermissionsEx<*>
        get() = collection.plugin.manager

    override fun asSubjectReference(): SubjectReference {
        return ref
    }

    override fun isSubjectDataPersisted(): Boolean {
        return true
    }

    override fun getCommandSource(): Optional<CommandSource> {
        val associated = this.baked.associatedObject
        return if (associated is CommandSource) associated.optionally() else Optional.empty()
    }

    override fun getContainingCollection(): PEXSubjectCollection<*> {
        return this.collection
    }

    override fun getSubjectData(): PEXSubjectData {
        return this.data
    }

    override fun getTransientSubjectData(): PEXSubjectData {
        return this.transientData
    }

    override fun getOption(contexts: Set<Context>, key: String): Optional<String> {
        return this.time.getOption.time {
            baked.getOption(contexts.toPex(this.manager), key)
        }
    }

    override fun getOption(key: String): Optional<String> {
        return baked.getOption(activePexContexts, key)
    }

    override fun hasPermission(contexts: Set<Context>, permission: String): Boolean {
        return getPermissionValue(contexts, permission).asBoolean()
    }

    override fun hasPermission(permission: String): Boolean {
        return baked.getPermission(activePexContexts, permission) > 0
    }

    override fun getPermissionValue(contexts: Set<Context>, permission: String): Tristate {
        return time.getPermission.time {
            val ret = baked.getPermission(contexts.toPex(manager), permission)
            when {
                ret == 0 -> Tristate.UNDEFINED
                ret > 0 -> Tristate.TRUE
                else -> Tristate.FALSE
            }
        }
    }

    override fun isChildOf(parent: SubjectReference): Boolean {
        return baked.parents.contains(immutableMapEntry(parent.collectionIdentifier, parent.subjectIdentifier))
    }

    override fun isChildOf(contexts: Set<Context>, parent: SubjectReference): Boolean {
        return getParents(contexts).contains(parent)
    }

    override fun getParents(): List<SubjectReference> {
        return baked.parents.map { PEXSubjectReference.of(this.manager.deserializeSubjectRef(it), containingCollection.plugin) }
    }

    val activePexContexts: ContextSet
        get() = this.activeContexts.get().pexContexts

    override fun getActiveContexts(): Set<Context> {
        return this.activeContexts.get().spongeContexts
    }

    override fun getParents(contexts: Set<Context>): List<SubjectReference> {
        return this.time.getParents.time {
            this.baked.getParents(contexts.toPex(this.manager)).map { PEXSubjectReference.of(this.manager.deserializeSubjectRef(it), this.containingCollection.plugin) }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is PEXSubject) {
            return false
        }
        return ref == other.ref && data == other.data
    }

    override fun hashCode(): Int {
        return Objects.hash(data, ref)
    }

    companion object {
        fun load(identifier: String, collection: PEXSubjectCollection<*>): CompletableFuture<PEXSubject> {
            return collection.getCalculatedSubject(identifier)
                .thenApply { baked: CalculatedSubject -> PEXSubject(baked, collection) }
        }
    }
}
