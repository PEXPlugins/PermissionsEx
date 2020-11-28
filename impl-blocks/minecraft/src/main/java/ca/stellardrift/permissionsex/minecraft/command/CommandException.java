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
package ca.stellardrift.permissionsex.minecraft.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.util.ComponentMessageThrowable;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * An exception caused during command execuction.
 */
public class CommandException extends RuntimeException implements ComponentMessageThrowable {

    private static final long serialVersionUID = -4818903806299876921L;
    private final Component message;

    public CommandException(final Component message) {
        super(null, null, true, false);
        this.message = requireNonNull(message, "message");
    }

    public CommandException(final Component message, final @Nullable Throwable cause) {
        super(null, cause, true, false);
        this.message = requireNonNull(message, "message");
    }

    @Override
    public String getMessage() {
        return PlainComponentSerializer.plain().serialize(this.message);
    }

    @Override
    public String getLocalizedMessage() {
        return PlainComponentSerializer.plain().serialize(GlobalTranslator.render(this.message, Locale.getDefault()));
    }

    @Override
    public Component componentMessage() {
        return this.message;
    }
}
