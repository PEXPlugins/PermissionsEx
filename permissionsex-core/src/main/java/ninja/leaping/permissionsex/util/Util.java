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

import com.google.common.collect.Maps;
import ninja.leaping.permissionsex.PermissionsEx;
import ninja.leaping.permissionsex.util.command.args.GenericArguments;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static ninja.leaping.permissionsex.util.Translations.t;
import static ninja.leaping.permissionsex.util.command.args.GameArguments.context;
import static ninja.leaping.permissionsex.util.command.args.GenericArguments.flags;

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

    public static GenericArguments.FlagCommandElementBuilder contextTransientFlags() {
        return flags()
                .flag("-transient")
                .valueFlag(context(t("context")), "-context", "-contexts", "c");
    }

    public static <T> CompletableFuture<T> failedFuture(Throwable error) {
        CompletableFuture<T> ret = new CompletableFuture<>();
        ret.completeExceptionally(error);
        return ret;
    }

    public static <I, T> CompletableFuture<T> failableFuture(I value, ThrowingFunction<I, T> func) {
        return failableFuture(() -> func.apply(value));
    }

    public static <T> CompletableFuture<T> failableFuture(ThrowingSupplier<T> func) {
        CompletableFuture<T> ret = new CompletableFuture<>();
        try {
            ret.complete(func.supply());
        } catch (Exception e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    public static <T> CompletableFuture<T> asyncFailableFuture(ThrowingSupplier<T> supplier, Executor exec) {
        CompletableFuture<T> ret = new CompletableFuture<>();
        exec.execute((Runnable & CompletableFuture.AsynchronousCompletionTask) () -> {
            try {
                ret.complete(supplier.supply());
            } catch (Exception e) {
                ret.completeExceptionally(e);
            }

        });
        return ret;
    }
}
