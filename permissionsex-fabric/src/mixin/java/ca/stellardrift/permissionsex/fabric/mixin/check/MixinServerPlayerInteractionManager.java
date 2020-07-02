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
import net.minecraft.block.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayerInteractionManager.class)
public class MixinServerPlayerInteractionManager {
    @Shadow
    public ServerWorld world;

    @Redirect(method = "tryBreakBlock", at = @At(value = "INVOKE", target = RedirectTargets.SERVER_IS_CREATIVE_LEVEL_TWO_OP))
    public boolean canBreakBlock(ServerPlayerEntity player, BlockPos pos) {
        final BlockState state = world.getBlockState(pos);
        final Block block = state.getBlock();
        String permission;
        if (block instanceof CommandBlock) {
            permission = MinecraftPermissions.COMMAND_BLOCK_BREAK;
        } else if (block instanceof StructureBlock) {
            permission = MinecraftPermissions.STRUCTURE_BLOCK_BREAK;
        } else if (block instanceof JigsawBlock) {
            permission = MinecraftPermissions.JIGSAW_BLOCK_BREAK;
        } else {
            return false;
        }
        return PermissionsExHooks.hasPermission(player, permission);
    }
}
