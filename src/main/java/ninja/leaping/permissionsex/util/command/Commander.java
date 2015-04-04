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


import ninja.leaping.permissionsex.data.CalculatedSubject;
import ninja.leaping.permissionsex.util.Translatable;

import java.util.Map;
import java.util.Set;

/**
 * Created by zml on 20.03.15.
 */
public interface Commander<TextType> {
    public String getName();
    public boolean hasPermission(String permission);
    public Set<Map.Entry<String, String>> getActiveContexts();
    public MessageFormatter<TextType> fmt();
    public void msg(Translatable text, Object... args);
    public void debug(Translatable text, Object... args);
    public void error(Translatable text, Object... args);
}
