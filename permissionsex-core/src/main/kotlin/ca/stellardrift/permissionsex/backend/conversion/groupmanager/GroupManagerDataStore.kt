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
import ca.stellardrift.permissionsex.backend.conversion.ConversionProvider
import ca.stellardrift.permissionsex.backend.conversion.ConversionResult
import ca.stellardrift.permissionsex.backend.conversion.ReadOnlyDataStore
import ca.stellardrift.permissionsex.data.ContextInheritance
import ca.stellardrift.permissionsex.data.ImmutableSubjectData
import ca.stellardrift.permissionsex.exception.PermissionsLoadingException
import ca.stellardrift.permissionsex.rank.FixedRankLadder
import ca.stellardrift.permissionsex.rank.RankLadder
import ca.stellardrift.permissionsex.util.Translations.t
import ca.stellardrift.permissionsex.util.configurate.get
import com.google.common.collect.Maps
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader
import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader
import org.yaml.snakeyaml.DumperOptions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * A pair of nodes representing a users and groups permissions file for a given world, resolved to the top-level `users`\`groups` key.
 */
data class UserGroupPair(val user: ConfigurationNode, val group: ConfigurationNode)

/**
 * Backend implementing GroupManager data storage format
 */
class GroupManagerDataStore internal constructor() : ReadOnlyDataStore(FACTORY) {

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
    override fun initializeInternal() {
        val rootFile = Paths.get(groupManagerRoot)
        if (!Files.isDirectory(rootFile)) {
            throw PermissionsLoadingException(t("GroupManager directory %s does not exist", rootFile))
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

    }

    private fun getDataGM(type: String, identifier: String): ImmutableSubjectData =
        GroupManagerSubjectData(identifier, this, EntityType.forTypeString(type))

    override fun getDataInternal(type: String, identifier: String): Mono<ImmutableSubjectData> {
        return getDataGM(type, identifier).toMono()
    }

    override fun getRankLadderInternal(ladder: String): Mono<RankLadder> {
        return FixedRankLadder(ladder, emptyList()).toMono() // GM does not have a concept of rank ladders
    }

    override fun getContextInheritanceInternal(): Mono<ContextInheritance> {
        return contextInheritance.toMono()
    }

    override fun close() {}

    override fun isRegistered(type: String, identifier: String): Mono<Boolean> {
        if (type == SUBJECTS_USER) {
            for ((_, value) in this.worldUserGroups) {
                if (!value.user[identifier].isVirtual) {
                    return true.toMono()
                }
            }
        } else if (type == SUBJECTS_GROUP) {
            if (!globalGroups["g:$identifier"].isVirtual) {
                return true.toMono()
            }
            for ((_, value) in this.worldUserGroups) {
                if (!value.group[identifier].isVirtual) {
                    return true.toMono()
                }
            }
        }
        return false.toMono()
    }

    override fun getAllIdentifiers(type: String): Flux<String> {
        return when (type) {
            SUBJECTS_USER -> return this.worldUserGroups.values.toFlux()
                .flatMapIterable { it.user.childrenMap.keys }
                .map(Any::toString)
            SUBJECTS_GROUP -> return (this.worldUserGroups.values
                .flatMap { it.group.childrenMap.keys } + this.globalGroups.childrenMap.keys).toFlux()
                .map {
                    if (it is String && it.startsWith("g:")) {
                        it.substring(2)
                    } else {
                        it.toString()
                    }
                }
            else -> Flux.empty<String>()
        }
    }

    override fun getRegisteredTypes(): Flux<String> {
        return Flux.just(SUBJECTS_USER, SUBJECTS_GROUP)
    }

    override fun getDefinedContextKeys(): Flux<String> {
        throw UnsupportedOperationException("Not necessary to perform conversion, so whatevs")
    }

    override fun getAll(): Flux<Pair<MutableMap.MutableEntry<String, String>, ImmutableSubjectData>> {
        return getAllIdentifiers(SUBJECTS_USER).map { Maps.immutableEntry(SUBJECTS_USER, it) }.concatWith(
                getAllIdentifiers(SUBJECTS_GROUP).map { Maps.immutableEntry(SUBJECTS_GROUP, it) })
            .map { Pair(it, getDataGM(it.key, it.value)) }.toFlux()
    }

    override fun getAllRankLadders(): Flux<String> {
        return Flux.empty()
    }

    override fun hasRankLadder(ladder: String): Mono<Boolean> {
        return Mono.just(false)
    }

    companion object : ConversionProvider {
        @JvmField
        val FACTORY = Factory("groupmanager", GroupManagerDataStore::class.java)
        override val name = t("GroupManager")

        override fun listConversionOptions(pex: PermissionsEx): List<ConversionResult> {
            val gmBaseDir = pex.baseDirectory.parent.resolve("GroupManager")
            return if (Files.exists(gmBaseDir.resolve("config.yml"))) { // we exist
                listOf(ConversionResult(GroupManagerDataStore(), t("File store"), "gm-file"))
            } else {
                emptyList()
            }
        }
    }
}
