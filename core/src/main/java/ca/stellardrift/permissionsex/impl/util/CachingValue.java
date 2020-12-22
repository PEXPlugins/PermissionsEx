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

import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class CachingValue<V> {
    private final LongSupplier currentTime;
    private final long maxDelta;
    private final Supplier<V> updater;
    private volatile long lastTime;
    private volatile V lastValue;

    /**
     * Create a value that is cached for a certain amount of time.
     */
    public static <V> CachingValue<V> timeBased(final long maxDelta, final Supplier<V> updateFunc) {
        return new CachingValue<>(System::currentTimeMillis, maxDelta, updateFunc);
    }

    public CachingValue(final LongSupplier currentTime, final long maxDelta, final Supplier<V> updater) {
        requireNonNull(currentTime, "currentTime");
        requireNonNull(updater, "updater");
        this.currentTime = currentTime;
        this.maxDelta = maxDelta;
        this.updater = updater;
        this.refresh();
    }

    public V get() {
        final long now = currentTime.getAsLong();
        if ((now - this.lastTime) > this.maxDelta) {
            this.lastValue = updater.get();
            this.lastTime = now;
        }
        return this.lastValue;
    }

    public void refresh() {
        this.lastValue = this.updater.get();
        this.lastTime = currentTime.getAsLong();
    }
}
