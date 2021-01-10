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
package ca.stellardrift.permissionsex.velocity;

import ca.stellardrift.permissionsex.minecraft.command.Commander;
import ca.stellardrift.permissionsex.minecraft.command.MessageFormatter;
import ca.stellardrift.permissionsex.proxycommon.ProxyCommon;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;

import static net.kyori.adventure.text.Component.text;

final class VelocityCommander implements Commander {
    private final PermissionsExPlugin pex;
    private final CommandSource source;

    VelocityCommander(final PermissionsExPlugin pex, final CommandSource source) {
        this.pex = pex;
        this.source = source;
    }

    @Override
    public Component name() {
        if (this.source instanceof Player) {
            return text(((Player) this.source).getUsername());
        } else {
            return text(ProxyCommon.IDENT_SERVER_CONSOLE.identifier());
        }
    }

    @Override
    public SubjectRef<?> subjectIdentifier() {
        if (this.source instanceof Player) {
            return SubjectRef.subject(pex.users().type(), ((Player) this.source).getUniqueId());
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
    public @NonNull CommandSource audience() {
        return this.source;
    }

}
