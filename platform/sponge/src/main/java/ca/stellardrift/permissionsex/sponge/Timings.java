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
package ca.stellardrift.permissionsex.sponge;

import co.aikar.timings.Timing;
import org.spongepowered.plugin.PluginContainer;

final class Timings {
    private final PluginContainer plugin;
    final OnlyIfSyncTiming getSubject;
    final OnlyIfSyncTiming getActiveContexts;
    final OnlyIfSyncTiming getPermission;
    final OnlyIfSyncTiming getOption;
    final OnlyIfSyncTiming getParents;

    Timings(final PluginContainer plugin) {
        this.plugin = plugin;
        this.getSubject = timing("getSubject");
        this.getActiveContexts = timing("getActiveContexts");
        this.getPermission = timing("getPermission");
        this.getOption = timing("getOption");
        this.getParents = timing("getParents");
    }

    private OnlyIfSyncTiming timing(final String key) {
        return new OnlyIfSyncTiming(co.aikar.timings.Timings.of(this.plugin, key));
    }

    static class OnlyIfSyncTiming implements AutoCloseable {
        private final Timing timing;

        OnlyIfSyncTiming(final Timing timing) {
            this.timing = timing;
        }

        public OnlyIfSyncTiming start() {
            this.timing.startTimingIfSync();
            return this;
        }

        @Override
        public void close() {
            this.timing.stopTimingIfSync();
        }

    }

}
