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
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for child commands
 */
class ChildCommandsTest : PermissionsExTest() {
    @Test
    fun testSimpleChildCommand() {
        val childExecuted = AtomicBoolean()
        command("parent") {
            children {
                child("child") {
                    executor { src, ctx -> childExecuted.set(true) }
                }
            }
        }.process(TestCommander(manager), "child")
        assertTrue(childExecuted.get())
    }

    override fun populate(): PermissionsExConfiguration<*> {
        return EmptyTestConfiguration()
    }
}
