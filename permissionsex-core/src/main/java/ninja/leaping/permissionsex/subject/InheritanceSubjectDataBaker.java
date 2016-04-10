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
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.util.Combinations;
import ninja.leaping.permissionsex.util.NodeTree;
import ninja.leaping.permissionsex.util.Util;
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
        private final List<Entry<String, String>> parents = new ArrayList<>();
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
        final Map.Entry<String, String> subject = data.getIdentifier();
        return processContexts(data.getManager(), activeContexts)
                .thenCompose(processedContexts -> {
                    final BakeState state = new BakeState(data, processedContexts);

                    final Multiset<Entry<String, String>> visitedSubjects = HashMultiset.create();
                    CompletableFuture<Void> ret = visitSubject(state, subject, visitedSubjects, 0);
                    Entry<String, String> defIdentifier = data.data().getCache().getDefaultIdentifier();
                    if (!subject.equals(defIdentifier)) {
                        visitSubject(state, defIdentifier, visitedSubjects, 1);
                        visitSubject(state, Maps.immutableEntry(PermissionsEx.SUBJECTS_DEFAULTS, PermissionsEx.SUBJECTS_DEFAULTS), visitedSubjects, 2); // Force in global defaults
                    }
                    return ret.thenApply(none -> state);

                }).thenApply(state -> new BakedSubjectData(NodeTree.of(state.combinedPermissions, state.defaultValue), ImmutableList.copyOf(state.parents), ImmutableMap.copyOf(state.options)));
    }

    private CompletableFuture<Void> visitSubject(BakeState state, Map.Entry<String, String> subject, Multiset<Entry<String, String>> visitedSubjects, int inheritanceLevel) {
        if (visitedSubjects.count(subject) > CIRCULAR_INHERITANCE_THRESHOLD) {
            state.pex.getLogger().warn(t("Potential circular inheritance found while traversing inheritance for %s when visiting %s", state.base.getIdentifier(), subject));
            return Util.emptyFuture();
        }
        visitedSubjects.add(subject);
        SubjectType type = state.pex.getSubjects(subject.getKey());
        return type.persistentData().getData(subject.getValue(), state.base).thenCombine(type.transientData().getData(subject.getValue(), state.base), (persistent, transientData) -> {
            CompletableFuture<Void> ret = Util.emptyFuture();
            for (Set<Entry<String, String>> combo : state.activeContexts) {
                ret = ret.thenRun(() -> visitSingle(state, transientData, combo, inheritanceLevel));
                for (Entry<String, String> parent : transientData.getParents(combo)) {
                    ret = ret.thenCompose(none -> visitSubject(state, parent, visitedSubjects, inheritanceLevel + 1));
                }
                ret = ret.thenRun(() -> visitSingle(state, persistent, combo, inheritanceLevel));
                for (Entry<String, String> parent : persistent.getParents(combo)) {
                    ret = ret.thenCompose(none -> visitSubject(state, parent, visitedSubjects, inheritanceLevel + 1));
                }
            }
            return ret;
        }).thenCompose(res -> res);
    }

    private void putPermIfNecessary(BakeState state, String perm, int val) {
        Integer existing = state.combinedPermissions.get(perm);
        if (existing == null || Math.abs(val) > Math.abs(existing)) {
            state.combinedPermissions.put(perm, val);
        }
    }

    private void visitSingle(BakeState state, ImmutableSubjectData data, Set<Entry<String, String>> specificCombination, int inheritanceLevel) {
        for (Map.Entry<String, Integer> ent : data.getPermissions(specificCombination).entrySet()) {
            String perm = ent.getKey();
            if (ent.getKey().startsWith("#")) { // Prefix to exclude from inheritance
                if (inheritanceLevel > 1) {
                    continue;
                }
                perm = perm.substring(1);
            }

            try {
                for (String matched : Globs.parse(perm)) {
                    putPermIfNecessary(state, matched, ent.getValue());
                }
            } catch (GlobParseException e) { // If the permission is not a valid glob, assume it's a literal
                putPermIfNecessary(state, perm, ent.getValue());
            }
        }

        state.parents.addAll(data.getParents(specificCombination));
        for (Map.Entry<String, String> ent : data.getOptions(specificCombination).entrySet()) {
            if (!state.options.containsKey(ent.getKey())) {
                state.options.put(ent.getKey(), ent.getValue());
            }
        }

        if (Math.abs(data.getDefaultValue(specificCombination)) > Math.abs(state.defaultValue)) {
            state.defaultValue = data.getDefaultValue(specificCombination);
        }
    }
}
