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

import ca.stellardrift.permissionsex.commands.Messages
import ca.stellardrift.permissionsex.context.ContextValue
import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle
import net.kyori.text.BuildableComponent
import net.kyori.text.Component
import net.kyori.text.ComponentBuilder
import net.kyori.text.ScopedComponent
import net.kyori.text.TextComponent
import net.kyori.text.TextComponent.space
import net.kyori.text.format.Style
import net.kyori.text.format.TextColor
import net.kyori.text.format.TextDecoration
import net.kyori.text.renderer.TranslatableComponentRenderer

fun Any?.toComponent(): Component {
    return when (this) {
        is ComponentBuilder<*, *> -> this.build()
        is Component -> this
        else -> TextComponent.of(toString())
    }
}

fun Boolean.toComponent(): Component {
    return if (this) {
        Messages.FORMATTER_BOOLEAN_TRUE.get().color(TextColor.GREEN)
    } else {
        Messages.FORMATTER_BOOLEAN_FALSE.get().color(TextColor.RED)
    }.build()
}
fun Boolean.unaryPlus() = this.toComponent()

fun String.toComponent() = TextComponent.of(this)
operator fun String.unaryPlus() = TextComponent.of(this)
operator fun String.unaryMinus() = TextComponent.builder(this)

infix fun String.colored(color: TextColor) = TextComponent.of(this, color)
infix fun String.decorated(decoration: TextDecoration) = TextComponent.of(this, Style.of(decoration))

fun style(color: TextColor? = null, vararg decoration: TextDecoration = arrayOf()) = Style.of(color, *decoration)
fun component(init: TextComponent.Builder.() -> Unit) = TextComponent.make(init)
operator fun <C : BuildableComponent<C, B>, B : ComponentBuilder<C, B>> B.invoke(init: B.() -> Unit): C {
    init()
    return build()
}

operator fun Component.plus(other: Component) = TextComponent.builder()
    .append(this)
    .append(other)
    .build()

operator fun Component.plus(other: ComponentBuilder<*, *>) = this + other.build()

infix fun Component.coloredIfNecessary(color: TextColor) =
    if (this.color() == null) {
        color(color)
    } else {
        this
    }

infix fun <C : ScopedComponent<C>> C.coloredIfNecessary(color: TextColor) =
    if (this.color() == null) {
        color(color)
    } else {
        this
    }

fun Component.styled(init: Style.Builder.() -> Unit): Component {
    val build = Style.builder()
    build.merge(this.style())
    build.init()
    return this.style(build)
}

fun Iterable<Any>.join(separator: Component? = space()): Component = component {
    val it = iterator()
    while (it.hasNext()) {
        append(it.next().toComponent())
        if (separator != null && it.hasNext()) {
            append(separator)
        }
    }
}

fun Sequence<Component>.join(separator: Component? = space()) = component {
    val it = iterator()
    while (it.hasNext()) {
        append(it.next())
        if (separator != null && it.hasNext()) {
            append(separator)
        }
    } }

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

fun Set<ContextValue<*>>.toComponent(): Component {
    return if (isEmpty()) {
        Messages.COMMON_ARGS_CONTEXT_GLOBAL()
    } else {
        +toString()
    }
}

object PEXComponentRenderer : TranslatableComponentRenderer<Locale>() {
    override fun translation(context: Locale, key: String): MessageFormat {
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
