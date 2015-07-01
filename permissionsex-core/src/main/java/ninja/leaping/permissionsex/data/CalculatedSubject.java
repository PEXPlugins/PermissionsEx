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

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.util.NodeTree;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * This is a holder that maintains the current subject data state
 */
public class CalculatedSubject implements Caching<ImmutableOptionSubjectData> {
    private final Map.Entry<String, String> identifier;
    private final PermissionsEx pex;

    private final LoadingCache<Set<Map.Entry<String, String>>, BakedSubjectData> data = CacheBuilder.newBuilder().maximumSize(5)
            .build(new CacheLoader<Set<Map.Entry<String, String>>, BakedSubjectData>() {
        @Override
        public BakedSubjectData load(Set<Map.Entry<String, String>> contexts) throws Exception {
            return SubjectDataBaker.bake(CalculatedSubject.this, contexts);
        }
    });

    public CalculatedSubject(Map.Entry<String, String> identifier, PermissionsEx pex) {
        Preconditions.checkNotNull(identifier, pex);
        this.identifier = identifier;
        this.pex = pex;
    }

    public Map.Entry<String, String> getIdentifier() {
        return identifier;
    }

    PermissionsEx getManager() {
        return pex;
    }

    public NodeTree getPermissions(Set<Map.Entry<String, String>> contexts) {
        Preconditions.checkNotNull(contexts, "contexts");
        try {
            return data.get(contexts).getPermissions();
        } catch (ExecutionException e) {
            return NodeTree.of(Collections.<String, Integer>emptyMap());
        }
    }

    public Map<String, String> getOptions(Set<Map.Entry<String, String>> contexts) {
        Preconditions.checkNotNull(contexts, "contexts");
        try {
            return data.get(contexts).getOptions();
        } catch (ExecutionException e) {
            return ImmutableMap.of();
        }
    }

    public List<Map.Entry<String, String>> getParents(Set<Map.Entry<String, String>> contexts) {
        Preconditions.checkNotNull(contexts, "contexts");
        try {
            return data.get(contexts).getParents();
        } catch (ExecutionException e) {
            return ImmutableList.of();
        }
    }

    public Set<Set<Map.Entry<String, String>>> getActiveContexts() {
        return data.asMap().keySet();
    }

    @Override
    public void clearCache(ImmutableOptionSubjectData newData) {
        data.invalidateAll();
        for (CalculatedSubject subject : pex.getActiveCalculatedSubjects()) {
            for (Set<Map.Entry<String, String>> ent : subject.getActiveContexts()) {
                if (subject.getParents(ent).contains(this.identifier)) {
                    subject.data.invalidateAll();
                    break;
                }
            }
        }
    }
}
