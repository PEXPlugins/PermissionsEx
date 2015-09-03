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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.ContextInheritance;
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.util.Combinations;
import ninja.leaping.permissionsex.util.NodeTree;
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
import java.util.concurrent.ExecutionException;

import static java.util.Map.Entry;

/**
 * Handles baking of subject data inheritance tree and context tree into a single data set
 */
class InheritanceSubjectDataBaker implements SubjectDataBaker {
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

    private static Set<Set<Entry<String, String>>> processContexts(PermissionsEx pex, Set<Entry<String, String>> rawContexts) {
        ContextInheritance inheritance = pex.getContextInheritance(null);
        Queue<Entry<String, String>> inProgressContexts = new LinkedList<>(rawContexts);
        Set<Entry<String, String>> contexts = new HashSet<>();
        Entry<String, String> context;
        while ((context = inProgressContexts.poll()) != null) {
            if (contexts.add(context)) {
                inProgressContexts.addAll(inheritance.getParents(context));
            }
        }
        return ImmutableSet.copyOf(Combinations.of(contexts));
    }

    @Override
    public BakedSubjectData bake(CalculatedSubject data, Set<Entry<String, String>> activeContexts) throws ExecutionException {
        final Map.Entry<String, String> subject = data.getIdentifier();
        final BakeState state = new BakeState(data, processContexts(data.getManager(), activeContexts));

        final Set<Map.Entry<String, String>> visitedSubjects = new HashSet<>();
        visitSubject(state, subject, visitedSubjects, 0);
        Entry<String, String> defIdentifier = data.data().getCache().getDefaultIdentifier();
        if (!subject.equals(defIdentifier)) {
            visitSubject(state, defIdentifier, visitedSubjects, 1);
            visitSubject(state, Maps.immutableEntry(PermissionsEx.SUBJECTS_DEFAULTS, PermissionsEx.SUBJECTS_DEFAULTS), visitedSubjects, 2); // Force in global defaults
        }

        return new BakedSubjectData(NodeTree.of(state.combinedPermissions, state.defaultValue), ImmutableList.copyOf(state.parents), ImmutableMap.copyOf(state.options));
    }

    private void visitSubject(BakeState state, Map.Entry<String, String> subject, Set<Map.Entry<String, String>> visitedSubjects, int inheritanceLevel) throws ExecutionException {
        if (visitedSubjects.contains(subject)) {
            state.pex.getLogger().warn("Potential circular inheritance found while traversing inheritance for " + state.base.getIdentifier() + " when visiting " + subject);
            return;
        }
        visitedSubjects.add(subject);
        ImmutableSubjectData data = state.pex.getSubjects(subject.getKey()).getData(subject.getValue(), state.base), transientData = state.pex.getTransientSubjects(subject.getKey()).getData(subject.getValue(), state.base);
        for (Set<Entry<String, String>> combo : state.activeContexts) {
            visitSingle(state, transientData, combo, inheritanceLevel);
            for (Entry<String, String> parent : transientData.getParents(combo)) {
                visitSubject(state, parent, visitedSubjects, inheritanceLevel + 1);
            }
            visitSingle(state, data, combo, inheritanceLevel);
            for (Entry<String, String> parent : data.getParents(combo)) {
                visitSubject(state, parent, visitedSubjects, inheritanceLevel + 1);
            }
        }
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
