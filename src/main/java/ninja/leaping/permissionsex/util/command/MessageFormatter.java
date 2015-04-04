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
    public TextType subject(Map.Entry<String, String> subject);
    public TextType booleanVal(boolean val);
    public TextType permission(String permission, int value);
    public TextType option(String permission, String value);
    public TextType highlighted(Translatable text);

    /**
     * Combines an array containing elements of type {@link TextType} and {@link java.lang.String} into a single message
     *
     * @param elements The elements to combine
     * @return A combined, formatted element
     */
    public TextType combined(Object... elements);

    public TextType translated(Translatable tr, Object... elements);
}
