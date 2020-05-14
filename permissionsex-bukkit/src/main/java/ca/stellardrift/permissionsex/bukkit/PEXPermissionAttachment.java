/*
 * PermissionsEx - a permissions plugin for your server ecosystem
 * Copyright © 2020 zml [at] stellardrift [dot] ca and PermissionsEx contributors
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

import ca.stellardrift.permissionsex.data.SubjectDataReference;
import com.google.common.collect.Maps;
import ca.stellardrift.permissionsex.PermissionsEx;
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

        this.subjectData.update(data -> data.setOption(PermissionsEx.GLOBAL_CONTEXT, "plugin", getPlugin().getName()));
    }

    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public Map<String, Boolean> getPermissions() {
        return Maps.transformValues(subjectData.get().getPermissions(PermissionsEx.GLOBAL_CONTEXT), val -> val > 0);
    }

    @Override
    public void setPermission(String name, boolean value) {
        subjectData.update(old -> old.setPermission(PermissionsEx.GLOBAL_CONTEXT, checkNotNull(name, "name"), value ? 1 : -1));
    }

    @Override
    public void setPermission(Permission perm, boolean value) {
        setPermission(checkNotNull(perm, "perm").getName(), value);
    }

    @Override
    public void unsetPermission(String name) {
        subjectData.update(old -> old.setPermission(PermissionsEx.GLOBAL_CONTEXT, checkNotNull(name, "name"), 0));
    }

    @Override
    public void unsetPermission(Permission perm) {
        unsetPermission(checkNotNull(perm, "perm").getName());
    }

    @Override
    public boolean remove() {
        this.subjectData.update(data -> null);
        return perm.removeAttachmentInternal(this);
    }
}
