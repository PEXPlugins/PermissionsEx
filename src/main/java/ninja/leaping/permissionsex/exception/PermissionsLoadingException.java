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

import java.util.Locale;

public class PermissionsLoadingException extends PermissionsException {
    public PermissionsLoadingException(Translatable message, Object... args) {
        super(message, args);
    }

    public PermissionsLoadingException(Translatable message, Throwable cause, Object... args) {
        super(message, cause, args);
    }
}
