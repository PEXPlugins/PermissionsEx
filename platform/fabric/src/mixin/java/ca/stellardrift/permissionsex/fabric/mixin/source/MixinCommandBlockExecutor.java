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
import ca.stellardrift.permissionsex.subject.SubjectType;
import net.minecraft.text.Text;
import net.minecraft.world.CommandBlockExecutor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CommandBlockExecutor.class)
public class MixinCommandBlockExecutor implements IPermissionCommandSource<String> {
    private volatile @MonotonicNonNull CalculatedSubject permissionsex$subject;

    @Shadow
    private Text customName;

    @Override
    public @NotNull SubjectType<String> getPermType() {
        return PermissionsExMod.INSTANCE.getCommandBlockSubjectType();
    }

    @Override
    public @NotNull String getPermIdentifier() {
        return this.customName.asString();
    }

    @Override
    public @NotNull CalculatedSubject asCalculatedSubject() {
        if (this.permissionsex$subject == null) {
            return this.permissionsex$subject = PermissionsExMod.INSTANCE.getManager().subjects(getPermType()).get(getPermIdentifier()).join();
        }
        return this.permissionsex$subject;
    }
}
