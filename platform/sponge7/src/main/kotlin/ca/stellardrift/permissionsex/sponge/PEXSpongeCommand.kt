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
package ca.stellardrift.permissionsex.sponge

import ca.stellardrift.permissionsex.impl.commands.parse.CommandException
import ca.stellardrift.permissionsex.impl.commands.parse.CommandSpec
import ca.stellardrift.permissionsex.util.optionally
import java.util.Optional
import org.spongepowered.api.command.CommandCallable
import org.spongepowered.api.command.CommandException as SpongeCommandException
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.text.Text
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.World

/**
 * Wrapper class between PEX commands and the Sponge command class
 */
internal class PEXSpongeCommand(private val command: CommandSpec, private val plugin: PermissionsExPlugin) : CommandCallable {

    @Throws(SpongeCommandException::class)
    override fun process(commandSource: CommandSource, arguments: String): CommandResult {
        this.command.process(SpongeCommander(this.plugin, commandSource), arguments)
        return CommandResult.success()
    }

    override fun testPermission(commandSource: CommandSource): Boolean {
        try {
            this.command.checkPermission(SpongeCommander(this.plugin, commandSource))
        } catch (e: CommandException) {
            return false
        }
        return true
    }

    override fun getShortDescription(commandSource: CommandSource): Optional<Text> {
        return this.command.description?.toSponge().optionally()
    }

    override fun getHelp(commandSource: CommandSource): Optional<Text> {
        return this.command.getExtendedDescription(SpongeCommander(plugin, commandSource)).toSponge().optionally()
    }

    override fun getUsage(commandSource: CommandSource): Text {
        return command.getUsage(SpongeCommander(plugin, commandSource)).toSponge()
    }

    @Throws(SpongeCommandException::class)
    override fun getSuggestions(commandSource: CommandSource, commandLine: String, targetPosition: Location<World>?): List<String> {
        return command.tabComplete(SpongeCommander(plugin, commandSource), commandLine)
    }
}
