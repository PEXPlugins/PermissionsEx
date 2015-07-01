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

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.PeekingIterator;

import java.util.Iterator;
import java.util.Set;

/**
 * An iterable class that provides all combinations of any of a given set of values
 * The algorithm used is a version of Algorithm T in section 7.2.1.3 of Knuth's The Art of Computer Programming,
 * modified to function as an Iterator. This implementation also begins with the original set,
 * and accepts the empty set as an input (unlike the original algorithm, only defined for n&gt;0 where n is the length of the set).
 */
public class Combinations<T> implements Iterable<Set<T>> {
    private final T[] items;

    @SuppressWarnings("unchecked")
    private Combinations(Set<T> items) {
        this.items = (T[]) items.toArray();
    }

    public static <T> Combinations<T> of(Set<T> items) {
        return new Combinations<>(ImmutableSet.copyOf(items));
    }

    private class CombinationIterator extends AbstractIterator<Set<T>> implements PeekingIterator<Set<T>> {
        private int j;
        private int currentLength = items.length;
        private int[] c; // Reuse the same array -- it isn't getting any bigger

        private void init()  {
            for (j = 0; j < currentLength; ++j) {
                c[j] = j;
            }
            c[currentLength] = items.length;
            c[currentLength + 1] = 0;
            j = currentLength - 1;
        }

        @Override
        protected Set<T> computeNext() {
            if (currentLength < 0) {
                return endOfData();
            }

            if (c == null) {
                c = new int[currentLength + 2];
                init();
            } else {
                if (c[0] + 1 < c[1]) {
                    c[0] = c[0] + 1;
                } else {
                    int x;
                    j = 1;
                    do {
                        ++j;
                        c[j - 2] = j - 2;
                        x = c[j - 1] + 1;
                    } while (x == c[j]);

                    if (j > currentLength) {
                        currentLength--;
                        init();
                    } else {
                        c[j - 1] = x;
                        j--;
                    }
                }
            }

            if (currentLength == 0) {
                --currentLength;
                return ImmutableSet.of();
            } else {
                ImmutableSet.Builder<T> build = ImmutableSet.builder();
                for (int i = 0; i < currentLength; ++i) {
                    build.add(items[c[i]]);
                }
                return build.build();
            }
        }
    }

    @Override
    public Iterator<Set<T>> iterator() {
        return new CombinationIterator();
    }
}
