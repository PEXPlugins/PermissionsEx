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
import ca.stellardrift.permissionsex.commands.Messages.COMMON_ARGS_CONTEXT
import ca.stellardrift.permissionsex.commands.Messages.COMMON_ARGS_POSITION
import ca.stellardrift.permissionsex.commands.Messages.COMMON_ARGS_RANK_LADDER
import ca.stellardrift.permissionsex.commands.Messages.COMMON_ARGS_SUBJECT
import ca.stellardrift.permissionsex.commands.Messages.DEMOTE_DESCRIPTION
import ca.stellardrift.permissionsex.commands.Messages.DEMOTE_ERROR_NOT_ON_LADDER
import ca.stellardrift.permissionsex.commands.Messages.DEMOTE_SUCCESS
import ca.stellardrift.permissionsex.commands.Messages.PROMOTE_DESCRIPTION
import ca.stellardrift.permissionsex.commands.Messages.PROMOTE_ERROR_ALREADY_AT_TOP
import ca.stellardrift.permissionsex.commands.Messages.PROMOTE_SUCCESS
import ca.stellardrift.permissionsex.commands.Messages.RANKING_ADD_ERROR_RELATIVE_ON_OUTSIDE_SUBJECT
import ca.stellardrift.permissionsex.commands.Messages.RANKING_ADD_SUCCESS
import ca.stellardrift.permissionsex.commands.Messages.RANKING_ADD_SUCCESS_POSITION
import ca.stellardrift.permissionsex.commands.Messages.RANKING_BUTTON_ADD_DESCRIPTION
import ca.stellardrift.permissionsex.commands.Messages.RANKING_BUTTON_DELETE_DESCRIPTION
import ca.stellardrift.permissionsex.commands.Messages.RANKING_BUTTON_MOVE_DOWN_DESCRIPTION
import ca.stellardrift.permissionsex.commands.Messages.RANKING_BUTTON_MOVE_UP_DESCRIPTION
import ca.stellardrift.permissionsex.commands.Messages.RANKING_DESCRIPTION
import ca.stellardrift.permissionsex.commands.Messages.RANKING_ERROR_EMPTY_LADDER
import ca.stellardrift.permissionsex.commands.Messages.RANKING_PAGINATION_HEADER
import ca.stellardrift.permissionsex.commands.Messages.RANKING_PAGINATION_SUBTITLE
import ca.stellardrift.permissionsex.commands.Messages.RANKING_REMOVE_ERROR_NOT_IN_LADDER
import ca.stellardrift.permissionsex.commands.Messages.RANKING_REMOVE_SUCCESS
import ca.stellardrift.permissionsex.commands.commander.ButtonType
import ca.stellardrift.permissionsex.commands.commander.Commander
import ca.stellardrift.permissionsex.commands.commander.MessageFormatter
import ca.stellardrift.permissionsex.commands.parse.ChildCommandBuilder
import ca.stellardrift.permissionsex.commands.parse.CommandContext
import ca.stellardrift.permissionsex.commands.parse.CommandException
import ca.stellardrift.permissionsex.commands.parse.StructuralArguments.UnknownFlagBehavior
import ca.stellardrift.permissionsex.commands.parse.StructuralArguments.flags
import ca.stellardrift.permissionsex.commands.parse.StructuralArguments.optional
import ca.stellardrift.permissionsex.commands.parse.StructuralArguments.seq
import ca.stellardrift.permissionsex.commands.parse.ValueElement
import ca.stellardrift.permissionsex.commands.parse.command
import ca.stellardrift.permissionsex.commands.parse.contextTransientFlags
import ca.stellardrift.permissionsex.commands.parse.int
import ca.stellardrift.permissionsex.commands.parse.rankLadder
import ca.stellardrift.permissionsex.commands.parse.subject
import ca.stellardrift.permissionsex.context.ContextValue
import ca.stellardrift.permissionsex.data.SubjectDataReference
import ca.stellardrift.permissionsex.rank.RankLadder
import ca.stellardrift.permissionsex.util.join
import ca.stellardrift.permissionsex.util.plus
import ca.stellardrift.permissionsex.util.styled
import ca.stellardrift.permissionsex.util.thenMessageSubject
import ca.stellardrift.permissionsex.util.toComponent
import ca.stellardrift.permissionsex.util.unaryMinus
import ca.stellardrift.permissionsex.util.unaryPlus
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference
import net.kyori.text.Component
import net.kyori.text.TextComponent

// Promotion and demotion along existing rank ladders

