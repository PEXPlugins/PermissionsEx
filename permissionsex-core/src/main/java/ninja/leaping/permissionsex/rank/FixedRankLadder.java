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
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import static java.util.Map.Entry;

public class FixedRankLadder implements RankLadder {
    private final String name;
    private final List<Entry<String, String>> ranks;

    public FixedRankLadder(String name, List<Entry<String, String>> ranks) {
        this.name = name;
        this.ranks = ImmutableList.copyOf(ranks);
    }

    @Override
    public String getName() {
        return this.name;

    }

    @Override
    public ImmutableOptionSubjectData promote(Set<Entry<String, String>> contexts, ImmutableOptionSubjectData input) {
        if (this.ranks.isEmpty()) {
            return input;
        }

        List<Entry<String, String>> parents = input.getParents(contexts);
        if (parents.isEmpty()) {
            return input.addParent(contexts, ranks.get(0).getKey(), ranks.get(0).getValue());
        } else {
            int index;
            parents = new ArrayList<>(parents);
            boolean found = false;
            for (ListIterator<Entry<String, String>> it = parents.listIterator(); it.hasNext();) {
                Entry<String, String> parent = it.next();
                if ((index = ranks.indexOf(parent)) > -1) {
                    if (index == ranks.size() - 1) {
                        return input;
                    } else {
                        it.set(ranks.get(index + 1));
                        found = true;
                    }
                }
            }
            if (found) {
                return input.setParents(contexts, parents); // Promotion happened
            } else {
                return input.addParent(contexts, ranks.get(0).getKey(), ranks.get(0).getValue());
            }
        }
    }

    @Override
    public ImmutableOptionSubjectData demote(Set<Entry<String, String>> contexts, ImmutableOptionSubjectData input) {
        if (this.ranks.isEmpty()) {
            return input;
        }

        List<Entry<String, String>> parents = input.getParents(contexts);
        if (parents.isEmpty()) {
            return input;
        } else {
            int index;
            parents = new ArrayList<>(parents);
            boolean found = false;
            for (ListIterator<Entry<String, String>> it = parents.listIterator(); it.hasNext();) {
                Entry<String, String> parent = it.next();
                if ((index = ranks.indexOf(parent)) > -1) {
                    if (index == 0) {
                        // At bottom of rank ladder, remove the rank entirely
                        it.remove();
                    } else {
                        it.set(ranks.get(index - 1));
                    }
                    found = true;
                }
            }
            if (found) {
                return input.setParents(contexts, parents);
            } else {
                return input;
            }
        }
    }

    @Override
    public boolean isOnLadder(Set<Entry<String, String>> contexts, ImmutableOptionSubjectData subject) {
        if (this.ranks.isEmpty()) {
            return false;
        }

        for (Entry<String, String> par : subject.getParents(contexts)) {
            if (this.ranks.indexOf(par) != -1) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int indexOfRank(Entry<String, String> subject) {
        return this.ranks.indexOf(subject);
    }

    @Override
    public RankLadder addRank(Entry<String, String> subject) {
        int indexOf = this.ranks.indexOf(subject);
        if (indexOf != -1) {
            List<Entry<String, String>> ents = new ArrayList<>(this.ranks);
            ents.remove(indexOf);
            ents.add(subject);
            return new FixedRankLadder(this.name, ents);
        } else {
            return new FixedRankLadder(this.name, ImmutableList.<Entry<String, String>>builder().addAll(this.ranks).add(subject).build());
        }

    }

    @Override
    public RankLadder addRankAt(Entry<String, String> subject, int index) {
        if (index > this.ranks.size() || index < 0) {
            return this;
        }

        int indexOf = this.ranks.indexOf(subject);
        final List<Entry<String, String>> newEnts = new ArrayList<>(this.ranks);
        newEnts.add(index, subject);
        if (indexOf != -1) {
            if (indexOf >= index) {
                ++indexOf;
            }
            newEnts.remove(indexOf);
        }
        return new FixedRankLadder(this.name, newEnts);
    }

    @Override
    public RankLadder removeRank(Entry<String, String> subject) {
        int indexOf = this.ranks.indexOf(subject);
        if (indexOf == -1) {
            return this;
        } else {
            List<Entry<String, String>> newRanks = new ArrayList<>(this.ranks);
            newRanks.remove(indexOf);
            return new FixedRankLadder(this.name, newRanks);
        }
    }

    @Override
    public List<? extends Entry<String, String>> getRanks() {
        return this.ranks;
    }
}
