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
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * Handler for command callbacks.
 */
public final class CallbackController {
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

    public Object createCommand() {
        throw new UnsupportedOperationException("Cloud does not yet exist :(");
        /*return command("callback", "cb") {
            val uid = uuid() key Messages.COMMAND_ARG_TYPE_CALLBACK_ID.tr()
            args = uid
            description = Messages.COMMAND_CALLBACK_DESCRIPTION.tr()
            executor { src, args ->
                    val callbackId = args[uid]
                val userCallbacks = knownCallbacks[src.mapKey]
                val callback = userCallbacks?.get(callbackId)
                when {
                    callback == null -> throw CommandException(Messages.COMMAND_CALLBACK_ERROR_UNKNOWN_ID.tr(callbackId))
                    callback.source.mapKey != src.mapKey -> throw CommandException(Messages.COMMAND_CALLBACK_ERROR_ONLY_OWN_ALLOWED.tr())
                    else -> try {
                        callback()
                    } finally {
                        if (callback.oneUse) {
                            userCallbacks.remove(callbackId)
                        }
                    }
                }
            }
        }*/
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
