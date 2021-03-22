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
package ca.stellardrift.permissionsex.sponge;

import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx;
import ca.stellardrift.permissionsex.minecraft.command.MessageFormatter;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.util.Nameable;

final class SpongeMessageFormatter extends MessageFormatter {

    SpongeMessageFormatter(final MinecraftPermissionsEx<?> manager) {
        super(manager);
    }

    @Override
    protected @Nullable <I> String friendlyName(final SubjectRef<I> reference) {
        final @Nullable Object associated = reference.type().getAssociatedObject(reference.identifier());
        return associated instanceof Nameable ? ((Nameable) associated).name() : super.friendlyName(reference);
    }

}
