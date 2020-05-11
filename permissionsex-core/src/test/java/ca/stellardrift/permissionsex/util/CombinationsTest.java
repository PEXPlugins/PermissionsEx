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

package ca.stellardrift.permissionsex.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


public class CombinationsTest {

    @SafeVarargs
    private static <T> ImmutableSet<T> set(T... items) {
        return ImmutableSet.copyOf(items);
    }

    @Test
    public void testInitialCombinations() {
        Set<Integer> combinations = ImmutableSet.of(1, 2, 3);
        List<Set<Integer>> expectedResults = ImmutableList.<Set<Integer>>of(set(1, 2, 3), set(1, 2), set(1, 3), set(2, 3), set(1), set(2), set(3), ImmutableSet.<Integer>of()), actualResults = new ArrayList<>(expectedResults.size());
        Iterators.addAll(actualResults, Combinations.of(combinations).iterator());
        assertIterableEquals(expectedResults, actualResults);
    }

    @Test
    public void testCombinationsOfEmpty() {
        Combinations<String> test = Combinations.of(ImmutableSet.<String>of());
        Iterator<Set<String>> testIt = test.iterator();
        assertTrue(testIt.hasNext());
        Set<String> emptyResult = testIt.next();
        assertIterableEquals(ImmutableSet.of(), emptyResult);
        assertFalse(testIt.hasNext());
    }

}
