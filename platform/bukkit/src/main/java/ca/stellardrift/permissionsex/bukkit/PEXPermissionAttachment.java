/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2021 zml [at] stellardrift [dot] ca and PermissionsEx contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ca.stellardrift.permissionsex.bukkit;

import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectType;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;

import static ca.stellardrift.permissionsex.context.ContextDefinitionProvider.GLOBAL_CONTEXT;

final class PEXPermissionAttachment extends PermissionAttachment implements SubjectRef<UUID> {

    static final SubjectType<UUID> ATTACHMENT_TYPE = SubjectType.builder("attachment", UUID.class)
        .serializedBy(UUID::toString)
        .deserializedBy(UUID::fromString)
        .build();

    private final PEXPermissible perm;
    private final UUID identifier = UUID.randomUUID();
    private final SubjectRef.ToData<UUID> subject;

    PEXPermissionAttachment(final Plugin owner, final Player parent, final PEXPermissible perm) {
        super(owner, parent);
        this.perm = perm;
        this.subject = perm.manager().subjects(ATTACHMENT_TYPE).transientData().referenceTo(this.identifier).join();
    }

    @Override
    public SubjectType<UUID> type() {
        return ATTACHMENT_TYPE;
    }

    @Override
    public UUID identifier() {
        return this.identifier;
    }

    @Override
    public Map<String, Boolean> getPermissions() {
        return PCollections.asMap(
            this.subject.get().segment(GLOBAL_CONTEXT).permissions(),
            (k, $) -> k,
            ($, v) -> v > 0
        );
    }

    @Override
    public void setPermission(final String name, final boolean value) {
        this.subject.update(GLOBAL_CONTEXT, s -> s.withPermission(name, value ? 1 : -1));
    }

    @Override
    public void setPermission(final Permission perm, final boolean value) {
        setPermission(perm.getName(), value);
    }

    @Override
    public void unsetPermission(final String name) {
        this.subject.update(GLOBAL_CONTEXT, s -> s.withPermission(name, 0));
    }

    @Override
    public void unsetPermission(final Permission perm) {
        unsetPermission(perm.getName());
    }


    @Override
    public boolean remove() {
        this.subject.update(s -> null);
        return this.perm.removeAttachmentInternal(this);
    }

}
