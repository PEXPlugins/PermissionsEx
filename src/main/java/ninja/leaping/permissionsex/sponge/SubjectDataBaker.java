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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.Combinations;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import org.spongepowered.api.service.permission.NodeTree;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles baking of subject data inheritance tree and context tree into a single data set
 */
public class SubjectDataBaker {
    private final PEXSubject start;
    private final Set<Context> activeContexts;

    SubjectDataBaker(PEXSubject start, Set<Context> activeContexts) {
        this.start = start;
        this.activeContexts = ImmutableSet.copyOf(activeContexts);
    }

    public BakedSubjectData bake() {
        Map<String, Integer> combinedPermissions = new HashMap<>();
        List<Subject> parents = new ArrayList<>();
        Map<String, String> options = new HashMap<>();
        final Combinations<Context> combos = Combinations.of(activeContexts);
        visitSubject(start, combos, combinedPermissions, parents, options);
        /*for (Set<Context> combo : combos) { // TODO: Bring transient data into the system
            visitSingle(start.getContainingCollection().getPlugin().getDefaultData(), combo, combinedPermissions, parents, options);
        }*/
        return new BakedSubjectData(activeContexts, NodeTree.of(Maps.transformValues(combinedPermissions, new Function<Integer, Boolean>() {
            @Nullable
            @Override
            public Boolean apply(Integer input) {
                return input > 0;
            }
        })), parents, options);
    }

    private void visitSubject(PEXSubject subject, Combinations<Context> contexts, Map<String, Integer> combinedPermissions, List<Subject> parents, Map<String, String> options) {
        for (Set<Context> combo : contexts) {
            /* visitSingle(subject.getTransientData(), combo, combinedPermissions, parents, options); // TODO: Bring transient data into the system
            for (Subject parent : subject.getTransientData().getParents(combo)) {
                visitSubject((PEXSubject) parent, contexts, combinedPermissions, parents, options);
            }*/
            visitSingle(subject.getData(), combo, combinedPermissions, parents, options);
            for (Subject parent : subject.getData().getParents(combo)) {
                visitSubject((PEXSubject) parent, contexts, combinedPermissions, parents, options);
            }
        }
    }

    private void visitSingle(PEXOptionSubjectData data, Set<Context> specificCombination, Map<String, Integer> combinedPermissions, List<Subject> parents, Map<String, String> options) {
        ImmutableOptionSubjectData current = data.getCurrent();
        for (Map.Entry<String, Integer> ent : current.getPermissions(specificCombination).entrySet()) {
            Integer existing = combinedPermissions.get(ent.getKey());
            if (existing == null || Math.abs(ent.getValue()) > Math.abs(existing)) {
                combinedPermissions.put(ent.getKey(), ent.getValue());
            }
        }
        parents.addAll(data.getParents(specificCombination));
        for (Map.Entry<String, String> ent : current.getOptions(specificCombination).entrySet()) {
            if (!options.containsKey(ent.getKey())) {
                options.put(ent.getKey(), ent.getValue());
            }
        }
    }
}
