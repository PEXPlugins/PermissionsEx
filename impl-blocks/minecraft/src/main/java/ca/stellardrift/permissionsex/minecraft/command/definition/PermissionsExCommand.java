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

import ca.stellardrift.permissionsex.datastore.ConversionResult;
import ca.stellardrift.permissionsex.impl.PermissionsEx;
import ca.stellardrift.permissionsex.impl.util.PCollections;
import ca.stellardrift.permissionsex.minecraft.MinecraftPermissionsEx;
import ca.stellardrift.permissionsex.minecraft.command.CommandException;
import ca.stellardrift.permissionsex.minecraft.command.CommandRegistrationContext;
import ca.stellardrift.permissionsex.minecraft.command.Commander;
import ca.stellardrift.permissionsex.minecraft.command.Elements;
import ca.stellardrift.permissionsex.minecraft.command.Formats;
import ca.stellardrift.permissionsex.minecraft.command.MessageFormatter;
import ca.stellardrift.permissionsex.minecraft.command.PEXCommandPreprocessor;
import ca.stellardrift.permissionsex.minecraft.command.Permission;
import ca.stellardrift.permissionsex.minecraft.command.argument.Parsers;
import ca.stellardrift.permissionsex.subject.SubjectDataCache;
import ca.stellardrift.permissionsex.subject.SubjectRef;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.subject.SubjectTypeCollection;
import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ca.stellardrift.permissionsex.minecraft.command.Elements.*;
import static net.kyori.adventure.text.Component.text;

/**
 * Provider for PermissionsEx commands.
 */
public final class PermissionsExCommand {

    private PermissionsExCommand() {
    }

    // PEX base command
    public static void register(final CommandRegistrationContext regCtx) {
        final Command<Commander> helpCommand = regCtx.register(base -> help(base, regCtx.commandManager(), regCtx.manager().messageFormatter()), "help", "?");

        regCtx.register(regCtx.head().handler(ctx -> {
            final Commander sender = ctx.getSender();
            final Component version = text(
                "v" + regCtx.manager().engine().version(),
                sender.formatter().highlightColor()
            );

            sender.sendMessage(text("PermissionsEx ").append(version));
            sender.sendMessage(text("Run " + Formats.formatCommand(helpCommand, PCollections.map()) + "for more information"));
        }));

        // Subcommands without arguments
        regCtx.register(PermissionsExCommand::debug, "debug", "d");
        regCtx.push(RankingCommands::register, "ranking", "rank");
        regCtx.register(PermissionsExCommand::commandImport, "import");
        regCtx.register(PermissionsExCommand::reload, "reload", "rel");
        regCtx.register(PermissionsExCommand::version, "version");

        // legacy commands
        regCtx.register(debug(regCtx.head().literal("toggle").literal("debug")));

        // <type> list
        final CommandArgument<Commander, SubjectType<?>> typeArg = CommandArgument.<Commander, SubjectType<?>>ofType(new TypeToken<SubjectType<?>>() {}, "type")
            .withParser(Parsers.subjectType())
            .build();

        final Command.Builder<Commander> typeBuilder = regCtx.head()
            .argument(typeArg);

        regCtx.register(list(typeBuilder.literal("list"), typeArg));

        // <type> <name>...
        // this is where things start to get hairy
        final CommandArgument<Commander, ?> identifierArg = CommandArgument.<Commander, Object>ofType(Object.class, "subject")
            .withParser(Parsers.subjectIdentifier(data -> data.get(typeArg)))
            .build();

            /* final CommandArgument<Commander, SubjectRef<?>> subjectRef = new ArgumentPair<Commander, SubjectType, Object, SubjectRef<?>>(
                true,
                "subject",
                Pair.of("type", "identifier"),
                Pair.of(SubjectType.class, Object.class),
                Pair.of(typeArg.getParser(), Parsers.subjectIdentifier(data -> (SubjectType) data.get(typeArg.getName()))),
                (cmd, pair) -> {
                    return SubjectRef.subject(pair.getFirst(), pair.getSecond());
                },
                new TypeToken<SubjectRef<?>>() {}
            ) {}; */

        regCtx.push(typeBuilder.argument(identifierArg), child -> {
            final SubjectRefProvider refArg = SubjectRefProvider.of(typeArg, identifierArg);
            child.register(build -> InfoSubcommand.register(build, refArg), "info", "i");

            child.push(child.head().flag(FLAG_TRANSIENT).flag(FLAG_CONTEXT), grandchild -> {
                grandchild.register(build -> DeleteSubcommand.register(build, refArg), "delete", "del", "remove", "rem", "rm");
                grandchild.push(build -> ParentSubcommand.register(build, refArg), "parents", "parent", "par");
                grandchild.register(build -> OptionSubcommand.register(build, refArg), "option", "options", "opt", "o", "meta");
                grandchild.register(build -> PermissionsSubcommands.permission(build, refArg), "permission", "permissions", "perm", "perms", "p");
                grandchild.register(build -> PermissionsSubcommands.permissionDefault(build, refArg), "permission-default", "perms-def", "permsdef", "pd", "default", "def");
            });
        });
    }

