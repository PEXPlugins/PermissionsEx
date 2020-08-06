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

import java.util.stream.Collector;

/**
 * Additional collectors to complement {@link java.util.stream.Collectors} focusing on Guava collections
 */
public class GuavaCollectors {

    public static <T> Collector<T, ImmutableList.Builder<T>, ImmutableList<T>> toImmutableList() {
        return Collector.of(ImmutableList::builder, ImmutableList.Builder::add, (a, b) -> a.addAll(b.build()), ImmutableList.Builder::build);
    }

    public static <T> Collector<T, ImmutableSet.Builder<T>, ImmutableSet<T>> toImmutableSet() {
        return Collector.of(ImmutableSet::builder, ImmutableSet.Builder::add, (a, b) -> a.addAll(b.build()), ImmutableSet.Builder::build);
    }

}
