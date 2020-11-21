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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

/**
 * An exception where PermissionsEx is involved.
 *
 * @since 2.0.0
 */
public class PermissionsException extends Exception {
    private static final long serialVersionUID = 138001301588644173L;
    private static final Component NULL = Component.text("null");

    private final @Nullable Component message;

    public PermissionsException(final @Nullable Component message) {
        this.message = message;
    }

    public PermissionsException(final @Nullable Component message, final @Nullable Throwable cause) {
        super(cause);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return getLocalizedMessage();
    }

    @Override
    public String getLocalizedMessage() {
        return getLocalizedMessage(Locale.getDefault());
    }

    public Component getComponent() {
        return this.message == null ? NULL : message;
    }

    public String getLocalizedMessage(Locale locale) {
        return PlainComponentSerializer.plain().serialize(GlobalTranslator.renderer().render(getComponent(), locale));
    }
}
