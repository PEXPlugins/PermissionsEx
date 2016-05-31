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
import ninja.leaping.permissionsex.data.ImmutableSubjectData;
import ninja.leaping.permissionsex.data.SubjectRef;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import static java.util.Map.Entry;

public abstract class AbstractRankLadder implements RankLadder {
    private final String name;

    public AbstractRankLadder(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public final ImmutableSubjectData promote(Set<Entry<String, String>> contexts, ImmutableSubjectData input) {
        if (getRanks().isEmpty()) {
            return input;
        }

        List<SubjectRef> parents = input.getParents(contexts);
        if (parents.isEmpty()) {
            return input.addParent(contexts, getRanks().get(0));
        } else {
            int index;
            parents = new ArrayList<>(parents);
            boolean found = false;
            for (ListIterator<SubjectRef> it = parents.listIterator(); it.hasNext();) {
                SubjectRef parent = it.next();
                if ((index = getRanks().indexOf(parent)) > -1) {
                    if (index == getRanks().size() - 1) {
                        return input;
                    } else {
                        it.set(getRanks().get(index + 1));
                        found = true;
                    }
                }
            }
            if (found) {
                return input.setParents(contexts, parents); // Promotion happened
            } else {
                return input.addParent(contexts, getRanks().get(0));
            }
        }
    }

    @Override
    public final ImmutableSubjectData demote(Set<Entry<String, String>> contexts, ImmutableSubjectData input) {
        if (getRanks().isEmpty()) {
            return input;
        }

        List<SubjectRef> parents = input.getParents(contexts);
        if (parents.isEmpty()) {
            return input;
        } else {
            int index;
            parents = new ArrayList<>(parents);
            boolean found = false;
            for (ListIterator<SubjectRef> it = parents.listIterator(); it.hasNext();) {
                SubjectRef parent = it.next();
                if ((index = getRanks().indexOf(parent)) > -1) {
                    if (index == 0) {
                        // At bottom of rank ladder, remove the rank entirely
                        it.remove();
                    } else {
                        it.set(getRanks().get(index - 1));
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
    public final boolean isOnLadder(Set<Entry<String, String>> contexts, ImmutableSubjectData subject) {
        if (getRanks().isEmpty()) {
            return false;
        }

        for (SubjectRef par : subject.getParents(contexts)) {
            if (getRanks().indexOf(par) != -1) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final int indexOfRank(SubjectRef subject) {
        return getRanks().indexOf(subject);
    }

    @Override
    public final RankLadder addRank(SubjectRef subject) {
        int indexOf = getRanks().indexOf(subject);
        if (indexOf != -1) {
            List<SubjectRef> ents = new ArrayList<>(this.getRanks());
            ents.remove(indexOf);
            ents.add(subject);
            return newWithRanks(ents);
        } else {
            return newWithRanks(ImmutableList.<SubjectRef>builder().addAll(this.getRanks()).add(subject).build());
        }

    }


    @Override
    public final RankLadder addRankAt(SubjectRef subject, int index) {
        if (index > getRanks().size() || index < 0) {
            return this;
        }

        int indexOf = getRanks().indexOf(subject);
        final List<SubjectRef> newEnts = new ArrayList<>(getRanks());
        newEnts.add(index, subject);
        if (indexOf != -1) {
            if (indexOf >= index) {
                ++indexOf;
            }
            newEnts.remove(indexOf);
        }
        return newWithRanks(newEnts);
    }

    @Override
    public final RankLadder removeRank(SubjectRef subject) {
        int indexOf = getRanks().indexOf(subject);
        if (indexOf == -1) {
            return this;
        } else {
            List<SubjectRef> newRanks = new ArrayList<>(getRanks());
            newRanks.remove(indexOf);
            return newWithRanks(newRanks);
        }
    }

    @Override
    public abstract List<? extends SubjectRef> getRanks();

    protected abstract RankLadder newWithRanks(List<SubjectRef> ents);
}
