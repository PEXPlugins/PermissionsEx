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
package ca.stellardrift.permissionsex.impl.rank;

import ca.stellardrift.permissionsex.rank.RankLadder;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.subject.ImmutableSubjectData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.pcollections.PVector;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

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

        return input.withSegment(contexts, seg -> {
            final List<? extends SubjectRef<?>> originalParents = seg.parents();
            if (originalParents.isEmpty()) {
                return seg.plusParent(ranks().get(0));
            } else {
                int index;
                final List<SubjectRef<?>> parents = new ArrayList<>(originalParents);
                boolean found = false;
                for (ListIterator<SubjectRef<?>> it = parents.listIterator(); it.hasNext();) {
                    SubjectRef<?> parent = it.next();
                    if ((index = ranks().indexOf(parent)) > -1) {
                        if (index == ranks().size() - 1) {
                            return seg;
                        } else {
                            it.set(ranks().get(index + 1));
                            found = true;
                        }
                    }
                }

                if (found) {
                    return seg.withParents(parents); // Promotion happened
                } else {
                    return seg.plusParent(ranks().get(0));
                }
            }
        });

    }

    @Override
    public final ImmutableSubjectData demote(Set<ContextValue<?>> contexts, ImmutableSubjectData input) {
        if (ranks().isEmpty()) {
            return input;
        }

        return input.withSegment(contexts, seg -> {
            List<? extends SubjectRef<?>> originalParents = seg.parents();
            if (originalParents.isEmpty()) {
                return seg;
            } else {
                int index;
                final List<SubjectRef<?>> parents = new ArrayList<>(originalParents); // TODO: rewrite to work with immutable login
                boolean found = false;
                for (ListIterator<SubjectRef<?>> it = parents.listIterator(); it.hasNext();) {
                    SubjectRef<?> parent = it.next();
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
                    return seg.withParents(parents);
                } else {
                    return seg;
                }
            }
        });
    }

    @Override
    public final boolean isOnLadder(final Set<ContextValue<?>> contexts, final ImmutableSubjectData subject) {
        if (ranks().isEmpty()) {
            return false;
        }

        for (final SubjectRef<?> par : subject.segment(contexts).parents()) {
            if (ranks().contains(par)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final int indexOf(final SubjectRef<?> subject) {
        return ranks().indexOf(subject);
    }

    @Override
    public final RankLadder with(final SubjectRef<?> subject) {
        int indexOf = ranks().indexOf(subject);
        if (indexOf != -1) {
            return newWithRanks(this.ranks().minus(indexOf).plus(SubjectRef.mapKeySafe(subject)));
        } else {
            return newWithRanks(this.ranks().plus(SubjectRef.mapKeySafe(subject)));
        }

    }


    @Override
    public final RankLadder with(final SubjectRef<?> subject, int index) {
        if (index > ranks().size() || index < 0) {
            return this;
        }

        PVector<SubjectRef<?>> entries = this.ranks();
        int indexOf = entries.indexOf(subject);
        entries = entries.plus(index, subject);
        if (indexOf != -1) {
            if (indexOf >= index) {
                ++indexOf;
            }
            entries = entries.minus(indexOf);
        }
        return newWithRanks(entries);
    }

    @Override
    public final RankLadder without(final SubjectRef<?> subject) {
        final PVector<SubjectRef<?>> out = this.ranks().minus(subject);
        return this.ranks() == out ? this : newWithRanks(out);
    }

    @Override
    public abstract PVector<SubjectRef<?>> ranks();

    protected abstract RankLadder newWithRanks(final PVector<SubjectRef<?>> ents);

    @Override
    public final Component asComponent() {
        return Component.text(build -> build.content(name())
                .decoration(TextDecoration.BOLD, true)
                .hoverEvent(HoverEvent.showText(Messages.FORMATTER_BUTTON_INFO_PROMPT.tr()))
                .clickEvent(ClickEvent.runCommand("/pex rank " + name())));
    }
}
