/**
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
package ninja.leaping.permissionsex.bukkit;

import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.SubjectDataReference;
import ninja.leaping.permissionsex.util.Tristate;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Permissions attachment that integrates with the PEX backend
 */
public class PEXPermissionAttachment extends PermissionAttachment {
    public static final String ATTACHMENT_TYPE = "attachment";
    private final String identifier = UUID.randomUUID().toString();
    private final SubjectDataReference subjectData;
    private final PEXPermissible perm;
    public PEXPermissionAttachment(Plugin plugin, Player parent, PEXPermissible perm) {
        super(plugin, parent);
        this.perm = perm;

        try {
            this.subjectData = perm.getManager().getSubjects(ATTACHMENT_TYPE).transientData().getReference(this.identifier).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public Map<String, Boolean> getPermissions() {
        return Maps.transformValues(subjectData.get().getSegment(PermissionsEx.GLOBAL_CONTEXT).getPermissions(), val -> val == Tristate.TRUE);
    }

    @Override
    public void setPermission(String name, boolean value) {
        subjectData.updateSegment(PermissionsEx.GLOBAL_CONTEXT, seg-> seg.withPermission(checkNotNull(name, "name"), value ? Tristate.TRUE : Tristate.FALSE));
    }

    @Override
    public void setPermission(Permission perm, boolean value) {
        setPermission(checkNotNull(perm, "perm").getName(), value);
    }

    @Override
    public void unsetPermission(String name) {
        subjectData.updateSegment(PermissionsEx.GLOBAL_CONTEXT, seg-> seg.withPermission(checkNotNull(name, "name"), Tristate.UNDEFINED));
    }

    @Override
    public void unsetPermission(Permission perm) {
        unsetPermission(checkNotNull(perm, "perm").getName());
    }

    @Override
    public boolean remove() {
        return perm.removeAttachmentInternal(this);
    }
}
