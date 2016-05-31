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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.util.command.args.GenericArguments;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static ninja.leaping.permissionsex.util.Translations.t;
import static ninja.leaping.permissionsex.util.command.args.GameArguments.context;
import static ninja.leaping.permissionsex.util.command.args.GenericArguments.flags;
import static ninja.leaping.permissionsex.util.command.args.GenericArguments.integer;

public class Util {
    public static Map.Entry<String, String> contextFromString(String input) {
        String[] entries = input.split(":", 2);
        if (entries.length != 2) {
            throw new IllegalArgumentException("Input string must be of the format key:value, but was '" + input + "'");
        } else {
            return Maps.immutableEntry(entries[0], entries[1]);
        }

    }

    public static String contextToString(Map.Entry<String, String> input) {
        return input.getKey() + ":" + input.getValue();
    }

    public static GenericArguments.FlagCommandElementBuilder contextTransientFlags() {
        return flags()
                .flag("-transient")
                .flag("n", "-non-inheritable")
                .valueFlag(integer(t("priority")), "p", "-priority")
                .valueFlag(context(t("context")), "-context", "-contexts", "c");
    }

    public static <T> CompletableFuture<T> failedFuture(Throwable error) {
        CompletableFuture<T> ret = new CompletableFuture<>();
        ret.completeExceptionally(error);
        return ret;
    }

    @SuppressWarnings("unchecked")
    private static final CompletableFuture<Object> EMPTY_FUTURE = new CompletableFuture<>();
    static {
        EMPTY_FUTURE.complete(null);
    }

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> emptyFuture() {
        return (CompletableFuture) EMPTY_FUTURE;
    }

    public static <I, T> CompletableFuture<T> failableFuture(I value, ThrowingFunction<I, T, ?> func) {
        return failableFuture(() -> func.apply(value));
    }

    public static <T> CompletableFuture<T> failableFuture(ThrowingSupplier<T, ?> func) {
        CompletableFuture<T> ret = new CompletableFuture<>();
        try {
            ret.complete(func.supply());
        } catch (Exception e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    public static <T> CompletableFuture<T> asyncFailableFuture(ThrowingSupplier<T, ?> supplier, Executor exec) {
        CompletableFuture<T> ret = new CompletableFuture<>();
        exec.execute(() -> {
            try {
                ret.complete(supplier.supply());
            } catch (Exception e) {
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
