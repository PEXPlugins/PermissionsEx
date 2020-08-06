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

package ca.stellardrift.permissionsex.util.command

import ca.stellardrift.permissionsex.EmptyTestConfiguration
import ca.stellardrift.permissionsex.PermissionsExTest
import ca.stellardrift.permissionsex.commands.parse.StructuralArguments.flags
import ca.stellardrift.permissionsex.commands.parse.command
import ca.stellardrift.permissionsex.commands.parse.int
import ca.stellardrift.permissionsex.commands.parse.string
import ca.stellardrift.permissionsex.config.PermissionsExConfiguration
import ca.stellardrift.permissionsex.util.unaryPlus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Test for command flags
 */
class CommandFlagsTest : PermissionsExTest() {
    @Test
    fun testFlaggedCommand() {
        val command = command("pex") {
            args = flags()
                .flag("a")
                .valueFlag(int() key +"quot", "q")
                .buildWith(string() key +"key")
            executor { _, args ->
                assertEquals(true, args.getOne("a"))
                assertEquals(42, args.getOne<Int>("quot"))
                assertEquals("something", args.getOne("key"))
            }
        }

        val cmd = TestCommander(manager)
        command.process(cmd, "-a -q 42 something")
        command.process(cmd, "-aq 42 something")
        command.process(cmd, "-a something -q 42")
    }

    override fun populate(): PermissionsExConfiguration<*> {
        return EmptyTestConfiguration()
    }
}
