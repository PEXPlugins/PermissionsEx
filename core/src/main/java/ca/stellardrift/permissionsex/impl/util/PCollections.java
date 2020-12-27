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
package ca.stellardrift.permissionsex.impl.util;

import org.pcollections.ConsPStack;
import org.pcollections.HashTreePMap;
import org.pcollections.HashTreePSet;
import org.pcollections.PBag;
import org.pcollections.PMap;
import org.pcollections.PSet;
import org.pcollections.PStack;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;

/**
 * Utilities for working with <a href="https://github.com/hrldcpr/pcollections">persistent collections</a>.
 *
 * <p>These are in three categories: converting from JDK collections to persistent ones,
 * Stream helpers, and shorthand factory methods.</p>
 */
public final class PCollections {

    private PCollections() {
    }

    // Conversions

    public static <K, V> PMap<K, V> asMap(final Map<K, V> map) {
        if (map instanceof PMap<?, ?>) {
            return (PMap<K, V>) map;
        } else {
            return HashTreePMap.from(map);
        }
    }

    public static <KI, VI, KO, VO> PMap<KO, VO> asMap(final Map<KI, VI> map, final BiFunction<KI, VI, KO> keyMapper, final BiFunction<KI, VI, VO> valueMapper) {
        PMap<KO, VO> out = map();
        for (final Map.Entry<KI, VI> entry : map.entrySet()) {
            out = out.plus(
                    keyMapper.apply(entry.getKey(), entry.getValue()),
                    valueMapper.apply(entry.getKey(), entry.getValue())
            );
        }
        return out;
    }

    // TODO: transforming methods for PMap?

    public static <E> PSet<E> asSet(final Collection<E> set) {
        if (set instanceof PSet<?>) {
            return (PSet<E>) set;
        } else {
            return HashTreePSet.from(set);
        }
    }

    public static <I, O> PSet<O> asSet(final Collection<I> list, final Function<? super I, ? extends O> xform) {
        PSet<O> out = HashTreePSet.empty();
        for (final I ent : list) {
            out = out.plus(xform.apply(ent));
        }
        return out;
    }

    public static <E> PVector<E> asVector(final Collection<E> list) {
        if (list instanceof PVector<?>) {
            return (PVector<E>) list;
        } else {
            return TreePVector.from(list);
        }
    }

    public static <I, O> PVector<O> asVector(final Collection<I> list, final Function<? super I, ? extends O> xform) {
        PVector<O> out = TreePVector.empty();
        for (final I ent : list) {
            out = out.plus(xform.apply(ent));
        }
        return out;
    }

    public static <E> PStack<E> asStack(final Collection<E> stack) {
        if (stack instanceof PStack<?>) {
            return (PStack<E>) stack;
        } else {
            return ConsPStack.from(stack);
        }
    }

    public static <I, O> PStack<O> asStack(final Collection<I> list, final Function<? super I, ? extends O> xform) {
        PStack<O> out = ConsPStack.empty();
        for (final I ent : list) {
            out = out.plus(xform.apply(ent));
        }
        return out;
    }

    // Collectors

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <K, V> Collector<Map.Entry<K, V>, ?, PMap<K, V>> toPMap() {
        return Collector.<Map.Entry<K, V>, PMap<K, V>[], PMap<K, V>>of(
                () -> new PMap[] { HashTreePMap.empty() },
                (arr, v) -> arr[0] = arr[0].plus(v.getKey(), v.getValue()),
                (a, b) -> new PMap[] {a[0].plusAll(b[0])},
                a -> a[0]
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E> Collector<E, ?, PSet<E>> toPSet() {
        return Collector.<E, PSet<E>[], PSet<E>>of(
                () -> new PSet[] { HashTreePSet.empty() },
                (arr, v) -> arr[0] = arr[0].plus(v),
                (a, b) -> new PSet[] {a[0].plusAll(b[0])},
                a -> a[0]
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E> Collector<E, ?, PVector<E>> toPVector() {
        return Collector.<E, PVector<E>[], PVector<E>>of(
                () -> new PVector[] { TreePVector.empty() },
                (arr, v) -> arr[0] = arr[0].plus(v),
                (a, b) -> new PVector[] {a[0].plusAll(b[0])},
                a -> a[0]
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E> Collector<E, ?, PStack<E>> toPStack() {
        return Collector.<E, PStack<E>[], PStack<E>>of(
                () -> new PStack[] { ConsPStack.empty() },
                (arr, v) -> arr[0] = arr[0].plus(v),
                (a, b) -> new PStack[] {a[0].plusAll(b[0])},
                a -> a[0]
        );
    }
    
    // narrowing -- valid because we have immutable collections
    
    @SuppressWarnings("unchecked")
    public static <K1 extends K2, V1 extends V2, K2, V2> PMap<K2, V2> narrow(final PMap<K1, V1> input) {
        return (PMap<K2, V2>) input;
    }
    
    @SuppressWarnings("unchecked")
    public static <E1 extends E2, E2> PSet<E2> narrow(final PSet<E1> input) {
        return (PSet<E2>) input;
    }

    @SuppressWarnings("unchecked")
    public static <E1 extends E2, E2> PVector<E2> narrow(final PVector<E1> input) {
        return (PVector<E2>) input;
    }

    @SuppressWarnings("unchecked")
    public static <E1 extends E2, E2> PStack<E2> narrow(final PStack<E1> input) {
        return (PStack<E2>) input;
    }

    @SuppressWarnings("unchecked")
    public static <E1 extends E2, E2> PBag<E2> narrow(final PBag<E1> input) {
        return (PBag<E2>) input;
    }

    // factory methods //

    public static <K, V> PMap<K, V> map() {
        return HashTreePMap.empty();
    }

    public static <K, V> PMap<K, V> map(final K key, final V value) {
        return HashTreePMap.singleton(key, value);
    }

    public static <E> PSet<E> set() {
        return HashTreePSet.empty();
    }

    public static <E> PSet<E> set(final E element) {
        return HashTreePSet.singleton(element);
    }

    public static <E> PSet<E> set(final E... elements) {
        PSet<E> out = set();
        for (final E element : elements) {
            out = out.plus(element);
        }
        return out;
    }

    public static <E> PVector<E> vector() {
        return TreePVector.empty();
    }

    public static <E> PVector<E> vector(final E element) {
        return TreePVector.singleton(element);
    }

    public static <E> PVector<E> vector(final E... elements) {
        PVector<E> out = vector();
        for (final E element : elements) {
            out = out.plus(element);
        }
        return out;
    }

    public static <E> PStack<E> stack() {
        return ConsPStack.empty();
    }

    public static <E> PStack<E> stack(final E element) {
        return ConsPStack.singleton(element);
    }

    public static <E> PStack<E> stack(final E... elements) {
        PStack<E> out = stack();
        for (final E element : elements) {
            out = out.plus(element);
        }
        return out;
    }
}
