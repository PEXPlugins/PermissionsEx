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

package ca.stellardrift.permissionsex.commands

import ca.stellardrift.permissionsex.PermissionsEx
import ca.stellardrift.permissionsex.commands.commander.ButtonType
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.data.Change
import ca.stellardrift.permissionsex.data.ImmutableSubjectData
import ca.stellardrift.permissionsex.data.SubjectDataReference
import ca.stellardrift.permissionsex.rank.RankLadder
import ca.stellardrift.permissionsex.util.Translations.t
import ca.stellardrift.permissionsex.util.Util
import ca.stellardrift.permissionsex.util.command.ChildCommands
import ca.stellardrift.permissionsex.util.command.CommandContext
import ca.stellardrift.permissionsex.util.command.CommandException
import ca.stellardrift.permissionsex.util.command.CommandExecutor
import ca.stellardrift.permissionsex.util.command.CommandSpec
import ca.stellardrift.permissionsex.util.command.args.GameArguments.rankLadder
import ca.stellardrift.permissionsex.util.command.args.GameArguments.subject
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.UnknownFlagBehavior
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.flags
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.integer
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.optional
import ca.stellardrift.permissionsex.util.command.args.GenericArguments.seq
import com.google.common.collect.ImmutableSet
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

    fun getPromoteCommand(pex: PermissionsEx<*>): CommandSpec {
        return CommandSpec.builder()
            .setAliases("promote", "prom")
            .setDescription(t("Promote a subject on the given ladder"))
            .setArguments(
                Util.contextTransientFlags(pex).buildWith(
                    seq(
                        subject(t("subject"), pex),
                        optional(
                            rankLadder(
                                t("ladder"),
                                pex
                            )
                        )
                    )
                )
            )
            .setExecutor(object : PermissionsExExecutor(pex) {
                @Throws(CommandException::class)
                override fun <TextType> execute(
                    src: Commander<TextType>,
                    args: CommandContext
                ) {
                    val ladderF =
                        if (args.hasAny("ladder")) args.getOne(
                            "ladder"
                        ) else pex.ladders["default", null]
                    val ref: SubjectDataReference = getDataRef(
                        src,
                        args,
                        "permissionsex.promote"
                    ) // ." + ladderF); // TODO: Re-add permissions checks for ladders
                    val contexts: Set<ContextValue<*>> =
                        ImmutableSet.copyOf(
                            args.getAll("context")
                        )
                    val ladderName =
                        AtomicReference<RankLadder>()
                    ladderF.thenCompose { ladder ->
                        ladderName.set(ladder)
                        ref.update { old ->
                            ladder.promote(
                                contexts,
                                old
                            )
                        }
                    }
                        .thenAccept { res: Change<ImmutableSubjectData> ->
                            if (res.new === res.old) {
                                throw RuntimeCommandException(
                                    t(
                                        "%s was already at the top of ladder %s",
                                        src.formatter.subject(ref),
                                        src.formatter.ladder(ladderName.get())
                                    )
                                )
                            }
                        }.thenMessageSubject(src) { ->
                        t(
                            "Promoted %s on ladder %s",
                            subject(ref),
                            ladder(ladderName.get()).hl()
                        )
                    }
                }
            })
            .build()
    }

    fun getDemoteCommand(pex: PermissionsEx<*>): CommandSpec {
        return CommandSpec.builder()
            .setAliases("demote", "dem")
            .setDescription(t("Demote a subject on the given ladder"))
            .setArguments(
                Util.contextTransientFlags(pex).buildWith(
                    seq(
                        subject(t("subject"), pex),
                        optional(
                            rankLadder(
                                t("ladder"),
                                pex
                            )
                        )
                    )
                )
            )
            .setExecutor(object : PermissionsExExecutor(pex) {
                @Throws(CommandException::class)
                override fun <TextType> execute(
                    src: Commander<TextType>,
                    args: CommandContext
                ) {
                    val ladderF =
                        if (args.hasAny("ladder")) args.getOne(
                            "ladder"
                        ) else pex.ladders["default", null]
                    val ref: SubjectDataReference = getDataRef(src, args, "permissionsex.demote") //." + ladder);
                    val contexts: Set<ContextValue<*>> =
                        ImmutableSet.copyOf(
                            args.getAll("context")
                        )
                    val ladderName =
                        AtomicReference<RankLadder>()
                        ladderF.thenCompose { ladder: RankLadder ->
                            ladderName.set(ladder)
                            ref.update { old: ImmutableSubjectData? ->
                                ladder.demote(
                                    contexts,
                                    old
                                )
                            }
                        }.thenAccept { res: Change<ImmutableSubjectData> ->
                            if (res.new === res.old) {
                                throw RuntimeCommandException(
                                    t(
                                        "%s was not on ladder %s",
                                        src.formatter.subject(ref),
                                        src.formatter.ladder(ladderName.get())
                                    )
                                )
                            }
                        }.thenMessageSubject(src) { ->
                        t(
                            "Demoted %s on ladder %s",
                            subject(ref),
                            ladder(ladderName.get())
                        )
                    }
                }
            })
            .build()
    }

    private fun <TextType> MessageFormatter<TextType>.deleteButton(
        rank: RankLadder,
        subject: Map.Entry<String, String>
    ): TextType {
        return button(
            ButtonType.NEGATIVE,
            t("-"),
            t("Remove this rank from the ladder"),
            "/pex rank ${rank.name} remove ${subject.key} ${subject.value}",
            true
        )
    }

    private fun <TextType> MessageFormatter<TextType>.moveDownButton(
        rank: RankLadder,
        subject: Map.Entry<String, String>
    ): TextType {
        return button(
            ButtonType.NEUTRAL,
            t("▼"),
            t("Move this rank to a lower position in the ladder"),
            "/pex rank ${rank.name} add -r -1 ${subject.key} ${subject.value}",
            true
        )
    }

    private fun <TextType> MessageFormatter<TextType>.moveUpButton(
        rank: RankLadder,
        subject: Map.Entry<String, String>
    ): TextType {
        return button(
            ButtonType.NEUTRAL,
            t("▲"),
            t("Move this rank to a higher position in the ladder"),
            "/pex rank ${rank.name} add -r 1 ${subject.key} ${subject.value}",
            true
        )
    }

    internal fun getRankingCommand(pex: PermissionsEx<*>): CommandSpec {
        val arg =
            ChildCommands.args(getRankAddChildCommand(pex), getRankRemoveCommand(pex))
        return CommandSpec.builder()
            .setAliases("ranking", "rank")
            .setDescription(t("Commands to modify ranking"))
            .setArguments(seq(rankLadder(t("ladder"), pex), optional(arg)))
            .setExecutor(
                ChildCommands.optionalExecutor(
                    arg,
                    object : CommandExecutor {
                        @Throws(CommandException::class)
                        override fun <TextType> execute(
                            src: Commander<TextType>,
                            args: CommandContext
                        ) {
                            val ladder =
                                    args.getOne<CompletableFuture<RankLadder>>("ladder").join()
                            val ranksList = mutableListOf<TextType>()
                            val rawRanks: List<Map.Entry<String, String>> = ladder.ranks.reversed()
                            src.formatter.apply {
                                if (rawRanks.size == 1) {
                                    ranksList.add(subject(rawRanks[0]) + deleteButton(ladder, rawRanks[0]))
                                } else if (rawRanks.size == 0) {
                                    throw CommandException(
                                        t(
                                            "No ranks in ladder %s",
                                            src.formatter.ladder(ladder)
                                        )
                                    )
                                } else {
                                    rawRanks.forEachIndexed { i, rank ->
                                        ranksList.add(
                                            when (i) {
                                                0 -> {
                                                    listOf(
                                                        -rank,
                                                        moveDownButton(ladder, rank),
                                                        deleteButton(ladder, rank)
                                                    )
                                                }
                                                rawRanks.size - 1 -> {
                                                    listOf(
                                                        -rank,
                                                        moveUpButton(ladder, rank),
                                                        deleteButton(ladder, rank)
                                                    )
                                                }
                                                else -> {
                                                    listOf(
                                                        -rank,
                                                        moveDownButton(ladder, rank),
                                                        moveUpButton(ladder, rank),
                                                        deleteButton(ladder, rank)
                                                    )
                                                }
                                            }.concat(-" ")
                                        )
                                    }
                                }
                                Unit

                            }
                            src.msgPaginated(
                                t(
                                    "Rank ladder %s %s",
                                    ladder.name,
                                    src.formatter.button(
                                        ButtonType.POSITIVE,
                                        t("+"),
                                        t("Add a rank to this ladder"),
                                        "/pex rank ${ladder.name} add ",
                                        false
                                    )
                                ),
                                t("Ranks are sorted from highest to lowest"), ranksList
                            )
                        }
                    })
            )
            .build()
    }

    private fun getRankAddChildCommand(pex: PermissionsEx<*>): CommandSpec {
        return CommandSpec.builder()
            .setAliases("add", "+")
            .setArguments(
                flags()
                    .flag("r", "-relative")
                    .setUnknownShortFlagBehavior(UnknownFlagBehavior.IGNORE)
                    .buildWith(
                        seq(optional(integer(
                                    t("position")
                                )), subject(t("subject"), pex, PermissionsEx.SUBJECTS_GROUP))
                    )
            )
            .setExecutor(object : PermissionsExExecutor(pex) {
                @Throws(CommandException::class)
                override fun <TextType> execute(
                    src: Commander<TextType>,
                    args: CommandContext
                ) {
                    val ladder =
                            args.getOne<CompletableFuture<RankLadder>>("ladder").join()
                    val toAdd =
                        args.getOne<Map.Entry<String, String>>("subject")
                    src.checkSubjectPermission(toAdd, "permissionsex.rank.add.${ladder.name}")
                    val position = args.getOne<Int>("position")
                    if (position != null) {
                        var addPosition = position
                        if (args.hasAny("r")) {
                            val currentIndex = ladder.indexOfRank(toAdd)
                            if (currentIndex == -1) {
                                throw CommandException(t("Cannot do a relative move on a rank that is not in the ladder"))
                            }
                            addPosition =
                                if (currentIndex + addPosition > 1) addPosition + 1 else addPosition // If we are adding to later, we need to add after the next rank (otherwise we end up staying in the same place)
                        }
                            pex.ladders.set(ladder.name, ladder.addRankAt(toAdd, addPosition)).thenMessageSubject(src) { ->
                                t(
                                    "Successfully added %s to ladder %s at position %s",
                                    -toAdd,
                                    -ladder,
                                    addPosition
                                )
                            }
                    } else {
                            pex.ladders.set(ladder.name, ladder.addRank(toAdd))
                                .thenMessageSubject(src) { ->
                                    t(
                                        "Successfully added %s to ladder %s",
                                        -toAdd,
                                        -ladder
                                    )
                                }
                    }
                }
            })
            .build()
    }

    private fun getRankRemoveCommand(pex: PermissionsEx<*>): CommandSpec {
        return CommandSpec.builder()
            .setAliases("remove", "rem", "-")
            .setArguments(subject(t("subject"), pex, PermissionsEx.SUBJECTS_GROUP))
            .setExecutor(object : PermissionsExExecutor(pex) {
                @Throws(CommandException::class)
                override fun <TextType> execute(
                    src: Commander<TextType>,
                    args: CommandContext
                ) {
                    val ladder =
                            args.getOne<CompletableFuture<RankLadder>>("ladder").join()
                    val toRemove =
                        args.getOne<Map.Entry<String, String>>("subject")
                    src.checkSubjectPermission(toRemove, "permissionsex.rank.remove." + ladder.name)
                    val newLadder = ladder.removeRank(toRemove)
                    if (newLadder === ladder) {
                        throw CommandException(
                            t(
                                "Rank %s was not in ladder %s",
                                src.formatter.subject(toRemove),
                                src.formatter.ladder(ladder)
                            )
                        )
                    } else {
                            pex.ladders.set(ladder.name, newLadder)
                                .thenMessageSubject(src) { ->
                                    t(
                                        "Successfully removed %s from ladder %s",
                                        -toRemove,
                                        -ladder
                                    )
                                }
                    }
                }
            })
            .build()
    }
