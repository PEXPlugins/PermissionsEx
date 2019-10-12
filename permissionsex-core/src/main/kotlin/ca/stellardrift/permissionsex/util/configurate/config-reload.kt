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

package ca.stellardrift.permissionsex.util.configurate

import ca.stellardrift.permissionsex.logging.TranslatableLogger
import ca.stellardrift.permissionsex.util.Translations.t
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object CloseWatchEventKind : WatchEvent.Kind<WatchServiceListener> {
    override fun type(): Class<WatchServiceListener> = WatchServiceListener::class.java
    override fun name(): String = "CLOSE"
}

class CloseWatchEvent internal constructor(val listener: WatchServiceListener): WatchEvent<WatchServiceListener> {
    override fun count(): Int {
        return 0
    }

    override fun kind(): WatchEvent.Kind<WatchServiceListener> {
        return CloseWatchEventKind
    }

    override fun context(): WatchServiceListener {
        return listener
    }

}

val DEFAULT_WATCH_EVENTS: Array<WatchEvent.Kind<*>> = arrayOf(StandardWatchEventKinds.OVERFLOW, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE)

typealias WatchServiceCallback = (WatchEvent<*>) -> Boolean

private data class DirectoryListenerRegistration(val key: WatchKey, val fileListeners: ConcurrentHashMap<Path, MutableList<WatchServiceCallback>>, val dirListeners: MutableList<WatchServiceCallback>)

