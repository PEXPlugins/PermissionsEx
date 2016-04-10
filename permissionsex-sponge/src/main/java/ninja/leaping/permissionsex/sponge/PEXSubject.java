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
import com.google.common.collect.Lists;
import ninja.leaping.permissionsex.subject.CalculatedSubject;
import ninja.leaping.permissionsex.exception.PermissionsLoadingException;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.option.OptionSubject;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static ninja.leaping.permissionsex.sponge.PEXOptionSubjectData.parSet;

/**
 * Permissions subject implementation
 */
@NonnullByDefault
class PEXSubject implements OptionSubject {
    private final PEXSubjectCollection collection;
    private final PEXOptionSubjectData data;
    private final PEXOptionSubjectData transientData;
    private volatile CalculatedSubject baked;
    private final String identifier;

    public PEXSubject(String identifier, PEXSubjectCollection collection) throws ExecutionException, PermissionsLoadingException {
        this.identifier = identifier;
        this.collection = collection;
        this.baked = collection.getCalculatedSubject(identifier);
        this.data = new PEXOptionSubjectData(baked.data(), identifier, collection.getPlugin());
        this.transientData = new PEXOptionSubjectData(baked.transientData(), identifier, collection.getPlugin());
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    private String identifyUser() {
        final Optional<CommandSource> source = getCommandSource();
        return getIdentifier() + (source.isPresent() ? "/" + source.get().getName() : "");
    }

    public CalculatedSubject getBaked() {
        return this.baked;
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
    public PEXOptionSubjectData getSubjectData() {
        return data;
    }

    @Override
    public PEXOptionSubjectData getTransientSubjectData() {
        return transientData;
    }

    @Override
    public Optional<String> getOption(Set<Context> contexts, String key) {
        Preconditions.checkNotNull(contexts, "contexts");
        Preconditions.checkNotNull(key, "key");
        return baked.getOption(parSet(contexts), key);
    }

    @Override
    public Optional<String> getOption(String key) {
        return getOption(getActiveContexts(), key);
    }

    @Override
    public boolean hasPermission(Set<Context> contexts, String permission) {
        return getPermissionValue(contexts, permission).asBoolean();
    }

    @Override
    public boolean hasPermission(String permission) {
        return hasPermission(getActiveContexts(), permission);
    }

    @Override
    public Tristate getPermissionValue(Set<Context> contexts, String permission) {
        Preconditions.checkNotNull(contexts, "contexts");
        Preconditions.checkNotNull(permission, "permission");
        int ret = baked.getPermission(parSet(contexts), permission);
        return ret == 0 ? Tristate.UNDEFINED : ret > 0 ? Tristate.TRUE : Tristate.FALSE;
    }


    @Override
    public boolean isChildOf(Subject parent) {
        return isChildOf(getActiveContexts(), parent);
    }

    @Override
    public boolean isChildOf(Set<Context> contexts, Subject parent) {
        Preconditions.checkNotNull(contexts, "contexts");
        Preconditions.checkNotNull(parent, "parent");
        return getParents(contexts).contains(parent);
    }

    @Override
    public Set<Context> getActiveContexts() {
        Set<Context> set = new HashSet<>();
        for (ContextCalculator<Subject> calc : this.collection.getPlugin().getContextCalculators()) {
            calc.accumulateContexts(this, set);
        }
        return Collections.unmodifiableSet(set);
    }

    @Override
    public List<Subject> getParents() {
        return getParents(getActiveContexts());
    }

    @Override
    public List<Subject> getParents(final Set<Context> contexts) {
        Preconditions.checkNotNull(contexts, "contexts");
        return Lists.transform(baked.getParents(parSet(contexts)), input -> collection.getPlugin().getSubjects(input.getKey()).get(input.getValue()));
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
