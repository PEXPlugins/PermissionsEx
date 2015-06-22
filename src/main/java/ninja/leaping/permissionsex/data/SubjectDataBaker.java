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
package ninja.leaping.permissionsex.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.util.Combinations;
import ninja.leaping.permissionsex.util.NodeTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static java.util.Map.Entry;

/**
 * Handles baking of subject data inheritance tree and context tree into a single data set
 */
class SubjectDataBaker {
    private final Entry<String, String> subject;
    private final PermissionsEx pex;
    private final Set<Entry<String, String>> activeContexts;
    private final Caching<ImmutableOptionSubjectData> updateListener;

    private final Map<String, Integer> combinedPermissions = new HashMap<>();
    private final List<Entry<String, String>> parents = new ArrayList<>();
    private final Map<String, String> options = new HashMap<>();
    private int defaultValue;

    private SubjectDataBaker(Caching<ImmutableOptionSubjectData> updateListener, Map.Entry<String, String> subject, PermissionsEx pex, Set<Entry<String, String>> activeContexts) {
        this.updateListener = updateListener;
        this.subject = subject;
        this.pex = pex;
        this.activeContexts = ImmutableSet.copyOf(activeContexts);
    }

    public static BakedSubjectData bake(CalculatedSubject data, Set<Entry<String, String>> activeContexts) throws ExecutionException {
        return new SubjectDataBaker(data, data.getIdentifier(), data.getManager(), activeContexts).bake();
    }

    public BakedSubjectData bake() throws ExecutionException {
        final Combinations<Entry<String, String>> combos = Combinations.of(activeContexts);
        final Set<Map.Entry<String, String>> visitedSubjects = new HashSet<>();
        visitSubject(subject, combos, visitedSubjects, 0);
        if (!subject.equals(pex.getDefaultIdentifier())) {
            visitSubject(pex.getDefaultIdentifier(), combos, visitedSubjects, 1);
        }

        return new BakedSubjectData(activeContexts, NodeTree.of(combinedPermissions, defaultValue), ImmutableList.copyOf(parents), ImmutableMap.copyOf(options));
    }

    private void visitSubject(Map.Entry<String, String> subject, Combinations<Entry<String, String>> contexts, Set<Map.Entry<String, String>> visitedSubjects, int inheritanceLevel) throws ExecutionException {
        if (visitedSubjects.contains(subject)) {
            pex.getLogger().warn("Potential circular inheritance found while traversing inheritance for " + this.subject + " when visiting " + subject);
            return;
        }
        visitedSubjects.add(subject);
        ImmutableOptionSubjectData data = pex.getSubjects(subject.getKey()).getData(subject.getValue(), updateListener), transientData = pex.getTransientSubjects(subject.getKey()).getData(subject.getValue(), updateListener);
        for (Set<Entry<String, String>> combo : contexts) {
            visitSingle(transientData, combo, inheritanceLevel);
            for (Entry<String, String> parent : transientData.getParents(combo)) {
                visitSubject(parent, contexts, visitedSubjects, inheritanceLevel + 1);
            }
            visitSingle(data, combo, inheritanceLevel);
            for (Entry<String, String> parent : data.getParents(combo)) {
                visitSubject(parent, contexts, visitedSubjects, inheritanceLevel + 1);
            }
        }
    }

    private void visitSingle(ImmutableOptionSubjectData data, Set<Entry<String, String>> specificCombination, int inheritanceLevel) {
        for (Map.Entry<String, Integer> ent : data.getPermissions(specificCombination).entrySet()) {
            String perm = ent.getKey();
            if (ent.getKey().startsWith("#")) { // Prefix to exclude from inheritance
                if (inheritanceLevel > 1) {
                    continue;
                }
                perm = perm.substring(1);
            }

            Integer existing = combinedPermissions.get(ent.getKey());
            if (existing == null || Math.abs(ent.getValue()) > Math.abs(existing)) {
                combinedPermissions.put(perm, ent.getValue());
            }
        }
        parents.addAll(data.getParents(specificCombination));
        for (Map.Entry<String, String> ent : data.getOptions(specificCombination).entrySet()) {
            if (!options.containsKey(ent.getKey())) {
                options.put(ent.getKey(), ent.getValue());
            }
        }
        if (Math.abs(data.getDefaultValue(specificCombination)) > Math.abs(defaultValue)) {
            defaultValue = data.getDefaultValue(specificCombination);
        }
    }
}
