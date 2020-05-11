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

package ca.stellardrift.permissionsex.backend.conversion.groupmanager

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_GROUP
import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_USER
import ca.stellardrift.permissionsex.backend.Messages.GROUPMANAGER_DESCRIPTION
import ca.stellardrift.permissionsex.backend.Messages.GROUPMANAGER_ERROR_NO_DIR
import ca.stellardrift.permissionsex.backend.Messages.GROUPMANAGER_NAME
import ca.stellardrift.permissionsex.backend.conversion.ConversionProvider
import ca.stellardrift.permissionsex.backend.conversion.ConversionResult
import ca.stellardrift.permissionsex.backend.conversion.ReadOnlyDataStore
import ca.stellardrift.permissionsex.data.ContextInheritance
import ca.stellardrift.permissionsex.data.ImmutableSubjectData
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException
import ca.stellardrift.permissionsex.rank.FixedRankLadder
import ca.stellardrift.permissionsex.rank.RankLadder
import com.google.common.collect.Maps
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.kotlin.get
import ninja.leaping.configurate.loader.ConfigurationLoader
import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader
import org.yaml.snakeyaml.DumperOptions
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

/**
 * A pair of nodes representing a users and groups permissions file for a given world, resolved to the top-level `users`\`groups` key.
 */
data class UserGroupPair(val user: ConfigurationNode, val group: ConfigurationNode)

/**
 * Backend implementing GroupManager data storage format
 */
class GroupManagerDataStore internal constructor(identifier: String) : ReadOnlyDataStore<GroupManagerDataStore>(identifier, FACTORY) {

    @Setting("group-manager-root")
    private val groupManagerRoot = "plugins/GroupManager"

    private lateinit var config: ConfigurationNode
    internal lateinit var globalGroups: ConfigurationNode
        private set
    private lateinit var worldUserGroups: MutableMap<String, UserGroupPair>
    private lateinit var contextInheritance: GroupManagerContextInheritance

    val knownWorlds: Collection<String>
        get() = this.worldUserGroups.keys

    private fun getLoader(file: Path): ConfigurationLoader<ConfigurationNode> {
        return YAMLConfigurationLoader.builder()
            .setFlowStyle(DumperOptions.FlowStyle.BLOCK)
            .setPath(file)
            .build()
    }

    internal fun getUserGroupsConfigForWorld(world: String): UserGroupPair? {
        return worldUserGroups[world]
    }

    @Throws(PermissionsLoadingException::class)
    override fun initializeInternal(): Boolean {
        val rootFile = Paths.get(groupManagerRoot)
        if (!Files.isDirectory(rootFile)) {
            throw PermissionsLoadingException(GROUPMANAGER_ERROR_NO_DIR(rootFile))
        }
        try {
            config = getLoader(rootFile.resolve("config.yml")).load()
            globalGroups = getLoader(rootFile.resolve("globalgroups.yml")).load()["groups"]
            worldUserGroups = hashMapOf()
            Files.list(rootFile.resolve("worlds"))
                .forEach { world ->
                    if (!Files.isDirectory(world)) {
                        return@forEach
                    }
                    worldUserGroups[world.fileName.toString()] = UserGroupPair(
                        getLoader(world.resolve("users.yml")).load()["users"],
                        getLoader(world.resolve("groups.yml")).load()["groups"]
                    )
                }
            contextInheritance = GroupManagerContextInheritance(config["settings", "mirrors"])
        } catch (e: IOException) {
            throw PermissionsLoadingException(e)
        }
        return true // read-only, no point in importing to us
    }

    private fun getDataGM(type: String, identifier: String): ImmutableSubjectData =
        GroupManagerSubjectData(identifier, this, EntityType.forTypeString(type))

    override fun getDataInternal(type: String, identifier: String): CompletableFuture<ImmutableSubjectData> {
        return completedFuture(getDataGM(type, identifier))
    }

    override fun getRankLadderInternal(ladder: String): CompletableFuture<RankLadder> {
        return completedFuture(FixedRankLadder(ladder, emptyList())) // GM does not have a concept of rank ladders
    }

    override fun getContextInheritanceInternal(): CompletableFuture<ContextInheritance> {
        return completedFuture(contextInheritance)
    }

    override fun close() {}

    override fun isRegistered(type: String, identifier: String): CompletableFuture<Boolean> {
        if (type == SUBJECTS_USER) {
            for ((_, value) in this.worldUserGroups) {
                if (!value.user[identifier].isVirtual) {
                    return completedFuture(true)
                }
            }
        } else if (type == SUBJECTS_GROUP) {
            if (!globalGroups["g:$identifier"].isVirtual) {
                return completedFuture(true)
            }
            for ((_, value) in this.worldUserGroups) {
                if (!value.group[identifier].isVirtual) {
                    return completedFuture(true)
                }
            }
        }
        return completedFuture(false)
    }

    override fun getAllIdentifiers(type: String): Set<String> {
        when (type) {
            SUBJECTS_USER -> return this.worldUserGroups.values
                .flatMap { it.user.childrenMap.keys }
                .map(Any::toString)
                .toSet()
            SUBJECTS_GROUP -> return (this.worldUserGroups.values
                .flatMap { it.group.childrenMap.keys } + this.globalGroups.childrenMap.keys)
                .map {
                    if (it is String && it.startsWith("g:")) {
                        it.substring(2)
                    } else {
                        it.toString()
                    }
                }
                .toSet()
            else -> return emptySet()
        }
    }

    override fun getRegisteredTypes(): Set<String> {
        return setOf(SUBJECTS_USER, SUBJECTS_GROUP)
    }

    override fun getDefinedContextKeys(): CompletableFuture<Set<String>> {
        throw UnsupportedOperationException("Not necessary to perform conversion, so whatevs")
    }

    override fun getAll(): Iterable<Map.Entry<Map.Entry<String, String>, ImmutableSubjectData>> {
        return (getAllIdentifiers(SUBJECTS_USER).map { Maps.immutableEntry(SUBJECTS_USER, it) } +
                getAllIdentifiers(SUBJECTS_GROUP).map { Maps.immutableEntry(SUBJECTS_GROUP, it) })
            .map { Maps.immutableEntry(it, getDataGM(it.key, it.value)) }
    }

    override fun getAllRankLadders(): Iterable<String> {
        return emptyList()
    }

    override fun hasRankLadder(ladder: String): CompletableFuture<Boolean> {
        return completedFuture(false)
    }

    companion object : ConversionProvider {
        @JvmField
        val FACTORY = Factory("groupmanager", GroupManagerDataStore::class.java, ::GroupManagerDataStore)
        override val name = GROUPMANAGER_NAME()

        override fun listConversionOptions(pex: PermissionsEx<*>): List<ConversionResult> {
            val gmBaseDir = pex.baseDirectory.parent.resolve("GroupManager")
            return if (Files.exists(gmBaseDir.resolve("config.yml"))) { // we exist
                listOf(ConversionResult(GroupManagerDataStore("gm-file"), GROUPMANAGER_DESCRIPTION()))
            } else {
                emptyList()
            }
        }
    }
}
