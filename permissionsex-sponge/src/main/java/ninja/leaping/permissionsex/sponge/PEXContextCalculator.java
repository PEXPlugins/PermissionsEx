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
package ninja.leaping.permissionsex.sponge;

import com.google.common.collect.ImmutableSet;
import ninja.leaping.permissionsex.config.PermissionsExConfiguration;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.context.Context;
import org.spongepowered.api.service.permission.context.ContextCalculator;

import java.util.Set;

/**
 * Adds PEX-specific contexts
 */
class PEXContextCalculator implements ContextCalculator {
    public static final String SERVER_TAG_KEY = "server-tag";
    private Set<Context> serverTags = ImmutableSet.of();

    void update(PermissionsExConfiguration config) {
        final ImmutableSet.Builder<Context> serverTagsBuild = ImmutableSet.builder();
        for (String tag : config.getServerTags()) {
            serverTagsBuild.add(new Context(SERVER_TAG_KEY, tag));
        }
        this.serverTags = serverTagsBuild.build();
    }

    @Override
    public void accumulateContexts(Subject subject, Set<Context> accumulator) {
        accumulator.addAll(serverTags);
    }

    @Override
    public boolean matches(Context context, Subject subject) {
        return serverTags.contains(context);
    }
}
