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
package ninja.leaping.permissionsex.rank;

import com.google.common.collect.ImmutableList;
import java.util.List;

import static java.util.Map.Entry;

public class FixedRankLadder extends AbstractRankLadder {
    private final List<Entry<String, String>> ranks;

    public FixedRankLadder(String name, List<Entry<String, String>> ranks) {
        super(name);
        this.ranks = ImmutableList.copyOf(ranks);
    }

    @Override
    public List<? extends Entry<String, String>> getRanks() {
        return this.ranks;
    }

    @Override
    protected RankLadder newWithRanks(List<Entry<String, String>> ents) {
        return new FixedRankLadder(getName(), ents);
    }
}
