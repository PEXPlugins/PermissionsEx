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
package ninja.leaping.permissionsex.util;

import com.google.common.base.Function;
import gnu.gettext.GettextResource;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;

/**
 * Utility class to handle translations to a given locale
 */
public class Translations {
    private static final Enumeration<String> EMPTY_ENUMERATION = new Enumeration<String>() {
        @Override
        public boolean hasMoreElements() {
            return false;
        }

        @Override
        public String nextElement() {
            throw new NoSuchElementException();
        }
    };

    private static final ResourceBundle EMPTY_RESOURCE_BUNDLE = new ResourceBundle() { // TODO: Fix maven plugin to expose classes during tests
        @Override
        protected Object handleGetObject(String key) {
            return null;
        }

        @Override
        public Enumeration<String> getKeys() {
            return EMPTY_ENUMERATION;
        }
    };

    private Translations() {}

    private static final String BASE_NAME = "ninja.leaping.permissionsex.locale.Messages";
    private static final ResourceBundle.Control CLASS_CONTROL = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_CLASS);

    private static ResourceBundle getBundle(Locale locale) {
        try {
            ResourceBundle ret = ResourceBundle.getBundle("ninja.leaping.permissionsex.locale.Messages", locale, CLASS_CONTROL);
            return ret;
        } catch (MissingResourceException ex) {
            return EMPTY_RESOURCE_BUNDLE;
        }
    }

    public static Translatable tr(final String key) {
        return new Translatable() {
            @Override
            public String translate(Locale input) {
                return GettextResource.gettext(getBundle(input), key);
            }
        };
    }

    public static Translatable ntr(final String key, final String keyPl, final long count) {
        return new Translatable() {
            @Override
            public String translate(Locale input) {
                return GettextResource.ngettext(getBundle(input), key, keyPl, count);
            }
        };
    }
}
