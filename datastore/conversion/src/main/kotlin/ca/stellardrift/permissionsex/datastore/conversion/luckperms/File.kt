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
package ca.stellardrift.permissionsex.datastore.conversion.luckperms

import ca.stellardrift.permissionsex.PermissionsEngine.SUBJECTS_GROUP
import ca.stellardrift.permissionsex.backend.AbstractDataStore
import ca.stellardrift.permissionsex.context.ContextInheritance
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.datastore.DataStore
import ca.stellardrift.permissionsex.datastore.StoreProperties
import ca.stellardrift.permissionsex.exception.PermissionsException
import ca.stellardrift.permissionsex.rank.AbstractRankLadder
import ca.stellardrift.permissionsex.rank.RankLadder
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData
import ca.stellardrift.permissionsex.util.toCompletableFuture
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.math.absoluteValue
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ScopedConfigurationNode
import org.spongepowered.configurate.gson.GsonConfigurationLoader
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.configurate.kotlin.extensions.contains
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.reference.ConfigurationReference
import org.spongepowered.configurate.reference.WatchServiceListener
import org.spongepowered.configurate.util.UnmodifiableCollections.immutableMapEntry
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader

val Path.nameWithoutExtension: String get() {
    return fileName.toString().substringBeforeLast('.')
}

@ConfigSerializable
class LuckPermsFileDataStore constructor(properties: StoreProperties<Config>) : AbstractDataStore<LuckPermsFileDataStore, LuckPermsFileDataStore.Config>(properties) {

    @ConfigSerializable
    data class Config(
        @Comment("The location of the luckperms base directory, relative to the server plugins folder")
        var pluginDir: String = "LuckPerms",

        @Comment("Whether or not all files are stored as one, equivalent to putting the '-combined' suffix on a data storage option in LuckPerms")
        var combined: Boolean = false,

        @Comment("The file format to attempt to load from")
        var format: ConfigFormat = ConfigFormat.YAML
    )

    private lateinit var lpConfig: ConfigurationReference<CommentedConfigurationNode>
    private lateinit var subjectLayout: SubjectLayout
    internal lateinit var watcher: WatchServiceListener

    internal val format get() = config().format

    override fun initializeInternal(): Boolean {
        val rootDir = manager.baseDirectory().parent.resolve(config().pluginDir)
        subjectLayout = (if (config().combined) {
            ::CombinedSubjectLayout
        } else {
            ::SeparateSubjectLayout
        })(this, rootDir.resolve(config().format.storageDirName))
        lpConfig =
            YamlConfigurationLoader.builder()
                .nodeStyle(NodeStyle.BLOCK)
                .path(rootDir.resolve("config.yml"))
                .build().loadToReference()
        watcher = WatchServiceListener.builder()
            .taskExecutor(manager.asyncExecutor())
            .build()
        return true
    }

    override fun close() {
        watcher.close()
    }

    override fun getAllIdentifiers(type: String): Set<String> {
        return this.subjectLayout.getIdentifiers(type)
    }

    override fun getRegisteredTypes(): Set<String> {
        return subjectLayout.types
    }

    override fun getDefinedContextKeys(): CompletableFuture<Set<String>> {
        TODO("not needed for import")
    }

    override fun getAll(): Iterable<Map.Entry<Map.Entry<String, String>, ImmutableSubjectData>> {
        return Collections.unmodifiableSet(this.registeredTypes.parallelStream()
            .flatMap { type -> getAllIdentifiers(type).stream().map { ident -> immutableMapEntry(type, ident) } }
            .map { key -> immutableMapEntry(key, getDataInternal(key.key, key.value).join()) }
            .collect(Collectors.toSet()))
    }

    override fun getDataInternal(type: String, identifier: String): CompletableFuture<ImmutableSubjectData> {
        return completedFuture(this.subjectLayout[type, identifier].toSubjectData())
    }

