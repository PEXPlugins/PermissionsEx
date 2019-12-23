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

package ca.stellardrift.permissionsex.velocity

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_USER
import ca.stellardrift.permissionsex.proxycommon.IDENT_SERVER_CONSOLE
import ca.stellardrift.permissionsex.rank.RankLadder
import ca.stellardrift.permissionsex.util.Translatable
import ca.stellardrift.permissionsex.util.Translations.t
import ca.stellardrift.permissionsex.util.command.ButtonType
import ca.stellardrift.permissionsex.util.command.CommandException
import ca.stellardrift.permissionsex.util.command.CommandSpec
import ca.stellardrift.permissionsex.util.command.Commander
import ca.stellardrift.permissionsex.util.command.MessageFormatter
import com.google.common.collect.Maps
import com.velocitypowered.api.command.Command
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import net.kyori.text.Component
import net.kyori.text.ComponentBuilder
import net.kyori.text.TextComponent
import net.kyori.text.TranslatableComponent
import net.kyori.text.event.ClickEvent
import net.kyori.text.event.HoverEvent
import net.kyori.text.format.TextColor
import net.kyori.text.format.TextDecoration
import net.kyori.text.renderer.FriendlyComponentRenderer
import java.text.MessageFormat
import java.util.Locale
import java.util.Optional
import java.util.regex.Pattern

class VelocityCommand(private val pex: PermissionsExPlugin, val cmd: CommandSpec) : Command {

    override fun execute(source: CommandSource, args: Array<out String>) {
        val src = VelocityCommander(pex.manager, source)
        cmd.process(src, args.joinToString(" "))
    }

    override fun suggest(source: CommandSource, currentArgs: Array<out String>): MutableList<String> {
        val src = VelocityCommander(pex.manager, source)
        return cmd.tabComplete(src, currentArgs.joinToString(" "))
    }

    override fun hasPermission(source: CommandSource, args: Array<out String>): Boolean {
        return try {
            cmd.checkPermission(VelocityCommander(pex.manager, source))
            true
        } catch (e: CommandException) {
            false
        }
    }
}

class VelocityCommander(internal val pex: PermissionsEx<*>, private val src: CommandSource) :
    Commander<ComponentBuilder<*, *>> {
    private val formatter = VelocityMessageFormatter(this)
    override fun getName(): String {
        return (src as? Player)?.username ?: IDENT_SERVER_CONSOLE.value
    }

    override fun getLocale(): Locale {
        return (src as? Player)?.playerSettings?.locale ?: Locale.getDefault()
    }

    override fun getSubjectIdentifier(): Optional<Map.Entry<String, String>> {
        return Optional.of(
            when (src) {
                is Player -> Maps.immutableEntry(SUBJECTS_USER, src.uniqueId.toString())
                else -> IDENT_SERVER_CONSOLE
            }
        )
    }

    override fun hasPermission(permission: String): Boolean {
        return src.hasPermission(permission)
    }

    override fun fmt(): MessageFormatter<ComponentBuilder<*, *>> {
        return formatter
    }

    override fun msg(text: ComponentBuilder<*, *>) {
        src.sendMessage(FixedTranslationComponentRenderer.render(text.color(TextColor.GOLD).build(), this))
    }

    override fun debug(text: ComponentBuilder<*, *>) {
        src.sendMessage(FixedTranslationComponentRenderer.render(text.color(TextColor.GRAY).build(), this))
    }

    override fun error(text: ComponentBuilder<*, *>) {
        src.sendMessage(FixedTranslationComponentRenderer.render(text.color(TextColor.RED).build(), this))
    }

    override fun msgPaginated(title: Translatable, header: Translatable?, text: Iterable<ComponentBuilder<*, *>>) {
        msg(fmt().combined("# ", fmt().tr(title), " #"))
        if (header != null) {
            msg(fmt().tr(header))
        }
        text.forEach(this::msg)

        msg(fmt().combined("#################################"))
    }

}

val EQUALS_SIGN = TextComponent.of("=", TextColor.GRAY)


