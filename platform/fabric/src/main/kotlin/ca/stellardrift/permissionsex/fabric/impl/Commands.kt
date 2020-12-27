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
package ca.stellardrift.permissionsex.fabric.impl

import ca.stellardrift.permissionsex.fabric.mixin.ServerCommandSourceAccess
import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx
import ca.stellardrift.permissionsex.minecraft.command.Commander
import ca.stellardrift.permissionsex.minecraft.command.MessageFormatter
import ca.stellardrift.permissionsex.minecraft.command.Permission
import ca.stellardrift.permissionsex.subject.SubjectRef
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.fabric.AdventureCommandSourceStack
import net.kyori.adventure.platform.fabric.FabricServerAudiences
import net.kyori.adventure.text.Component
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Nameable

/*fun registerCommand(spec: CommandSpec, dispatch: CommandDispatcher<ServerCommandSource>) {
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
            if (suggestionPrefix.length == 0) {
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
            FabricPermissionsExImpl.logger().error(Messages.COMMAND_ERROR_CONSOLE.tr(spec.aliases[0], context.source.name), e)
            cmd.error(Messages.COMMAND_ERROR_TO_SENDER.tr(spec.aliases[0]), e)
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
            FabricPermissionsExImpl.logger().error(Messages.COMMAND_ERROR_CONSOLE.tr(spec.aliases[0], context.source.name), e)
            cmd.error(Messages.COMMAND_ERROR_TO_SENDER.tr(spec.aliases[0]), e)
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
}*/

@Suppress("UNCHECKED_CAST")
internal fun ServerCommandSource.asCommander(): Commander {
    return FabricCommander(this)
}

class FabricCommander(private val src: ServerCommandSource) : Commander {
    private val output = src as AdventureCommandSourceStack

    override fun hasPermission(permission: String): Boolean {
        return if (src is PermissionCommandSourceBridge<*>) {
            src.hasPermission(permission)
        } else {
            src.hasPermissionLevel(src.minecraftServer.opPermissionLevel)
        }
    }

    override fun hasPermission(permission: Permission): Boolean {
        var ret = 0
        if (src is PermissionCommandSourceBridge<*>) {
            ret = (src as PermissionCommandSourceBridge<*>).asCalculatedSubject().permission(permission.value())
        }
        if (ret == 0) { // op status
            ret = (src as ServerCommandSourceAccess).level
        }
        if (ret == 0) { // permission def value
            ret = permission.defaultValue()
        }
        return ret > 0
    }

    override fun audience(): Audience {
        return this.output
    }

    override fun name(): Component {
        return FabricServerAudiences.of(this.src.minecraftServer).toAdventure(this.src.displayName)
    }

    override fun subjectIdentifier(): SubjectRef<*>? {
        return if (src is PermissionCommandSourceBridge<*>) {
            src.asReference()
        } else {
            null
        }
    }

    override fun formatter(): MessageFormatter {
        return FabricPermissionsExImpl.formatter!!
    }
}

class FabricMessageFormatter constructor(manager: MinecraftPermissionsEx<*>) : MessageFormatter(manager) {

    override fun <I> friendlyName(reference: SubjectRef<I>): String? {
        return (reference.type().getAssociatedObject(reference.identifier()) as? Nameable)?.name?.asString()
    }
}
