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
package ca.stellardrift.permissionsex.minecraft;

import ca.stellardrift.permissionsex.minecraft.command.argument.PatternParser;
import cloud.commandframework.CommandManager;
import cloud.commandframework.brigadier.BrigadierManagerHolder;
import cloud.commandframework.brigadier.CloudBrigadierManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;

final class BrigadierRegistration {

    private BrigadierRegistration() {

    }

    @SuppressWarnings("unchecked")
    static void registerArgumentTypes(final CommandManager<?> manager) {
        if (!(manager instanceof BrigadierManagerHolder)) {
            return;
        }

        final @Nullable CloudBrigadierManager<?, ?> brig = ((BrigadierManagerHolder<?>) manager).brigadierManager();
        if (brig == null) {
            return;
        }

        brig.registerMapping(TypeToken.get(PatternParser.class), true, parser -> {
            if (parser.greedy()) {
                return StringArgumentType.greedyString();
            } else {
                return StringArgumentType.string();
            }
        });
    }

}