private object FixedTranslationComponentRenderer : FriendlyComponentRenderer<VelocityCommander>() {
    private val tokenPattern = Pattern.compile("%s")
    override fun translation(context: VelocityCommander, key: String): MessageFormat { // why is this different from how MC does it????
        val matcher = tokenPattern.matcher(key)
        var num = 0
        val newMessage = StringBuffer()
        while (matcher.find()) {
            matcher.appendReplacement(newMessage, "{${num++}}")
        }
        matcher.appendTail(newMessage)
        return MessageFormat(newMessage.toString())
    }

}


class VelocityMessageFormatter(private val cmd: VelocityCommander, private val hlColor: TextColor = TextColor.YELLOW) :
    MessageFormatter<ComponentBuilder<*, *>> {
    override fun subject(subject: Map.Entry<String, String>): ComponentBuilder<*, *> {
        val name = cmd.pex.getSubjects(subject.key).get(subject.value).join().data().get().getOptions(setOf())["name"]
        val nameText = if (name != null) {
            TextComponent.builder(subject.value, TextColor.GRAY).append("/").append(name).build()
        } else {
            TextComponent.of(subject.value)
        }

        return TextComponent.builder().apply {
            it.append(TextComponent.builder(subject.key).decoration(TextDecoration.BOLD, true).build())
            it.append(" ")
                .append(nameText)
                .hoverEvent(HoverEvent.showText(tr(t("Click to view more info")).build()))
                .clickEvent(ClickEvent.runCommand("/pex ${subject.key} ${subject.value} info"))

        }
    }

    override fun ladder(ladder: RankLadder): ComponentBuilder<*, *> {
        return TextComponent.builder(ladder.name)
            .decoration(TextDecoration.BOLD, true)
            .hoverEvent(HoverEvent.showText(tr(t("click here to view more info")).build()))
            .clickEvent(ClickEvent.runCommand("/pex rank ${ladder.name}"))
    }

    override fun booleanVal(opt: Boolean): ComponentBuilder<*, *> {
        return tr(if (opt) t("true") else t("false"))
            .color(if (opt) TextColor.GREEN else TextColor.RED)
    }

    override fun button(
        type: ButtonType,
        label: Translatable,
        tooltip: Translatable?,
        command: String,
        execute: Boolean
    ): ComponentBuilder<*, *> {
        val builder = tr(label)
        builder.color(
            when (type) {
                ButtonType.POSITIVE -> TextColor.GREEN
                ButtonType.NEGATIVE -> TextColor.RED
                ButtonType.NEUTRAL -> hlColor
            }
        )

        if (tooltip != null) {
            builder.hoverEvent(HoverEvent.showText(tr(tooltip).build()))
        }

        builder.clickEvent(
            if (execute) {
                ClickEvent.runCommand(command)
            } else {
                ClickEvent.suggestCommand(command)
            }
        )
        return builder
    }

    override fun permission(permission: String, value: Int): ComponentBuilder<*, *> {
        return TextComponent.builder().append(
            TextComponent.of(
                permission, when {
                    value > 0 -> TextColor.GREEN
                    value < 0 -> TextColor.RED
                    else -> TextColor.GRAY
                }
            )
        ).append(EQUALS_SIGN).append(TextComponent.of(value))
    }

    override fun option(permission: String, value: String): ComponentBuilder<*, *> {
        return TextComponent.builder(permission).append(EQUALS_SIGN).append(TextComponent.of(value))
    }

    override fun header(text: ComponentBuilder<*, *>): ComponentBuilder<*, *> {
        return text.decoration(TextDecoration.BOLD, true)
    }

    override fun hl(text: ComponentBuilder<*, *>): ComponentBuilder<*, *> {
        return text.color(hlColor)
    }

    override fun combined(vararg elements: Any): ComponentBuilder<*, *> {
        val builder = TextComponent.builder()
        elements.forEach {
            builder.append(it.asComponent())
        }
        return builder
    }

    private fun Any.asComponent(): Component {
        return when (this) {
            is Translatable -> tr(this).build()
            is ComponentBuilder<*, *> -> this.build()
            is Component -> this
            else -> TextComponent.of(toString())
        }
    }

    override fun tr(tr: Translatable): ComponentBuilder<*, *> {
        val args: List<Component> = tr.args.map { it.asComponent() }
        return TranslatableComponent.builder(tr.translate(cmd.locale)).args(args)
    }
}
