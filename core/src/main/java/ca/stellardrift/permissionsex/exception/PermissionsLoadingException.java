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

package ca.stellardrift.permissionsex.exception;

import ca.stellardrift.permissionsex.Messages;
import net.kyori.text.Component;

public class PermissionsLoadingException extends PermissionsException {
    private static final long serialVersionUID = -3894757413277685930L;

    public PermissionsLoadingException(Component message) {
        super(message);
    }

    public PermissionsLoadingException(Component message, Throwable cause) {
        super(message, cause);
    }

    public PermissionsLoadingException(Throwable cause) {
        super(Messages.ERROR_GENERAL_LOADING.toComponent(), cause);
    }
}
