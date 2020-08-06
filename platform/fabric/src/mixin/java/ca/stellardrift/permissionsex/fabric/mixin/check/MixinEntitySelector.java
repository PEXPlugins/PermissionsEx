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
import net.minecraft.command.EntitySelector;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntitySelector.class)
public class MixinEntitySelector {

    /**
     * Redirect op level selector usage check to a permission
     *
     * @param src The source requesting this permission
     * @param permissionLevel Generally 2, unused
     * @return Permission result
     */
    @Redirect(method = "checkSourcePermission",
            at = @At(value = "INVOKE", target = RedirectTargets.SERVER_COMMAND_SOURCE_HAS_PERM_LEVEL))
    public boolean commandSourceHasPermission(ServerCommandSource src, int permissionLevel) {
        return PermissionsExHooks.hasPermission(src, MinecraftPermissions.USE_SELECTOR);
    }

}
