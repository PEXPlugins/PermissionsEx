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
package ca.stellardrift.permissionsex.fabric.impl

import ca.stellardrift.permissionsex.fabric.IdentifierContextDefinition
import ca.stellardrift.permissionsex.subject.CalculatedSubject
import java.util.function.Consumer
import net.minecraft.server.function.CommandFunctionManager
import net.minecraft.util.Identifier

/**
 * Types related to applying permissions data for MC functions.
 *
 * PEX injects into function execution in three ways:
 *
 * 1. Functions in the tags `#minecraft:tick` and `#minecraft:init` are executed with subjects specific to those tags
 * 2. The currently executing functions are exposed as contexts in permission resolution.
 * 3. When loading functions, we apply a function-specific subject as well
 *
 * The first is fairly simple -- we just capture the calls to all functions in the tags in CommandFunctionManager#tick
 *
 * The second is a bit more tricky. Mojang uses a depth-first queue to store function entries. We
 * can use this to tell when we enter into a function, but it cannot tell us when we exit a function. To resolve that
 * issue, we inject our own sentinel [Entry] instances that pop the currently executing function from the stack.
 *
 * To ensure this stack doesn't end with an invalid state due to an exception being thrown, we also
 * clear the stack at the end of a top-level execution (in [CommandFunctionManager]).
 */

object PopExecutingFunctionEntry : CommandFunctionManager.Entry(null, null, null) {
    override fun execute(stack: java.util.ArrayDeque<CommandFunctionManager.Entry>?, maxChainLength: Int) {
        FunctionContextDefinition.currentFunctions.get().removeLast()
        // no-op
    }

    override fun toString(): String {
        return "<PEX sentinel>"
    }
}

/**
 * A context populating with every currently executing function.
 */
object FunctionContextDefinition : IdentifierContextDefinition("function") {
    internal val currentFunctions = ThreadLocal.withInitial { ArrayDeque<Identifier>() }
    override fun accumulateCurrentValues(subject: CalculatedSubject, consumer: Consumer<Identifier>) {
        currentFunctions.get().forEach(consumer)
    }
}
