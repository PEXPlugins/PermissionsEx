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

import ca.stellardrift.permissionsex.PermissionsEngine;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.impl.util.CachingValue;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.permission.SubjectReference;
import org.spongepowered.api.util.Tristate;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public class PEXSubject implements Subject {
    private final CalculatedSubject baked;
    private final PEXSubjectCollection<?> collection;
    private final PEXSubjectData data;
    private final PEXSubjectData transientData;
    private final SubjectReference ref;
    private final CachingValue<ActiveContextsHolder> activeContexts;

    static CompletableFuture<PEXSubject> load(final String identifier, final PEXSubjectCollection<?> collection) {
        return collection.getCalculatedSubject(identifier)
            .thenApply(baked -> new PEXSubject(baked, collection));
    }

    @SuppressWarnings("try")
    PEXSubject(final CalculatedSubject baked, final PEXSubjectCollection<?> collection) {
        this.baked = baked;
        this.collection = collection;
        this.data = new PEXSubjectData(baked.data(), this);
        this.transientData = new PEXSubjectData(baked.transientData(), this);

        this.ref = PEXSubjectReference.asSponge(baked.identifier(), this.collection.service());
        this.activeContexts = collection.service().tickBasedCachingValue(1L, () -> {
            try (final Timings.OnlyIfSyncTiming ignored = this.time().getActiveContexts.start()) {
                final Set<ContextValue<?>> pexContexts = baked.activeContexts();
                final Set<Context> spongeContexts = Contexts.toSponge(pexContexts);
                final Set<Context> spongeContextsAccum = new HashSet<>();
                for (final ContextCalculator<Subject> spongeCalc : this.collection.service().contextCalculators()) {
                    spongeCalc.accumulateContexts(this, spongeContextsAccum);
                }
                spongeContexts.addAll(spongeContextsAccum);
                pexContexts.addAll(Contexts.toPex(spongeContextsAccum, this.engine()));
                return new ActiveContextsHolder(Collections.unmodifiableSet(spongeContexts), PCollections.asSet(pexContexts));
            }
        });
    }

    Timings time() {
        return this.collection.service().timings();
    }

    PermissionsEngine engine() {
        return this.collection.service().manager();
    }

    Set<ContextValue<?>> activePexContexts() {
        return this.activeContexts.get().pexContexts;
    }

    @Override
    public PEXSubjectCollection<?> getContainingCollection() {
        return this.collection;
    }

    @Override
    public SubjectReference asSubjectReference() {
        return this.ref;
    }

    @Override
    public boolean isSubjectDataPersisted() {
        return true;
    }

    @Override
    public SubjectData getSubjectData() {
        return this.data;
    }

    @Override
    public SubjectData getTransientSubjectData() {
        return transientData;
    }

    @Override
    @SuppressWarnings("try")
    public boolean hasPermission(final String permission) {
        try (final Timings.OnlyIfSyncTiming ignored = this.time().getPermission.start()) {
            return this.baked.permission(this.activePexContexts(), permission) > 0;
        }
    }

    @Override
    @SuppressWarnings("try")
    public Tristate getPermissionValue(final Set<Context> contexts, final String permission) {
        requireNonNull(contexts, "contexts");
        requireNonNull(permission, "permission");

        try (final Timings.OnlyIfSyncTiming ignored = this.time().getPermission.start()) {
            final int ret = this.baked.permission(Contexts.toPex(contexts, this.engine()), permission);
            if (ret == 0) {
                return Tristate.UNDEFINED;
            } else if (ret > 0) {
                return Tristate.TRUE;
            } else {
                return Tristate.FALSE;
            }
        }
    }

    @Override
    public boolean isChildOf(final SubjectReference parent) {
        requireNonNull(parent, "parent");

        return this.baked.parents(this.activePexContexts())
            .contains(PEXSubjectReference.asPex(parent, this.collection.service()));
    }

    @Override
    public boolean isChildOf(final Set<Context> contexts, final SubjectReference parent) {
        requireNonNull(contexts, "contexts");
        requireNonNull(parent, "parent");

        return this.baked.parents(Contexts.toPex(contexts, this.engine()))
            .contains(PEXSubjectReference.asPex(parent, this.collection.service()));
    }

    @Override
    @SuppressWarnings("try")
    public List<SubjectReference> getParents() {
        try (final Timings.OnlyIfSyncTiming ignored = this.time().getParents.start()) {
            return PCollections.asVector(
                baked.parents(this.activePexContexts()),
                it -> PEXSubjectReference.asSponge(it, this.getContainingCollection().service())
            );
        }
    }

    @Override
    @SuppressWarnings("try")
    public List<SubjectReference> getParents(final Set<Context> contexts) {
        try (final Timings.OnlyIfSyncTiming ignored = this.time().getParents.start()) {
            return PCollections.asVector(
                baked.parents(Contexts.toPex(contexts, this.engine())),
                it -> PEXSubjectReference.asSponge(it, this.getContainingCollection().service())
            );
        }
    }

    @Override
    @SuppressWarnings("try")
    public Optional<String> getOption(final String key) {
        requireNonNull(key, "key");
        try (final Timings.OnlyIfSyncTiming ignored = this.time().getOption.start()) {
            return this.baked.option(this.activePexContexts(), key);
        }
    }

    @Override
    @SuppressWarnings("try")
    public Optional<String> getOption(final Set<Context> contexts, final String key) {
        try (final Timings.OnlyIfSyncTiming ignored = this.time().getOption.start()) {
            return baked.option(Contexts.toPex(contexts, this.engine()), key);
        }
    }

    @Override
    public String getIdentifier() {
        return this.ref.getSubjectIdentifier();
    }

    @Override
    public Optional<String> getFriendlyIdentifier() {
        return Optional.empty();
    }

    @Override
    public Set<Context> getActiveContexts() {
        return this.activeContexts.get().spongeContexts;
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PEXSubject)) {
            return false;
        }
        final PEXSubject that = (PEXSubject) other;
        return this.data.equals(that.data)
            && this.ref.equals(that.ref);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.data, this.ref);
    }

    static final class ActiveContextsHolder {
        final Set<Context> spongeContexts;
        final Set<ContextValue<?>> pexContexts;

        ActiveContextsHolder(
            final Set<Context> spongeContexts,
            final Set<ContextValue<?>> pexContexts
        ) {
            this.spongeContexts = spongeContexts;
            this.pexContexts = pexContexts;
        }
    }

}
