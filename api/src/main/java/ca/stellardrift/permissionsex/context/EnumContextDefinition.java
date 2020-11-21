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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.pcollections.HashTreePSet;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;

public abstract class EnumContextDefinition<T extends Enum<T>> extends ContextDefinition<T> {
    private final Class<T> enumClass;
    private final Set<T> values;

    protected EnumContextDefinition(final String name, final Class<T> enumClass) {
        super(name);
        this.enumClass = enumClass;
        assert this.enumClass.isEnum(); // true because of type bound

        this.values = HashTreePSet.from(Arrays.asList(this.enumClass.getEnumConstants()));
    }

    @Override
    public final String serialize(final T canonicalValue) {
        return canonicalValue.name();
    }

    @Override
    public final @Nullable T deserialize(final String userValue) {
        try {
            return Enum.valueOf(enumClass, userValue.toUpperCase());
        } catch (final IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public boolean matches(final T ownVal, final T testVal) {
        return ownVal == testVal;
    }

    @Override
    public void accumulateCurrentValues(final CalculatedSubject subject, final Consumer<T> consumer) {
    }

    @Override
    public Set<T> suggestValues(final CalculatedSubject subject) {
        return this.values;
    }
}
