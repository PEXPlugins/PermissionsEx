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
package ca.stellardrift.permissionsex.fabric.impl.context;

import net.minecraft.server.command.ServerCommandSource;

import java.util.function.Consumer;

/**
 * An interface that can be implemented by context definitions that can draw from
 * a {@link ServerCommandSource} to get current context data.
 */
public interface CommandSourceContextDefinition<T> {
    void accumulateCurrentValues(ServerCommandSource source, Consumer<T> consumer);

}
