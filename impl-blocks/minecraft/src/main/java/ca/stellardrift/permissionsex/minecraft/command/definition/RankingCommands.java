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
package ca.stellardrift.permissionsex.minecraft.command.definition;

import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.minecraft.command.ButtonType;
import ca.stellardrift.permissionsex.minecraft.command.CommandException;
import ca.stellardrift.permissionsex.minecraft.command.CommandRegistrationContext;
import ca.stellardrift.permissionsex.minecraft.command.Commander;
import ca.stellardrift.permissionsex.minecraft.command.Elements;
import ca.stellardrift.permissionsex.minecraft.command.Permission;
import ca.stellardrift.permissionsex.minecraft.command.argument.Parsers;
import ca.stellardrift.permissionsex.rank.RankLadder;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectType;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import cloud.commandframework.minecraft.extras.RichDescription;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import static ca.stellardrift.permissionsex.minecraft.command.Elements.*;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.TextComponent.ofChildren;

public final class RankingCommands {

    // All commands

    private static CommandArgument<Commander, RankLadder> ladderArgument(final boolean required) {
        final CommandArgument.Builder<Commander, RankLadder> ladder = CommandArgument.<Commander, RankLadder>ofType(RankLadder.class, "ladder")
            .withParser(Parsers.rankLadder());
        if (!required) {
            ladder.asOptionalWithDefault("default");
        }
        return ladder.build();
    }

    private static final CommandArgument<Commander, RankLadder> LADDER_ARG = ladderArgument(true);

    // Only for subcommands that modify
    private static final CommandFlag<Void> FLAG_RELATIVE = CommandFlag.newBuilder("relative")
        .withDescription(RichDescription.of(Messages.RANK_ARG_RELATIVE_DESCRIPTION))
        .withAliases("r")
        .build();

    private RankingCommands() {
    }

    static void register(final CommandRegistrationContext ctx) {
        // ctx.register(RankingCommands::list, "")
        ctx.push(ctx.head().argument(LADDER_ARG, RichDescription.of(Messages.RANK_ARG_LADDER_DESCRIPTION)), child -> {
            ctx.register(list(ctx.head()));
            ctx.register(RankingCommands::add, "add", "+");
            ctx.register(RankingCommands::remove, "remove", "rem", "delete", "del", "-");
        });
    }

