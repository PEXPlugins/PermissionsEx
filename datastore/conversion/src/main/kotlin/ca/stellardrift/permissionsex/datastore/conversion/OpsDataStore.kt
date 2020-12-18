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
package ca.stellardrift.permissionsex.datastore.conversion

import ca.stellardrift.permissionsex.BaseDirectoryScope
import ca.stellardrift.permissionsex.PermissionsEngine
import ca.stellardrift.permissionsex.PermissionsEngine.SUBJECTS_GROUP
import ca.stellardrift.permissionsex.PermissionsEngine.SUBJECTS_USER
import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.backend.Messages.OPS_DESCRIPTION
import ca.stellardrift.permissionsex.backend.Messages.OPS_ERROR_NO_FILE
import ca.stellardrift.permissionsex.backend.Messages.OPS_NAME
import ca.stellardrift.permissionsex.backend.memory.MemoryContextInheritance
import ca.stellardrift.permissionsex.backend.memory.MemorySubjectData
import ca.stellardrift.permissionsex.context.ContextInheritance
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.datastore.ConversionResult
import ca.stellardrift.permissionsex.datastore.DataStoreFactory
import ca.stellardrift.permissionsex.datastore.StoreProperties
import ca.stellardrift.permissionsex.rank.FixedRankLadder
import ca.stellardrift.permissionsex.rank.RankLadder
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData
import ca.stellardrift.permissionsex.subject.SubjectTypeImpl
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import net.kyori.adventure.text.Component
import org.pcollections.PVector
import org.pcollections.TreePVector
import org.spongepowered.configurate.BasicConfigurationNode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.gson.GsonConfigurationLoader
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
import org.spongepowered.configurate.reference.ConfigurationReference
import org.spongepowered.configurate.reference.WatchServiceListener
import org.spongepowered.configurate.util.UnmodifiableCollections.immutableMapEntry

/**
 * An extremely rudimentary data store that allows importing data from a server ops list.
 *
 */
class OpsDataStore(props: StoreProperties<Config>) : ReadOnlyDataStore<OpsDataStore, OpsDataStore.Config>(props) {
    companion object : Factory<OpsDataStore, Config>("ops", Config::class.java, ::OpsDataStore), DataStoreFactory.Convertable {

        override fun friendlyName(): Component {
            return OPS_NAME()
        }

        override fun listConversionOptions(pex: PermissionsEngine): PVector<ConversionResult> {
            val opsFile = (pex as PermissionsEx<*>).baseDirectory(BaseDirectoryScope.SERVER).resolve("ops.json")
            return if (Files.exists(opsFile)) {
                TreePVector.singleton(
                    ConversionResult.builder()
                    .store(OpsDataStore(StoreProperties.of("ops", Config(opsFile.fileName.toString()), this)))
                    .description(OPS_DESCRIPTION())
                    .build())
            } else {
                TreePVector.empty()
            }
        }
    }

    @ConfigSerializable
    data class Config(var fileName: String = "ops.json")

    private lateinit var file: Path
    private lateinit var configListener: WatchServiceListener
    private lateinit var opsListNode: ConfigurationReference<BasicConfigurationNode>
    private var opsList = listOf<OpsListEntry>()

    override fun initializeInternal(): Boolean {
        if (!this::file.isInitialized) {
            file = (manager as PermissionsEx<*>).baseDirectory(BaseDirectoryScope.SERVER).resolve(config().fileName) // todo
        }

        if (!Files.exists(file)) {
            manager.logger().warn(OPS_ERROR_NO_FILE())
        }

        this.configListener = WatchServiceListener.builder()
            .taskExecutor(manager.asyncExecutor())
            .build()
        this.opsListNode = configListener.listenToConfiguration({ GsonConfigurationLoader.builder()
            .lenient(true)
            .path(it)
            .build() }, file)
        this.opsListNode.updates().subscribe {
            reload(it)
        }

        reload(opsListNode.node(), false)
        return true
    }

    private fun reload(node: ConfigurationNode, notify: Boolean = true) {
        this.opsList = node.getList(OpsListEntry::class.java, listOf())
        if (notify) {
            (manager.subjectType(SUBJECTS_USER) as SubjectTypeImpl).update(this)
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
            immutableMapEntry(immutableMapEntry(SUBJECTS_USER, it.uuid.toString()), it)
        }
    }

    override fun getRegisteredTypes(): Set<String> {
        return setOf(
            SUBJECTS_USER,
            SUBJECTS_GROUP
        )
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
internal data class OpsListEntry(
    val uuid: UUID = UUID.randomUUID(),
    val name: String = "unset",
    val level: Int = 4,
    @Setting("bypassesPlayerLimit") val bypassesPlayerLimit: Boolean = false
) : OpsListSubjectData() {

    override fun getDefaultValue(contexts: Set<ContextValue<*>>): Int {
        return if (filterContexts(contexts) && level == 4) {
            1
        } else {
            0
        }
    }

    override fun getParents(contexts: Set<ContextValue<*>>): List<Map.Entry<String, String>> {
        return if (level > 0 && filterContexts(contexts)) {
            listOf(immutableMapEntry(SUBJECTS_GROUP, "op$level"))
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
