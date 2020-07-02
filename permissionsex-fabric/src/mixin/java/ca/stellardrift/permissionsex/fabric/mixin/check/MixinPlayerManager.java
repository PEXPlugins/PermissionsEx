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
import ca.stellardrift.permissionsex.fabric.PermissionsExHooks;
import ca.stellardrift.permissionsex.fabric.PermissionsExMod;
import ca.stellardrift.permissionsex.fabric.RedirectTargets;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.OperatorList;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerManager.class)
public abstract class MixinPlayerManager {

    @Shadow protected abstract void sendCommandTree(ServerPlayerEntity player, int opLevel);

    /**
     * Calculate the appropriate client permission level. Currently this is always 4.
     * This means the server is responsible for fine-grained control of permissions.
     * <p>
     * At some point it may make sense to return a more specific value.
     *
     * @param player Target to send a command tree to
     * @reason All logic in the original method needs to be replaced
     * @author zml
     */
    @Overwrite
    public void sendCommandTree(ServerPlayerEntity player) {
        sendCommandTree(player, 4); // TODO: Is this the right choice?
    }

    @Redirect(method = "isWhitelisted", at = @At(value = "INVOKE", target = RedirectTargets.OPERATOR_LIST_CONTAINS))
    protected boolean canBypassWhitelist(OperatorList list, Object profile) {
        return PermissionsExHooks.hasPermission((GameProfile) profile, MinecraftPermissions.BYPASS_WHITELIST);
    }

    @Inject(method = "isOperator", at = @At("HEAD"))
    private void logOpCheck(final GameProfile profile, final CallbackInfoReturnable<Boolean> ci) {
        PermissionsExMod.INSTANCE.logUnredirectedPermissionsCheck("PlayerManager#isOperator");
    }
}
