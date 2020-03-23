package ca.stellardrift.permissionsex.commands.commander

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.commands.Messages
import ca.stellardrift.permissionsex.rank.RankLadder
import ca.stellardrift.permissionsex.subject.SubjectType
import ca.stellardrift.permissionsex.util.Translatable
import net.kyori.text.Component
import net.kyori.text.ComponentBuilder
import net.kyori.text.TextComponent
import net.kyori.text.TranslatableComponent
import net.kyori.text.event.ClickEvent
import net.kyori.text.event.HoverEvent
import net.kyori.text.format.TextColor
import net.kyori.text.format.TextDecoration
import net.kyori.text.renderer.TranslatableComponentRenderer
import java.text.MessageFormat
import java.util.concurrent.ExecutionException
import java.util.regex.Pattern

val EQUALS_SIGN = TextComponent.of("=", TextColor.GRAY)
abstract class AbstractMessageFormatter(val cmd: Commander<ComponentBuilder<*, *>>,
                                        private val pex: PermissionsEx<*>,
                                        val hlColor: TextColor = TextColor.AQUA) :
    MessageFormatter<ComponentBuilder<*, *>> {

    /**
     * Given a command in standard format, correct it to refer to specifically the proxy format
     */
    protected open fun transformCommand(cmd: String) = cmd

    protected open val Map.Entry<String, String>.friendlyName: String? get()= null

    override fun subject(subject: Map.Entry<String, String>): ComponentBuilder<*, *> {
        val subjType: SubjectType = pex.getSubjects(subject.key)
        var name = subject.friendlyName
        if (name == null) {
            try {
                name = subjType.persistentData().getData(subject.value, null).get()
                    .getOptions(PermissionsEx.GLOBAL_CONTEXT)["name"]
            } catch (e: ExecutionException) {
                throw RuntimeException(e)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }

        val nameText = if (name != null) {
            TextComponent.builder(
                subject.value,
                TextColor.GRAY
            ).append("/").append(name).build()
        } else {
            TextComponent.of(subject.value)
        }

        return TextComponent.builder().apply {
            it.append(
                TextComponent.builder(subject.key)
                    .decoration(TextDecoration.BOLD, true).build())
            it.append(" ")
                .append(nameText)
                .hoverEvent(
                    HoverEvent.showText(
                        Messages.FORMATTER_BUTTON_INFO_PROMPT().build()
                    )
                )
                .clickEvent(ClickEvent.runCommand(transformCommand("/pex ${subject.key} ${subject.value} info")))

        }
    }

    override fun ladder(ladder: RankLadder): ComponentBuilder<*, *> {
        return TextComponent.builder(ladder.name)
            .decoration(TextDecoration.BOLD, true)
            .hoverEvent(
                HoverEvent.showText(
                    Messages.FORMATTER_BUTTON_INFO_PROMPT().build()
                )
            )
            .clickEvent(ClickEvent.runCommand(transformCommand("/pex rank ${ladder.name}")))
    }

    override fun booleanVal(value: Boolean): ComponentBuilder<*, *> {
        return (if (value) Messages.FORMATTER_BOOLEAN_TRUE() else Messages.FORMATTER_BOOLEAN_FALSE())
            .color(if (value) TextColor.GREEN else TextColor.RED)
    }

    override fun button(
        type: ButtonType,
        label: Translatable,
        tooltip: Translatable?,
        command: String,
        execute: Boolean
    ): ComponentBuilder<*, *> {
        val builder = label.tr()
        builder.color(
            when (type) {
                ButtonType.POSITIVE -> TextColor.GREEN
                ButtonType.NEGATIVE -> TextColor.RED
                ButtonType.NEUTRAL -> hlColor
            }
        )

        if (tooltip != null) {
            builder.hoverEvent(HoverEvent.showText(tooltip.tr().build()))
        }

        builder.clickEvent(
            if (execute) {
                ClickEvent.runCommand(transformCommand(command))
            } else {
                ClickEvent.suggestCommand(transformCommand(command))
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
        return TextComponent.builder(permission)
            .append(EQUALS_SIGN).append(TextComponent.of(value))
    }

    override fun ComponentBuilder<*, *>.header(): ComponentBuilder<*, *> {
        return decoration(TextDecoration.BOLD, true)
    }

    override fun ComponentBuilder<*, *>.hl(): ComponentBuilder<*, *> {
        return color(hlColor)
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
            is Translatable -> this.tr().build()
            is ComponentBuilder<*, *> -> this.build()
            is Component -> this
            else -> TextComponent.of(toString())
        }
    }

    override fun Translatable.tr(): ComponentBuilder<*, *> {
        val args: List<Component> = args.map { it.asComponent() }
        return TranslatableComponent.builder(translate(cmd.locale)).args(args)
    }


    override fun String.unaryMinus(): ComponentBuilder<*, *> {
        return TextComponent.builder(this)
    }

    override fun ComponentBuilder<*, *>.plus(other: ComponentBuilder<*, *>): ComponentBuilder<*, *> {
        return TextComponent.builder().append(this).append(other)
    }

    override fun Collection<ComponentBuilder<*, *>>.concat(separator: ComponentBuilder<*, *>): ComponentBuilder<*, *> {
        val builtSep = separator.build()
        return foldIndexed(TextComponent.builder()) { idx, acc, el ->
            if (idx != 0) {
                acc.append(builtSep)
            }
            acc.append(el)
            acc
        }
    }
}

object FixedTranslationComponentRenderer : TranslatableComponentRenderer<Commander<ComponentBuilder<*, *>>>() {
    private val tokenPattern = Pattern.compile("%s")
    override fun translation(context: Commander<ComponentBuilder<*, *>>, key: String): MessageFormat { // why is this different from how MC does it????
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
