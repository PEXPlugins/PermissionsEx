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
package ca.stellardrift.permissionsex.exception;

import ca.stellardrift.permissionsex.util.Translatable;
import ca.stellardrift.permissionsex.util.Translations;

public class PermissionsLoadingException extends PermissionsException {
    private static final long serialVersionUID = -7821206470445564912L;

    public PermissionsLoadingException(Translatable message) {
        super(message);
    }

    public PermissionsLoadingException(Translatable message, Throwable cause) {
        super(message, cause);
    }

    public PermissionsLoadingException(Throwable cause) {
        super(Translations.t("Error while loading permissions"), cause);
    }
}
