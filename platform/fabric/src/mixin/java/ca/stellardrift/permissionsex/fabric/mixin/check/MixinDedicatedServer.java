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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.OperatorList;
import net.minecraft.server.dedicated.DedicatedPlayerManager;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static ca.stellardrift.permissionsex.PermissionsEx.SUBJECTS_USER;

@Mixin(MinecraftDedicatedServer.class)
public class MixinDedicatedServer {

    /**
     * If we have no users defined, don't enable spawn protection -- equivalent to no ops check
     *
     * @param ops The list being checked
     * @return Whether or not spawn protection is ignored
     */
    @Redirect(method = "isSpawnProtected",
            at = @At(value = "INVOKE", target = RedirectTargets.OPERATOR_LIST_IS_EMPTY))
    public boolean isSpawnProtectionIgnored(OperatorList ops) {
        return !PermissionsExMod.INSTANCE.getManager().getRegisteredSubjectTypes().contains(SUBJECTS_USER);
    }

    /*
     * Check if a specific player is allowed to bypass spawn protection
     */
    @Redirect(method = "isSpawnProtected",
            at = @At(value = "INVOKE", target = RedirectTargets.DEDICATED_PLAYER_MANAGER_IS_OP))
    public boolean isSpawnProtectionBypassed(DedicatedPlayerManager manager, GameProfile profile, ServerWorld world, BlockPos buildPosition, PlayerEntity player) {
        return PermissionsExHooks.hasPermission(player, MinecraftPermissions.BYPASS_SPAWN_PROTECTION);
    }
}
