/*
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

package ca.stellardrift.permissionsex.util.glob;

import java.util.Objects;

/**
 * A compiled version of a glob expression.
 *
 * A glob expression is a literal string, with several additional tokens:
 * <dl>
 *     <dt>{a,b,c}</dt>
 *     <dd>Any of the comma-separated tokens, represented by {@link OrNode}</dd>
 *     <dt>[a-ce]</dt>
 *     <dd>Any of the characters specified. a - indicates a character range</dd>
 * </dl>
 */
public abstract class GlobNode implements Iterable<String> {
    GlobNode() {}

    /**
     * Check if the input matches this glob
     *
     * @param input text to validate
     * @return whether we match
     */
    public boolean matches(String input) {
        Objects.requireNonNull(input, "input");
        for (String value : this) {
            if (input.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesIgnoreCase(String input) {
        Objects.requireNonNull(input, "input");
        for (String value : this) {
            if (input.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
