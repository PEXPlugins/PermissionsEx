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

package ca.stellardrift.permissionsex.util;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;

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

    public static final ResourceBundle EMPTY_RESOURCE_BUNDLE = new ResourceBundle() { // TODO: Fix maven plugin to expose classes during tests
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

    private static final String BASE_NAME = "ca.stellardrift.permissionsex.locale.messages";

    private static ResourceBundle getBundle(Locale locale) {
        try {
            return ResourceBundle.getBundle(BASE_NAME, locale);
        } catch (MissingResourceException ex) {
            return EMPTY_RESOURCE_BUNDLE;
        }
    }

    @NonNull
    public static Translatable t(final String key, Object... args) {
        return new Translatable(args) {
            @Override
            public String getUntranslated() {
                return key;
            }

            @Override
            public String translate(Locale input) {
                return key;
                //return getBundle(input).getString(key);
            }
        };
    }

    @NonNull
    public static Translatable tn(final String key, final String keyPl, final long count, Object... args) {
        return new Translatable(args) {
            @Override
            public String getUntranslated() {
                return count != 1 ? keyPl : key;
            }

            @Override
            public String translate(Locale input) {
                return getUntranslated();
                //return getBundle(input).getString(count != 1 ? keyPl : key);
            }
        };
    }

    @NonNull
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
