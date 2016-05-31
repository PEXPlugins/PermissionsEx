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

/**
 * A tristate value
 */
public enum Tristate {
    TRUE(1),
    FALSE(-1),
    UNDEFINED(0);

    private final int intVal;

    private Tristate(int intVal) {
        this.intVal = intVal;
    }

    public int asInt() {
        return this.intVal;
    }
}
