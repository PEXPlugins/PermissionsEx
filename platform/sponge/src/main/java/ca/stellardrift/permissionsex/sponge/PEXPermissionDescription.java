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

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.plugin.PluginContainer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

/**
 * A permisisons description used by PEX.
 *
 * @since 2.0.0
 */
public final class PEXPermissionDescription implements PermissionDescription  {
    private final PermissionsExService service;
    private final String permId;
    private final Optional<Component> description;
    private final Optional<PluginContainer> owner;

    PEXPermissionDescription(
        final PermissionsExService service,
        final String permId,
        final @Nullable Component description,
        final @Nullable PluginContainer owner
    ) {
        this.service = service;
        this.permId = requireNonNull(permId, "permId");
        this.description = Optional.ofNullable(description);
        this.owner = Optional.ofNullable(owner);
    }


    @Override
    public String getId() {
        return this.permId;
    }

    @Override
    public Optional<Component> getDescription() {
        return this.description;
    }

    @Override
    public Optional<PluginContainer> getOwner() {
        return this.owner;
    }

    @Override
    public CompletableFuture<Map<SubjectReference, Boolean>> findAssignedSubjects(
        final String collectionIdentifier
    ) {
        return this.service.loadCollection(collectionIdentifier)
            .thenCompose(coll -> coll.getAllWithPermission(this.permId));
    }

    @Override
    public Map<Subject, Boolean> getAssignedSubjects(final String collectionIdentifier) {
        return this.service.getCollection(collectionIdentifier)
            .map(it -> it.getLoadedWithPermission(this.permId))
            .orElse(Collections.emptyMap());
    }

    public static final class Builder implements PermissionDescription.Builder {
        private final PluginContainer owner;
        private final PermissionsExService service;
        private @Nullable String id;
        private @Nullable Component description;
        private final Map<String, Integer> ranks = new HashMap<>();

        Builder(final PluginContainer owner, final PermissionsExService service) {
            this.owner = owner;
            this.service = service;
        }

        @Override
        public PermissionDescription.Builder id(final String permissionId) {
            this.id = requireNonNull(permissionId, "permissionId");
            return this;
        }

        @Override
        public PermissionDescription.Builder description(@Nullable final Component description) {
            this.description = description;
            return this;
        }

        @Override
        public PermissionDescription.Builder assign(final String role, final boolean value) {
            return this.assign(role, value ? 1 : -1);
        }

        public PermissionDescription.Builder assign(final String role, final int value) {
            this.ranks.put(requireNonNull(role, "role"), value);
            return this;
        }

        @Override
        public PermissionDescription register() throws IllegalStateException {
            requireNonNull(this.id, "An ID must be set");
            final PEXPermissionDescription ret = new PEXPermissionDescription(this.service, this.id, this.description, this.owner);
            this.service.registerDescription(ret, this.ranks);
            return ret;
        }

    }

}