class WatchServiceListener @JvmOverloads constructor(
    private val executor: ExecutorService = Executors.newCachedThreadPool(),
    val logger: TranslatableLogger = TranslatableLogger.forLogger(LoggerFactory.getLogger(WatchServiceListener::class.java)),
    fileSystem: FileSystem = FileSystems.getDefault()
) : AutoCloseable {
    private val watchService = fileSystem.newWatchService()
    private val activeListeners = ConcurrentHashMap<WatchKey, DirectoryListenerRegistration>()

    init {
        executor.submit {
            while (!executor.isShutdown) {
                val key = watchService.take()
                val registration = activeListeners[key]
                if (registration != null) {
                    for (event in key.pollEvents()) {

                        // Process listeners
                        executor.submit {
                            try {
                                val items = synchronized(activeListeners) {registration.dirListeners + (registration.fileListeners[event.context()] ?: listOf())}
                                val itemsToRemove = mutableSetOf<WatchServiceCallback>()

                                items.forEach {
                                    if (!it(event)) {
                                        itemsToRemove.add(it)
                                    }
                                }

                                synchronized(activeListeners) {
                                    registration.dirListeners.removeAll(itemsToRemove)
                                    val fileListeners = registration.fileListeners[event.context()]
                                    fileListeners?.removeAll(itemsToRemove)
                                    if ((fileListeners ?: emptyList<WatchServiceCallback>()).isEmpty()) {
                                        registration.fileListeners.remove(event.context())
                                    }
                                    if (registration.dirListeners.isEmpty() && registration.fileListeners.isEmpty()) {
                                        key.cancel()
                                        activeListeners.remove(key)
                                    }
                                }
                            } catch (thr: Throwable) {
                                this.logger.error(t("Error while running reload task for file %s", key.watchable()), thr)
                            }
                        }

                        // If the watch key is no longer valid, send all listeners a close event
                        if (!key.reset()) {
                            val oldListeners = activeListeners.remove(key)
                            if (oldListeners != null) {
                                val closeEvent = CloseWatchEvent(this)
                                oldListeners.fileListeners.values.forEach { it.forEach { it(closeEvent)} }
                                oldListeners.dirListeners.forEach { it(closeEvent) }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Listen for changes to a specific file or directory.
     *
     * @param file The path of the file or directory to listen for changes on
     * @param listener A callback function that will be called when changes are made. If return value is false, we will stop monitoring for changes.
     */
    @Throws(IOException::class)
    fun listenToFile(file: Path, listener: WatchServiceCallback): WatchKey {
        require(!Files.isDirectory(file)) { "Path $file must be a file"}

        val key = file.parent.register(watchService, *DEFAULT_WATCH_EVENTS)
        val fileName = file.fileName
        activeListeners.getOrPut(key, { DirectoryListenerRegistration(key, ConcurrentHashMap(), mutableListOf())}).fileListeners.getOrPut(fileName, { mutableListOf() }) += listener
        return key

    }

    @Throws(IOException::class)
    fun listenToDirectory(directory: Path, listener: WatchServiceCallback): WatchKey {
        require(Files.isDirectory(directory) || !Files.exists(directory)) { "Path $directory must be a directory"}

        val key = directory.register(watchService, *DEFAULT_WATCH_EVENTS)
        activeListeners.getOrPut(key, { DirectoryListenerRegistration(key, ConcurrentHashMap(), mutableListOf()) }).dirListeners += listener
        return key
    }

    @Throws(IOException::class)
    fun <T : ConfigurationNode> createConfig(
        loader: (Path) -> ConfigurationLoader<T>,
        file: Path,
        callback: (T) -> Unit
    ): ReloadableConfig<T> {
        val config = ReloadableConfig(loader(file), callback)
        listenToFile(file, config::onWatchEvent)
        return config
    }

    override fun close() {
        watchService.close()
        executor.shutdownNow()
        val closeEvent = CloseWatchEvent(this)
        activeListeners.forEach { (_, u) ->
            u.fileListeners.values.forEach { it.forEach { it(closeEvent) } }
            u.dirListeners.forEach { it(closeEvent)}
        }
        activeListeners.clear()
    }
}


enum class ConfigPhase {
    LOADING, SAVING
}

class ReloadableConfig<T : ConfigurationNode> internal constructor(
    val loader: ConfigurationLoader<T>,
    private val callback: (T) -> Unit
) : AutoCloseable {
    var node: T private set
    @Volatile
    private var ignoreUpdates: Boolean = false // for when we write to the file
    @Volatile
    private var open: Boolean = true

    var errorCallback: (Exception, ConfigPhase) -> Unit = { it, _ -> it.printStackTrace() }

    @Throws(IOException::class)
    constructor(loader: ConfigurationLoader<T>) : this(loader, {})

    init {
        node = loader.load()
    }

    @Throws(IOException::class)
    fun reload() {
        synchronized(this) {
            node = loader.load()
            callback(node)
        }
    }

    internal fun onWatchEvent(event: WatchEvent<*>): Boolean {
        if (event is CloseWatchEvent) {
            this.open = false
            return false
        }

        if (!ignoreUpdates) {
            try {
                reload()
            } catch (e: Exception) {
                errorCallback(e, ConfigPhase.LOADING)
            }
        }
        return open
    }

    @JvmOverloads
    @Throws(IOException::class)
    fun save(node: T = this.node) {
        synchronized(this) {
            ignoreUpdates = true
            this.node = node
            try {
                loader.save(node)
            } catch (e: Exception) {
                errorCallback(e, ConfigPhase.SAVING)
            } finally {
                ignoreUpdates = false
            }
        }
    }

    /**
     * Safely update the backing node, taking into account reloads.
     * The provided update function may be called multiple times, until configuration state is consistent.
     */
    fun update(func: (T) -> Unit) {
        while (true) {
            val node = this.node
            func(node)
            synchronized(this) {
                if (node == this.node) {
                    save(node)
                    return
                }
            }
        }
    }

    operator fun get(vararg path: Any): T {
        @Suppress("UNCHECKED_CAST") // subclasses of ConfigurationNode override this (but we can't know that!)
        return node.getNode(*path) as T
    }

    operator fun set(vararg path: Any, value: Any?) {
        node.getNode(*path).value = value
    }

    operator fun set(path: Any, value: Any?) {
        node.getNode(path).value = value
    }

    operator fun contains(path: Array<Any>): Boolean {
        return !node.getNode(*path).isVirtual
    }

    /**
     * Contains for a single level
     *
     * @param path a single path element
     */
    operator fun contains(path: Any): Boolean {
        return !node.getNode(path).isVirtual
    }

    override fun close() {
        this.open = false
    }
}

