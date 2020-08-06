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

package ca.stellardrift.permissionsex.fabric

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.commands.commander.Permission
import ca.stellardrift.permissionsex.commands.parse.CommandSpec
import ca.stellardrift.permissionsex.fabric.mixin.AccessorServerCommandSource
import ca.stellardrift.permissionsex.util.PEXComponentRenderer
import ca.stellardrift.permissionsex.util.SubjectIdentifier
import ca.stellardrift.permissionsex.util.coloredIfNecessary
import ca.stellardrift.text.fabric.ComponentCommandSource
import com.google.common.collect.Maps
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.context.CommandContext as BrigadierCommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.function.Predicate
import net.kyori.text.Component
import net.kyori.text.format.TextColor
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.LiteralText
import net.minecraft.util.Nameable

fun registerCommand(spec: CommandSpec, dispatch: CommandDispatcher<ServerCommandSource>) {
    val cmdCallbacks = PEXBrigadierCommand(spec)
    val noArgsCallbacks = PEXNoArgsBrigadierCommand(spec)
    val cmdNode = dispatch.register(literal(spec.aliases[0])
        .then(argument("args", greedyString())
            .suggests(cmdCallbacks)
            .requires(cmdCallbacks)
            .executes(cmdCallbacks))
            // then without args
        .requires(cmdCallbacks)
        .executes(noArgsCallbacks))
    if (spec.aliases.size > 1) {
        val it = spec.aliases.iterator()
        it.next()
        while (it.hasNext()) {
            dispatch.register(literal(it.next()).redirect(cmdNode))
        }
    }
}

class PEXBrigadierCommand(private val spec: CommandSpec) : Predicate<ServerCommandSource>, Command<ServerCommandSource>,
    SuggestionProvider<ServerCommandSource> {
    override fun getSuggestions(
        context: BrigadierCommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val argsSoFar = try { getString(context, "args") } catch (e: Exception) { "" }
        val suggestionPrefix = argsSoFar.substringBeforeLast(' ', "")
        this.spec.tabComplete(context.source.asCommander(), argsSoFar).forEach {
            if (suggestionPrefix.isEmpty()) {
                builder.suggest(it)
            } else {
                builder.suggest("$suggestionPrefix $it")
            }
        }
        return builder.buildFuture()
    }

    override fun test(t: ServerCommandSource): Boolean {
        val perm = spec.permission
        return perm == null || t.hasPermission(perm.value)
    }

    override fun run(context: BrigadierCommandContext<ServerCommandSource>): Int {
        val cmd = context.source.asCommander()
        try {
            val args = getString(context, "args")
            this.spec.process(cmd, args)
        } catch (e: Exception) {
            PermissionsExMod.logger.error(Messages.COMMAND_ERROR_CONSOLE(spec.aliases[0], context.source.name), e)
            cmd.error(Messages.COMMAND_ERROR_TO_SENDER(spec.aliases[0]), e)
        }
        return 1
    }
}

class PEXNoArgsBrigadierCommand(private val spec: CommandSpec) : Command<ServerCommandSource>, SuggestionProvider<ServerCommandSource> {
    override fun run(context: BrigadierCommandContext<ServerCommandSource>): Int {
        val cmd = context.source.asCommander()
        try {
            this.spec.process(cmd, "")
        } catch (e: Exception) {
            PermissionsExMod.logger.error(Messages.COMMAND_ERROR_CONSOLE(spec.aliases[0], context.source.name), e)
            cmd.error(Messages.COMMAND_ERROR_TO_SENDER(spec.aliases[0]), e)
        }
        return 1
    }

    override fun getSuggestions(
        context: BrigadierCommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        this.spec.tabComplete(context.source.asCommander(), "").forEach {
            builder.suggest(it)
        }
        return builder.buildFuture() // todo actually run async? - for commands api refactor
    }
}

@Suppress("UNCHECKED_CAST")
internal fun ServerCommandSource.asCommander(): Commander {
    return FabricCommander(this)
}

class FabricCommander(private val src: ServerCommandSource) : Commander {
    private val output = ComponentCommandSource.of(src)
    override val manager: PermissionsEx<*> = PermissionsExMod.manager
    override val name: String get() = src.name
    override val locale: Locale get() {
        return (output.output as? LocaleHolder)?.locale ?: Locale.getDefault()
    }
    override val subjectIdentifier: SubjectIdentifier?
        get() {
            return if (src is IPermissionCommandSource) {
                Maps.immutableEntry(src.permType, src.permIdentifier)
            } else {
                null
            }
        }

    override fun hasPermission(permission: String): Boolean {
        return if (src is IPermissionCommandSource) {
            src.hasPermission(permission)
        } else {
            src.hasPermissionLevel(src.minecraftServer.opPermissionLevel)
        }
    }

    override fun hasPermission(permission: Permission): Boolean {
        var ret = 0
        if (src is IPermissionCommandSource) {
            ret = (src as IPermissionCommandSource).asCalculatedSubject().getPermission(permission.value)
        }
        if (ret == 0) { // op status
            ret = (src as AccessorServerCommandSource).level
        }
        if (ret == 0) { // permission def value
            ret = permission.default
        }
        return ret > 0
    }

    override val formatter: MessageFormatter = FabricMessageFormatter(this)

    override fun msg(text: Component) {
        try {
            output.sendFeedback(PEXComponentRenderer.render(text coloredIfNecessary TextColor.DARK_AQUA, locale), false)
        } catch (t: Throwable) {
            src.sendFeedback(LiteralText("Error occurred while sending feedback, see console for details"), false)
            t.printStackTrace()
        }
    }
}

class FabricMessageFormatter constructor(src: FabricCommander) :
    MessageFormatter(src, src.manager) {

    override val SubjectIdentifier.friendlyName: String?
        get() = (PermissionsExMod.manager.getSubjects(key)[value].join().associatedObject as? Nameable)?.name?.asString()
}
