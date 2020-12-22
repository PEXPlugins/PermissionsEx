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
import ca.stellardrift.permissionsex.fabric.impl.ServerCommandSourceBridge;
import ca.stellardrift.permissionsex.fabric.impl.FunctionContextDefinition;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import com.mojang.brigadier.CommandDispatcher;
import kotlin.collections.ArrayDeque;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.FunctionLoader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(FunctionLoader.class)
public class FunctionLoaderMixin {

    // When loading functions, use a function subject named the
    @Dynamic("Lambda method in FunctionLoader#reload")
    @Redirect(method = "method_29451", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/function/CommandFunction;create(Lnet/minecraft/util/Identifier;Lcom/mojang/brigadier/CommandDispatcher;Lnet/minecraft/server/command/ServerCommandSource;Ljava/util/List;)Lnet/minecraft/server/function/CommandFunction;"))
    private CommandFunction pex$createWithRedirectedPermission(final Identifier id, final CommandDispatcher<ServerCommandSource> dispatcher, final ServerCommandSource source, final List<String> lines) {
        // TODO: Figure out the best permissions model for functions
        final ArrayDeque<Identifier> runningFunctions = FunctionContextDefinition.INSTANCE.getCurrentFunctions$fabric().get();
        runningFunctions.add(id);
        try {
            return CommandFunction.create(id, dispatcher, ((ServerCommandSourceBridge) source).withSubjectOverride(SubjectRef.subject(FabricPermissionsEx.getFunctionSubjectType(), id)), lines);
        } finally {
            runningFunctions.removeLast();
        }
    }
}
