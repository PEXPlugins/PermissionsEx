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
import ca.stellardrift.permissionsex.fabric.RedirectTargets;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.EntityType;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {

    @Shadow
    public ServerPlayerEntity player;

    @Redirect(method = "onQueryEntityNbt",
            at = @At(value = "INVOKE", target = RedirectTargets.SERVER_PLAYER_HAS_PERMISSION_LEVEL))
    public boolean canQueryEntityNbt(ServerPlayerEntity entity, int permLevel) {
        return PermissionsExHooks.hasPermission(entity, MinecraftPermissions.QUERY_ENTITY_NBT, permLevel);
    }

    @Redirect(method = "onQueryBlockNbt",
            at = @At(value = "INVOKE", target = RedirectTargets.SERVER_PLAYER_HAS_PERMISSION_LEVEL))
    public boolean canQueryBlockNbt(ServerPlayerEntity entity, int permLevel) {
        return PermissionsExHooks.hasPermission(entity, MinecraftPermissions.QUERY_BLOCK_NBT, permLevel);
    }

    @Redirect(method = {"onUpdateDifficulty", "onUpdateDifficultyLock"},
            at = @At(value = "INVOKE", target = RedirectTargets.SERVER_PLAYER_HAS_PERMISSION_LEVEL))
    public boolean canUpdateDifficulty(ServerPlayerEntity entity, int permLevel) {
        return PermissionsExHooks.hasPermission(entity, MinecraftPermissions.UPDATE_DIFFICULTY, permLevel);
    }

    @Redirect(method = "onGameMessage",
            at = @At(value = "INVOKE", target = RedirectTargets.PLAYER_MANAGER_IS_OP))
    public boolean canBypassSpamLimit(PlayerManager manager, GameProfile profile) {
        return PermissionsExHooks.hasPermission(player, MinecraftPermissions.BYPASS_CHAT_SPAM);
    }

    @Redirect(method = "onVehicleMove",
            at = @At(value = "INVOKE", target = RedirectTargets.SERVER_NETWORK_HANDLER_IS_HOST))
    public boolean canVehicleMoveTooFast(ServerPlayNetworkHandler self) {
        final Identifier vehicleIdent = EntityType.getId(self.player.getRootVehicle().getType());
        return PermissionsExHooks.hasPermission(self.player,
                MinecraftPermissions.BYPASS_MOVE_SPEED_VEHICLE + "." + vehicleIdent.getNamespace() + "." + vehicleIdent.getPath());
    }

    @Redirect(method = "onPlayerMove",
            at = @At(value = "INVOKE", target = RedirectTargets.SERVER_NETWORK_HANDLER_IS_HOST))
    public boolean canPlayerMoveTooFast(ServerPlayNetworkHandler self) {
        return PermissionsExHooks.hasPermission(self.player, MinecraftPermissions.BYPASS_MOVE_SPEED_PLAYER);
    }

    @Redirect(method = {"onUpdateCommandBlock", "onUpdateCommandBlockMinecart"},
            at = @At(value = "INVOKE", target = RedirectTargets.SERVER_IS_CREATIVE_LEVEL_TWO_OP))
    public boolean canUpdateCommandBlock(ServerPlayerEntity player) {
        return player.isCreative() && PermissionsExHooks.hasPermission(player, MinecraftPermissions.COMMAND_BLOCK_EDIT);
    }

    @Redirect(method = "onStructureBlockUpdate",
            at = @At(value = "INVOKE", target = RedirectTargets.SERVER_IS_CREATIVE_LEVEL_TWO_OP))
    public boolean canUpdateStructureBlock(ServerPlayerEntity player) {
        return player.isCreative() && PermissionsExHooks.hasPermission(player, MinecraftPermissions.STRUCTURE_BLOCK_EDIT);
    }

    @Redirect(method = "onJigsawUpdate",
            at = @At(value = "INVOKE", target = RedirectTargets.SERVER_IS_CREATIVE_LEVEL_TWO_OP))
    public boolean canUpdateJigsaw(ServerPlayerEntity player) {
        return player.isCreative() && PermissionsExHooks.hasPermission(player, MinecraftPermissions.JIGSAW_BLOCK_EDIT);
    }
}
