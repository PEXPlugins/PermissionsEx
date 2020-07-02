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
import net.minecraft.server.dedicated.DedicatedPlayerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DedicatedPlayerManager.class)
public class MixinDedicatedPlayerManager {
    /**
     * Replace operator check with permission check for server player limit
     *
     * @param profile game profile to check
     * @return Whether the user represented by the given profile is permitted to bypass the server's player limit
     * @reason One-line method, so easiest to just overwrite
     * @author zml
     */
    @Overwrite
    public boolean canBypassPlayerLimit(GameProfile profile) {
        return PermissionsExHooks.hasPermission(profile, MinecraftPermissions.BYPASS_PLAYER_LIMIT);
    }

    @Redirect(method = "isWhitelisted", at = @At(value = "INVOKE", target = RedirectTargets.DEDICATED_PLAYER_MANAGER_IS_OP))
    public boolean canBypassWhitelist(DedicatedPlayerManager manager, GameProfile profile) {
        return PermissionsExHooks.hasPermission(profile, MinecraftPermissions.BYPASS_WHITELIST);
    }
}
