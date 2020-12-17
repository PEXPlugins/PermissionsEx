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
package ca.stellardrift.permissionsex.sponge.command

import ca.stellardrift.permissionsex.commands.parse.CommandSpec
import ca.stellardrift.permissionsex.sponge.Messages
import ca.stellardrift.permissionsex.sponge.PermissionsExPlugin
import ca.stellardrift.permissionsex.sponge.register
import ca.stellardrift.permissionsex.util.optionally
import io.leangen.geantyref.TypeToken
import java.util.Optional
import net.kyori.adventure.text.Component
import org.spongepowered.api.ResourceKey
import org.spongepowered.api.command.CommandCause
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.exception.CommandException
import org.spongepowered.api.command.manager.CommandManager
import org.spongepowered.api.command.manager.CommandMapping
import org.spongepowered.api.command.registrar.CommandRegistrar
import org.spongepowered.api.command.registrar.tree.ClientCompletionKeys
import org.spongepowered.api.command.registrar.tree.CommandTreeNode
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent
import org.spongepowered.api.event.lifecycle.RegisterRegistryValueEvent
import org.spongepowered.plugin.PluginContainer

fun registerRegistrar(plugin: PermissionsExPlugin) {
    plugin.game.eventManager.register<RegisterRegistryValueEvent<CommandRegistrar<*>>>(plugin.container) {
        plugin.logger.debug("state :: RegisterCatalogEvent<CommandRegistrar<*>>")
        val registrar = PEXCommandRegistrar(plugin, it.game.commandManager)
        it.register(registrar.key, registrar)
    }
}

class PEXCommandRegistrar internal constructor(private val plugin: PermissionsExPlugin, private val manager: CommandManager) : CommandRegistrar<CommandSpec> {
    private val key: ResourceKey = ResourceKey.of(plugin.container.metadata.id, "commands")
    private val type = TypeToken.get(CommandSpec::class.java)
    private val commands = mutableMapOf<String, CommandSpec>()

    override fun getKey(): ResourceKey = key
    override fun handledType(): TypeToken<CommandSpec> = type

    private operator fun get(mapping: CommandMapping): CommandSpec {
        return commands[mapping.primaryAlias] ?: throw CommandException(Messages.COMMANDS_REGISTRAR_ERROR_UNKNOWN(mapping.primaryAlias))
    }

    override fun register(container: PluginContainer, command: CommandSpec, primaryAlias: String, vararg secondaryAliases: String): CommandMapping {
        val node = command.asTree()
        val mapping = manager.registerAlias(this, container, node, primaryAlias, *secondaryAliases)
        this.commands[mapping.primaryAlias] = command
        return mapping
    }

    override fun process(cause: CommandCause, mapping: CommandMapping, command: String, arguments: String): CommandResult {
        val commander = SpongeCommander(plugin, cause)
        this[mapping].process(commander, arguments) // TODO: result
        return CommandResult.success()
    }

    override fun suggestions(cause: CommandCause, mapping: CommandMapping, command: String, arguments: String): List<String> {
        val commander = SpongeCommander(plugin, cause)
        return this[mapping].tabComplete(commander, arguments)
    }

    override fun help(cause: CommandCause, mapping: CommandMapping): Optional<Component> {
        return this[mapping].description.optionally()
    }

    override fun canExecute(cause: CommandCause, mapping: CommandMapping): Boolean {
        return this[mapping].permission?.let { cause.hasPermission(it.value) } ?: true
    }

    override fun reset() {
        require(manager.isResetting) { throw IllegalStateException("Manager must be resetting to clear this registrar!") }
        this.commands.clear()
    }
}

/**
 * Register a command spec with its provided aliases.
 */
fun RegisterCommandEvent<CommandSpec>.register(container: PluginContainer, spec: CommandSpec): CommandMapping {
    val primaryAlias = spec.aliases.first()
    val remainingAliases = spec.aliases.subList(1, spec.aliases.size)
    return register(container, spec, primaryAlias, *remainingAliases.toTypedArray())
}

/**
 * Convert a command spec node into a tree
 */
fun CommandSpec.asTree(): CommandTreeNode.Root {
    val tree = CommandTreeNode.root()
    // base
    tree.executable()
        .customSuggestions()

    permission?.also { perm ->
        tree.requires { it.hasPermission(perm.value) }
    }

    // arguments TODO: generate an actual command tree

    return tree.child("args", ClientCompletionKeys.STRING.get()
        .createNode()
        .greedy()
        .customSuggestions())
}
