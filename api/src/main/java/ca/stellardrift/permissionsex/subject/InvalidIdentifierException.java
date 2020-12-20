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
package ca.stellardrift.permissionsex.subject;

/**
 * An exception thrown when an identifier is provided that isn't valid for a subject type.
 *
 * @since 2.0.0
 */
public final class InvalidIdentifierException extends IllegalArgumentException {
    private static final long serialVersionUID = 300758874983936090L;

    private final String unparsedIdentifier;

    public InvalidIdentifierException(final String unparsedIdentifier) {
        super("Invalid subject identifier: " + unparsedIdentifier);

        this.unparsedIdentifier = unparsedIdentifier;
    }

    public String unparsedIdentifier() {
        return this.unparsedIdentifier;
    }
}