    override fun setDataInternal(
        type: String?,
        identifier: String?,
        data: ImmutableSubjectData?
    ): CompletableFuture<ImmutableSubjectData> {
        TODO("read-only")
    }

    override fun isRegistered(type: String, identifier: String): CompletableFuture<Boolean> {
        return completedFuture(Pair(type, identifier) in subjectLayout)
    }

    override fun getContextInheritanceInternal(): CompletableFuture<ContextInheritance> {
        return completedFuture(contextParentsFromConfig(lpConfig.node()))
    }

    override fun setContextInheritanceInternal(contextInheritance: ContextInheritance?): CompletableFuture<ContextInheritance> {
            val inherit = if (contextInheritance != null && contextInheritance !is LuckPermsContextInheritance) {
                LuckPermsContextInheritance(contextInheritance.allParents)
            } else {
                contextInheritance as LuckPermsContextInheritance?
            }

            return lpConfig.updateAsync {
                if (inherit == null) {
                    lpConfig["world-rewrite"].set(emptyMap<String, String>())
                } else {
                    inherit.writeToConfig(lpConfig.node())
                }
                it
            }.toCompletableFuture<CommentedConfigurationNode>().thenApply { inherit }
    }

    override fun <T : Any?> performBulkOperationSync(function: Function<DataStore, T>): T {
        return function.apply(this)
    }

    override fun getAllRankLadders(): Iterable<String> {
        return this.subjectLayout.tracks.node().childrenMap().keys.map(Any::toString)
    }

    override fun hasRankLadder(ladder: String): CompletableFuture<Boolean> {
        return completedFuture(ladder in this.subjectLayout.tracks.node())
    }

    override fun getRankLadderInternal(ladder: String): CompletableFuture<RankLadder> {
        return completedFuture(
            LuckPermsTrack(
                ladder,
                this.subjectLayout.tracks[ladder, "groups"].getList(String::class.java, listOf())
            )
        )
    }

    override fun setRankLadderInternal(ladder: String, newLadder: RankLadder?): CompletableFuture<RankLadder?> {
        val lpLadder: LuckPermsTrack? = when {
            newLadder is LuckPermsTrack -> newLadder
            newLadder != null -> LuckPermsTrack(
                ladder,
                newLadder.ranks.filter { it.key == SUBJECTS_GROUP }.map { it.value })
            else -> null
        }

        return this.subjectLayout.tracks.updateAsync {
            if (lpLadder == null) {
                it.node(ladder).raw(null)
            } else {
                it.node(ladder, "groups").set(lpLadder.groups)
            }
            it
        }.toCompletableFuture().thenApply { lpLadder }
    }
}

/** Configuration format to use -- gives format-specific paths/loaders **/
enum class ConfigFormat(val loaderProvider: Function<Path, ConfigurationLoader<out ScopedConfigurationNode<*>>>, val extension: String) {
    YAML(Function { YamlConfigurationLoader.builder().path(it).build() }, "yml"),
    JSON(Function { GsonConfigurationLoader.builder().path(it).build() }, "json"),
    HOCON(Function { HoconConfigurationLoader.builder().path(it).build() }, "conf");

    fun listen(listener: WatchServiceListener, target: Path): ConfigurationReference<ConfigurationNode> {
        // TODO: Support auto-reloading here
        return this.loaderProvider.apply(target).loadToReference() as ConfigurationReference<ConfigurationNode>
    }

    val storageDirName: String = "${this.name.toLowerCase(Locale.ROOT)}-storage"
}

interface SubjectLayout {
    operator fun get(type: String, ident: String): ConfigurationNode
    operator fun contains(subj: Pair<String, String>): Boolean = isRegistered(subj.first, subj.second)
    fun isRegistered(type: String, ident: String): Boolean
    fun getIdentifiers(type: String): Set<String>
    val types: Set<String>

    val tracks: ConfigurationReference<ConfigurationNode>
}

