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
        TestWeight get = new TestWeight(15);
        WeightedImmutableSet<TestWeight> subject = WeightedImmutableSet.of(new TestWeight(5), new TestWeight(80), new TestWeight(34), new TestWeight(40), get);
        assertEquals(get, subject.get(15));

        assertEquals(null, subject.get(6));
        assertEquals(null, subject.get(-1));
    }

    @Test
    public void testMap() {
        WeightedImmutableSet<TestWeight> subject = WeightedImmutableSet.of(new TestWeight(5), new TestWeight(80), new TestWeight(34)),
        expected = WeightedImmutableSet.of(new TestWeight(6), new TestWeight(81), new TestWeight(34));

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
