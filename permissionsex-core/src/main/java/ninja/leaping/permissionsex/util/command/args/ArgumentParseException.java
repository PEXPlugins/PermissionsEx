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
package ninja.leaping.permissionsex.util.command.args;

import com.google.common.base.Strings;
import ninja.leaping.permissionsex.util.Translatable;
import ninja.leaping.permissionsex.util.command.CommandException;

import java.util.Locale;

/**
 * Exception thrown when arguments are parsed
 */
public class ArgumentParseException extends CommandException {
    private final String source;
    private final int position;

    public ArgumentParseException(Translatable message, String source, int position) {
        super(message);
        this.source = source;
        this.position = position;
    }

    public ArgumentParseException(Translatable message, Throwable cause, String source, int position) {
        super(message, cause);
        this.source = source;
        this.position = position;
    }

    @Override
    public String getLocalizedMessage(Locale locale) {
        if (this.source == null || this.source.isEmpty()) {
            return super.getLocalizedMessage(locale);
        } else {
            return super.getLocalizedMessage(locale) + '\n' + getAnnotatedPosition();
        }
    }

    public String getAnnotatedPosition() {
        String source = this.source;
        int position = this.position;
        if (source.length() > 80) {
            if (position >= 37)  {
                int startPos = position - 37, endPos = Math.min(source.length(), position + 37);
                if (endPos < source.length()) {
                    source = "..." + source.substring(startPos, endPos) + "...";
                } else {
                    source = "..." + source.substring(startPos, endPos);
                }
                position -= 40;
            } else {
                source = source.substring(0, 77) + "...";
            }
        }
        return source + "\n" + Strings.repeat(" ", position) + "^";
    }

    public int getPosition() {
        return this.position;
    }

    public String getSourceString() {
        return this.source;
    }
}
