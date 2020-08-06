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

import ca.stellardrift.permissionsex.fabric.IPermissionCommandSource;
import ca.stellardrift.permissionsex.fabric.PermissionsExMod;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import kotlin.jvm.Volatile;
import net.minecraft.text.Text;
import net.minecraft.world.CommandBlockExecutor;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static ca.stellardrift.permissionsex.fabric.FabricDefinitions.SUBJECTS_COMMAND_BLOCK;

@Mixin(CommandBlockExecutor.class)
public class MixinCommandBlockExecutor implements IPermissionCommandSource {
    private volatile CalculatedSubject subj;

    @Shadow
    private Text customName;

    @Override
    public @NotNull String getPermType() {
        return SUBJECTS_COMMAND_BLOCK;
    }

    @Override
    public @NotNull String getPermIdentifier() {
        return customName.asString();
    }

    @Override
    public @NotNull CalculatedSubject asCalculatedSubject() {
        if (subj == null) {
            return subj = PermissionsExMod.INSTANCE.getManager().getSubjects(getPermType()).get(getPermIdentifier()).join();
        }
        return subj;
    }
}
