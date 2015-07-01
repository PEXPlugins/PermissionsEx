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
package ninja.leaping.permissionsex.command;

import com.google.common.collect.ImmutableSet;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.ImmutableOptionSubjectData;
import ninja.leaping.permissionsex.data.SubjectCache;
import ninja.leaping.permissionsex.rank.RankLadder;
import ninja.leaping.permissionsex.util.Util;
import ninja.leaping.permissionsex.util.command.ButtonType;
import ninja.leaping.permissionsex.util.command.ChildCommands;
import ninja.leaping.permissionsex.util.command.CommandContext;
import ninja.leaping.permissionsex.util.command.CommandException;
import ninja.leaping.permissionsex.util.command.CommandExecutor;
import ninja.leaping.permissionsex.util.command.CommandSpec;
import ninja.leaping.permissionsex.util.command.Commander;
import ninja.leaping.permissionsex.util.command.args.CommandElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ninja.leaping.permissionsex.util.Translations._;
import static ninja.leaping.permissionsex.util.command.args.GameArguments.rankLadder;
import static ninja.leaping.permissionsex.util.command.args.GameArguments.subject;
import static ninja.leaping.permissionsex.util.command.args.GenericArguments.*;

public class RankingCommands {

    public static CommandSpec getPromoteCommand(PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("promote", "prom")
                .setDescription(_("Promote a subject on the given ladder"))
                .setArguments(Util.contextTransientFlags().buildWith(seq(subject(_("subject"), pex), optional(rankLadder(_("ladder"), pex)))))
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        Map.Entry<String, String> subject = subjectOrSelf(src, args);
                        RankLadder ladder = args.hasAny("ladder") ? args.<RankLadder>getOne("ladder") : pex.getLadders().get("default", null);
                        checkSubjectPermission(src, subject, "permissionsex.promote." + ladder);
                        Set<Map.Entry<String, String>> contexts = ImmutableSet.copyOf(args.<Map.Entry<String, String>>getAll("context"));
                        SubjectCache dataCache = args.hasAny("transient") ? pex.getTransientSubjects(subject.getKey()) : pex.getSubjects(subject.getKey());
                        ImmutableOptionSubjectData data = getSubjectData(dataCache, subject.getValue());
                        ImmutableOptionSubjectData newData = ladder.promote(contexts, data);
                        if (newData == data) {
                            throw new CommandException(_("%s was already at the top of ladder %s", src.fmt().subject(subject), src.fmt().ladder(ladder)));
                        }
                        messageSubjectOnFuture(dataCache.update(subject.getValue(), newData), src, _("Promoted %s on ladder %s", src.fmt().subject(subject), src.fmt().hl(src.fmt().combined(ladder.getName()))));
                    }
                })
                .build();
    }

    public static CommandSpec getDemoteCommand(PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("demote", "dem")
                .setDescription(_("Demote a subject on the given ladder"))
                .setArguments(Util.contextTransientFlags().buildWith(seq(subject(_("subject"), pex), optional(rankLadder(_("ladder"), pex)))))
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        Map.Entry<String, String> subject = subjectOrSelf(src, args);
                        RankLadder ladder = args.hasAny("ladder") ? args.<RankLadder>getOne("ladder") : pex.getLadders().get("default", null);
                        checkSubjectPermission(src, subject, "permissionsex.demote." + ladder);
                        Set<Map.Entry<String, String>> contexts = ImmutableSet.copyOf(args.<Map.Entry<String, String>>getAll("context"));
                        SubjectCache dataCache = args.hasAny("transient") ? pex.getTransientSubjects(subject.getKey()) : pex.getSubjects(subject.getKey());
                        ImmutableOptionSubjectData data = getSubjectData(dataCache, subject.getValue());
                        ImmutableOptionSubjectData newData = ladder.demote(contexts, data);
                        if (newData == data) {
                            throw new CommandException(_("%s was not on ladder %s", src.fmt().subject(subject), src.fmt().ladder(ladder)));
                        }
                        messageSubjectOnFuture(dataCache.update(subject.getValue(), newData), src, _("Demoted %s on ladder %s", src.fmt().subject(subject), src.fmt().hl(src.fmt().combined(ladder.getName()))));
                    }
                })
                .build();
    }

    private static <TextType> TextType deleteButton(Commander<TextType> cmd, RankLadder rank, Map.Entry<String, String> subject) {
        return cmd.fmt().button(ButtonType.NEGATIVE, _("-"), _("Remove this rank from the ladder"), "/pex rank " + rank.getName() + " remove " + subject.getKey() + " " + subject.getValue(), true);
    }

    private static <TextType> TextType moveDownButton(Commander<TextType> cmd, RankLadder rank, Map.Entry<String, String> subject) {
        return cmd.fmt().button(ButtonType.NEUTRAL, _("\u25bc"), _("Move this rank to a lower position in the ladder"), "/pex rank " + rank.getName() + " add -r -1 " + subject.getKey() + " " + subject.getValue(), true);
    }

    private static <TextType> TextType moveUpButton(Commander<TextType> cmd, RankLadder rank, Map.Entry<String, String> subject) {
        return cmd.fmt().button(ButtonType.NEUTRAL, _("\u25b2"), _("Move this rank to a higher position in the ladder"), "/pex rank " + rank.getName() + " add -r 1 " + subject.getKey() + " " + subject.getValue(), true);
    }

    public static CommandSpec getRankingCommand(PermissionsEx pex) {
        final CommandElement arg = ChildCommands.args(getRankAddChildCommand(pex), getRankRemoveCommand(pex));
        return CommandSpec.builder()
                .setAliases("ranking", "rank")
                .setDescription(_("Commands to modify ranking"))
                .setArguments(seq(rankLadder(_("ladder"), pex), optional(arg)))
                .setExecutor(ChildCommands.optionalExecutor(arg, new CommandExecutor() {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        final RankLadder ladder = args.getOne("ladder");
                        List<TextType> ranksList = new ArrayList<>();
                        List<? extends Map.Entry<String, String>> rawRanks = new ArrayList<>(ladder.getRanks());
                        Collections.reverse(rawRanks);
                        if (rawRanks.size() == 1) {
                            ranksList.add(src.fmt().combined(src.fmt().subject(rawRanks.get(0)), deleteButton(src, ladder, rawRanks.get(0))));
                        } else if (rawRanks.size() == 0) {
                            throw new CommandException(_("No ranks in ladder %s", src.fmt().ladder(ladder)));
                        } else {
                            for (int i = 0; i < rawRanks.size(); ++i) {
                                Map.Entry<String, String> rank = rawRanks.get(i);
                                if (i == 0) {
                                    ranksList.add(src.fmt().combined(src.fmt().subject(rawRanks.get(i)),
                                            " ", moveDownButton(src, ladder, rank),
                                            " ", deleteButton(src, ladder, rank)));
                                } else if (i == rawRanks.size() - 1) {
                                    ranksList.add(src.fmt().combined(src.fmt().subject(rawRanks.get(i)),
                                            " ", moveUpButton(src, ladder, rank),
                                            " ", deleteButton(src, ladder, rank)));
                                } else {
                                    ranksList.add(src.fmt().combined(src.fmt().subject(rawRanks.get(i)),
                                            " ", moveDownButton(src, ladder, rank),
                                            " ", moveUpButton(src, ladder, rank),
                                            " ", deleteButton(src, ladder, rank)));
                                }
                            }
                        }
                        src.msgPaginated(_("Rank ladder %s %s", ladder.getName(), src.fmt().button(ButtonType.POSITIVE, _("+"), _("Add a rank to this ladder"), "/pex rank " + ladder.getName() + " add ", false)),
                                _("Ranks are sorted from highest to lowest"), ranksList);

                    }
                }))
                .build();
    }

    private static CommandSpec getRankAddChildCommand(PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("add", "+")
                .setArguments(flags()
                        .flag("r", "-relative")
                        .setUnknownShortFlagBehavior(UnknownFlagBehavior.IGNORE)
                        .buildWith(seq(optional(integer(_("position"))), subject(_("subject"), pex, "group"))))
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        final RankLadder ladder = args.getOne("ladder");
                        Map.Entry<String, String> toAdd = args.getOne("subject");
                        checkSubjectPermission(src, toAdd, "permissionsex.rank.add." + ladder.getName());
                        Integer position = args.getOne("position");
                        if (position != null) {
                            int addPosition = position;
                            if (args.hasAny("r")) {
                                int currentIndex = ladder.indexOfRank(toAdd);
                                if (currentIndex == -1) {
                                    throw new CommandException(_("Cannot do a relative move on a rank that is not in the ladder"));
                                }
                                addPosition = currentIndex + addPosition > 1 ? addPosition + 1 : addPosition; // If we are adding to later, we need to add after the next rank (otherwise we end up staying in the same place)
                            }
                            messageSubjectOnFuture(pex.getLadders().update(ladder.getName(), ladder.addRankAt(toAdd, addPosition)), src, _("Successfully added %s to ladder %s at position %s", src.fmt().subject(toAdd), src.fmt().ladder(ladder), addPosition));
                        } else {
                            messageSubjectOnFuture(pex.getLadders().update(ladder.getName(), ladder.addRank(toAdd)), src, _("Successfully added %s to ladder %s", src.fmt().subject(toAdd), src.fmt().ladder(ladder)));

                        }
                    }
                })
                .build();

    }

    private static CommandSpec getRankRemoveCommand(PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("remove", "rem", "-")
                .setArguments(subject(_("subject"), pex, "group"))
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        final RankLadder ladder = args.getOne("ladder");
                        Map.Entry<String, String> toRemove = args.getOne("subject");
                        checkSubjectPermission(src, toRemove, "permissionsex.rank.remove." + ladder.getName());
                        RankLadder newLadder = ladder.removeRank(toRemove);
                        if (newLadder == ladder) {
                            throw new CommandException(_("Rank %s was not in ladder %s", src.fmt().subject(toRemove), src.fmt().ladder(ladder)));
                        } else {
                            messageSubjectOnFuture(pex.getLadders().update(ladder.getName(), newLadder), src, _("Successfully removed %s from ladder %s", src.fmt().subject(toRemove), src.fmt().ladder(ladder)));
                        }
                    }
                })
                .build();
    }
}
