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
package ninja.leaping.permissionsex.exception;

import com.google.common.base.Function;
import ninja.leaping.permissionsex.util.Translatable;

import javax.annotation.Nullable;
import java.util.Locale;

public class PermissionsException extends Exception {
    private final Translatable message;
    private final Object[] args;
    private final boolean translatableArgs;

    private static boolean hasTranslatableArgs(Object... args) {
        for (Object arg : args) {
            if (arg instanceof Translatable) {
                return true;
            }
        }
        return false;
    }

    public PermissionsException(Translatable message, Object... args) {
        this.message = message;
        this.args = args;
        translatableArgs = hasTranslatableArgs(args);
    }

    public PermissionsException(Translatable message, Throwable cause, Object... args) {
        super(cause);
        this.message = message;
        this.args = args;
        translatableArgs = hasTranslatableArgs(args);
    }

    @Override
    public String getMessage() {
        return getLocalizedMessage();
    }

    @Override
    public String getLocalizedMessage() {
        return getLocalizedMessage(Locale.getDefault());
    }

    public Translatable getTranslatableMessage() {
        return new Translatable() {
            @Override
            public String translate(Locale locale) {
                return getLocalizedMessage(locale);
            }
        };
    }

    public String getLocalizedMessage(Locale locale) {
        if (translatableArgs) {
            Object[] translatedArgs = new Object[args.length];
            for (int i = 0; i < this.args.length; ++i) {
                Object arg = this.args[i];
                if (arg instanceof Function) {
                    arg = ((Translatable) arg).translate(locale);
                }
                translatedArgs[i] = arg;
            }
            return String.format(locale, message.translate(locale), translatedArgs);
        } else {
            return String.format(locale, message.translate(locale), args);
        }
    }
}
