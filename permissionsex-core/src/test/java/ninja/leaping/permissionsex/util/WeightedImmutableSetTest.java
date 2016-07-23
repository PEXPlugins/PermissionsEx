/**
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
package ninja.leaping.permissionsex.util;

import org.junit.Test;

import java.util.Iterator;
import java.util.Objects;

import static org.junit.Assert.*;

/**
 * Tests for {@link WeightedImmutableSet}
 */
public class WeightedImmutableSetTest {
    private static class TestWeight implements Weighted {
        private final int weight;

        private TestWeight(int weight) {
            this.weight = weight;
        }

        @Override
        public int getWeight() {
            return this.weight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TestWeight)) return false;
            TestWeight that = (TestWeight) o;
            return weight == that.weight;
        }

        @Override
        public int hashCode() {
            return Objects.hash(weight);
        }

        @Override
        public String toString() {
            return "@" + this.weight;
        }
    }

    @Test
    public void testCreate() {
        WeightedImmutableSet<TestWeight> subject = WeightedImmutableSet.of(new TestWeight(5));
        assertFalse(subject.isEmpty());
        assertEquals(5, subject.iterator().next().getWeight());
        subject = WeightedImmutableSet.of(new TestWeight(5), new TestWeight(3));
        assertEquals(3, subject.iterator().next().getWeight());
    }

    @Test
    public void testAdding() {
        WeightedImmutableSet<TestWeight> subject = WeightedImmutableSet.of(new TestWeight(5));
        Iterator<TestWeight> it = subject.iterator();
        it.next();
        assertFalse(it.hasNext());

        subject = subject.with(new TestWeight(2));
        it = subject.iterator();
        assertEquals(2, it.next().getWeight());
        it.next();
        assertFalse(it.hasNext());
    }

    @Test
    public void testGet() {
        TestWeight get15 = new TestWeight(15), get40 = new TestWeight(40);
        WeightedImmutableSet<TestWeight> subject = WeightedImmutableSet.of(new TestWeight(5), new TestWeight(80), new TestWeight(34), get40, get15);
        assertEquals(get15, subject.get(15));
        assertEquals(get40, subject.get(40));

        assertEquals(null, subject.get(6));
        assertEquals(null, subject.get(-1));
    }

    @Test
    public void testMap() {
        WeightedImmutableSet<TestWeight> subject = WeightedImmutableSet.of(new TestWeight(5), new TestWeight(80), new TestWeight(34)),
        expected = WeightedImmutableSet.of(new TestWeight(6), new TestWeight(81), new TestWeight(35));

        WeightedImmutableSet<TestWeight> newSet = subject.map(w -> new TestWeight(w.getWeight() + 1));
        assertEquals(expected, newSet);
    }

    @Test
    public void testWithAll() {
        WeightedImmutableSet<TestWeight> subject = WeightedImmutableSet.of(new TestWeight(5), new TestWeight(80), new TestWeight(34)),
                two = WeightedImmutableSet.of(new TestWeight(6), new TestWeight(48), new TestWeight(280)),
                expected = WeightedImmutableSet.of(new TestWeight(5), new TestWeight(80), new TestWeight(34), new TestWeight(6), new TestWeight(48), new TestWeight(280));

        assertEquals(expected, subject.withAll(two));
    }

    @Test
    public void testWithAllContainsNoDuplicates() {
        WeightedImmutableSet<TestWeight> subject = WeightedImmutableSet.of(new TestWeight(5), new TestWeight(80), new TestWeight(34)),
                two = WeightedImmutableSet.of(new TestWeight(6), new TestWeight(80), new TestWeight(280)),
                expected = WeightedImmutableSet.of(new TestWeight(5), new TestWeight(80), new TestWeight(34), new TestWeight(6), new TestWeight(280));

        assertEquals(expected, subject.withAll(two));
    }
}
