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
package ca.stellardrift.permissionsex.sponge.command;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.exceptions.ArgumentParseException;
import cloud.commandframework.exceptions.CommandExecutionException;
import cloud.commandframework.exceptions.InvalidCommandSenderException;
import cloud.commandframework.exceptions.InvalidSyntaxException;
import cloud.commandframework.exceptions.NoPermissionException;
import cloud.commandframework.exceptions.NoSuchCommandException;
import cloud.commandframework.meta.CommandMeta;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

final class CloudCommandCallable<C> implements CommandCallable {

    private static final Text MESSAGE_INTERNAL_ERROR = Text.of(TextColors.RED,
        "An internal error occurred while attempting to perform this command.");
    private static final Text MESSAGE_NO_PERMS = Text.of(TextColors.RED,
        "I'm sorry, but you do not have permission to perform this command. "
            + "Please contact the server administrators if you believe that this is in error.");
    private static final Text MESSAGE_UNKNOWN_COMMAND = Text.of("Unknown command. Type \"/help\" for help.");

    private final CommandArgument<?, ?> command;
    private final Command<C> cloudCommand;
    private final SpongeApi7CommandManager<C> manager;

    CloudCommandCallable(
        final CommandArgument<?, ?> command,
        final Command<C> cloudCommand,
        final SpongeApi7CommandManager<C> manager
    ) {
        this.command = command;
        this.cloudCommand = cloudCommand;
        this.manager = manager;
    }

    @Override
    public CommandResult process(final CommandSource source, final String arguments) {
        final C cloudSender = this.manager.getCommandSourceMapper().apply(source);
        this.manager.executeCommand(cloudSender, this.command.getName() + " " + arguments)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    if (throwable instanceof CompletionException) {
                        throwable = throwable.getCause();
                    }
                    final Throwable finalThrowable = throwable;
                    if (throwable instanceof InvalidSyntaxException) {
                        this.manager.handleException(cloudSender,
                            InvalidSyntaxException.class,
                            (InvalidSyntaxException) throwable, (c, e) ->
                                source.sendMessage(Text.of(TextColors.RED,
                                    "Invalid Command Syntax. Correct command syntax is: ",
                                    Text.of(TextColors.GRAY, ((InvalidSyntaxException) finalThrowable).getCorrectSyntax())))
                        );
                    } else if (throwable instanceof InvalidCommandSenderException) {
                        this.manager.handleException(cloudSender,
                            InvalidCommandSenderException.class,
                            (InvalidCommandSenderException) throwable, (c, e) ->
                                source.sendMessage(Text.of(TextColors.RED, finalThrowable.getMessage()))
                        );
                    } else if (throwable instanceof NoPermissionException) {
                        this.manager.handleException(cloudSender,
                            NoPermissionException.class,
                            (NoPermissionException) throwable, (c, e) ->
                                source.sendMessage(MESSAGE_NO_PERMS)
                        );
                    } else if (throwable instanceof NoSuchCommandException) {
                        this.manager.handleException(cloudSender,
                            NoSuchCommandException.class,
                            (NoSuchCommandException) throwable, (c, e) ->
                                source.sendMessage(MESSAGE_UNKNOWN_COMMAND)
                        );
                    } else if (throwable instanceof ArgumentParseException) {
                        this.manager.handleException(cloudSender,
                            ArgumentParseException.class,
                            (ArgumentParseException) throwable, (c, e) ->
                                source.sendMessage(Text.of("Invalid Command Argument: ",
                                    Text.of(TextColors.GRAY,
                                        finalThrowable.getCause().getMessage())))
                        );
                    } else if (throwable instanceof CommandExecutionException) {
                        this.manager.handleException(cloudSender,
                            CommandExecutionException.class,
                            (CommandExecutionException) throwable, (c, e) -> {
                                source.sendMessage(MESSAGE_INTERNAL_ERROR);
                                this.manager.getOwningPlugin().getLogger().error(
                                    "Exception executing command handler",
                                    finalThrowable.getCause()
                                );
                            }
                        );
                    } else {
                        source.sendMessage(MESSAGE_INTERNAL_ERROR);
                        this.manager.getOwningPlugin().getLogger().error(
                            "An unhandled exception was thrown during command execution",
                            throwable
                        );
                    }
                }
            });
        return CommandResult.success();
    }

    @Override
    public List<String> getSuggestions(final CommandSource source, final String arguments, final @Nullable Location<World> targetPosition) {
        return this.manager.suggest(this.manager.getCommandSourceMapper().apply(source), this.command.getName() + " " + arguments);
    }

    @Override
    public boolean testPermission(final CommandSource source) {
        // TODO: We check if any permissions match, need to have some sort of CommandPermission.test(predicate);
        return source.hasPermission(cloudCommand.getCommandPermission().toString());
    }

    @Override
    public Optional<Text> getShortDescription(final CommandSource source) {
        final Optional<Text> richDesc = this.cloudCommand.getCommandMeta().get(SpongeApi7MetaKeys.RICH_DESCRIPTION);
        if (richDesc.isPresent()) {
            return richDesc;
        }

        return this.cloudCommand.getCommandMeta().get(CommandMeta.DESCRIPTION).map(Text::of);
    }

    @Override
    public Optional<Text> getHelp(final CommandSource source) {
        final Optional<Text> possible = this.cloudCommand.getCommandMeta().get(CommandMeta.LONG_DESCRIPTION).map(Text::of);
        if (!possible.isPresent()) {
            return this.cloudCommand.getCommandMeta().get(CommandMeta.DESCRIPTION).map(Text::of);
        }
        return possible;
    }

    @Override
    public Text getUsage(final CommandSource source) {
        return Text.of(this.manager.getCommandSyntaxFormatter().apply(
            this.cloudCommand.getArguments(),
            this.manager.getCommandTree().getNamedNode(this.command.getName())
        ));
    }

}
