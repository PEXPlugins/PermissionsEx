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
package ninja.leaping.permissionsex.subject;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.DataSegment;
import ninja.leaping.permissionsex.data.SubjectRef;
import ninja.leaping.permissionsex.util.Combinations;
import ninja.leaping.permissionsex.util.NodeTree;
import ninja.leaping.permissionsex.util.Tristate;
import ninja.leaping.permissionsex.util.Util;
import ninja.leaping.permissionsex.util.WeightedImmutableSet;
import ninja.leaping.permissionsex.util.glob.GlobParseException;
import ninja.leaping.permissionsex.util.glob.Globs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.Map.Entry;
import static ninja.leaping.permissionsex.util.Translations.t;

/**
 * Handles baking of subject data inheritance tree and context tree into a single data set
 */
class InheritanceSubjectDataBaker implements SubjectDataBaker {
    private static final int CIRCULAR_INHERITANCE_THRESHOLD = 3;
    static final SubjectDataBaker INSTANCE = new InheritanceSubjectDataBaker();

    private InheritanceSubjectDataBaker() {
    }

    private static class BakeState {
        // Accumulators
        private final Map<String, Integer> combinedPermissions = new HashMap<>();
        private final List<SubjectRef> parents = new ArrayList<>();
        private final Map<String, String> options = new HashMap<>();
        private int defaultValue;

        // State objects
        private final CalculatedSubject base;
        private final PermissionsEx pex;
        private final Set<Set<Entry<String, String>>> activeContexts;

        private BakeState(CalculatedSubject base, Set<Set<Entry<String, String>>> activeContexts) {
            this.base = base;
            this.activeContexts = activeContexts;
            this.pex = base.getManager();
        }
    }

    private static CompletableFuture<Set<Set<Entry<String, String>>>> processContexts(PermissionsEx pex, Set<Entry<String, String>> rawContexts) {
        return pex.getContextInheritance(null).thenApply(inheritance -> {
            Queue<Entry<String, String>> inProgressContexts = new LinkedList<>(rawContexts);
            Set<Entry<String, String>> contexts = new HashSet<>();
            Entry<String, String> context;
            while ((context = inProgressContexts.poll()) != null) {
                if (contexts.add(context)) {
                    inProgressContexts.addAll(inheritance.getParents(context));
                }
            }
            return ImmutableSet.copyOf(Combinations.of(contexts));

        });
    }

    @Override
    public CompletableFuture<BakedSubjectData> bake(CalculatedSubject data, Set<Entry<String, String>> activeContexts) {
        final SubjectRef subject = data.getIdentifier();
        return processContexts(data.getManager(), activeContexts)
                .thenCompose(processedContexts -> {
                    final BakeState state = new BakeState(data, processedContexts);

                    final Multiset<SubjectRef> visitedSubjects = HashMultiset.create();
                    CompletableFuture<Void> ret = visitSubject(state, subject, visitedSubjects, 0);
                    SubjectRef defIdentifier = data.data().getCache().getDefaultIdentifier();
                    if (!subject.equals(defIdentifier)) {
                        visitSubject(state, defIdentifier, visitedSubjects, 1);
                        visitSubject(state, SubjectRef.of(PermissionsEx.SUBJECTS_DEFAULTS, PermissionsEx.SUBJECTS_DEFAULTS), visitedSubjects, 2); // Force in global defaults
                    }
                    return ret.thenApply(none -> state);

                }).thenApply(state -> new BakedSubjectData(NodeTree.of(state.combinedPermissions, state.defaultValue), ImmutableList.copyOf(state.parents), ImmutableMap.copyOf(state.options)));
    }

    private CompletableFuture<Void> visitSubject(BakeState state, SubjectRef subject, Multiset<SubjectRef> visitedSubjects, int inheritanceLevel) {
        if (visitedSubjects.count(subject) > CIRCULAR_INHERITANCE_THRESHOLD) {
            state.pex.getLogger().warn(t("Potential circular inheritance found while traversing inheritance for %s when visiting %s", state.base.getIdentifier(), subject));
            return Util.emptyFuture();
        }
        visitedSubjects.add(subject);
        SubjectType type = state.pex.getSubjects(subject.getType());
        return type.persistentData().getData(subject.getIdentifier(), state.base).thenCombine(type.transientData().getData(subject.getIdentifier(), state.base), (persistent, transientData) -> {
            CompletableFuture<Void> ret = Util.emptyFuture();
            for (Set<Entry<String, String>> combo : state.activeContexts) {
                WeightedImmutableSet<DataSegment> transientSegs = transientData.getAllSegments(combo, true);
                if (inheritanceLevel <= 1) {
                    transientSegs = transientSegs.withAll(transientData.getAllSegments(combo, false));
                }
                final WeightedImmutableSet<DataSegment> finalTransientSegs = transientSegs;
                ret = ret.thenRun(() -> visitSingle(state, finalTransientSegs));
                for (DataSegment seg : transientSegs) {
                    for (SubjectRef parent : seg.getParents()) {
                        ret = ret.thenCompose(none -> visitSubject(state, parent, visitedSubjects, inheritanceLevel + 1));
                    }
                }

                WeightedImmutableSet<DataSegment> persistentSegs = transientData.getAllSegments(combo, true);
                if (inheritanceLevel <= 1) {
                    persistentSegs = persistentSegs.withAll(transientData.getAllSegments(combo, false));
                }
                final WeightedImmutableSet<DataSegment> finalPersistentSegs = persistentSegs;
                ret = ret.thenRun(() -> visitSingle(state, finalPersistentSegs));
                for (DataSegment seg : persistentSegs) {
                    for (SubjectRef parent : seg.getParents()) {
                        ret = ret.thenCompose(none -> visitSubject(state, parent, visitedSubjects, inheritanceLevel + 1));
                    }
                }
            }
            return ret;
        }).thenCompose(res -> res);
    }

    private void putPermIfNecessary(BakeState state, String perm, int weight, Tristate status) {
        int val = status.asInt() * (Math.abs(weight) + 1);
        Integer existing = state.combinedPermissions.get(perm);
        if (existing == null || Math.abs(val) > Math.abs(existing)) {
            state.combinedPermissions.put(perm, val);
        }
    }

    private void visitSingle(BakeState state, WeightedImmutableSet<DataSegment> segments) {
        for (DataSegment data : segments.reverse()) {
            for (Map.Entry<String, Tristate> ent : data.getPermissions().entrySet()) {
                String perm = ent.getKey();
                try {
                    for (String matched : Globs.parse(perm)) {
                        putPermIfNecessary(state, matched, data.getWeight(), ent.getValue());
                    }
                } catch (GlobParseException e) { // If the permission is not a valid glob, assume it's a literal
                    putPermIfNecessary(state, perm, data.getWeight(), ent.getValue());
                }
            }

            state.parents.addAll(data.getParents());
            for (Map.Entry<String, String> ent : data.getOptions().entrySet()) {
                if (!state.options.containsKey(ent.getKey())) {
                    state.options.put(ent.getKey(), ent.getValue());
                }
            }

            int defaultVal = data.getPermissionDefault().asInt() * (Math.abs(data.getWeight()) + 1);

            if (Math.abs(defaultVal) > Math.abs(state.defaultValue)) {
                state.defaultValue = defaultVal;
            }
        }
    }
}
