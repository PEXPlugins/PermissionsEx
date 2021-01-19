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
package ca.stellardrift.permissionsex.fabric.impl.commands;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.keys.CloudKey;
import cloud.commandframework.keys.SimpleCloudKey;
import io.leangen.geantyref.TypeToken;
import net.minecraft.command.CommandSource;

/**
 * Keys used in {@link CommandContext}s available within a {@link FabricCommandManager}
 */
public final class FabricCommandContextKeys {

    private FabricCommandContextKeys() {
    }

    public static final CloudKey<CommandSource> NATIVE_COMMAND_SOURCE = SimpleCloudKey.of(
            "cloud:fabric_command_source",
            TypeToken.get(CommandSource.class)
    );

}
