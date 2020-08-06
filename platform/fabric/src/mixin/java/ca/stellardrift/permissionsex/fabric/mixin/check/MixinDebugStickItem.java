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
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DebugStickItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DebugStickItem.class)
public class MixinDebugStickItem {

    @Redirect(method = "use", at = @At(value = "INVOKE",
            target = RedirectTargets.IS_CREATIVE_LEVEL_TWO_OP))
    public boolean canUseDebugStick(PlayerEntity ply, PlayerEntity unused, BlockState block,
                                    WorldAccess world, BlockPos pos, boolean isRightClick, ItemStack heldItem) {
        Identifier usedBlock = Registry.BLOCK.getId(block.getBlock());
        return PermissionsExHooks.hasPermission(ply, MinecraftPermissions.makeSpecific(MinecraftPermissions.DEBUG_STICK_USE, usedBlock));
    }
}
