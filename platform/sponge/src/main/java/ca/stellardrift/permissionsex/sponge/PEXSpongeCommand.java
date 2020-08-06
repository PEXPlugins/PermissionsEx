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

package ca.stellardrift.permissionsex.sponge;

import ca.stellardrift.permissionsex.commands.parse.CommandSpec;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Wrapper class between PEX commands and the Sponge command class
 */
class PEXSpongeCommand implements CommandCallable {
    private final CommandSpec command;
    private final PermissionsExPlugin plugin;

    public PEXSpongeCommand(CommandSpec command, PermissionsExPlugin plugin) {
        this.command = command;
        this.plugin = plugin;
    }

    @Override
    public CommandResult process(CommandSource commandSource, String arguments) throws CommandException {
        command.process(new SpongeCommander(plugin, commandSource), arguments);
        return CommandResult.success();
    }

    @Override
    public boolean testPermission(CommandSource commandSource) {
        try {
            command.checkPermission(new SpongeCommander(plugin, commandSource));
        } catch (ca.stellardrift.permissionsex.commands.parse.CommandException e) {
            return false;
        }
        return true;
    }

    private static Text toText(Component comp) {
        return TextSerializers.JSON.deserialize(GsonComponentSerializer.INSTANCE.serialize(comp));
    }

    @Override
    public Optional<Text> getShortDescription(CommandSource commandSource) {
        @Nullable Component desc = command.getDescription();
        if (desc == null) {
            return Optional.empty();
        }
        return Optional.of(toText(desc));
    }

    @Override
    public Optional<Text> getHelp(CommandSource commandSource) {
        return Optional.of(toText(command.getExtendedDescription(new SpongeCommander(plugin, commandSource))));
    }

    @Override
    public Text getUsage(CommandSource commandSource) {
        return toText(command.getUsage(new SpongeCommander(plugin, commandSource)));
    }

    @Override
    public List<String> getSuggestions(CommandSource commandSource, String commandLine, @Nullable Location<World> targetPosition) throws CommandException {
        return command.tabComplete(new SpongeCommander(plugin, commandSource), commandLine);
    }
}
