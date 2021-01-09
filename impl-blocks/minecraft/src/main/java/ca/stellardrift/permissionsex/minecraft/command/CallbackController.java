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

import ca.stellardrift.permissionsex.subject.SubjectRef;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.standard.UUIDArgument;
import cloud.commandframework.meta.CommandMeta;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * Handler for command callbacks.
 */
public final class CallbackController {
    private static final CommandArgument<Commander, UUID> CALLBACK_ID = UUIDArgument.of("id");
    private final ConcurrentMap<String, java.util.concurrent.ConcurrentMap<UUID, CallbackInstance>> knownCallbacks = new ConcurrentHashMap<>();

    /**
     * Register a callback, returning the command string to send to execute the provided function.
     *
     * @return the command to execute for the provided function
     */
    public String registerCallback(final Commander source, final Consumer<Commander> callback)  {
        final UUID id = UUID.randomUUID();
        knownCallbacks.computeIfAbsent(mapKey(source), $ -> new ConcurrentHashMap<>())
                .put(id, new CallbackInstance(source, callback, false));
        return "/pex cb " + id;
    }

    private String mapKey(final Commander cmd) {
        final @Nullable SubjectRef<?> ident = cmd.subjectIdentifier();
        if (ident != null) {
            return ident.serializedIdentifier();
        } else {
            return PlainComponentSerializer.plain().serialize(cmd.name());
        }
    }

    public void clearOwnedBy(final String name) {
        this.knownCallbacks.remove(name);
    }

    public void clearOwnedBy(final UUID name) {
        knownCallbacks.remove(name.toString().toLowerCase(Locale.ROOT));
    }

    public void registerCommand(final CommandRegistrationContext registration) {
        registration.register(builder -> builder.argument(CALLBACK_ID)
            .meta(CommandMeta.DESCRIPTION, "Trigger a registered command callback")
            .hidden()
            .handler(ctx -> {
                final UUID id = ctx.get(CALLBACK_ID);
                final Map<UUID, CallbackInstance> userCalllbacks = knownCallbacks.get(mapKey(ctx.getSender()));
                if (userCalllbacks == null) {
                    throw new CommandException(Messages.COMMAND_CALLBACK_ERROR_UNKNOWN_ID.tr(id));
                }
                final CallbackInstance callback = userCalllbacks.get(id);
                if (callback == null) {
                    throw new CommandException(Messages.COMMAND_CALLBACK_ERROR_UNKNOWN_ID.tr(id));
                }

                if (!mapKey(callback.source).equals(mapKey(ctx.getSender()))) {
                    throw new CommandException(Messages.COMMAND_CALLBACK_ERROR_ONLY_OWN_ALLOWED.tr());
                }

                try {
                    callback.callback.accept(ctx.getSender());
                } finally {
                    if (callback.oneUse) {
                        userCalllbacks.remove(id);
                    }
                }
            }), "callback", "cb").toString();
    }


    static final class CallbackInstance {
        final Commander source;
        final Consumer<Commander> callback;
        final boolean oneUse;

        CallbackInstance(final Commander source, final Consumer<Commander> callback, final boolean oneUse) {
            this.source = source;
            this.callback = callback;
            this.oneUse = oneUse;
        }
    }
}
