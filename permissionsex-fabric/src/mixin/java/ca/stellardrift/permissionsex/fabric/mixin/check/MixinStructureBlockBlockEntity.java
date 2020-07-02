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
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructureBlockBlockEntity.class)
public class MixinStructureBlockBlockEntity {
    @Redirect(method = "openScreen", at = @At(value = "INVOKE", target = "net.minecraft.entity.player.PlayerEntity.isCreativeLevelTwoOp()Z"))
    public boolean canViewStructureBlock(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity
                && !PermissionsExHooks.hasPermission(player, MinecraftPermissions.STRUCTURE_BLOCK_VIEW)) {
            ((ServerPlayerEntity) player).networkHandler.sendPacket(new CloseScreenS2CPacket());
            return false;
        }
        return true;
    }
}
