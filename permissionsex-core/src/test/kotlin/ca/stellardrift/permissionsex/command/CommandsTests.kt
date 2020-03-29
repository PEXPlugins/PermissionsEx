package ca.stellardrift.permissionsex.command

import ca.stellardrift.permissionsex.commands.parse.Permission
import ca.stellardrift.permissionsex.commands.parse.command
import ca.stellardrift.permissionsex.commands.parse.string
import kotlin.test.Test

class CommandsTests {
    @Test
    fun definitionTest() {
        val cmd = command("me", "emote") {
            permission = Permission.forPex("me")
            val player = string() key "player"

        }
        cmd.execute()
    }
}