    // Direct literal subcommands

    private static Command.Builder<Commander> help(final Command.Builder<Commander> base, final CommandManager<Commander> mgr, final MessageFormatter formatter) {
        final CommandArgument<Commander, String> query = StringArgument.optional("query", StringArgument.StringMode.GREEDY);
        final Command.Builder<Commander> helpCommand = base
            .meta(CommandMeta.DESCRIPTION, "Get help for PermissionsEx")
            .argument(query)
            .permission(Permission.pex("help"));

        final MinecraftHelp<Commander> help = new MinecraftHelp<>(
                Formats.formatCommand(helpCommand, PCollections.map()),
                cmd -> cmd,
                mgr
        );

        help.setHelpColors(MinecraftHelp.HelpColors.of(
            formatter.responseColor(),
            formatter.highlightColor(),
            Formats.lerp(0.2f, formatter.highlightColor(), NamedTextColor.BLACK),
            NamedTextColor.GRAY,
            NamedTextColor.DARK_GRAY
        ));

        return helpCommand
                .handler(ctx -> help.queryCommands(ctx.getOrDefault(query, ""), ctx.getSender()));
    }

    private static Command.Builder<Commander> debug(final Command.Builder<Commander> base) {
        final CommandArgument<Commander, Pattern> filterArg = CommandArgument.<Commander, Pattern>ofType(Pattern.class, "pattern")
            .asOptional()
            .withParser(Parsers.greedyPattern())
            .build();

        return base
            .meta(CommandMeta.DESCRIPTION, Messages.DEBUG_DESCRIPTION.key()) // TODO
            .permission(Permission.pex("debug"))
            .argument(filterArg)
            .handler(handler((source, engine, ctx) -> {

                final boolean debugEnabled = !engine.debugMode();
                final @Nullable Pattern filter = ctx.contains(filterArg.getName()) ? ctx.get(filterArg) : null;

                if (filter != null) {
                    engine.debugMode(debugEnabled, filter);
                    source.sendMessage(
                        Messages.DEBUG_SUCCESS_FILTER.tr(
                            Formats.bool(debugEnabled),
                            source.formatter().hl(text().content(filter.pattern()))
                        )
                    );
                } else {
                    engine.debugMode(debugEnabled);
                    source.sendMessage(Messages.DEBUG_SUCCESS.tr(Formats.bool(debugEnabled)));
                }
            }));
    }

    private static Command.Builder<Commander> commandImport(final Command.Builder<Commander> base) {
        final CommandArgument<Commander, String> dataStoreArg = StringArgument.<Commander>newBuilder("data store")
            .withSuggestionsProvider((ctx, input) -> {
                final PermissionsEx<?> engine = ctx.get(PEXCommandPreprocessor.PEX_ENGINE);
                return PCollections.asVector(engine.getAvailableConversions(), conv -> conv.store().identifier());
                // TODO: include data store names here
            })
            .asOptional()
            .build();

        return base
            .meta(CommandMeta.DESCRIPTION, "Import data from another store")
            .argument(dataStoreArg)
            .permission(Permission.pex("import"))
            .handler(ctx -> {
                final Commander source = ctx.getSender();
                final PermissionsEx<?> engine = ctx.get(PEXCommandPreprocessor.PEX_ENGINE);
                final @Nullable String requestedName = ctx.contains(dataStoreArg.getName()) ? ctx.get(dataStoreArg) : null;
                if (requestedName == null) {
                    /* list available conversion actions */
                    source.sendPaginated(
                        Messages.IMPORT_LISTING_HEADER.tr(),
                        Messages.IMPORT_LISTING_SUBTITLE.tr(source.formatter().hl(text().content("/pex import [id]"))),
                        engine.getAvailableConversions().stream().map(conv ->
                            source.callback(text()
                                .append(conv.description())
                                .append(text(" - /pex import "))
                                .append(text(conv.store().identifier())), src -> {
                                src.sendMessage(Messages.IMPORT_ACTION_BEGINNING.tr(conv.description()));
                                engine.importDataFrom(conv)
                                    .whenComplete(messageSender(src, Messages.IMPORT_ACTION_SUCCESS.tr(conv.description())));
                            })
                        )
                    );
                } else {
                    /* execute a specific import action */
                    for (final ConversionResult result : engine.getAvailableConversions()) {
                        if (result.store().identifier().equalsIgnoreCase(requestedName)) {
                            source.sendMessage(Messages.IMPORT_ACTION_BEGINNING.tr(result.description()));
                            engine.importDataFrom(result)
                                .whenComplete(messageSender(source, Messages.IMPORT_ACTION_SUCCESS.tr(result.description())));
                            return;
                        }
                    }

                    if (engine.config().getDataStore(requestedName) == null) {
                        throw new CommandException(Messages.IMPORT_ERROR_UNKNOWN_STORE.tr(requestedName));
                    }
                    source.sendMessage(Messages.IMPORT_ACTION_BEGINNING.tr(requestedName));
                    engine.importDataFrom(requestedName)
                        .whenComplete(messageSender(source, Messages.IMPORT_ACTION_SUCCESS.tr(requestedName)));
                }
            });
    }

