/**
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
package ninja.leaping.permissionsex.bukkit;

import gnu.gettext.GettextResource;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.Translations;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Utility class to handle translations to a given locale
 */
public class BukkitTranslations {

    private BukkitTranslations() {}

    private static ResourceBundle getBundle(Locale locale) {
        try {
            return ResourceBundle.getBundle("ninja.leaping.permissionsex.bukkit.Messages", locale, Translations.CLASS_CONTROL);
        } catch (MissingResourceException ex) {
            return Translations.EMPTY_RESOURCE_BUNDLE;
        }
    }

    public static Translatable t(final String key, Object... args) {
        return new Translatable(args) {
            @Override
            public String getUntranslated() {
                return key;
            }

            @Override
            public String translate(Locale input) {
                return GettextResource.gettext(getBundle(input), key);
            }
        };
    }

    public static Translatable tn(final String key, final String keyPl, final long count, Object... args) {
        return new Translatable(args) {
            @Override
            public String getUntranslated() {
                return count != 1 ? keyPl : key;
            }

            @Override
            public String translate(Locale input) {
                return GettextResource.ngettext(getBundle(input), key, keyPl, count);
            }
        };
    }

    public static Translatable untr(final String key) {
        return new Translatable() {

            @Override
            public String getUntranslated() {
                return key;
            }

            @Override
            public String translate(Locale locale) {
                return getUntranslated();
            }
        };
    }
}
