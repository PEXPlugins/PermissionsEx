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

package ca.stellardrift.permissionsex.sponge

import ca.stellardrift.permissionsex.commands.commander.ButtonType
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.rank.RankLadder
import ca.stellardrift.permissionsex.sponge.SpongeTranslations.t
import ca.stellardrift.permissionsex.util.Translatable
import ca.stellardrift.permissionsex.util.Util
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.service.permission.SubjectData
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.action.TextActions
import org.spongepowered.api.text.format.TextColor
import org.spongepowered.api.text.format.TextColors
import org.spongepowered.api.text.format.TextStyles
import org.spongepowered.api.text.translation.Translation
import org.spongepowered.api.util.annotation.NonnullByDefault
import java.util.Locale
import java.util.concurrent.ExecutionException

/**
 * Factory to create formatted elements of messages
 */
internal class SpongeMessageFormatter(private val pex: PermissionsExPlugin) :
    MessageFormatter<Text.Builder> {
    override fun subject(subject: Map.Entry<String, String>): Text.Builder {
        val source =
            Util.castOptional(
                pex.manager.getSubjects(subject.key).typeInfo.getAssociatedObject(subject.value),
                CommandSource::class.java
            )
        val name: String? =
            source.map { obj: CommandSource -> obj.name }
                .orElseGet {
                    try {
                        pex.loadCollection(subject.key).get().loadSubject(subject.value).get()
                            .subjectData
                            .getOptions(SubjectData.GLOBAL_CONTEXT)["name"]
                    } catch (e: InterruptedException) {
                        pex.logger.error(t("Unable to get name for subject %s", subject), e)
                        null
                    } catch (e: ExecutionException) {
                        pex.logger.error(t("Unable to get name for subject %s", subject), e)
                        null
                    }
                }
        val nameText: Text = if (name != null) {
            Text.of(
                Text.of(
                    TextColors.GRAY,
                    subject.value
                ), "/", name
            )
        } else {
            Text.of(subject.value)
        }
        // <bold>{type}>/bold>:{identifier}/{name} (on click: /pex {type} {identifier} info)
        return Text.builder().append(
            Text.builder(subject.key).style(TextStyles.BOLD).build(),
            Text.of(" "),
            nameText
        )
            .onHover(TextActions.showText(t("Click to view more info").tr().build()))
            .onClick(TextActions.runCommand("/pex " + subject.key + " " + subject.value + " info"))
    }

    override fun ladder(ladder: RankLadder): Text.Builder {
        return Text.builder(ladder.name)
            .style(TextStyles.BOLD)
            .onHover(TextActions.showText(t("Click here to view more info").tr().build()))
            .onClick(TextActions.runCommand("/pex rank " + ladder.name))
    }

    override fun booleanVal(value: Boolean): Text.Builder {
        return (if (value) t("true") else t("false")).tr().color(if (value) TextColors.GREEN else TextColors.RED)
    }

    override fun button(
        type: ButtonType,
        label: Translatable,
        tooltip: Translatable?,
        command: String,
        execute: Boolean
    ): Text.Builder {
        val builder = label.tr()
        val textColor: TextColor = when (type) {
            ButtonType.POSITIVE -> TextColors.GREEN
            ButtonType.NEGATIVE -> TextColors.RED
            ButtonType.NEUTRAL -> TextColors.AQUA
        }
        builder.color(textColor)
        if (tooltip != null) {
            builder.onHover(TextActions.showText(tooltip.tr().build()))
        }
        builder.onClick(
            if (execute) {
                TextActions.runCommand(command)
            } else {
                TextActions.suggestCommand(command)
            }
        )
        return builder
    }

    override fun permission(
        permission: String,
        value: Int
    ): Text.Builder {
        val valueColor: TextColor = when {
            value > 0 -> TextColors.GREEN
            value < 0 -> TextColors.RED
            else -> TextColors.GRAY
        }
        return Text.builder().append(
            Text.of(valueColor, permission),
            EQUALS_SIGN,
            Text.of(value)
        )
    }

    override fun option(
        permission: String,
        value: String
    ): Text.Builder {
        return Text.builder(permission)
            .append(EQUALS_SIGN, Text.of(value))
    }

    override fun callback(title: Translatable, callback: (Commander<Text.Builder>) -> Unit): Text.Builder {
        return title.tr()
            .style(TextStyles.UNDERLINE)
            .color(TextColors.AQUA)
            .onClick(
                TextActions.executeCallback { src -> callback(SpongeCommander(pex, src)) })
    }

    override fun Text.Builder.header(): Text.Builder {
        return style(TextStyles.BOLD)
    }

    override fun Text.Builder.hl(): Text.Builder {
        return this.color(TextColors.AQUA)
    }

    override fun combined(vararg elements: Any): Text.Builder {
        val build = Text.builder()
        for (el in elements) {
            if (el is Text.Builder) {
                build.append(el.build())
            } else {
                build.append(Text.of(el))
            }
        }
        return build
    }

    override fun Translatable.tr(): Text.Builder {
        var unwrapArgs = false
        for (arg in args) {
            if (arg is Translatable || arg is Text.Builder) {
                unwrapArgs = true
                break
            }
        }
        var args = args
        if (unwrapArgs) {
            val oldArgs = args
            args = arrayOfNulls(oldArgs.size)
            for (i in oldArgs.indices) {
                var arg = oldArgs[i]
                if (arg is Translatable) {
                    arg = arg.tr().build()
                } else if (arg is Text.Builder) {
                    arg = arg.build()
                }
                args[i] = arg
            }
        }
        return Text.builder(PEXTranslation(this), *args)
    }


    companion object {
        private val EQUALS_SIGN =
            Text.of(TextColors.GRAY, "=")
    }

    override fun String.unaryMinus(): Text.Builder = Text.builder(this)

    override fun Text.Builder.plus(other: Text.Builder): Text.Builder {
        return Text.builder().append(this.build(), other.build())
    }

    override fun Collection<Text.Builder>.concat(separator: Text.Builder): Text.Builder {
        return Text.joinWith(separator.build(), this.map { it.build() }).toBuilder()
    }
}

@NonnullByDefault
internal class PEXTranslation(private val translation: Translatable) :
    Translation {
    override fun getId(): String {
        return translation.untranslated
    }

    override fun get(locale: Locale): String {
        return translation.translate(locale)
    }

    override fun get(locale: Locale, vararg objects: Any): String {
        return translation.translateFormatted(locale)
    }

}
