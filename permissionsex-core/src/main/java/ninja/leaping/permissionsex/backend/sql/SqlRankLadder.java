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
package ninja.leaping.permissionsex.backend.sql;

import ninja.leaping.permissionsex.rank.AbstractRankLadder;
import ninja.leaping.permissionsex.rank.RankLadder;
import ninja.leaping.permissionsex.util.GuavaCollectors;

import java.util.List;
import java.util.Map;

public class SqlRankLadder extends AbstractRankLadder {
    private final List<SubjectRef> entries;

    public SqlRankLadder(String name, List<SubjectRef> entries) {
        super(name);
        this.entries = entries;
    }

    @Override
    public List<SubjectRef> getRanks() {
        return entries;
    }

    @Override
    protected RankLadder newWithRanks(List<Map.Entry<String, String>> ents) {
        return new SqlRankLadder(getName(), ents.stream()
                .map(ent -> ent instanceof SubjectRef ? (SubjectRef) ent : SubjectRef.unresolved(ent.getKey(), ent.getValue()))
                .collect(GuavaCollectors.toImmutableList()));
    }
}
