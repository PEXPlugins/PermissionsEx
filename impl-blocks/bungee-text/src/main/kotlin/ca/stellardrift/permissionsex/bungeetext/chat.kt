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

package ca.stellardrift.permissionsex.bungeetext

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.commands.commander.ButtonType
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.rank.RankLadder
import ca.stellardrift.permissionsex.smartertext.CallbackController
import ca.stellardrift.permissionsex.subject.SubjectType
import ca.stellardrift.permissionsex.util.Translatable
import ca.stellardrift.permissionsex.util.Translations.t
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.HoverEvent.Action
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.TranslatableComponent
import java.util.concurrent.ExecutionException
import kotlin.collections.Map.Entry

/**
 * Factory to create formatted elements of messages
 */
abstract class BungeeMessageFormatter(protected val cmd: Commander<BaseComponent>, protected val pex: PermissionsEx<*>, private val hlColour: ChatColor = ChatColor.AQUA, private val callbacks: CallbackController) :
    MessageFormatter<BaseComponent> {
    companion object {
        val EQUALS_SIGN: BaseComponent = TextComponent("=").apply {
            color = ChatColor.GRAY
        }
        val EMPTY: BaseComponent = TextComponent()
    }

    abstract fun getFriendlyName(subj: Entry<String, String>): String?

    override fun subject(subject: Entry<String, String>): BaseComponent {
        val subjType: SubjectType = pex.getSubjects(subject.key)
        var name = getFriendlyName(subject)
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
        // <bold>{type}>/bold>:{identifier}/{name} (on click: /pex {type} {identifier}
        val nameText = if (name != null) {
            TextComponent("").apply {
                val child = TextComponent(subject.value)
                child.color = ChatColor.GRAY
                addExtra(child)
                addExtra("/")
                addExtra(name)
            }
        } else {
            TextComponent(subject.value)
        }

        return TextComponent("").apply {
            val typeComponent: BaseComponent = TextComponent(subject.key)
            typeComponent.isBold = true
            addExtra(typeComponent)
            addExtra(" ")
            addExtra(nameText)
            hoverEvent = HoverEvent(Action.SHOW_TEXT, arrayOf(t("Click to view more info").tr()))
            clickEvent =
                ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pex " + subject.key + " " + subject.value + " info")
        }
    }

    override fun ladder(ladder: RankLadder): BaseComponent {
        return TextComponent(ladder.name).apply {
            isBold = true
            hoverEvent = HoverEvent(Action.SHOW_TEXT, arrayOf(t("click here to view more info").tr()))
            clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pex rank " + ladder.name)
        }
    }

    override fun booleanVal(value: Boolean): BaseComponent {
        val ret = if (value) t("true").tr() else t("false").tr()
        ret.color = if (value) ChatColor.GREEN else ChatColor.RED
        return ret
    }

    override fun button(
        type: ButtonType,
        label: Translatable,
        tooltip: Translatable?,
        command: String,
        execute: Boolean
    ): BaseComponent {
        return label.tr().apply {
            color = when (type) {
                ButtonType.POSITIVE -> ChatColor.GREEN
                ButtonType.NEGATIVE -> ChatColor.RED
                ButtonType.NEUTRAL -> hlColour
                else -> throw IllegalArgumentException("Provided with unknown ButtonType $type")
            }

            if (tooltip != null) {
                hoverEvent = HoverEvent(
                    Action.SHOW_TEXT,
                    arrayOf(tooltip.tr())
                )
            }
            clickEvent = ClickEvent(
                if (execute) ClickEvent.Action.RUN_COMMAND else ClickEvent.Action.SUGGEST_COMMAND,
                command
            )
        }
    }

    override fun callback(title: Translatable, callback: (Commander<BaseComponent>) -> Unit): BaseComponent {
        val command = callbacks.registerCallback(source = cmd, func = {callback(it)})
        return title.tr().apply {
            isUnderlined = true
            color = hlColour
            clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, command)
        }
    }

    override fun permission(
        permission: String,
        value: Int
    ): BaseComponent {
        val valueColor = when {
            value > 0 -> ChatColor.GREEN
            value < 0 -> ChatColor.RED
            else -> ChatColor.GRAY
        }
        return TextComponent("").apply {
            val perm = TextComponent(permission)
            perm.color = valueColor
            addExtra(perm)
            addExtra(EQUALS_SIGN)
            addExtra(value.toString())
        }
    }

    override fun option(
        permission: String,
        value: String
    ): BaseComponent {
        val ret: BaseComponent = TextComponent(permission)
        ret.addExtra(EQUALS_SIGN)
        ret.addExtra(value)
        return ret
    }

    override fun BaseComponent.header(): BaseComponent {
        this.isBold = true
        return this
    }

    override fun BaseComponent.hl(): BaseComponent {
        this.color = hlColour
        return this
    }

    override fun combined(vararg elements: Any): BaseComponent {
        return if (elements.isEmpty()) {
            TextComponent("")
        } else {
            val base = componentFrom(elements[0])
            for (i in 1 until elements.size) {
                base.addExtra(componentFrom(elements[i]))
            }
            base
        }
    }

    private fun componentFrom(obj: Any): BaseComponent {
        return when (obj) {
            is Translatable -> obj.tr()
            is BaseComponent -> obj
            else -> TextComponent(obj.toString())
        }
    }

    override fun Translatable.tr(): BaseComponent {
        val args = args.map { componentFrom(it) }.toTypedArray()
        return TranslatableComponent(translate(cmd.locale), *args)
    }

    override fun String.unaryMinus(): BaseComponent {
        return TextComponent(this)
    }

    override fun BaseComponent.plus(other: BaseComponent): BaseComponent {
        return TextComponent().also {
            it.addExtra(this)
            it.addExtra(other)
        }
    }

    override fun Collection<BaseComponent>.concat(separator: BaseComponent): BaseComponent {
        return this.foldIndexed(TextComponent()) { idx, acc, el ->
            if (idx != 0) {
                acc.addExtra(separator)
            }
            acc.addExtra(el)
            acc
        }
    }
}
