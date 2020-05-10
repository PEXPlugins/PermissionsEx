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
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Part of brigadier, so we don't need to remap anything!
 * @param <S> Source
 * @param <T> Argument builder type
 */
@Mixin(value = ArgumentBuilder.class, remap = false)
public abstract class MixinArgumentBuilder<S, T extends ArgumentBuilder<S, T>> {
    @Shadow
    protected abstract T getThis();

    @Shadow
    private Predicate<S> requirement;

    @Shadow
    private CommandNode<S> target;


    /**
     * For built-in minecraft commands, replaces any provided requirement (which for all commands is an op level check)
     * with a permission check that attempts to be appropriate and take into account aliases
     * @author zml
     * @reason replace simple setter with more complex logic
     * @param requirement The requirement that may be an op check
     * @return this
     */
    @Overwrite
    @SuppressWarnings({"ConstantConditions"})
    public ArgumentBuilder<S, T> requires(final Predicate<S> requirement) {
        if ((Object) this instanceof LiteralArgumentBuilder && requirement.getClass().getName().startsWith("net.minecraft")) { // Builtin commands
           final String commandName = getUnaliasedCommandName();
           if (commandName != null) {
               this.requirement = PermissionsExHooks.commandPermissionCheck(MinecraftPermissions.forCommand(commandName));
               return getThis();
           }
        }

        this.requirement = requirement;
        return getThis();
    }

    private String getUnaliasedCommandName() {
        if (target != null) {
            final Set<CommandNode<?>> visited = new HashSet<>(); // Avoid any circular inheritance that might crop up
            CommandNode<S> redirect = target;
            String name = null;
            while (redirect != null && !visited.contains(redirect)) {
                name = redirect.getName();
                visited.add(redirect);
                redirect = redirect.getRedirect();
            }
            return name;
        } else {
            final ArgumentBuilder<S, T> build = getThis();
            if (build instanceof LiteralArgumentBuilder) {
                return ((LiteralArgumentBuilder) build).getLiteral();
            }
        }
        return null;
    }
}
