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
package ninja.leaping.permissionsex.sponge;

import com.google.common.base.Preconditions;
import ninja.leaping.permissionsex.util.Tristate;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of a permission description
 */
class PEXPermissionDescription implements PermissionDescription {
    private final PermissionsExPlugin plugin;
    private final String permId;
    private final Text description;
    private final PluginContainer owner;

    public PEXPermissionDescription(PermissionsExPlugin plugin, String permId, Text description, PluginContainer owner) {
        this.plugin = plugin;
        this.permId = permId;
        this.description = description;
        this.owner = owner;
    }

    @Override
    public String getId() {
        return this.permId;
    }

    @Override
    public Text getDescription() {
        return this.description;
    }

    @Override
    public Map<Subject, Boolean> getAssignedSubjects(String type) {
        return plugin.getSubjects(type).getAllWithPermission(getId());
    }

    @Override
    public PluginContainer getOwner() {
        return this.owner;
    }

    static class Builder implements PermissionDescription.Builder {
        private final PluginContainer owner;
        private final PermissionsExPlugin plugin;
        private String id;
        private Text description;
        private Map<String, Tristate> ranks = new HashMap<>();

        Builder(PluginContainer owner, PermissionsExPlugin plugin) {
            this.owner = owner;
            this.plugin = plugin;
        }

        @Override
        public Builder id(String id) {
            Preconditions.checkNotNull(id, "id");
            this.id = id;
            return this;
        }

        @Override
        public Builder description(Text text) {
            Preconditions.checkNotNull(text, "text");
            this.description = text;
            return this;
        }

        @Override
        public Builder assign(String s, boolean b) {
            return assign(s, b ? Tristate.TRUE : Tristate.FALSE);
        }

        public Builder assign(String rankTemplate, Tristate power) {
            ranks.put(rankTemplate, power);
            return this;
        }

        @Override
        public PEXPermissionDescription register() throws IllegalStateException {
            Preconditions.checkNotNull(id, "id");
            Preconditions.checkNotNull(description, "description");

            final PEXPermissionDescription ret = new PEXPermissionDescription(plugin, this.id, this.description, this.owner);
            this.plugin.registerDescription(ret, ranks);
            return ret;
        }
    }
}
