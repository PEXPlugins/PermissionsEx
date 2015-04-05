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
package ninja.leaping.permissionsex.util.command;

import ninja.leaping.permissionsex.util.Translatable;

import java.util.Map;

/**
 * Interface specifying code to get specific elements of commands
 */
public interface MessageFormatter<TextType> {
    /**
     * Print the subject in a user-friendly manner. May link to the subject info printout
     *
     * @param subject The subject to show
     * @return the formatted value
     */
    public TextType subject(Map.Entry<String, String> subject);

    /**
     * Print the given boolean in a user-friendly manner.
     * Generally this means green if true, or red if false
     * @param val The value to print
     * @return the formatted value
     */
    public TextType booleanVal(boolean val);
    public TextType permission(String permission, int value);
    public TextType option(String permission, String value);

    /**
     * Format the given line of text to be used in a header
     * @param text
     * @return
     */
    public TextType header(TextType text);

    /**
     * Highlight the passed text
     * @param text The text to highlight
     * @return The highlighted text
     */
    public TextType hl(TextType text);

    /**
     * Combines an array containing elements of type {@link TextType} and {@link java.lang.String} into a single message
     *
     * @param elements The elements to combine
     * @return A combined, formatted element
     */
    public TextType combined(Object... elements);

    /**
     * Return the internal representation of the given translatable text.
     *
     * @param tr The translatable text
     * @return the formatted value
     */
    public TextType tr(Translatable tr);
}
