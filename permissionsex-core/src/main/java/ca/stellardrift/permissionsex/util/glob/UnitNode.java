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

package ca.stellardrift.permissionsex.util.glob;

import com.google.common.collect.Iterators;

import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

class UnitNode extends GlobNode {
    private final String value;

    public UnitNode(String value) {
        Objects.requireNonNull(value, "value");
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean matches(String input) {
        return value.equals(input);
    }

    @Override
    public boolean matchesIgnoreCase(String input) {
        return value.equalsIgnoreCase(input);
    }

    @Override
    public Iterator<String> iterator() {
        return Iterators.singletonIterator(value);
    }

    @Override
    public void forEach(Consumer<? super String> consumer) {
        consumer.accept(value);
    }

    @Override
    public Spliterator<String> spliterator() {
        return Spliterators.spliterator(new Object[] {value}, 1);
    }

    @Override
    public String toString() {
        return "literal(" + value + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnitNode)) return false;
        UnitNode strings = (UnitNode) o;
        return Objects.equals(value, strings.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
