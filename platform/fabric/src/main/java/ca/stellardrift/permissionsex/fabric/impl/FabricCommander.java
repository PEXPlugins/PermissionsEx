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
package ca.stellardrift.permissionsex.fabric.impl;

import ca.stellardrift.permissionsex.fabric.impl.bridge.PermissionCommandSourceBridge;
import ca.stellardrift.permissionsex.fabric.mixin.ServerCommandSourceAccess;
import ca.stellardrift.permissionsex.minecraft.command.Commander;
import ca.stellardrift.permissionsex.minecraft.command.MessageFormatter;
import ca.stellardrift.permissionsex.minecraft.command.Permission;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.fabric.AdventureCommandSourceStack;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.server.command.ServerCommandSource;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

final class FabricCommander implements Commander {
    private final ServerCommandSource source;
    private final AdventureCommandSourceStack output;

    FabricCommander(final ServerCommandSource source) {
        this.source = source;
        this.output = (AdventureCommandSourceStack) source;
    }

    ServerCommandSource source() {
        return this.source;
    }

    @Override
    public Component name() {
        return FabricServerAudiences.of(this.source.getMinecraftServer()).toAdventure(this.source.getDisplayName());
    }

    @Override
    public @Nullable SubjectRef<?> subjectIdentifier() {
        if (this.source instanceof PermissionCommandSourceBridge<?>) {
            return ((PermissionCommandSourceBridge<?>) this.source).asReference();
        }
        return null;
    }

    @Override
    public MessageFormatter formatter() {
        return FabricPermissionsExImpl.INSTANCE.manager().messageFormatter();
    }

    @Override
    public boolean hasPermission(final String permission) {
        if (this.source instanceof PermissionCommandSourceBridge<?>) {
            return ((PermissionCommandSourceBridge<?>) this.source).hasPermission(permission);
        } else {
            return this.source.hasPermissionLevel(this.source.getMinecraftServer().getOpPermissionLevel());
        }
    }

    @Override
    public boolean hasPermission(final Permission permission) {
        int ret = 0;
        if (source instanceof PermissionCommandSourceBridge<?>) {
            ret = ((PermissionCommandSourceBridge<?>) this.source).asCalculatedSubject().permission(permission.value());
        }
        if (ret == 0) { // op status
            ret = ((ServerCommandSourceAccess) this.source).getLevel();
        }
        if (ret == 0) { // permission def value
            ret = permission.defaultValue();
        }
        return ret > 0;
    }

    @Override
    public @NonNull Audience audience() {
        return this.output;
    }

}
