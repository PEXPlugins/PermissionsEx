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

import ca.stellardrift.permissionsex.fabric.impl.Functions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;

@Mixin(CommandFunction.FunctionElement.class)
public class CommandFunctionFunctionElementMixin {

    // Track current function

    @Inject(method = "method_17914", at = @At("HEAD"), remap = false)
    private static void pex$addFunctionSentinel(
            final int i,
            final ArrayDeque<CommandFunctionManager.Entry> steps,
            final CommandFunctionManager manager,
            final ServerCommandSource source,
            final CommandFunction function,
            final CallbackInfo ci) {
        steps.addFirst(Functions.PopExecutingEntry.INSTANCE);
    }

    @Inject(method = "method_17914", at = @At("TAIL"), remap = false)
    private static void pex$addFunctionId(
            final int i,
            final ArrayDeque<CommandFunctionManager.Entry> steps,
            final CommandFunctionManager manager,
            final ServerCommandSource source,
            final CommandFunction function,
            final CallbackInfo ci) {
        Functions.Context.currentFunctions().add(function.getId());
    }
}
