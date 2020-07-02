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

package ca.stellardrift.permissionsex.fabric.mixin.check;

import ca.stellardrift.permissionsex.fabric.MinecraftPermissions;
import ca.stellardrift.permissionsex.fabric.PermissionsExMod;
import ca.stellardrift.permissionsex.fabric.RedirectTargets;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static ca.stellardrift.permissionsex.fabric.PermissionsExHooks.hasPermission;

@Mixin(ServerCommandSource.class)
public class MixinServerCommandSource {

    @Redirect(method = "sendToOps",
            at = @At(value = "INVOKE", target = RedirectTargets.PLAYER_MANAGER_IS_OP))
    public boolean canReceiveOpBroadcast(PlayerManager manager, GameProfile target) {
        @SuppressWarnings("ConstantConditions")
        final ServerCommandSource source = (ServerCommandSource) (Object) this;
        final ServerPlayerEntity sourcePlayer = source.getEntity() instanceof ServerPlayerEntity ? (ServerPlayerEntity) source.getEntity() : null;
        boolean sourceCanSend = hasPermission(source, MinecraftPermissions.BROADCAST_SEND + "." + target.getId().toString())
                || hasPermission(source, MinecraftPermissions.BROADCAST_SEND + "." + target.getName());
        boolean targetCanReceive = hasPermission(target, MinecraftPermissions.BROADCAST_RECEIVE + "." + source.getName())
                || (sourcePlayer != null && hasPermission(target, MinecraftPermissions.BROADCAST_RECEIVE + "." + sourcePlayer.getGameProfile().getId().toString()));
        return sourceCanSend && targetCanReceive;
    }

    @Inject(method = "hasPermissionLevel", at = @At("HEAD"))
    private void logUncheckedPermission(final int level, final CallbackInfoReturnable<Boolean> ci) {
        PermissionsExMod.INSTANCE.logUnredirectedPermissionsCheck("ServerCommandSource#hasPermissionLevel");
        System.out.println("blah");
    }

}