    private static Command.Builder<Commander> reload(final Command.Builder<Commander> base) {
        return base
            .meta(CommandMeta.DESCRIPTION, "Reload PermissionsEx")
            .permission(Permission.pex("reload"))
            .handler(ctx -> {
                ctx.getSender().sendMessage(Messages.RELOAD_ACTION_BEGIN.tr());
                ctx.<PermissionsEx<?>>get(PEXCommandPreprocessor.PEX_ENGINE).reload()
                    .whenComplete(messageSender(ctx.getSender(), Messages.RELOAD_ACTION_SUCCESS.tr()));
            });
    }

    private static Command.Builder<Commander> version(final Command.Builder<Commander> base) {
        final CommandFlag<Void> verboseFlag = CommandFlag.newBuilder("verbose").withAliases("v").build();
        return base
            .meta(CommandMeta.DESCRIPTION, "Get details about the currently running PermissionsEx version")
            .permission(Permission.pex("version"))
            .flag(verboseFlag)
            .handler(ctx -> {
                final boolean verbose = ctx.flags().isPresent(verboseFlag.getName());
                ctx.<MinecraftPermissionsEx<?>>get(PEXCommandPreprocessor.PEX_MANAGER)
                    .describe(ctx.getSender(), verbose);
            });
    }

    private static Command.Builder<Commander> list(final Command.Builder<Commander> base, final CommandArgument<Commander, SubjectType<?>> subjectTypeArg) {
        final CommandArgument<Commander, String> filterArg = StringArgument.optional("filter");
        final Permission basePerm = Permission.pex("list");
        return base
            .meta(CommandMeta.DESCRIPTION, "List all subjects of a certain type")
            // .permission(basePerm) // TODO: Allow prefix permissions
            .argument(filterArg)
            .flag(Elements.FLAG_TRANSIENT)
            .handler(handler((source, engine, ctx) -> {
                final SubjectType<?> type = ctx.get(subjectTypeArg);
                source.checkPermission(basePerm.then(type.name()));
                printList(
                    source,
                    engine.subjects(type),
                    ctx.flags().isPresent(Elements.FLAG_TRANSIENT.getName()),
                    ctx.contains(filterArg.getName()) ? ctx.get(filterArg) : null
                );
            }));
    }

    private static <I> void printList(
        final Commander source,
        final SubjectTypeCollection<I> collection,
        final boolean transientData,
        final @Nullable String filter
    ) {
        final SubjectDataCache<I> data;
        if (transientData) {
            data = collection.transientData();
        } else {
            data = collection.persistentData();
        }

        Stream<SubjectRef<I>> identifiers = data.getAllIdentifiers()
            .map(id -> SubjectRef.subject(collection.type(), id));

        if (filter != null && !filter.isEmpty()) {
            final String lowerFilter = filter.toLowerCase(Locale.ROOT);
            identifiers = identifiers.filter(it -> it.serializedIdentifier()
                .toLowerCase(Locale.ROOT)
                .startsWith(lowerFilter));
        }


        source.sendPaginated(
            Messages.PEX_LIST_HEADER.tr(collection.type().name()),
            Messages.PEX_LIST_SUBTITLE.tr(collection.type().name()),
            identifiers.map(source.formatter()::subject)
        );
    }
}
