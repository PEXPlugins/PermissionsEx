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
package ca.stellardrift.permissionsex.minecraft.command;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.permission.CommandPermission;
import net.kyori.adventure.text.Component;

/**
 * Provider for PermissionsEx commands.
 */
public final class PermissionsExCommand {

    private PermissionsExCommand() {
    }

    // PEX base command
    public static void pexCommand(final CommandRegistrationContext reg) {
        reg.register(reg.pex().handler(ctx -> {
            final Commander sender = ctx.getSender();
            final Component version = Component.text("v" + reg.manager().engine().version(), sender.formatter().highlightColor());
            sender.sendMessage(Component.text("PermissionsEx ").append(version));
            sender.sendMessage(Component.text("Run " + sender.formatter().transformCommand("/pex help") + "for more information"));
        }));
        reg.register(base -> help(base, reg.commandManager()), "help", "?");
    }

    // Direct literal subcommands

    private static Command.Builder<Commander> help(final Command.Builder<Commander> base, final CommandManager<Commander> mgr) {
        final CommandArgument<Commander, String> query = StringArgument.optional("query", StringArgument.StringMode.GREEDY);
        final MinecraftHelp<Commander> help = new MinecraftHelp<>(
                "/pex help",
                cmd -> cmd,
                mgr
        );

        return base.literal("help", "?")
                .argument(query)
                .permission(Permission.pex("help"))
                .handler(ctx -> help.queryCommands(ctx.getOrDefault(query, ctx.getSender().formatter().transformCommand("pex")), ctx.getSender()));
    }
}
