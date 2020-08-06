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
import ca.stellardrift.permissionsex.commands.parse.command
import ca.stellardrift.permissionsex.config.PermissionsExConfiguration
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CommandSpecTest : PermissionsExTest() {
    @Test
    fun testNoArgsFunctional() {
        command("something") {
            executor { _, _ -> }
        }.process(TestCommander(manager), "")
    }

    @Test
    fun testExecutorRequired() {
        assertThrows(IllegalArgumentException::class.java,
            {
                command("something") {}
            },
            "An executor is required"
        )
    }

    @Test
    fun testAliasesRequired() {
        assertThrows(
            IllegalArgumentException::class.java,
            {
                command {
                    executor { _, _ -> }
                }
            },
            "A command may not have no aliases"
        )
    }

    override fun populate(): PermissionsExConfiguration<*> {
        return EmptyTestConfiguration()
    }
}