class CombinedSubjectLayout(private val store: LuckPermsFileDataStore, private val rootDir: Path) : SubjectLayout {
    private val files: MutableMap<String, ConfigurationReference<ConfigurationNode>> = mutableMapOf()

    override val tracks: ConfigurationReference<ConfigurationNode> = with(store) { format.listen(watcher, rootDir.resolve("tracks.${format.extension}")) }

    override val types: Set<String>
        get() {
            Files.list(rootDir).use { list ->
                return list
                    .map { it.nameWithoutExtension }
                    .filter { it != "tracks" && it != "uuidcache" }
                    .map { it.substring(0, it.length - 1) }
                    .collect(Collectors.toSet())
            }
        }

    private fun getFileFor(type: String): ConfigurationReference<out ConfigurationNode> {
        return files.computeIfAbsent(type) { key ->
            with(store) { format.listen(watcher, rootDir.resolve("${key}s.${format.extension}")) }
        }
    }

    override fun get(type: String, ident: String): ConfigurationNode {
        val rootNode = getFileFor(type)
        return rootNode[ident]
    }

    override fun isRegistered(type: String, ident: String): Boolean {
        if (!Files.exists(rootDir.resolve("${type}s.${store.format.extension}"))) {
            return false
        }

        return !this[type, ident].virtual()
    }

    override fun getIdentifiers(type: String): Set<String> {
        return getFileFor(type).node().childrenMap().keys.map(Any::toString).toSet()
    }
}

class SeparateSubjectLayout(private val store: LuckPermsFileDataStore, private val rootDir: Path) : SubjectLayout {

    override val tracks: ConfigurationReference<ConfigurationNode> = with(store) { format.listen(watcher, rootDir.resolve("tracks.${store.format.extension}")) }

    override val types: Set<String>
        get() {
            return Files.list(rootDir).use { list ->
                list.filter { Files.isDirectory(it) }
                .map { val name = it.fileName.toString()
                    name.substring(0, name.length - 1) }
                .collect(Collectors.toSet())
            }
        }

    override fun isRegistered(type: String, ident: String): Boolean {
        return Files.exists(rootDir.resolve("${type}s").resolve("$ident.${store.format.extension}"))
    }

    override fun get(type: String, ident: String): ConfigurationNode {
        return store.format.loaderProvider.apply(rootDir.resolve("${type}s").resolve("$ident.${store.format.extension}"))
            .load()
    }

    override fun getIdentifiers(type: String): Set<String> {
        return Files.list(rootDir.resolve("${type}s")).use { list ->
            list.map { x ->
                x.nameWithoutExtension
            }.collect(Collectors.toSet())
        }
    }
}

fun contextParentsFromConfig(node: ConfigurationNode): LuckPermsContextInheritance {
    val worldRewrite = node.node("world-rewrite").get<Map<String, String>>(mapOf())
    val ret = mutableMapOf<ContextValue<*>, List<ContextValue<*>>>()
    worldRewrite.forEach {
        ret[ContextValue<String>("world", it.key)] = listOf(ContextValue<String>("world", it.value))
    }
    return LuckPermsContextInheritance(ret)
}

/**
 * Our generalization of LP world inheritance
 */
class LuckPermsContextInheritance(private val contextParents: Map<ContextValue<*>, List<ContextValue<*>>>) :
    ContextInheritance {
    override fun getParents(context: ContextValue<*>): List<ContextValue<*>>? {
        return contextParents[context]
    }

    override fun setParents(context: ContextValue<*>, parents: List<ContextValue<*>>?): LuckPermsContextInheritance {
        require(context.key().equals("world", ignoreCase = true))
        return if (parents == null) {
            LuckPermsContextInheritance(contextParents - context)
        } else {
            require(parents.size == 1) { "worlds can only have one parent" }
            require(
                parents.first().key().equals(
                    "world",
                    ignoreCase = true
                )
            ) { "context inheritance only happens between worlds" }
            LuckPermsContextInheritance(contextParents + (context to parents))
        }
    }

    override fun getAllParents(): Map<ContextValue<*>, List<ContextValue<*>>> {
        return contextParents
    }

    fun writeToConfig(node: ConfigurationNode) {
        val worldRewriteNode = node.node("world-rewrite")
        worldRewriteNode.raw(null)
        this.contextParents.forEach { (k, v) ->
            worldRewriteNode.node(k.rawValue()).set(v.first().rawValue())
        }
    }
}

