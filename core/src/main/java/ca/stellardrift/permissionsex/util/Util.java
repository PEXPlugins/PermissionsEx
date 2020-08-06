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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import ca.stellardrift.permissionsex.PermissionsEx;
import ninja.leaping.configurate.util.CheckedFunction;
import ninja.leaping.configurate.util.CheckedSupplier;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class Util {
    public static Map.Entry<String, String> subjectFromString(String input) {
        String[] entries = input.split(":", 2);
        if (entries.length == 1) {
            return Maps.immutableEntry(PermissionsEx.SUBJECTS_GROUP, entries[0]);
        } else {
            return Maps.immutableEntry(entries[0], entries[1]);
        }

    }

    public static String subjectToString(Map.Entry<String, String> input) {
        return input.getKey() + ":" + input.getValue();
    }

    /**
     * Given an {@link Optional} of an unknown type, safely cast it to the expected type.
     * If the optional is not of the required type, an empty optional is returned.
     *
     * @param input The input value
     * @param clazz The class to cast to
     * @param <T> The type of the class
     * @return A casted or empty Optional
     */
    public static <T> Optional<T> castOptional(Optional<?> input, Class<T> clazz) {
        return input.filter(clazz::isInstance).map(clazz::cast);
    }

    public static <T> CompletableFuture<T> failedFuture(Throwable error) {
        CompletableFuture<T> ret = new CompletableFuture<>();
        ret.completeExceptionally(error);
        return ret;
    }

    private static final CompletableFuture<Object> EMPTY_FUTURE = new CompletableFuture<>();
    static {
        EMPTY_FUTURE.complete(null);
    }

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> emptyFuture() {
        return (CompletableFuture) EMPTY_FUTURE;
    }

    public static <I, T> CompletableFuture<T> failableFuture(I value, CheckedFunction<I, T, ?> func) {
        return failableFuture(() -> func.apply(value));
    }

    public static <T> CompletableFuture<T> failableFuture(CheckedSupplier<T, ?> func) {
        CompletableFuture<T> ret = new CompletableFuture<>();
        try {
            ret.complete(func.get());
        } catch (Throwable e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    public static <T> CompletableFuture<T> asyncFailableFuture(CheckedSupplier<T, ?> supplier, Executor exec) {
        CompletableFuture<T> ret = new CompletableFuture<>();
        exec.execute(() -> {
            try {
                ret.complete(supplier.get());
            } catch (Throwable e) {
                ret.completeExceptionally(e);
            }

        });
        return ret;
    }

    public static <K, V> Map<K, V> updateImmutable(Map<K, V> input, K newKey, V newVal) {
        if (input == null) {
            return ImmutableMap.of(newKey, newVal);
        }
        Map<K, V> ret = new HashMap<>(input);
        if (newVal == null) {
            ret.remove(newKey);
        } else {
            ret.put(newKey, newVal);
        }
        return Collections.unmodifiableMap(ret);
    }

    public static <T> ImmutableList<T> appendImmutable(List<T> input, T entry) {
        if (input == null) {
            return ImmutableList.of(entry);
        }
        ImmutableList.Builder<T> ret = ImmutableList.builder();
        ret.addAll(input);
        ret.add(entry);
        return ret.build();
    }
}
