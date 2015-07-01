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

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import ninja.leaping.permissionsex.util.Translatable;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
* Created by zml on 04.04.15.
*/
class TestCommander implements Commander<String> {

    @Override
    public String getName() {
        return "Test";
    }

    @Override
    public Locale getLocale() {
        return Locale.ROOT;
    }

    @Override
    public Optional<Map.Entry<String, String>> getSubjectIdentifier() {
        return Optional.absent();
    }

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }

    @Override
    public Set<Map.Entry<String, String>> getActiveContexts() {
        return Collections.emptySet();
    }

    @Override
    public MessageFormatter<String> fmt() {
        return TestMessageFormatter.INSTANCE;
    }

    @Override
    public void msg(Translatable text) {
        msg(text.translateFormatted(Locale.ROOT));
    }

    @Override
    public void debug(Translatable text) {
        debug(text.translateFormatted(Locale.ROOT));
    }

    @Override
    public void error(Translatable text) {
        error(text.translateFormatted(Locale.ROOT));
    }

    @Override
    public void msg(String text) {
        System.out.println("msg: " + text);
    }

    @Override
    public void debug(String text) {
        System.out.println("debug: " + text);
    }

    @Override
    public void error(String text) {
        System.err.println("error: " + text);
    }

    @Override
    public void msgPaginated(Translatable title, Translatable header, Iterable<String> text) {
        final String titleStr = title.translateFormatted(Locale.ROOT);
        System.out.println(titleStr);
        System.out.println(header.translateFormatted(Locale.ROOT));
        System.out.println(Strings.repeat("=", titleStr.length()));
        for (String line : text) {
            System.out.println(line);
        }
    }
}