fun Boolean.asInteger(): Int {
    return if (this) {
        1
    } else {
        -1
    }
}

class LuckPermsTrack internal constructor(name: String, val groups: List<String>) : AbstractRankLadder(name) {

    constructor(name: String) : this(name, listOf())

    override fun getRanks(): List<Map.Entry<String, String>> {
        return this.groups.map { immutableMapEntry(SUBJECTS_GROUP, it) }
    }

    override fun newWithRanks(ents: List<Map.Entry<String, String>>): LuckPermsTrack {
        return LuckPermsTrack(this.name, ents.map { (k, v) ->
            if (k != SUBJECTS_GROUP) {
                throw PermissionsException(Messages.ERROR_INVALID_TRACK.tr(k, this.name))
            }
            v
        })
    }
}

private data class LuckPermsDefinition<ValueType>(
    val key: String,
    val value: ValueType,
    val weight: Int,
    val contexts: Set<ContextValue<*>>
)

private fun addIfNotVirtual(
    parentNode: ConfigurationNode,
    key: String,
    values: MutableSet<ContextValue<*>>,
    keyAs: String = key
) {
    val childNode = parentNode.node(key)
    if (!childNode.virtual()) {
        values.add(ContextValue<String>(keyAs, childNode.string!!))
    }
}

private fun <T> defListFromNode(
    node: ConfigurationNode,
    valueConverter: (ConfigurationNode) -> T,
    valueKey: String = "value",
    keyOverride: String? = null,
    keyPath: String = "permission"
): Set<LuckPermsDefinition<T>> {
    val ret = mutableSetOf<LuckPermsDefinition<T>>()
    node.childrenList().forEach {
        val contexts = mutableSetOf<ContextValue<*>>()
        it.node("context").childrenMap().forEach { (k, v) -> contexts.add(ContextValue<String>(k.toString(), v.string!!)) }
        addIfNotVirtual(it, "world", contexts)
        addIfNotVirtual(it, "server", contexts, "server-tag")
        addIfNotVirtual(it, "expiry", contexts, "before-time")

        ret.add(
            LuckPermsDefinition(
                keyOverride
                    ?: it.node(keyPath).string!!.replace(".*", ""), valueConverter(it.node(valueKey)), it.node("priority").getInt(0), contexts
            )
        )
    }
    return ret
}

private fun ConfigurationNode.toSubjectData(): LuckPermsSubjectData {
    val options: Set<LuckPermsDefinition<String>> = defListFromNode(this.node("meta"), { it.string!! }, keyPath = "key") +
            defListFromNode(this.node("prefixes"), { it.string!! }, keyOverride = "prefix", valueKey = "prefix") +
            defListFromNode(this.node("suffixes"), { it.string!! }, keyOverride = "suffix", valueKey = "suffix")
    return LuckPermsSubjectData(
        defListFromNode(this.node("permissions"), ConfigurationNode::getBoolean),
        options,
        defListFromNode(this.node("parents"), { immutableMapEntry("group", it.string!!) },
            valueKey = "group", keyOverride = "group")
    )
}

