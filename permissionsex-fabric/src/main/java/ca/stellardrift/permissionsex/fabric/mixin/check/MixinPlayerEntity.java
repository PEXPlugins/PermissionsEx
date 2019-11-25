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

import ca.stellardrift.permissionsex.fabric.PermissionsExMod;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static ca.stellardrift.permissionsex.util.Translations.t;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {


    /**
     * Warn when permissions checks have not been appropriately redirected to PEX
     *
     * @param ci Mixin callback holder
     */
    @Inject(method = "isCreativeLevelTwoOp", at = @At("HEAD"))
    public void warnOnOpCheck(CallbackInfoReturnable<Boolean> ci) {
        PermissionsExMod.INSTANCE.getLogger().warn(t("A permission check has been made using Minecraft's built-in op system, not PermissionsEx. " +
                "This is most likely incorrect. Please report this, including the stacktrace below, to the PEX team."), new Exception());
    }
}
