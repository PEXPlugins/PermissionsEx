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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableListIterator;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable list that is sorted based on weight. All elements are unique, and assumed to be equal if their weights are equal.
 *
 * An object's weight is assumed to be constant, and any changing of the weight will result in undefined behavior.
 */
public class WeightedImmutableSet<E extends Weighted> implements Iterable<E> {
    private final Object[] elements;

    private WeightedImmutableSet(Object... elements) {
        this.elements = elements;
    }

    private static class EmptyWeightedImmutableSet<E extends Weighted> extends WeightedImmutableSet<E> {
        @SuppressWarnings("unchecked")
        private static final WeightedImmutableSet EMPTY_SET = new EmptyWeightedImmutableSet<>();

        EmptyWeightedImmutableSet() {
            super();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public WeightedImmutableSet<E> with(E element) {
            return of(element);
        }

        @Override
        public WeightedImmutableSet<E> withAll(WeightedImmutableSet<E> elements) {
            return elements;
        }

        @Override
        public WeightedImmutableSet<E> without(E element) {
            return this;
        }

        @Override
        public WeightedImmutableSet<E> without(int weight) {
            return this;
        }

        @Override
        public <N extends E> WeightedImmutableSet<N> map(Function<E, N> func) {
            return of();
        }

        @Override
        public List<E> asList() {
            return ImmutableList.of();
        }

        @Override
        public Set<E> asSet() {
            return ImmutableSet.of();
        }

        @Override
        public Iterator<E> iterator() {
            return Iterators.emptyIterator();
        }

        @Override
        public void forEach(Consumer<? super E> action) {
            // no-op
        }

        @Override
        public Spliterator<E> spliterator() {
            return Spliterators.emptySpliterator();
        }
    }


    @SuppressWarnings("unchecked")
    public static <E extends Weighted> WeightedImmutableSet<E> of() {
        return EmptyWeightedImmutableSet.EMPTY_SET;
    }

    @SuppressWarnings("unchecked")
    public static <E extends Weighted> WeightedImmutableSet<E> copyOf(Iterable<? extends E> it) {
        if (it instanceof WeightedImmutableSet) {
            return (WeightedImmutableSet<E>) it;
        }
        E[] elements = (E[]) Iterables.toArray(it, Object.class);
        if (elements.length == 0) {
            return of();
        }
        return new WeightedImmutableSet<>(elements);
    }

    @SuppressWarnings("unchecked")
    public static <E extends Weighted> WeightedImmutableSet<E> of(E element) {
        return new WeightedImmutableSet<>(element);
    }

    @SuppressWarnings("unchecked")
    public static <E extends Weighted> WeightedImmutableSet<E> of(E... elements) {
        if (elements.length == 0) {
            return of();
        }

        Object[] newArr = Arrays.copyOf(elements, elements.length, Object[].class);
        Arrays.sort(newArr, (Comparator) Weighted.COMPARATOR);
        return new WeightedImmutableSet<>(newArr);
    }

    @SuppressWarnings("unchecked")
    public static <E extends Weighted> WeightedImmutableSet<E> ofStream(Stream<E> stream) {
        return new WeightedImmutableSet<E>(stream.sorted(Weighted.COMPARATOR).toArray());
    }

    public boolean isEmpty() {
        return this.elements.length == 0;
    }

    public E get(int weight) {
        int idx = indexOf(weight);
        if (idx == -1) {
            return null;
        }
        return getElement(idx);
    }

    @SuppressWarnings("unchecked")
    private E getElement(int index) {
        return (E) this.elements[index];
    }

    private int indexOf(int weight) {
        if (this.elements.length == 0) {
            return -1;
        }
        if (weight < getElement(0).getWeight() || weight > getElement(this.elements.length - 1).getWeight()) {
            return -1;
        }

        int min = 0, max = this.elements.length - 1, mid;
        do {
            mid = Math.floorDiv((max + min), 2);
            int midWeight = getElement(mid).getWeight();
            if (midWeight == weight) {
                return mid;
            } else if (midWeight > weight) { // We're in the bottom half
                max = mid;
            } else { // The element is located in the top half
                min = mid + 1;
            }
        } while (min != max);
        return -1;
    }

