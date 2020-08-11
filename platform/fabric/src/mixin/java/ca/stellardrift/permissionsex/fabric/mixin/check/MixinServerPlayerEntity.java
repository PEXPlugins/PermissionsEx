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
import com.mojang.authlib.GameProfile;
import net.minecraft.block.entity.JigsawBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity {

    public MixinServerPlayerEntity(World world_1, BlockPos pos, float yaw, GameProfile gameProfile_1) {
        super(world_1, pos,yaw, gameProfile_1);
    }

    @Inject(method = "openCommandBlockScreen", at = @At("HEAD"), cancellable = true)
    public void onOpenCommandBlock(CallbackInfo ci) {
        if (!PermissionsExHooks.hasPermission(this, MinecraftPermissions.COMMAND_BLOCK_VIEW)) {
            ((ServerPlayerEntity) (Object) this).networkHandler.sendPacket(new CloseScreenS2CPacket()); // Close command block gui
            ci.cancel();
        }
    }

    @Override
    public void openJigsawScreen(JigsawBlockEntity jigsaw) {
        if (!PermissionsExHooks.hasPermission(this, MinecraftPermissions.JIGSAW_BLOCK_VIEW)) {
            ((ServerPlayerEntity) (Object) this).networkHandler.sendPacket(new CloseScreenS2CPacket()); // Close command block gui
        }
    }

    // Validation checks, to make sure we're catching as many permissions requests as we can

    @Override
    public boolean isCreativeLevelTwoOp() {
        PermissionsExMod.INSTANCE.logUnredirectedPermissionsCheck("ServerPlayerEntity#isCreativeLevelTwoOp");
        return super.isCreativeLevelTwoOp();
    }

    @Override
    public boolean hasPermissionLevel(int level) {
        PermissionsExMod.INSTANCE.logUnredirectedPermissionsCheck("ServerPlayerEntity#hasPermissionLevel");
        return super.hasPermissionLevel(level);
    }
}
