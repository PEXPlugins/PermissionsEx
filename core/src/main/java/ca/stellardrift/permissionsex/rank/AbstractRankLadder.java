/*
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
package ca.stellardrift.permissionsex.rank;

import com.google.common.collect.ImmutableList;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import static java.util.Map.Entry;

public abstract class AbstractRankLadder implements RankLadder {
    private final String name;

    protected AbstractRankLadder(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public final ImmutableSubjectData promote(Set<ContextValue<?>> contexts, ImmutableSubjectData input) {
        if (ranks().isEmpty()) {
            return input;
        }

        List<Entry<String, String>> parents = input.getParents(contexts);
        if (parents.isEmpty()) {
            return input.addParent(contexts, ranks().get(0).getKey(), ranks().get(0).getValue());
        } else {
            int index;
            parents = new ArrayList<>(parents);
            boolean found = false;
            for (ListIterator<Entry<String, String>> it = parents.listIterator(); it.hasNext();) {
                Entry<String, String> parent = it.next();
                if ((index = ranks().indexOf(parent)) > -1) {
                    if (index == ranks().size() - 1) {
                        return input;
                    } else {
                        it.set(ranks().get(index + 1));
                        found = true;
                    }
                }
            }
            if (found) {
                return input.setParents(contexts, parents); // Promotion happened
            } else {
                return input.addParent(contexts, ranks().get(0).getKey(), ranks().get(0).getValue());
            }
        }
    }

    @Override
    public final ImmutableSubjectData demote(Set<ContextValue<?>> contexts, ImmutableSubjectData input) {
        if (ranks().isEmpty()) {
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
                if ((index = ranks().indexOf(parent)) > -1) {
                    if (index == 0) {
                        // At bottom of rank ladder, remove the rank entirely
                        it.remove();
                    } else {
                        it.set(ranks().get(index - 1));
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
    public final boolean isOnLadder(Set<ContextValue<?>> contexts, ImmutableSubjectData subject) {
        if (ranks().isEmpty()) {
            return false;
        }

        for (Entry<String, String> par : subject.getParents(contexts)) {
            if (ranks().indexOf(par) != -1) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final int indexOf(Entry<String, String> subject) {
        return ranks().indexOf(subject);
    }

    @Override
    public final RankLadder with(Entry<String, String> subject) {
        int indexOf = ranks().indexOf(subject);
        if (indexOf != -1) {
            List<Entry<String, String>> ents = new ArrayList<>(this.ranks());
            ents.remove(indexOf);
            ents.add(subject);
            return newWithRanks(ents);
        } else {
            return newWithRanks(ImmutableList.<Entry<String, String>>builder().addAll(this.ranks()).add(subject).build());
        }

    }


    @Override
    public final RankLadder with(Entry<String, String> subject, int index) {
        if (index > ranks().size() || index < 0) {
            return this;
        }

        int indexOf = ranks().indexOf(subject);
        final List<Entry<String, String>> newEnts = new ArrayList<>(ranks());
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
    public final RankLadder without(Entry<String, String> subject) {
        int indexOf = ranks().indexOf(subject);
        if (indexOf == -1) {
            return this;
        } else {
            List<Entry<String, String>> newRanks = new ArrayList<>(ranks());
            newRanks.remove(indexOf);
            return newWithRanks(newRanks);
        }
    }

    @Override
    public abstract List<? extends Entry<String, String>> ranks();

    protected abstract RankLadder newWithRanks(List<Entry<String,String>> ents);

    @Override
    public final Component asComponent() {
        return Component.text(build -> build.content(name())
                .decoration(TextDecoration.BOLD, true)
                .hoverEvent(HoverEvent.showText(Messages.FORMATTER_BUTTON_INFO_PROMPT.tr()))
                .clickEvent(ClickEvent.runCommand("/pex rank " + name())));
    }
}
