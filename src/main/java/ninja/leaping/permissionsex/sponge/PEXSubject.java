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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import ninja.leaping.permissionsex.data.Caching;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import org.spongepowered.api.service.permission.MemorySubjectData;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.service.permission.context.ContextCalculator;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.command.CommandSource;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Permissions subject implementation
 */
public class PEXSubject implements Subject, Caching {
    private final PEXSubjectCollection collection;
    private final PEXOptionSubjectData data;
    private final SubjectData transientData;
    private final String identifier;

    public PEXSubject(String identifier, PEXOptionSubjectData data, PEXSubjectCollection collection) {
        this.identifier = identifier;
        this.data = data;
        this.collection = collection;
        this.transientData = new MemorySubjectData(collection.getPlugin());
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        return getContainingCollection().getCommandSource(this.identifier);
    }

    @Override
    public PEXSubjectCollection getContainingCollection() {
        return this.collection;
    }

    @Override
    public SubjectData getData() {
        return data;
    }

    @Override
    public SubjectData getTransientData() {
        return transientData;
    }

    @Override
    public boolean hasPermission(Set<Context> contexts, String permission) {
        return getPermissionValue(contexts, permission).asBoolean();
    }

    @Override
    public boolean hasPermission(String permission) {
        return hasPermission(getActiveContexts(), permission);
    }

    private <K, V> Map<K, V> emptyOrNull(Map<K, V> test) {
        return test == null ? Collections.<K, V>emptyMap() : test;
    }

    @Override
    public Tristate getPermissionValue(Set<Context> contexts, String permission) {
        Integer value = emptyOrNull(data.getCurrent().getPermissions(contexts)).get(permission);
        if (value == null) {
             value = emptyOrNull(data.getCurrent().getPermissions(SubjectData.GLOBAL_CONTEXT)).get(permission);
        }
        if (value == null) {
            value = data.getCurrent().getDefaultValue(contexts);
        }
        if (value == 0) {
            value = data.getCurrent().getDefaultValue(SubjectData.GLOBAL_CONTEXT);
        }

        if (value < 0) {
            return Tristate.FALSE;
        } else if (value == 0) {
            return Tristate.UNDEFINED;
        } else {
            return Tristate.TRUE;
        }
    }

    @Override
    public boolean isChildOf(Subject parent) {
        return false;
    }

    @Override
    public boolean isChildOf(Set<Context> contexts, Subject parent) {
        return false;
    }

    @Override
    public Set<Context> getActiveContexts() {
        Set<Context> set = new HashSet<>();
        for (ContextCalculator calc : this.collection.getPlugin().getContextCalculators()) {
            calc.accumulateContexts(this, set);
        }
        return ImmutableSet.copyOf(set);
    }

    @Override
    public List<Subject> getParents() {
        return Collections.emptyList();
    }

    @Override
    public List<Subject> getParents(Set<Context> contexts) {
        return null;
    }


    @Override
    public void clearCache(ImmutableOptionSubjectData newData) {

    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PEXSubject)) {
            return false;
        }

        PEXSubject otherSubj = (PEXSubject) other;

        return this.identifier.equals(otherSubj.identifier)
                && this.data.equals(otherSubj.data);
    }
}
