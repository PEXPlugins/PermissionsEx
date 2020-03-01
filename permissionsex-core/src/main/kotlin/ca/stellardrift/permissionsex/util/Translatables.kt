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

import java.util.Locale
import java.util.ResourceBundle

class TranslatableProvider(val key: String, val resourceBundleName: String) {
    operator fun get(vararg args: Any): Translatable = ResourceBundleTranslatable(key, resourceBundleName, args)
    operator fun invoke(args: Array<out Any>): Translatable {
        return get(*args)
    }
}

class ResourceBundleTranslatable internal constructor(
    val key: String,
    val resourceBundleName: String,
    args: Array<out Any>
) : Translatable(args) {
    override val untranslated: String =
        key

    override fun translate(locale: Locale): String {
        ResourceBundle.getBundle(resourceBundleName, locale).apply {
            return if (containsKey(key)) {
                getString(key)
            } else {
                key
            }
        }
    }
}

class FixedTranslatable internal constructor(val contents: String, args: Array<out Any>): Translatable(args) {
    override val untranslated: String
        get() = contents

    override fun translate(locale: Locale): String
            = contents

}

sealed class Translatable(val args: Array<out Any>) {

    abstract val untranslated: String
    abstract fun translate(locale: Locale): String

    fun translateFormatted(locale: Locale): String {
        val translatedArgs = args.map {
            if (it is Translatable) {
                it.translateFormatted(locale)
            } else {
                it
            }
        }.toTypedArray()
        return translate(locale).format(locale, *translatedArgs)
    }

    override fun toString(): String {
        return "Translatable{" +
                "untranslated=" + untranslated +
                "args=" + args.contentToString() +
                '}'
    }

    companion object {
        // TODO: Does it make sense to have this?
        private fun hasTranslatableArgs(vararg args: Any): Boolean {
            for (arg in args) {
                if (arg is Translatable) {
                    return true
                }
            }
            return false
        }

        @JvmStatic
        fun fixed(contents: String, vararg args: Any): Translatable {
            return FixedTranslatable(contents, args)
        }
    }
}
