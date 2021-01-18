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

import ca.stellardrift.permissionsex.minecraft.command.Commander;
import ca.stellardrift.permissionsex.minecraft.command.MessageFormatter;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.service.pagination.PaginationList;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kyori.adventure.text.Component.text;

final class SpongeCommander implements Commander {
    private final PermissionsExPlugin pex;
    private final CommandCause cause;

    SpongeCommander(final PermissionsExPlugin pex, final CommandCause cause) {
        this.pex = pex;
        this.cause = cause;
    }

    @Override
    public Component name() {
        return text(cause.getSubject().getFriendlyIdentifier().orElse(cause.getSubject().getIdentifier()));
    }

    @Override
    public @Nullable SubjectRef<?> subjectIdentifier() {
        final @Nullable PermissionsExService service = this.pex.service();
        return service != null ? PEXSubjectReference.asPex(this.cause.asSubjectReference(), service) : null;
    }

    @Override
    public MessageFormatter formatter() {
        return pex.manager().messageFormatter();
    }

    @Override
    public boolean hasPermission(final String permission) {
        return this.cause.hasPermission(permission);
    }

    @Override
    public @NonNull Audience audience() {
        return this.cause.getAudience();
    }

    @Override
    public void sendPaginated(
        final ComponentLike title,
        final @Nullable ComponentLike header,
        final Stream<? extends ComponentLike> lines
    ) {
        final PaginationList.Builder build = pex.game().getServiceProvider().paginationService().builder();

        build.title(title.asComponent().style(s -> formatter().header(formatter().hl(s))));
        if (header != null) {
            build.header(header.asComponent().color(NamedTextColor.GRAY));
        }
        build.contents(lines.map(line -> line.asComponent()
            .colorIfAbsent(formatter().responseColor())).collect(Collectors.toList()))
            .sendTo(this.audience());
    }

    CommandCause cause() {
        return this.cause;
    }

}
