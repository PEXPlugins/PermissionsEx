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

import java.util.Arrays;
import java.util.Locale;

public abstract class Translatable {
    private final Object[] args;

    protected Translatable(Object... args) {
        this.args = args;
    }

    public Object[] getArgs() {
        return this.args;
    }

    public abstract String getUntranslated();

    public abstract String translate(Locale locale);

    // TODO: Does it make sense to have this?
    private static boolean hasTranslatableArgs(Object... args) {
        for (Object arg : args) {
            if (arg instanceof Translatable) {
                return true;
            }
        }
        return false;
    }

    public String translateFormatted(Locale locale) {
        Object[] translatedArgs = new Object[args.length];
        for (int i = 0; i < this.args.length; ++i) {
            Object arg = this.args[i];
            if (arg instanceof Translatable) {
                arg = ((Translatable) arg).translate(locale);
            }
            translatedArgs[i] = arg;
        }
        return String.format(locale, translate(locale), translatedArgs);
    }

    @Override
    public String toString() {
        return "Translatable{" +
                "untranslated=" + getUntranslated() +
                "args=" + Arrays.toString(args) +
                '}';
    }
}
