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

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import ninja.leaping.permissionsex.rank.RankLadder;
import ninja.leaping.permissionsex.util.Translatable;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

/**
* Created by zml on 04.04.15.
*/
class TestMessageFormatter implements MessageFormatter<String> {
    public static final TestMessageFormatter INSTANCE = new TestMessageFormatter();

    @Override
    public String subject(Map.Entry<String, String> subject) {
        return subject.getKey() + ":" + subject.getValue();
    }

    @Override
    public String ladder(RankLadder ladder) {
        return ladder.getName();
    }

    @Override
    public String booleanVal(boolean val) {
        return String.valueOf(val);
    }

    @Override
    public String button(ButtonType type, Translatable label, @Nullable Translatable tooltip, String command, boolean execute) {
        return tr(label);
    }

    @Override
    public String permission(String permission, int value) {
        return permission + "=" + value;
    }

    @Override
    public String option(String permission, String value) {
        return permission + "=" + value;
    }

    @Override
    public String header(String text) {
        return text + '\n' + Strings.repeat("=", text.length());
    }

    @Override
    public String hl(String text) {
        return "*" + text + "*";
    }

    @Override
    public String combined(Object... elements) {
        return Joiner.on("").join(elements);
    }

    @Override
    public String tr(Translatable tr) {
        return tr.translateFormatted(Locale.ROOT);
    }
}
