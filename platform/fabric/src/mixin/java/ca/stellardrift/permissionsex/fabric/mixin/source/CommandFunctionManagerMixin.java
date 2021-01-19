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

import ca.stellardrift.permissionsex.fabric.FabricPermissionsEx;
import ca.stellardrift.permissionsex.fabric.impl.Functions;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

@Debug(export = true)
@Mixin(CommandFunctionManager.class)
public abstract class CommandFunctionManagerMixin {
    @Shadow public abstract ServerCommandSource shadow$getTaggedFunctionSource();
    @Shadow public abstract int shadow$execute(CommandFunction function, ServerCommandSource source);

    // executeAll: perform our own iteration
    @Inject(method = "method_29460", at = @At(value = "INVOKE", target = "Ljava/util/Collection;iterator()Ljava/util/Iterator;"))
    private void pex$executeFunctionsWithSubject(final Collection<CommandFunction> functions, final Identifier tagName, final CallbackInfo ci) {
        final ServerCommandSource source = FabricPermissionsEx.withSubjectOverride(
                this.shadow$getTaggedFunctionSource(),
                SubjectRef.subject(FabricPermissionsEx.functions(), tagName));

        for (final CommandFunction function : functions) {
            this.shadow$execute(function, source);
        }
    }

    // and modify the existing iterator to be empty:
    @ModifyVariable(method = "method_29460", at = @At(value = "INVOKE_ASSIGN", target = "Ljava/util/Collection;iterator()Ljava/util/Iterator;"), ordinal = 0)
    private Iterator<CommandFunction> pex$makeIteratorEmpty(final Iterator<CommandFunction> it) {
        return Collections.emptyIterator();
    }

    // The last finally block of execute:
    // Since `finally` elements don't really exist, let's target the invocation of `this.chain.clear()`
    // since the instruction specifically targets ArrayDeque#clear.
    // There is only one variable of this type, and it is only cleared at the very end, once state tracking is no longer necessary.

    @Inject(method = "execute", at = @At(value = "INVOKE", target = "Ljava/util/ArrayDeque;clear()V"))
    private void pex$clearExecutingFunctions(final CallbackInfoReturnable<Integer> cir) {
        Functions.Context.currentFunctions().clear();
    }

    @Inject(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/function/CommandFunction;getElements()[Lnet/minecraft/server/function/CommandFunction$Element;"))
    private void pex$addFunctionContext(final CommandFunction function, final ServerCommandSource source, final CallbackInfoReturnable<Integer> cir) {
        Functions.Context.currentFunctions().add(function.getId());
    }
}