fun getPromoteCommand(pex: PermissionsEx<*>) =
    command("promote", "prom") {
        description = PROMOTE_DESCRIPTION()
        args =
            contextTransientFlags(pex).buildWith(
                seq(
                    subject(pex) key COMMON_ARGS_SUBJECT(),
                    optional(rankLadder(pex) key COMMON_ARGS_RANK_LADDER())
                )
            )
        executor(object : PermissionsExExecutor(pex) {
            @Throws(CommandException::class)
            override fun execute(src: Commander, args: CommandContext) {
                val ladderF =
                    if (args.hasAny(COMMON_ARGS_RANK_LADDER)) args.getOne(
                        COMMON_ARGS_RANK_LADDER
                    )!! else pex.ladders["default", null]
                val ref: SubjectDataReference = getDataRef(src, args, "permissionsex.promote")
                // ." + ladderF); // TODO: Re-add permissions checks for ladders
                val contexts = args.getAll<ContextValue<*>>(COMMON_ARGS_CONTEXT).toSet()
                val ladderName = AtomicReference<RankLadder>()
                ladderF.thenCompose { ladder ->
                    ladderName.set(ladder)
                    ref.update { old -> ladder.promote(contexts, old) }
                }.thenAccept { res ->
                    if (res.new === res.old) {
                        throw CommandException(
                            PROMOTE_ERROR_ALREADY_AT_TOP(src.formatter.subject(ref), ladderName.get().toComponent())
                        )
                    }
                }.thenMessageSubject(src) { send ->
                    send(PROMOTE_SUCCESS(+ref, ladderName.get().toComponent().styled { hl() }))
                }
            }
        })
    }

fun getDemoteCommand(pex: PermissionsEx<*>) =
    command("demote", "dem") {
        description = DEMOTE_DESCRIPTION()
        args = contextTransientFlags(pex).buildWith(
            seq(
                subject(pex) key COMMON_ARGS_SUBJECT(),
                optional(
                    rankLadder(pex) key
                            COMMON_ARGS_RANK_LADDER()
                )
            )
        )
        executor(object : PermissionsExExecutor(pex) {
            @Throws(CommandException::class)
            override fun execute(src: Commander, args: CommandContext) {
                val ladderF =
                    if (args.hasAny(COMMON_ARGS_RANK_LADDER)) args.getOne(
                        COMMON_ARGS_RANK_LADDER
                    )!! else pex.ladders["default", null]
                val ref: SubjectDataReference = getDataRef(src, args, "permissionsex.demote") // ." + ladder);
                val contexts = args.getAll<ContextValue<*>>(COMMON_ARGS_CONTEXT).toSet()
                val ladderName =
                    AtomicReference<RankLadder>()
                ladderF.thenCompose { ladder: RankLadder ->
                    ladderName.set(ladder)
                    ref.update { old -> ladder.demote(contexts, old) }
                }.thenAccept { res ->
                    if (res.new === res.old) {
                        throw CommandException(
                            DEMOTE_ERROR_NOT_ON_LADDER(src.formatter.subject(ref), ladderName.get().toComponent())
                        )
                    }
                }.thenMessageSubject(src) { send ->
                    send(DEMOTE_SUCCESS(+ref, TextComponent.builder().append(ladderName.get().toComponent()).hl()))
                }
            }
        })
    }

// Formatting elements

private fun MessageFormatter.deleteButton(
    rank: RankLadder,
    subject: Map.Entry<String, String>
): Component {
    return (-"-").button(
        ButtonType.NEGATIVE,
        RANKING_BUTTON_DELETE_DESCRIPTION(),
        "/pex rank ${rank.name} remove ${subject.key} ${subject.value}",
        true
    ).build()
}

private fun MessageFormatter.moveDownButton(
    rank: RankLadder,
    subject: Map.Entry<String, String>
): Component {
    return (-"▼").button(
        ButtonType.NEUTRAL,
        RANKING_BUTTON_MOVE_DOWN_DESCRIPTION(),
        "/pex rank ${rank.name} add -r -1 ${subject.key} ${subject.value}",
        true
    ).build()
}

private fun MessageFormatter.moveUpButton(
    rank: RankLadder,
    subject: Map.Entry<String, String>
): Component {
    return (-"▲").button(
        ButtonType.NEUTRAL,
        RANKING_BUTTON_MOVE_UP_DESCRIPTION(),
        "/pex rank ${rank.name} add -r 1 ${subject.key} ${subject.value}",
        true
    ).build()
}

// Rank ladder management

