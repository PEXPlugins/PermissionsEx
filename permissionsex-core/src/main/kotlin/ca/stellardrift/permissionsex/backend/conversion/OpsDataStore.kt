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

package ca.stellardrift.permissionsex.backend.conversion

import ca.stellardrift.permissionsex.BaseDirectoryScope
import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_GROUP
import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_USER
import ca.stellardrift.permissionsex.backend.Messages.OPS_DESCRIPTION
import ca.stellardrift.permissionsex.backend.Messages.OPS_ERROR_NO_FILE
import ca.stellardrift.permissionsex.backend.Messages.OPS_NAME
import ca.stellardrift.permissionsex.backend.memory.MemoryContextInheritance
import ca.stellardrift.permissionsex.backend.memory.MemorySubjectData
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.data.ContextInheritance
import ca.stellardrift.permissionsex.data.ImmutableSubjectData
import ca.stellardrift.permissionsex.rank.FixedRankLadder
import ca.stellardrift.permissionsex.rank.RankLadder
import com.google.common.collect.Maps.immutableEntry
import com.google.common.reflect.TypeToken
import net.kyori.text.Component
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.gson.GsonConfigurationLoader
import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable
import ninja.leaping.configurate.reference.ConfigurationReference
import ninja.leaping.configurate.reference.WatchServiceListener
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

/**
 * An extremely rudimentary data store that allows importing data from a server ops list
 *
 */
class OpsDataStore(identifier: String) : ReadOnlyDataStore<OpsDataStore>(identifier, FACTORY) {
    companion object : ConversionProvider {
        @JvmField
        val FACTORY = Factory("ops", OpsDataStore::class.java, ::OpsDataStore)
        override val name: Component = OPS_NAME()

        override fun listConversionOptions(pex: PermissionsEx<*>): List<ConversionResult> {
            val opsFile = pex.getBaseDirectory(BaseDirectoryScope.SERVER).resolve("ops.json")
            return if (Files.exists(opsFile)) {
                listOf(ConversionResult(
                    OpsDataStore(
                        "ops",
                        opsFile
                    ), OPS_DESCRIPTION()))
            } else {
                listOf()
            }
        }

    }

    @Setting("file-name")
    var fileName: String = "ops.json"

    private lateinit var file: Path
    private lateinit var  configListener: WatchServiceListener
    private lateinit var opsListNode: ConfigurationReference<ConfigurationNode>
    private var opsList = listOf<OpsListEntry>()

    constructor(identifier: String, opsFile: Path): this(identifier) {
        this.file = opsFile
    }


    override fun initializeInternal(): Boolean {
        if (!this::file.isInitialized) {
            file = manager.getBaseDirectory(BaseDirectoryScope.SERVER).resolve(fileName)
        }

        if (!Files.exists(file)) {
            manager.logger.warn(OPS_ERROR_NO_FILE())
        }

        this.configListener = WatchServiceListener.builder()
            .setTaskExecutor(manager.asyncExecutor)
            .build()
        this.opsListNode = configListener.listenToConfiguration({GsonConfigurationLoader.builder()
            .setLenient(true)
            .setPath(it)
            .build()}, file)
        this.opsListNode.updates().subscribe {
            reload(it)
        }

        reload(opsListNode.node, false)
        return true
    }

    private fun reload(node: ConfigurationNode, notify: Boolean = true) {
        this.opsList = node.getList(TypeToken.of(OpsListEntry::class.java), listOf())
        if (notify) {
            manager.getSubjects(SUBJECTS_USER).update(this)
        }
    }

    override fun getDefinedContextKeys(): CompletableFuture<Set<String>> {
        return completedFuture(setOf())
    }

    override fun getContextInheritanceInternal(): CompletableFuture<ContextInheritance> {
        return completedFuture(BlankContextInheritance())
    }

    override fun getAll(): Iterable<Map.Entry<Map.Entry<String, String>, ImmutableSubjectData>> {
        return opsList.map {
            immutableEntry(immutableEntry(SUBJECTS_USER, it.uuid.toString()), it)
        }
    }

    override fun getRegisteredTypes(): Set<String> {
        return setOf(SUBJECTS_USER, SUBJECTS_GROUP)
    }

    override fun getAllIdentifiers(type: String): Set<String> {
        return when (type) {
            /*SUBJECTS_GROUP -> {
                setOf("op4", "op3", "op2", "op1")
            }*/
            SUBJECTS_USER -> {
                opsList.map { it.uuid.toString() }.toSet()
            }
            else -> setOf()
        }
    }

    override fun getAllRankLadders(): Iterable<String> {
        return setOf()
    }

    override fun close() {
        if (this::configListener.isInitialized) {
            this.configListener.close()
        }
    }

    override fun getDataInternal(type: String, identifier: String): CompletableFuture<ImmutableSubjectData> {
        return completedFuture(when (type) {
            SUBJECTS_USER -> {
                opsList.find { it.uuid.toString().equals(identifier, ignoreCase = true) } ?: BlankSubjectData()
            }
            else -> BlankSubjectData()
        })
    }

    override fun hasRankLadder(ladder: String): CompletableFuture<Boolean> {
        return completedFuture(false)
    }

    override fun isRegistered(type: String, identifier: String): CompletableFuture<Boolean> {
        return completedFuture(type == SUBJECTS_USER && opsList.find { it.uuid.toString().equals(identifier, ignoreCase = true) } != null)
    }

    override fun getRankLadderInternal(ladder: String): CompletableFuture<RankLadder> {
        return completedFuture(FixedRankLadder(ladder, listOf()))
    }
}

private class BlankSubjectData : MemorySubjectData()
private class BlankContextInheritance : MemoryContextInheritance()

@ConfigSerializable
internal data class OpsListEntry(@Setting val uuid: UUID, @Setting val name: String, @Setting val level: Int, @Setting val bypassesPlayerLimit: Boolean): OpsListSubjectData() {
    constructor() : this(UUID.randomUUID(), "unset", 4, false)

    override fun getDefaultValue(contexts: Set<ContextValue<*>>): Int {
        return if (filterContexts(contexts) && level == 4) {
            1
        } else {
            0
        }
    }

    override fun getParents(contexts: Set<ContextValue<*>>): List<Map.Entry<String, String>> {
        return if (level > 0 && filterContexts(contexts)) {
            listOf(immutableEntry(SUBJECTS_GROUP, "op$level"))
        } else {
            listOf()
        }
    }


    override fun getActiveContexts(): Set<Set<ContextValue<*>>> {
        return if (level > 0) {
            setOf(setOf())
        } else {
            setOf()
        }
    }

    override fun getAllDefaultValues(): Map<Set<ContextValue<*>>, Int> {
        return if (level == 4) {
            mapOf(setOf<ContextValue<*>>() to 1)
        } else {
            mapOf()
        }
    }

}

internal abstract class OpsListSubjectData : ReadOnlySubjectData() {

    protected fun filterContexts(contexts: Set<ContextValue<*>>): Boolean {
        return contexts.isEmpty()
    }

    override fun getPermissions(contexts: Set<ContextValue<*>>): Map<String, Int> = mapOf()
    override fun getAllOptions(): Map<Set<ContextValue<*>>, Map<String, String>> = mapOf()
    override fun getAllPermissions(): Map<Set<ContextValue<*>>, Map<String, Int>> = mapOf()
    override fun getOptions(contexts: Set<ContextValue<*>>?): Map<String, String> = mapOf()
    override fun getAllParents(): Map<Set<ContextValue<*>>, List<Map.Entry<String, String>>> = mapOf(setOf<ContextValue<*>>() to getParents(setOf()))
}
