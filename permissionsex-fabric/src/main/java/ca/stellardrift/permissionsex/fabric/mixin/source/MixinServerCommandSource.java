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
 *
 */

package ca.stellardrift.permissionsex.fabric.mixin;

import ca.stellardrift.permissionsex.fabric.IPermissionCommandSource;
import ca.stellardrift.permissionsex.fabric.PermissionsExMod;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerCommandSource.class)
public abstract class MixinServerCommandSource implements IPermissionCommandSource {

    @Shadow
    private CommandOutput output;

    @Shadow
    private String simpleName;

    @Shadow
    public abstract boolean hasPermissionLevel(int level);

    @Shadow
    public abstract MinecraftServer getMinecraftServer();

    @NotNull
    @Override
    public String getPermType() {
        return output instanceof IPermissionCommandSource ? ((IPermissionCommandSource) output).getPermType() : PermissionsExMod.SUBJECTS_SYSTEM;
    }

    @NotNull
    @Override
    public String getPermIdentifier() {
        return output instanceof IPermissionCommandSource ? ((IPermissionCommandSource) output).getPermIdentifier() : simpleName;
    }

    @Override
    public boolean hasPermission(@NotNull String perm) {
        return output instanceof IPermissionCommandSource ? ((IPermissionCommandSource) output).hasPermission(perm) : hasPermissionLevel(getMinecraftServer().getOpPermissionLevel());
    }

    @NotNull
    @Override
    public CalculatedSubject asCalculatedSubject() {
        return output instanceof IPermissionCommandSource ? ((IPermissionCommandSource) output).asCalculatedSubject() : PermissionsExMod.INSTANCE.getManager().getSubjects(getPermType()).get(getPermIdentifier()).join();
    }
}
