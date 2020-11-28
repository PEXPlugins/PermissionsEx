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
package ca.stellardrift.permissionsex.util

import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle
import net.kyori.adventure.text.BuildableComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.empty
import net.kyori.adventure.text.Component.space
import net.kyori.adventure.text.ComponentBuilder
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.Style.style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer

fun Any?.toComponent(): Component {
    return when (this) {
        is Boolean -> +this
        is ComponentLike -> this.asComponent()
        is ComponentBuilder<*, *> -> this.build()
        is Component -> this
        else -> Component.text(toString())
    }
}

fun Boolean.toComponent(): Component {
    return +this.toString()
    /*return if (this) {
        Messages.FORMATTER_BOOLEAN_TRUE.bTr().color(NamedTextColor.GREEN)
    } else {
        Messages.FORMATTER_BOOLEAN_FALSE.bTr().color(NamedTextColor.RED)
    }.build()*/
}
operator fun Boolean.unaryPlus() = this.toComponent()

fun String.toComponent() = Component.text(this)
operator fun String.unaryPlus() = Component.text(this)
operator fun String.unaryMinus() = Component.text().content(this)

infix fun String.colored(color: TextColor) = Component.text(this, color)
infix fun String.decorated(decoration: TextDecoration) = Component.text(this, style(decoration))

fun component(init: TextComponent.Builder.() -> Unit): TextComponent = Component.text().also(init).build()
operator fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> B.invoke(init: B.() -> Unit): C {
    init()
    return build()
}

operator fun Component.plus(other: Component) = Component.text()
    .append(this)
    .append(other)
    .build()

operator fun Component.plus(other: ComponentBuilder<*, *>) = this + other.build()

fun Component.styled(init: Style.Builder.() -> Unit): Component {
    val build = Style.style()
    build.merge(this.style())
    build.init()
    return this.style(build)
}

fun Iterable<Any>.join(separator: Component? = space()): Component = Component.join(separator ?: empty(), this.map { it.toComponent() })

fun Sequence<Component>.join(separator: Component? = space()) = Component.join(separator ?: empty(), Iterable { iterator() })

operator fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> B.plusAssign(other: Component) {
    this.append(other)
}

operator fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> B.plusAssign(other: ComponentBuilder<*, *>) {
    this.append(other)
}

operator fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> B.plusAssign(other: Iterable<Component>) {
    this.append(other)
}

// -- PEX-Specific

object PEXComponentRenderer : TranslatableComponentRenderer<Locale>() {
    override fun translate(key: String, context: Locale): MessageFormat {
        val idx = key.indexOf("/")
        if (idx == -1) {
            return MessageFormat(key)
        }
        val bundle = key.substring(0, idx)
        val resource = key.substring(idx + 1)

        val message = ResourceBundle.getBundle(bundle, context).run {
            if (containsKey(resource)) {
                getString(resource)
            } else {
                resource
            }
        }
        return MessageFormat(message)
    }
}
