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
import com.google.common.util.concurrent.Futures;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.data.SubjectDataReference;
import ninja.leaping.permissionsex.data.SubjectRef;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static ninja.leaping.permissionsex.util.Translations.t;
import static ninja.leaping.permissionsex.util.command.args.GameArguments.rankLadder;
import static ninja.leaping.permissionsex.util.command.args.GameArguments.subject;
import static ninja.leaping.permissionsex.util.command.args.GenericArguments.*;

public class RankingCommands {

    public static CommandSpec getPromoteCommand(PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("promote", "prom")
                .setDescription(t("Promote a subject on the given ladder"))
                .setArguments(Util.contextTransientFlags().buildWith(seq(subject(t("subject"), pex), optional(rankLadder(t("ladder"), pex)))))
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        CompletableFuture<RankLadder> ladderF = args.hasAny("ladder") ? args.getOne("ladder") : pex.getLadders().get("default", null);
                        SubjectDataReference ref = getDataRef(src, args, "permissionsex.promote"); // ." + ladderF); // TODO: Re-add permissions checks for ladders
                        Set<Map.Entry<String, String>> contexts = ImmutableSet.copyOf(args.<Map.Entry<String, String>>getAll("context"));
                        final AtomicReference<RankLadder> ladderName = new AtomicReference<>();
                        messageSubjectOnFuture(ladderF.thenCompose(ladder -> {
                            ladderName.set(ladder);
                            return ref.update(old -> ladder.promote(contexts, old));
                        })
                                .thenAccept(res -> {
                                    if (res.getNew() == res.getOld()) {
                                        throw new RuntimeCommandException(t("%s was already at the top of ladder %s", src.fmt().subject(ref), src.fmt().ladder(ladderName.get())));
                                    }
                                }), src, () -> t("Promoted %s on ladder %s", src.fmt().subject(ref), src.fmt().hl(src.fmt().ladder(ladderName.get()))));
                    }
                })
                .build();
    }

    public static CommandSpec getDemoteCommand(PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("demote", "dem")
                .setDescription(t("Demote a subject on the given ladder"))
                .setArguments(Util.contextTransientFlags().buildWith(seq(subject(t("subject"), pex), optional(rankLadder(t("ladder"), pex)))))
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        CompletableFuture<RankLadder> ladderF = args.hasAny("ladder") ? args.getOne("ladder") : pex.getLadders().get("default", null);
                        SubjectDataReference ref = getDataRef(src, args, "permissionsex.demote"); //." + ladder);
                        Set<Map.Entry<String, String>> contexts = ImmutableSet.copyOf(args.<Map.Entry<String, String>>getAll("context"));
                        final AtomicReference<RankLadder> ladderName = new AtomicReference<>();
                        messageSubjectOnFuture(ladderF.thenCompose(ladder -> {
                            return ref.update(old -> ladder.demote(contexts, old));}).thenAccept(res -> {
                            if (res.getNew() == res.getOld()) {
                                throw new RuntimeCommandException(t("%s was not on ladder %s", src.fmt().subject(ref), src.fmt().ladder(ladderName.get())));
                            }
                        }), src, () -> t("Demoted %s on ladder %s", src.fmt().subject(ref), src.fmt().hl(src.fmt().ladder(ladderName.get()))));
                    }
                })
                .build();
    }

    private static <TextType> TextType deleteButton(Commander<TextType> cmd, RankLadder rank, SubjectRef subject) {
        return cmd.fmt().button(ButtonType.NEGATIVE, t("-"), t("Remove this rank from the ladder"), "/pex rank " + rank.getName() + " remove " + subject.getType() + " " + subject.getIdentifier(), true);
    }

    private static <TextType> TextType moveDownButton(Commander<TextType> cmd, RankLadder rank, SubjectRef subject) {
        return cmd.fmt().button(ButtonType.NEUTRAL, t("\u25bc"), t("Move this rank to a lower position in the ladder"), "/pex rank " + rank.getName() + " add -r -1 " + subject.getType() + " " + subject.getIdentifier(), true);
    }

    private static <TextType> TextType moveUpButton(Commander<TextType> cmd, RankLadder rank, SubjectRef subject) {
        return cmd.fmt().button(ButtonType.NEUTRAL, t("\u25b2"), t("Move this rank to a higher position in the ladder"), "/pex rank " + rank.getName() + " add -r 1 " + subject.getType() + " " + subject.getIdentifier(), true);
    }

    public static CommandSpec getRankingCommand(PermissionsEx pex) {
        final CommandElement arg = ChildCommands.args(getRankAddChildCommand(pex), getRankRemoveCommand(pex));
        return CommandSpec.builder()
                .setAliases("ranking", "rank")
                .setDescription(t("Commands to modify ranking"))
                .setArguments(seq(rankLadder(t("ladder"), pex), optional(arg)))
                .setExecutor(ChildCommands.optionalExecutor(arg, new CommandExecutor() {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        final RankLadder ladder = Futures.getUnchecked(args.<CompletableFuture<RankLadder>>getOne("ladder"));
                        List<TextType> ranksList = new ArrayList<>();
                        List<? extends SubjectRef> rawRanks = new ArrayList<>(ladder.getRanks());
                        Collections.reverse(rawRanks);
                        if (rawRanks.size() == 1) {
                            ranksList.add(src.fmt().combined(src.fmt().subject(rawRanks.get(0)), deleteButton(src, ladder, rawRanks.get(0))));
                        } else if (rawRanks.size() == 0) {
                            throw new CommandException(t("No ranks in ladder %s", src.fmt().ladder(ladder)));
                        } else {
                            for (int i = 0; i < rawRanks.size(); ++i) {
                                SubjectRef rank = rawRanks.get(i);
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
                        src.msgPaginated(t("Rank ladder %s %s", ladder.getName(), src.fmt().button(ButtonType.POSITIVE, t("+"), t("Add a rank to this ladder"), "/pex rank " + ladder.getName() + " add ", false)),
                                t("Ranks are sorted from highest to lowest"), ranksList);

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
                        .buildWith(seq(optional(integer(t("position"))), subject(t("subject"), pex, PermissionsEx.SUBJECTS_GROUP))))
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        final RankLadder ladder = Futures.getUnchecked(args.<CompletableFuture<RankLadder>>getOne("ladder"));
                        SubjectRef toAdd = args.getOne("subject");
                        checkSubjectPermission(src, toAdd, "permissionsex.rank.add." + ladder.getName());
                        Integer position = args.getOne("position");
                        if (position != null) {
                            int addPosition = position;
                            if (args.hasAny("r")) {
                                int currentIndex = ladder.indexOfRank(toAdd);
                                if (currentIndex == -1) {
                                    throw new CommandException(t("Cannot do a relative move on a rank that is not in the ladder"));
                                }
                                addPosition = currentIndex + addPosition > 1 ? addPosition + 1 : addPosition; // If we are adding to later, we need to add after the next rank (otherwise we end up staying in the same place)
                            }
                            messageSubjectOnFuture(pex.getLadders().set(ladder.getName(), ladder.addRankAt(toAdd, addPosition)), src, t("Successfully added %s to ladder %s at position %s", src.fmt().subject(toAdd), src.fmt().ladder(ladder), addPosition));
                        } else {
                            messageSubjectOnFuture(pex.getLadders().set(ladder.getName(), ladder.addRank(toAdd)), src, t("Successfully added %s to ladder %s", src.fmt().subject(toAdd), src.fmt().ladder(ladder)));

                        }
                    }
                })
                .build();

    }

    private static CommandSpec getRankRemoveCommand(PermissionsEx pex) {
        return CommandSpec.builder()
                .setAliases("remove", "rem", "-")
                .setArguments(subject(t("subject"), pex, PermissionsEx.SUBJECTS_GROUP))
                .setExecutor(new PermissionsExExecutor(pex) {
                    @Override
                    public <TextType> void execute(Commander<TextType> src, CommandContext args) throws CommandException {
                        final RankLadder ladder = Futures.getUnchecked(args.<CompletableFuture<RankLadder>>getOne("ladder"));
                        SubjectRef toRemove = args.getOne("subject");
                        checkSubjectPermission(src, toRemove, "permissionsex.rank.remove." + ladder.getName());
                        RankLadder newLadder = ladder.removeRank(toRemove);
                        if (newLadder == ladder) {
                            throw new CommandException(t("Rank %s was not in ladder %s", src.fmt().subject(toRemove), src.fmt().ladder(ladder)));
                        } else {
                            messageSubjectOnFuture(pex.getLadders().set(ladder.getName(), newLadder), src, t("Successfully removed %s from ladder %s", src.fmt().subject(toRemove), src.fmt().ladder(ladder)));
                        }
                    }
                })
                .build();
    }
}