private class LuckPermsSubjectData(val permissions: Set<LuckPermsDefinition<Boolean>>, val options: Set<LuckPermsDefinition<String>>, val parents: Set<LuckPermsDefinition<Map.Entry<String, String>>>) :
    ImmutableSubjectData {

    override fun getAllOptions(): Map<Set<ContextValue<*>>, Map<String, String>> {
        return options.groupBy({ it.contexts }, { it.key to it.value }).mapValues { (_, v) -> v.toMap() }
    }

    override fun getOptions(contexts: Set<ContextValue<*>>?): Map<String, String> {
        return options.filter { it.contexts == contexts }.associate { it.key to it.value }
    }

    override fun setOption(contexts: Set<ContextValue<*>>, key: String, value: String?): ImmutableSubjectData {
        TODO("read-only")
    }

    override fun setOptions(contexts: Set<ContextValue<*>>, values: Map<String, String>?): ImmutableSubjectData {
        TODO("read-only")
    }

    override fun clearOptions(contexts: Set<ContextValue<*>>): ImmutableSubjectData {
        TODO("read-only")
    }

    override fun clearOptions(): ImmutableSubjectData {
        TODO("read-only")
    }

    override fun getAllPermissions(): Map<Set<ContextValue<*>>, Map<String, Int>> {
        return permissions.groupBy({ it.contexts }, { it.key to if (it.value) { 1 } else { -1 } }).mapValues { (_, v) -> v.toMap() }
    }

    override fun getPermissions(contexts: Set<ContextValue<*>>): Map<String, Int> {
        return this.permissions.filter { it.contexts == contexts }.associate { it.key to if (it.value) 1 else -1 }
    }

    override fun setPermission(
        contexts: Set<ContextValue<*>>,
        permission: String,
        value: Int
    ): ImmutableSubjectData {
        TODO("read-only")
    }

    override fun setPermissions(
        contexts: Set<ContextValue<*>>,
        values: Map<String, Int>?
    ): ImmutableSubjectData {
        TODO("read-only")
    }

    override fun clearPermissions(): ImmutableSubjectData {
        TODO("read-only")
    }

    override fun clearPermissions(contexts: Set<ContextValue<*>>): ImmutableSubjectData {
        TODO("read-only")
    }

    override fun getAllParents(): Map<Set<ContextValue<*>>, List<Map.Entry<String, String>>> {
        return parents.groupBy({ it.contexts }, { it.value })
    }

    override fun getParents(contexts: Set<ContextValue<*>>): List<Map.Entry<String, String>> {
        return parents.filter { it.contexts == contexts }.map { it.value }
    }

    override fun addParent(
        contexts: Set<ContextValue<*>>,
        type: String,
        identifier: String
    ): ImmutableSubjectData {
        TODO("read-only")
    }

    override fun removeParent(
        contexts: MutableSet<ContextValue<*>>,
        type: String,
        identifier: String
    ): ImmutableSubjectData {
        TODO("read-only")
    }

    override fun setParents(
        contexts: Set<ContextValue<*>>,
        parents: List<Map.Entry<String, String>>?
    ): ImmutableSubjectData {
        TODO("read-only")
    }

    override fun clearParents(): ImmutableSubjectData {
        TODO("read-only")
    }

    override fun clearParents(contexts: Set<ContextValue<*>>): ImmutableSubjectData {
        TODO("read-only")
    }

    override fun getDefaultValue(contexts: Set<ContextValue<*>>): Int {
        return permissions.filter { it.contexts == contexts && it.key == "*" }.map { it.value.asInteger() }.maxByOrNull { it.absoluteValue } ?: 0
    }

    override fun setDefaultValue(contexts: Set<ContextValue<*>>, defaultValue: Int): ImmutableSubjectData {
        TODO("read-only")
    }

    override fun getAllDefaultValues(): Map<Set<ContextValue<*>>, Int> {
        return permissions.filter { it.key == "*" }.groupBy({ it.contexts }, { it.value.asInteger() }).mapValues { (_, v) -> v.maxByOrNull { it.absoluteValue } ?: 0 }
    }

    override fun getActiveContexts(): Set<Set<ContextValue<*>>> {
        return (this.permissions + this.options + this.parents).map { it.contexts }.distinct().toSet()
    }
}
