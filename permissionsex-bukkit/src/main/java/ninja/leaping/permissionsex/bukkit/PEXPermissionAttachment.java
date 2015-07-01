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

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.data.SubjectCache;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Permissions attachment that integrates with the PEX backend
 */
public class PEXPermissionAttachment extends PermissionAttachment implements Caching<ImmutableOptionSubjectData> {
    public static final String ATTACHMENT_TYPE = "attachment";
    private final String identifier = UUID.randomUUID().toString();
    private ImmutableOptionSubjectData subjectData;
    private final PEXPermissible perm;
    private final SubjectCache cache;
    public PEXPermissionAttachment(Plugin plugin, Player parent, PEXPermissible perm) {
        super(plugin, parent);
        this.perm = perm;
        this.cache = perm.getManager().getTransientSubjects(ATTACHMENT_TYPE);

        try {
            this.cache.getData(this.identifier, this);
        } catch (ExecutionException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private void updateData(ImmutableOptionSubjectData newData) {
        if (newData != this.subjectData) {
            this.cache.update(getIdentifier(), newData);
        }
    }

    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public void clearCache(ImmutableOptionSubjectData newData) {
        this.subjectData = newData;
    }

    @Override
    public Map<String, Boolean> getPermissions() {
        return Maps.transformValues(subjectData.getPermissions(PermissionsExPlugin.GLOBAL_CONTEXT), new Function<Integer, Boolean>() {
            @Nullable
            @Override
            public Boolean apply(Integer input) {
                return input > 0;
            }
        });
    }

    @Override
    public void setPermission(String name, boolean value) {
        updateData(subjectData.setPermission(PermissionsExPlugin.GLOBAL_CONTEXT, name, value ? 1 : -1));
    }

    @Override
    public void setPermission(Permission perm, boolean value) {
        setPermission(perm.getName(), value);
    }

    @Override
    public void unsetPermission(String name) {
        updateData(subjectData.setPermission(PermissionsExPlugin.GLOBAL_CONTEXT, name, 0));
    }

    @Override
    public void unsetPermission(Permission perm) {
        unsetPermission(perm.getName());
    }

    @Override
    public boolean remove() {
        return perm.removeAttachmentInternal(this);
    }
}
