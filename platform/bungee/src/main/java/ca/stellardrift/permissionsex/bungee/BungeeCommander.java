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
package ca.stellardrift.permissionsex.bungee;

import ca.stellardrift.permissionsex.minecraft.command.Commander;
import ca.stellardrift.permissionsex.minecraft.command.MessageFormatter;
import ca.stellardrift.permissionsex.proxycommon.ProxyCommon;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;

import static net.kyori.adventure.text.Component.text;

final class BungeeCommander implements Commander {
    private final PermissionsExPlugin pex;
    private final CommandSender source;
    private final Audience audience;

    BungeeCommander(final PermissionsExPlugin pex, final CommandSender source) {
        this.pex = pex;
        this.source = source;
        this.audience = pex.adventure().sender(source);
    }

    @Override
    public Component name() {
        return text(this.source.getName());
    }

    @Override
    public SubjectRef<?> subjectIdentifier() {
        if (this.source instanceof ProxiedPlayer) {
            return SubjectRef.subject(this.pex.users().type(), ((ProxiedPlayer) this.source).getUniqueId());
        } else {
            return ProxyCommon.IDENT_SERVER_CONSOLE;
        }
    }

    @Override
    public MessageFormatter formatter() {
        return this.pex.manager().messageFormatter();
    }

    @Override
    public boolean hasPermission(final String permission) {
        return this.source.hasPermission(permission);
    }

    @Override
    public @NonNull Audience audience() {
        return this.audience;
    }

    CommandSender source() {
        return this.source;
    }

}
