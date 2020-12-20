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
package ca.stellardrift.permissionsex.context;

import ca.stellardrift.permissionsex.subject.CalculatedSubject;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Context definition for types that have no complex data.
 */
public class SimpleContextDefinition extends ContextDefinition<String> {

    /**
     * Create a basic context definition with the provided key and accumulation function.
     *
     * @param name context name
     * @param valueAccumulator a callback taking a subject and a value accumulator
     * @return a new context definition
     * @see ContextDefinitionProvider#registerContextDefinition(ContextDefinition) to add this
     *       context to an engine.
     */
    public static SimpleContextDefinition context(final String name, final BiConsumer<CalculatedSubject, Consumer<String>> valueAccumulator) {
        requireNonNull(name, "name");
        requireNonNull(valueAccumulator, "valueAccumulator");

        return new SimpleContextDefinition.FromFunction(name, valueAccumulator);
    }

    protected SimpleContextDefinition(final String name) {
        super(name);
    }

    @Override
    public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<String> consumer) {
    }

    @Override
    public boolean matches(final String ownVal, final String testVal) {
        return Objects.equals(ownVal, testVal);
    }

    @Override
    public String serialize(final String canonicalValue) {
        return canonicalValue;
    }

    @Override
    public String deserialize(final String userValue) {
        return userValue;
    }

    public static final class Fallback extends SimpleContextDefinition {
        public Fallback(String name) {
            super(name);
        }
    }

    static final class FromFunction extends SimpleContextDefinition {
        private final BiConsumer<CalculatedSubject, Consumer<String>> valueAccumulator;

        FromFunction(String name, final BiConsumer<CalculatedSubject, Consumer<String>> valueAccumulator) {
            super(name);
            this.valueAccumulator = valueAccumulator;
        }

        @Override
        public void accumulateCurrentValues(CalculatedSubject subject, Consumer<String> consumer) {
            this.valueAccumulator.accept(subject, consumer);
        }
    }
}