    static Command.Builder<Commander> list(final Command.Builder<Commander> builder) {
        final Permission listPerm = Permission.pex("ranking.list");
        return builder
            .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Messages.RANK_LIST_DESCRIPTION.tr())
            .permission(listPerm)
            .handler(ctx -> {
                final Commander source = ctx.getSender();
                final RankLadder ladder = ctx.get(LADDER_ARG);
                final List<Component> ranksList = new ArrayList<>();
                final List<? extends SubjectRef<?>> rawRanks = ladder.ranks();

                // Build list
                if (rawRanks.size() == 0) {
                    throw new CommandException(Messages.RANKING_ERROR_EMPTY_LADDER.tr(ladder));
                } else if (rawRanks.size() == 1) {
                    ranksList.add(source.formatter().subject(rawRanks.get(0)));
                } else {
                    for (final ListIterator<? extends SubjectRef<?>> it = rawRanks.listIterator(rawRanks.size()); it.hasPrevious(); ) {
                        final int idx = it.previousIndex();
                        final SubjectRef<?> rank = it.previous();
                        final Component rendered;
                        if (idx == rawRanks.size()) { // first
                            rendered = ofChildren(
                                source.formatter().subject(rank),
                                moveDownButton(source, ladder, rank),
                                deleteButton(source, ladder, rank)
                            );
                        } else if (idx == 0) { // last
                            rendered = ofChildren(
                                source.formatter().subject(rank),
                                moveUpButton(source, ladder, rank),
                                deleteButton(source, ladder, rank)
                            );
                        } else { // in-between
                            rendered = ofChildren(
                                source.formatter().subject(rank),
                                moveDownButton(source, ladder, rank),
                                moveUpButton(source, ladder, rank),
                                deleteButton(source, ladder, rank)
                            );
                        }
                        ranksList.add(rendered);
                    }
                }

                // Send as a pagination
                source.sendPaginated(
                    Messages.RANKING_PAGINATION_HEADER.tr(
                        ladder.name(),
                        source.formatter().button(
                            text().content("+"),
                            ButtonType.POSITIVE,
                            Messages.RANKING_BUTTON_ADD_DESCRIPTION,
                            "/pex rank " + ladder.name() + " add ",
                            false
                        )
                    ),
                    Messages.RANKING_PAGINATION_SUBTITLE,
                    ranksList
                );
            });
    }

    private static Component deleteButton(
        final Commander cmd,
        final RankLadder rank,
        final SubjectRef<?> subject
    ) {
        return cmd.formatter().button(
            text().content("-"),
            ButtonType.NEGATIVE,
            Messages.RANKING_BUTTON_DELETE_DESCRIPTION.tr(),
            "/pex rank " + rank.name() + " remove " + subject.type().name() + " " + subject.serializedIdentifier(),
            true
        );
    }

    private static Component moveDownButton(
        final Commander cmd,
        final RankLadder rank,
        final SubjectRef<?> subject
    ) {
        return cmd.formatter().button(
            text().content("▼"),
            ButtonType.NEUTRAL,
            Messages.RANKING_BUTTON_MOVE_DOWN_DESCRIPTION.tr(),
            "/pex rank " + rank.name() + " add " + subject.type().name() + " " + subject.serializedIdentifier() + " -r -1",
            true
        );
    }

    private static Component moveUpButton(
        final Commander cmd,
        final RankLadder ladder,
        final SubjectRef<?> subject
    ) {
        return cmd.formatter().button(
            text().content("▲"),
            ButtonType.NEUTRAL,
            Messages.RANKING_BUTTON_MOVE_UP_DESCRIPTION.tr(),
            "/pex rank " + ladder.name() + " add " + subject.type().name() + " " + subject.serializedIdentifier() + " -r 1",
            true
        );
    }

    // Other subcommands

    private static CommandArgument<Commander, SubjectType<?>> rankTypeArgument() {
        return CommandArgument.<Commander, SubjectType<?>>ofType(new TypeToken<SubjectType<?>>() {}, "type")
            .withParser(Parsers.subjectType())
            .build();
    }

    private static CommandArgument<Commander, ?> rankIdentifierArgument(final CommandArgument<Commander, SubjectType<?>> parentType) {
        return CommandArgument.<Commander, Object>ofType(Object.class, "identifier")
            .withParser(Parsers.subjectIdentifier(data -> data.get(parentType)))
            .build();
    }


    static Command.Builder<Commander> add(final Command.Builder<Commander> builder) {
        final Permission perm = Permission.pex("rank.add");
        final CommandArgument<Commander, SubjectType<?>> rankTypeArg = rankTypeArgument();
        final CommandArgument<Commander, ?> rankIdentifierArg = rankIdentifierArgument(rankTypeArg);
        final CommandArgument<Commander, Integer> positionArg = IntegerArgument.optional("position");
        final SubjectRefProvider rankProvider = SubjectRefProvider.of(rankTypeArg, rankIdentifierArg);

        return builder
            .flag(FLAG_RELATIVE)
            .argument(rankTypeArg)
            .argument(rankIdentifierArg)
            .argument(positionArg)
            .permission(perm)
            .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Messages.RANK_ADD_DESCRIPTION.tr())
            .handler(handler((source, engine, ctx) -> {
                final RankLadder ladder = ctx.get(LADDER_ARG);
                source.checkPermission(perm.then(ladder.name()));
                final SubjectRef<?> toAdd = rankProvider.provide(ctx);
                final @Nullable Integer position = ctx.getOrDefault(positionArg, null);

                if (position != null) {
                    int addPosition = position;
                    if (ctx.flags().isPresent(FLAG_RELATIVE.getName())) {
                        final int currentIndex = ladder.indexOf(toAdd);
                        if (currentIndex == -1) {
                            throw new CommandException(Messages.RANKING_ADD_ERROR_RELATIVE_ON_OUTSIDE_SUBJECT.tr());
                        }
                        addPosition = (currentIndex + addPosition > 1) ? addPosition + 1 : addPosition; // If we are adding to later, we need to add after the next rank (otherwise we end up staying in the same place)
                    }
                    engine.ladders().set(ladder.name(), ladder.with(toAdd, addPosition))
                        .whenComplete(messageSender(source, Messages.RANKING_ADD_SUCCESS_POSITION.tr(
                            source.formatter().subject(toAdd),
                            ladder.asComponent(),
                            text(addPosition)
                        )));
                } else {
                    // Append
                    engine.ladders().set(ladder.name(), ladder.with(toAdd))
                        .whenComplete(messageSender(source, Messages.RANKING_ADD_SUCCESS.tr(
                            source.formatter().subject(toAdd),
                            ladder.asComponent()
                        )));
                }
            }));
    }

    static Command.Builder<Commander> remove(final Command.Builder<Commander> builder) {
        final Permission perm = Permission.pex("rank.remove");
        final CommandArgument<Commander, SubjectType<?>> rankTypeArg = rankTypeArgument();
        final CommandArgument<Commander, ?> rankIdentifierArg = rankIdentifierArgument(rankTypeArg);
        final SubjectRefProvider rankProvider = SubjectRefProvider.of(rankTypeArg, rankIdentifierArg);

        return builder
            .argument(rankTypeArg)
            .argument(rankIdentifierArg)
            .permission(perm)
            .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Messages.RANK_REMOVE_DESCRIPTION.tr())
            .handler(handler((source, engine, ctx) -> {
                final RankLadder ladder = ctx.get(LADDER_ARG);
                source.checkPermission(perm.then(ladder.name()));
                final SubjectRef<?> toRemove = rankProvider.provide(ctx);
                final RankLadder newLadder = ladder.without(toRemove);

                if (newLadder == ladder) {
                    throw new CommandException(Messages.RANKING_REMOVE_ERROR_NOT_IN_LADDER.tr(
                        source.formatter().subject(toRemove),
                        ladder.asComponent()
                    ));
                } else {
                    engine.ladders().set(ladder.name(), newLadder)
                        .whenComplete(messageSender(source, Messages.RANKING_REMOVE_SUCCESS.tr(
                            source.formatter().subject(toRemove),
                            ladder.asComponent()
                        )));
                }
            }));
    }

    public static Command.Builder<Commander> promote(final Command.Builder<Commander> builder) {
        final Permission promote = Permission.pex("promote");
        final CommandArgument<Commander, SubjectType<?>> rankTypeArg = rankTypeArgument();
        final CommandArgument<Commander, ?> rankIdentifierArg = rankIdentifierArgument(rankTypeArg);
        final CommandArgument<Commander, RankLadder> ladderArg = ladderArgument(false);
        final SubjectRefProvider rankProvider = SubjectRefProvider.of(rankTypeArg, rankIdentifierArg);

        return builder
            .flag(FLAG_TRANSIENT)
            .flag(FLAG_CONTEXT)
            .argument(rankTypeArg)
            .argument(rankIdentifierArg)
            .argument(ladderArg)
            .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Messages.RANK_PROMOTE_DESCRIPTION.tr())
            .permission(promote)
            .handler(handler((source, engine, ctx) -> {
                final RankLadder ladder = ctx.get(ladderArg);
                final Set<ContextValue<?>> contexts = Elements.contexts(ctx);
                final SubjectRef.ToData<?> subject = rankProvider.provideData(ctx, promote);
                subject.update(data -> ladder.promote(contexts, data))
                    .thenAccept(change -> {
                        if (!change.changed()) {
                            throw new CommandException(Messages.PROMOTE_ERROR_ALREADY_AT_TOP.tr(
                                source.formatter().subject(subject),
                                ladder.asComponent()
                            ));
                        }
                    }).whenComplete(messageSender(source, Messages.PROMOTE_SUCCESS.tr(
                    source.formatter().subject(subject),
                    ladder.asComponent().style(source.formatter()::hl)
                )));
            }));
    }

    public static Command.Builder<Commander> demote(final Command.Builder<Commander> builder) {
        final Permission promote = Permission.pex("demote");
        final CommandArgument<Commander, SubjectType<?>> rankTypeArg = rankTypeArgument();
        final CommandArgument<Commander, ?> rankIdentifierArg = rankIdentifierArgument(rankTypeArg);
        final CommandArgument<Commander, RankLadder> ladderArg = ladderArgument(false);
        final SubjectRefProvider rankProvider = SubjectRefProvider.of(rankTypeArg, rankIdentifierArg);

        return builder
            .flag(FLAG_TRANSIENT)
            .flag(FLAG_CONTEXT)
            .argument(rankTypeArg)
            .argument(rankIdentifierArg)
            .argument(ladderArg)
            .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Messages.RANK_DEMOTE_DESCRIPTION.tr())
            .permission(promote)
            .handler(handler((source, engine, ctx) -> {
                final RankLadder ladder = ctx.get(ladderArg);
                final Set<ContextValue<?>> contexts = Elements.contexts(ctx);
                final SubjectRef.ToData<?> subject = rankProvider.provideData(ctx, promote);
                subject.update(data -> ladder.promote(contexts, data))
                    .thenAccept(change -> {
                        if (!change.changed()) {
                            throw new CommandException(Messages.DEMOTE_ERROR_NOT_ON_LADDER.tr(
                                source.formatter().subject(subject),
                                ladder.asComponent()
                            ));
                        }
                    }).whenComplete(messageSender(source, Messages.DEMOTE_SUCCESS.tr(
                    source.formatter().subject(subject),
                    ladder.asComponent().style(source.formatter()::hl)
                )));
            }));
    }

}
