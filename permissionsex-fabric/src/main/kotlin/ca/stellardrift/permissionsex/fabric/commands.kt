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

import ca.stellardrift.permissionsex.PermissionsEx.GLOBAL_CONTEXT
import ca.stellardrift.permissionsex.rank.RankLadder
import ca.stellardrift.permissionsex.util.Translatable
import ca.stellardrift.permissionsex.util.Translations.t
import ca.stellardrift.permissionsex.util.Util
import ca.stellardrift.permissionsex.util.command.ButtonType
import ca.stellardrift.permissionsex.util.command.CommandSpec
import ca.stellardrift.permissionsex.util.command.Commander
import ca.stellardrift.permissionsex.util.command.MessageFormatter
import com.google.common.collect.Maps
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.Nameable
import java.util.Locale
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.function.Predicate
import com.mojang.brigadier.context.CommandContext as BrigadierCommandContext

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

class PEXBrigadierCommand(private val spec: CommandSpec): Predicate<ServerCommandSource>, Command<ServerCommandSource>,
    SuggestionProvider<ServerCommandSource> {
    override fun getSuggestions(
        context: BrigadierCommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val argsSoFar = try {getString(context, "args") } catch (e: Exception) { "" }
        val suggestionPrefix = argsSoFar.substringBeforeLast(' ')
        this.spec.tabComplete(FabricCommander(context.source), argsSoFar).forEach {
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
        return perm == null || t.hasPermission(perm)
    }

    override fun run(context: BrigadierCommandContext<ServerCommandSource>): Int {
        val cmd = FabricCommander(context.source)
        try {
            val args = getString(context, "args")
            this.spec.process(cmd, args)
        } catch (e: Exception) {
            PermissionsExMod.logger.error(t("Unable to execute command %s for sender %s due to an error", spec.aliases[0], context.source.name), e)
            cmd.error(t("Error executing commands %s, see console for details", spec.aliases[0]))
        }
        return 1
    }
}

class PEXNoArgsBrigadierCommand(private val spec: CommandSpec): Command<ServerCommandSource>, SuggestionProvider<ServerCommandSource> {
    override fun run(context: com.mojang.brigadier.context.CommandContext<ServerCommandSource>): Int {
        val cmd = FabricCommander(context.source)
        try {
            this.spec.process(cmd, "")
        } catch (e: Exception) {
            PermissionsExMod.logger.error(t("Unable to execute command %s for sender %s due to an error", spec.aliases[0], context.source.name), e)
            cmd.error(t("Error executing commands %s, see console for details", spec.aliases[0]))
        }
        return 1
    }

    override fun getSuggestions(
        context: com.mojang.brigadier.context.CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        this.spec.tabComplete(FabricCommander(context.source), "").forEach {
            builder.suggest(it)
        }
        return CompletableFuture.completedFuture(builder.build()) // todo actually run async? - for commands api refactor
    }
}

class FabricCommander(val sender: ServerCommandSource) : Commander<Text> {
    private val formatter = FabricMessageFormatter(this)

    override fun getName(): String {
        return sender.name
    }

    override fun getLocale(): Locale {
        return (sender as? LocaleHolder)?.locale ?: Locale.getDefault()
    }

    override fun getSubjectIdentifier(): Optional<Map.Entry<String, String>> {
        return if (sender is IPermissionCommandSource) {
            Optional.of(Maps.immutableEntry(sender.permType, sender.permIdentifier))
        } else {
            Optional.empty()
        }
    }

    override fun hasPermission(permission: String): Boolean {
        return sender.hasPermission(permission)
    }

    override fun fmt(): MessageFormatter<Text> {
        return formatter
    }

    override fun msg(text: Text) {
        sender.sendFeedback(text.formatted(Formatting.DARK_AQUA), false)
    }

    override fun debug(text: Text) {
        sender.sendFeedback(text.formatted(Formatting.GRAY), false)
    }

    override fun error(text: Text) {
        sender.sendError(text)
    }

    override fun msgPaginated(title: Translatable, header: Translatable?, text: Iterable<Text>) {
        msg(fmt().combined("# ", fmt().tr(title), " #"))
        if (header != null) {
            msg(fmt().tr(header))
        }
        text.forEach(this::msg)

        msg(fmt().combined("#################################"))
    }

}

val EQUALS_SIGN: Text = LiteralText("=").formatted(Formatting.GRAY)
class FabricMessageFormatter(private val cmd: FabricCommander, private val hlColor: Formatting =  Formatting.AQUA) : MessageFormatter<Text> {

    override fun subject(subject: Map.Entry<String, String>): Text {
        val subj = PermissionsExMod.manager.getSubjects(subject.key)[subject.value].join()

        val name = Util.castOptional(subj.associatedObject, Nameable::class.java).map(Nameable::getName).orElseGet {
            val nameStr = subj.data().get().getOptions(GLOBAL_CONTEXT)["name"]
            if (nameStr != null) LiteralText(nameStr) else null
        }

        val nameText = if (name != null) {
            LiteralText(subject.value).formatted(Formatting.GRAY).append("/").append(name)
        } else {
            LiteralText(subject.value)
        }

        return LiteralText("").apply {
            append(LiteralText(subject.key).styled { it.isBold = true})
            append(" ")
            append(nameText)

            styled {
                it.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, tr(t("Click here to view more info")))
                it.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pex ${subject.key} ${subject.value} info")
            }
        }

    }

    override fun ladder(ladder: RankLadder): Text {
        val ret = LiteralText(ladder.name)
        ret.style.apply {
            isBold = true
            hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, tr(t("Click here to view more info")))
            clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pex rank ${ladder.name}")
        }
        return ret
    }

    override fun booleanVal(flag: Boolean): Text {
        return tr(if (flag) t("true") else t("false"))
            .formatted(if (flag) Formatting.GREEN else Formatting.RED)
    }

    override fun button(
        type: ButtonType,
        label: Translatable,
        tooltip: Translatable?,
        command: String,
        execute: Boolean
    ): Text {
        val text = tr(label)
        text.formatted(when (type) {
            ButtonType.POSITIVE -> Formatting.GREEN
            ButtonType.NEGATIVE -> Formatting.RED
            ButtonType.NEUTRAL -> hlColor
        })

        if (tooltip != null) {
            text.style.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, tr(tooltip))
        }

        text.style.clickEvent = ClickEvent(if (execute) ClickEvent.Action.RUN_COMMAND else ClickEvent.Action.SUGGEST_COMMAND, command)
        return text
    }

    override fun permission(permission: String, value: Int): Text {
        return combined(LiteralText(permission).formatted(when {
            value > 0 -> Formatting.GREEN
            value < 0 -> Formatting.RED
            else -> Formatting.GRAY
        }), EQUALS_SIGN, value.toString())
    }

    override fun option(permission: String, value: String): Text {
        return LiteralText(permission).append(EQUALS_SIGN).append(value)
    }

    override fun header(text: Text): Text {
        return text.styled { it.isBold = true }
    }

    override fun hl(text: Text): Text {
        return text.formatted(hlColor)
    }

    override fun combined(vararg elements: Any): Text {
        val comp = LiteralText("")
        elements.forEach {
            comp.append(it.asText())
        }
        return comp
    }

    private fun Any.asText(): Text {
        return when (this) {
            is Translatable -> tr(this)
            is Text -> this
            else -> LiteralText(this.toString())
        }
    }

    override fun tr(tr: Translatable): Text {
        return TranslatableText(tr.translate(cmd.locale), *tr.args.map {it.asText()}.toTypedArray())
    }

}