internal fun getRankingCommand(pex: PermissionsEx<*>) =
    command("ranking", "rank") {
        description = RANKING_DESCRIPTION()

        val rankLadderArg = rankLadder(pex) key COMMON_ARGS_RANK_LADDER()

        val childBuilder = ChildCommandBuilder()
        with(childBuilder) {
            child(getRankAddChildCommand(pex, rankLadderArg))
            child(getRankRemoveCommand(pex, rankLadderArg))

            fallback { src, args ->
                val ladder = args[rankLadderArg].join()
                val ranksList = mutableListOf<Component>()
                val rawRanks: List<Map.Entry<String, String>> = ladder.ranks.reversed()
                src.formatter.apply {
                    when (rawRanks.size) {
                        1 -> {
                            ranksList.add(subject(rawRanks[0]) + deleteButton(ladder, rawRanks[0]))
                        }
                        0 -> {
                            throw CommandException(
                                RANKING_ERROR_EMPTY_LADDER(ladder.toComponent())
                            )
                        }
                        else -> {
                            rawRanks.forEachIndexed { i, rank ->
                                ranksList.add(
                                    when (i) {
                                        0 -> {
                                            listOf(
                                                rank.toComponent(),
                                                moveDownButton(ladder, rank),
                                                deleteButton(ladder, rank)
                                            )
                                        }
                                        rawRanks.size - 1 -> {
                                            listOf(
                                                rank.toComponent(),
                                                moveUpButton(ladder, rank),
                                                deleteButton(ladder, rank)
                                            )
                                        }
                                        else -> {
                                            listOf(
                                                rank.toComponent(),
                                                moveDownButton(ladder, rank),
                                                moveUpButton(ladder, rank),
                                                deleteButton(ladder, rank)
                                            )
                                        }
                                    }.join()
                                )
                            }
                        }
                    }
                    src.msgPaginated(
                        RANKING_PAGINATION_HEADER(
                            ladder.name,
                            (-"+").button(
                                ButtonType.POSITIVE,
                                RANKING_BUTTON_ADD_DESCRIPTION(),
                                "/pex rank ${ladder.name} add ",
                                false
                            )
                        ),
                        RANKING_PAGINATION_SUBTITLE(), ranksList
                    )
                }
            }
        }

        val (_, childArg, childExec) = childBuilder.build()
        args = seq(rankLadderArg, childArg)
        executor(childExec)
    }

private fun getRankAddChildCommand(
    pex: PermissionsEx<*>,
    rankLadderArg: ValueElement<CompletableFuture<RankLadder>>
) =
    command("add", "+") {
        val subject = subject(pex, PermissionsEx.SUBJECTS_GROUP) key COMMON_ARGS_SUBJECT()
        args = flags()
            .flag("r", "-relative")
            .setUnknownShortFlagBehavior(UnknownFlagBehavior.IGNORE)
            .buildWith(seq(optional(int() key COMMON_ARGS_POSITION()), subject))
        executor(object : PermissionsExExecutor(pex) {
            @Throws(CommandException::class)
            override fun execute(
                src: Commander,
                args: CommandContext
            ) {
                val ladder = args[rankLadderArg].join()
                val toAdd = args[subject]
                src.checkSubjectPermission(toAdd, "permissionsex.rank.add.${ladder.name}")
                val position = args.getOne<Int>(COMMON_ARGS_POSITION)
                if (position != null) {
                    var addPosition = position
                    if (args.hasAny("r")) {
                        val currentIndex = ladder.indexOfRank(toAdd)
                        if (currentIndex == -1) {
                            throw CommandException(RANKING_ADD_ERROR_RELATIVE_ON_OUTSIDE_SUBJECT())
                        }
                        addPosition =
                            if (currentIndex + addPosition > 1) addPosition + 1 else addPosition // If we are adding to later, we need to add after the next rank (otherwise we end up staying in the same place)
                    }
                    pex.ladders.set(ladder.name, ladder.addRankAt(toAdd, addPosition)).thenMessageSubject(src) { send ->
                        send(RANKING_ADD_SUCCESS_POSITION(+toAdd, ladder.toComponent(), +addPosition.toString()))
                    }
                } else {
                    pex.ladders.set(ladder.name, ladder.addRank(toAdd))
                        .thenMessageSubject(src) { send ->
                            send(RANKING_ADD_SUCCESS(+toAdd, ladder.toComponent()))
                        }
                }
            }
        })
    }

private fun getRankRemoveCommand(
    pex: PermissionsEx<*>,
    rankLadderArg: ValueElement<CompletableFuture<RankLadder>>
) =
    command("remove", "rem", "-") {
        val subject = subject(pex, PermissionsEx.SUBJECTS_GROUP) key COMMON_ARGS_SUBJECT()
        args = subject
        executor(object : PermissionsExExecutor(pex) {
            @Throws(CommandException::class)
            override fun execute(src: Commander, args: CommandContext) {
                val ladder = args[rankLadderArg].join()
                val toRemove = args[subject]
                src.checkSubjectPermission(toRemove, "permissionsex.rank.remove." + ladder.name)
                val newLadder = ladder.removeRank(toRemove)
                if (newLadder === ladder) {
                    throw CommandException(
                        RANKING_REMOVE_ERROR_NOT_IN_LADDER(src.formatter.subject(toRemove), ladder.toComponent())
                    )
                } else {
                    pex.ladders.set(ladder.name, newLadder)
                        .thenMessageSubject(src) { send ->
                            send(RANKING_REMOVE_SUCCESS(+toRemove, ladder.toComponent()))
                        }
                }
            }
        })
    }