    @SuppressWarnings("unchecked")
    public WeightedImmutableSet<E> with(E element) {
        checkNotNull(element, "element");
        int weightIndex = indexOf(element.getWeight());
        Object[] newArr;
        if (weightIndex == -1) {
            newArr = Arrays.copyOf(elements, elements.length + 1);
            newArr[newArr.length - 1] = element;
            Arrays.sort(newArr, (Comparator) Weighted.COMPARATOR);
        } else {
            newArr = Arrays.copyOf(elements, elements.length);
            newArr[weightIndex] = element;
        }
        return new WeightedImmutableSet<>(newArr);
    }

    public WeightedImmutableSet<E> withAll(WeightedImmutableSet<E> elements) {
        checkNotNull(elements, "elements");
        Object[] newArr = ImmutableSet.builder().addAll(elements).addAll(this).build().toArray();
        /*Object[] newArr = new Object[this.elements.length + elements.elements.length];
        System.arraycopy(this.elements, 0, newArr, 0, this.elements.length);
        System.arraycopy(elements.elements, 0, newArr, this.elements.length, elements.elements.length);*/
        Arrays.sort(newArr, (Comparator) Weighted.COMPARATOR);
        return new WeightedImmutableSet<>(newArr);
    }

    public WeightedImmutableSet<E> without(E element) {
        return without(element.getWeight());
    }

    public WeightedImmutableSet<E> without(int weight) {
        int weightIdx = indexOf(weight);
        if (weightIdx == -1) {
            return this;
        }
        if (this.elements.length == 1) {
            return of();
        }

        Object[] newArr = new Object[this.elements.length - 1];
        System.arraycopy(this.elements, 0, newArr, 0, weightIdx);
        System.arraycopy(this.elements, weightIdx + 1, newArr, weightIdx, this.elements.length - weightIdx);
        return new WeightedImmutableSet<>(newArr);
    }

    public <N extends E> WeightedImmutableSet<N> map(Function<E, N> func) {
        Object[] newArr = new Object[this.elements.length];
        for (int i = 0; i < newArr.length; ++i) {
            newArr[i] = func.apply(getElement(i));
        }
        return new WeightedImmutableSet<>(newArr);
    }

    @SuppressWarnings("unchecked")
    public List<E> asList() {
        return (List) ImmutableList.copyOf(this.elements);
    }

    @SuppressWarnings("unchecked")
    public Set<E> asSet() {
        return (Set) ImmutableSet.copyOf(this.elements);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<E> iterator() {
        return (Iterator) Iterators.forArray(this.elements);
    }

    public Iterable<E> reverse() {
        return ReverseUnmodifiableArrayIterator::new;

    }

    private class ReverseUnmodifiableArrayIterator extends UnmodifiableListIterator<E> {
        private int currentIndex = -1;

        @Override
        public boolean hasPrevious() {
            return currentIndex - 1 >= 0;
        }

        private int getRelative(int mod) {
            if (currentIndex + mod < 0 || currentIndex + mod >= elements.length) {
                throw new NoSuchElementException();
            }
            return elements.length - 1 - (currentIndex += mod);
        }

        @Override
        public E previous() {
            return getElement(getRelative(-1));
        }

        @Override
        public int nextIndex() {
            return currentIndex + 1 >= elements.length ? elements.length : (currentIndex + 1);
        }

        @Override
        public int previousIndex() {
            return currentIndex - 1 < 0 ? elements.length : (currentIndex - 1);
        }

        @Override
        public boolean hasNext() {
            return currentIndex + 1 < elements.length;
        }

        @Override
        public E next() {
            return getElement(getRelative(1));
        }
    }

    @SuppressWarnings("unchecked")
    public Stream<E> stream() {
        return (Stream) Stream.of(elements);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super E> action) {
        for (Object el : elements) {
            action.accept((E) el);
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(elements, Spliterator.IMMUTABLE | Spliterator.ORDERED | Spliterator.SORTED);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WeightedImmutableSet)) return false;
        WeightedImmutableSet<?> that = (WeightedImmutableSet<?>) o;
        return Arrays.equals(elements, that.elements);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(elements) + 7;
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("elements", Arrays.toString(elements))
                .toString();
    }
}
