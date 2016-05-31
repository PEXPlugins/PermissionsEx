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

import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.util.Translatable;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Interface implemented by objects that can execute commands and receive command output
 */
public interface Commander<TextType> {
    String getName();
    Locale getLocale();
    Optional<SubjectRef> getSubjectIdentifier();
    boolean hasPermission(String permission);
    Set<Map.Entry<String, String>> getActiveContexts();
    MessageFormatter<TextType> fmt();

    default void msg(Translatable text) {
        msg(fmt().tr(text));
    }
    default void debug(Translatable text) {
        debug(fmt().tr(text));
    }

    default void error(Translatable text) {
        error(fmt().tr(text));
    }

    void msg(TextType text);
    void debug(TextType text);
    void error(TextType text);
    void msgPaginated(Translatable title, @Nullable Translatable header, Iterable<TextType> text);
}
