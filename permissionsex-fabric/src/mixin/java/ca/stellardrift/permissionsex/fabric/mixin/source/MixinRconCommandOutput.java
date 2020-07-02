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

package ca.stellardrift.permissionsex.fabric.mixin.source;

import ca.stellardrift.permissionsex.fabric.FabricDefinitions;
import ca.stellardrift.permissionsex.fabric.IPermissionCommandSource;
import net.minecraft.server.rcon.RconCommandOutput;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Class is misnamed in Fabric -- this is actually the command output for rcon connections
 */
@Mixin(RconCommandOutput.class)
public class MixinRconCommandOutput implements IPermissionCommandSource {
    @NotNull
    @Override
    public String getPermType() {
        return FabricDefinitions.SUBJECTS_SYSTEM;
    }

    @NotNull
    @Override
    public String getPermIdentifier() {
        return FabricDefinitions.IDENTIFIER_RCON;
    }
}
