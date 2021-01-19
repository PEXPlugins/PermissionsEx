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
package ca.stellardrift.permissionsex.fabric.impl;

import ca.stellardrift.permissionsex.fabric.impl.context.IdentifierContextDefinition;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.util.Identifier;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

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
public final class Functions {

    private Functions() {
    }

    public static final class PopExecutingEntry extends CommandFunctionManager.Entry {
        public static final PopExecutingEntry INSTANCE = new PopExecutingEntry();

        private PopExecutingEntry() {
            super(null, null, null);
        }

        @Override
        public void execute(final ArrayDeque<CommandFunctionManager.Entry> stack, final int maxChainLength) {
            Context.currentFunctions().removeLast();
        }

        @Override
        public String toString() {
            return "<PEX context sentinel>";
        }

    }

    /**
     * A context populating with every currently executing function.
     */
    public static final class Context extends IdentifierContextDefinition {
        public static final Context INSTANCE = new Context();
        private static final ThreadLocal<Deque<Identifier>> CURRENT_FUNCTIONS = ThreadLocal.withInitial(ArrayDeque::new);

        private Context() {
            super("function");
        }

        public static Deque<Identifier> currentFunctions() {
            return CURRENT_FUNCTIONS.get();
        }

        @Override
        public void accumulateCurrentValues(
            final CalculatedSubject subject,
            final Consumer<Identifier> consumer
        ) {
            CURRENT_FUNCTIONS.get().forEach(consumer);
        }

    }

}
