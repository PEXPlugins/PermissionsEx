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

import ca.stellardrift.permissionsex.proxycommon.ProxyCommon;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.concurrent.CompletableFuture;

final class PEXPermissionFunction implements PermissionFunction {
    private final PermissionsExPlugin plugin;
    private final PermissionSubject source;
    private volatile @MonotonicNonNull CalculatedSubject subject;

    public PEXPermissionFunction(final PermissionsExPlugin plugin, final PermissionSubject source) {
        this.plugin = plugin;
        this.source = source;
    }

    CalculatedSubject subject() {
        CalculatedSubject subject = this.subject;
        if (subject == null) {
            final CompletableFuture<CalculatedSubject> subjectFuture;
            if (source instanceof Player) {
                subjectFuture = plugin.users().get(((Player) source).getUniqueId());
            } else {
                subjectFuture = plugin.engine().subject(ProxyCommon.IDENT_SERVER_CONSOLE);
            }

            subject = this.subject = subjectFuture.join();
        }
        return subject;
    }

    @Override
    public Tristate getPermissionValue(final String permission) {
        return asTristate(this.subject().permission(permission));
    }

    static Tristate asTristate(final int i) {
        if (i < 0) {
            return Tristate.FALSE;
        } else if (i > 0) {
            return Tristate.TRUE;
        } else {
            return Tristate.UNDEFINED;
        }
    }

}
